package com.pharmacy.prescription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.entity.*;
import com.pharmacy.enums.OperatorType;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.messaging.OrderPaymentPendingEvent;
import com.pharmacy.mapper.OrderStatusLogMapper;
import com.pharmacy.mapper.PharmacyOrderItemMapper;
import com.pharmacy.mapper.PharmacyOrderMapper;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class PrescriptionService {
    private static final long MAX_SIZE=10L*1024*1024;
    private static final Set<String> ALLOWED=Set.of("application/pdf","image/jpeg","image/png");
    private final PrescriptionMapper mapper; private final PrescriptionItemMapper itemMapper;
    private final PharmacyOrderMapper orderMapper; private final PharmacyOrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper logMapper; private final InventoryService inventoryService;
    private final ApplicationEventPublisher events;
    @Value("${app.prescription.storage-path:./data/prescriptions}") private String storagePath;

    @Transactional public Prescription upload(Long userId,MultipartFile file,List<Long> medicineIds,List<Integer> quantities){
        if(file==null||file.isEmpty()||file.getSize()>MAX_SIZE)throw new BusinessException(ErrorCode.PARAM_INVALID,"处方文件不能为空且不得超过 10 MB");
        if(medicineIds==null||quantities==null||medicineIds.size()!=quantities.size()||medicineIds.isEmpty())throw new BusinessException(ErrorCode.PARAM_INVALID,"处方药品明细不合法");
        if(quantities.stream().anyMatch(q->q==null||q<=0))throw new BusinessException(ErrorCode.PARAM_INVALID,"处方数量必须大于零");
        try(InputStream in=file.getInputStream()){
            String detected=new Tika().detect(in,file.getOriginalFilename());
            if(!ALLOWED.contains(detected))throw new BusinessException(ErrorCode.PARAM_INVALID,"只允许真实内容为 PDF、JPEG 或 PNG 的文件");
            byte[] bytes=file.getBytes();String key=UUID.randomUUID()+extension(detected);
            Path root=Paths.get(storagePath).toAbsolutePath().normalize();Files.createDirectories(root);Path target=root.resolve(key).normalize();
            if(!target.startsWith(root))throw new BusinessException(ErrorCode.PARAM_INVALID,"非法文件路径");
            Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(target);}catch(IOException ignored){}}});
            Prescription p=new Prescription();p.setPrescriptionNo("RX"+System.currentTimeMillis());p.setUserId(userId);p.setStorageKey(key);p.setOriginalFilename(safeName(file.getOriginalFilename()));p.setContentType(detected);p.setSizeBytes(file.getSize());p.setSha256(hex(MessageDigest.getInstance("SHA-256").digest(bytes)));p.setStatus("PENDING_REVIEW");p.setCreateTime(LocalDateTime.now());p.setUpdateTime(LocalDateTime.now());mapper.insert(p);
            for(int i=0;i<medicineIds.size();i++){PrescriptionItem item=new PrescriptionItem();item.setPrescriptionId(p.getId());item.setMedicineId(medicineIds.get(i));item.setPrescribedQty(quantities.get(i));itemMapper.insert(item);}return p;
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(ErrorCode.SYSTEM_ERROR,"处方文件保存失败");}
    }

    public void validateForOrder(Long prescriptionId,Long userId,List<PharmacyOrderItem> orderItems){
        Prescription p=mapper.selectById(prescriptionId);if(p==null||!p.getUserId().equals(userId))throw new BusinessException(ErrorCode.NOT_FOUND,"处方不存在");
        if(p.getOrderId()!=null||!"PENDING_REVIEW".equals(p.getStatus()))throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"处方已被使用或已审核");
        Map<Long,Integer> allowed=new HashMap<>();for(PrescriptionItem i:itemMapper.selectList(new LambdaQueryWrapper<PrescriptionItem>().eq(PrescriptionItem::getPrescriptionId,prescriptionId)))allowed.put(i.getMedicineId(),i.getPrescribedQty());
        for(PharmacyOrderItem item:orderItems)if(!allowed.containsKey(item.getMedicineId())||allowed.get(item.getMedicineId())<item.getQuantity())throw new BusinessException(ErrorCode.PARAM_INVALID,"订单药品或数量超出处方范围");
    }

    @Transactional public void attach(Long prescriptionId,Long orderId){mapper.update(null,new LambdaUpdateWrapper<Prescription>().eq(Prescription::getId,prescriptionId).isNull(Prescription::getOrderId).set(Prescription::getOrderId,orderId));}

    @Transactional public void review(Long reviewerId,Long id,boolean approved,String reason){
        Prescription p=mapper.lockById(id);if(p==null)throw new BusinessException(ErrorCode.NOT_FOUND,"处方不存在");if(!"PENDING_REVIEW".equals(p.getStatus())||p.getOrderId()==null)throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"处方当前不可审核");
        PharmacyOrder order=orderMapper.selectById(p.getOrderId());if(order==null||order.getOrderStatus()!=OrderStatus.PENDING_REVIEW)throw new BusinessException(ErrorCode.ORDER_STATUS_CONFLICT,"订单当前不可审核");
        OrderStatus target;
        if(approved){List<PharmacyOrderItem> items=orderItemMapper.selectList(new LambdaQueryWrapper<PharmacyOrderItem>().eq(PharmacyOrderItem::getOrderId,order.getId()));if(inventoryService.canReserve(items)){LocalDateTime deadline=LocalDateTime.now().plusMinutes(30);inventoryService.reserve(order.getId(),items,deadline,reviewerId);order.setPaymentDeadline(deadline);target=OrderStatus.PENDING_PAYMENT;}else{target=OrderStatus.CLOSED_STOCK_SHORTAGE;reason="处方审核通过，但可售批次库存不足";}}
        else {if(reason==null||reason.isBlank())throw new BusinessException(ErrorCode.PARAM_INVALID,"驳回时必须填写原因");target=OrderStatus.REVIEW_REJECTED;}
        p.setStatus(approved?"APPROVED":"REJECTED");p.setReviewerId(reviewerId);p.setReviewReason(reason);p.setReviewedTime(LocalDateTime.now());p.setUpdateTime(LocalDateTime.now());mapper.updateById(p);
        order.setOrderStatus(target);order.setUpdateTime(LocalDateTime.now());orderMapper.updateById(order);addLog(order.getId(),target,reviewerId,reason);if(target==OrderStatus.PENDING_PAYMENT)events.publishEvent(new OrderPaymentPendingEvent(order.getId(),order.getOrderNo(),order.getPaymentDeadline()));
    }

    public Prescription getAuthorized(Long id,Long requesterId,boolean privileged){Prescription p=mapper.selectById(id);if(p==null||(!privileged&&!p.getUserId().equals(requesterId)))throw new BusinessException(ErrorCode.NOT_FOUND,"处方不存在");return p;}
    public Resource resource(Prescription p){Path root=Paths.get(storagePath).toAbsolutePath().normalize();Path path=root.resolve(p.getStorageKey()).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new BusinessException(ErrorCode.NOT_FOUND,"处方文件不存在");return new FileSystemResource(path);}
    public List<Prescription> pending(){return mapper.selectList(new LambdaQueryWrapper<Prescription>().eq(Prescription::getStatus,"PENDING_REVIEW").isNotNull(Prescription::getOrderId).orderByAsc(Prescription::getCreateTime));}
    private void addLog(Long orderId,OrderStatus target,Long actor,String reason){OrderStatusLog log=new OrderStatusLog();log.setOrderId(orderId);log.setBeforeStatus(OrderStatus.PENDING_REVIEW);log.setAfterStatus(target);log.setOperatorType(OperatorType.ADMIN);log.setOperatorId(actor);log.setRemark(reason);log.setCreateTime(LocalDateTime.now());logMapper.insert(log);}
    private static String extension(String type){return switch(type){case "application/pdf"->".pdf";case "image/jpeg"->".jpg";default->".png";};}
    private static String safeName(String name){if(name==null)return "prescription";return Paths.get(name).getFileName().toString().replaceAll("[\\r\\n]","");}
    private static String hex(byte[] b){return java.util.HexFormat.of().formatHex(b);}
}
