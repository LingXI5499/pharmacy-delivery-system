
package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.entity.*;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.mapper.*;
import com.pharmacy.service.DashboardService;
import com.pharmacy.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final PharmacyOrderMapper orderMapper;
    private final PharmacyOrderItemMapper itemMapper;
    private final MedicineMapper medicineMapper;
    private final MedicineCategoryMapper categoryMapper;

    @Override
    public DashboardSummaryVO summary() {
        LocalDateTime start=LocalDate.now().atStartOfDay();
        List<PharmacyOrder> today=orderMapper.selectList(new LambdaQueryWrapper<PharmacyOrder>().ge(PharmacyOrder::getCreateTime,start));
        BigDecimal sales=today.stream().filter(o->o.getOrderStatus()==OrderStatus.COMPLETED).map(PharmacyOrder::getOrderAmount).reduce(BigDecimal.ZERO,BigDecimal::add);
        return new DashboardSummaryVO(today.size(),sales,
                countStatus(today,OrderStatus.PENDING_REVIEW)+countStatus(today,OrderStatus.PENDING_PAYMENT)+countStatus(today,OrderStatus.PENDING_ACCEPT)+countStatus(today,OrderStatus.TO_PACK)+countStatus(today,OrderStatus.TO_DISPATCH)+countStatus(today,OrderStatus.DELIVERING)+countStatus(today,OrderStatus.REFUNDING),
                medicineMapper.selectCount(new LambdaQueryWrapper<Medicine>().gt(Medicine::getStock,0).apply("stock <= warning_stock")),
                countStatus(today,OrderStatus.PENDING_REVIEW)+countStatus(today,OrderStatus.PENDING_PAYMENT)+countStatus(today,OrderStatus.PENDING_ACCEPT),countStatus(today,OrderStatus.TO_PACK),countStatus(today,OrderStatus.TO_DISPATCH),countStatus(today,OrderStatus.DELIVERING),countStatus(today,OrderStatus.COMPLETED));
    }
    @Override public List<TrendPointVO> orderTrend(int days){return trend(days,false);}
    @Override public List<TrendPointVO> salesTrend(int days){return trend(days,true);}
    private List<TrendPointVO> trend(int days,boolean completedOnly){days=Math.max(1,Math.min(days,30));LocalDate from=LocalDate.now().minusDays(days-1);List<PharmacyOrder> orders=orderMapper.selectList(new LambdaQueryWrapper<PharmacyOrder>().ge(PharmacyOrder::getCreateTime,from.atStartOfDay()).eq(completedOnly,PharmacyOrder::getOrderStatus,OrderStatus.COMPLETED));Map<LocalDate,List<PharmacyOrder>> group=orders.stream().collect(Collectors.groupingBy(o->o.getCreateTime().toLocalDate()));List<TrendPointVO> result=new ArrayList<>();for(int i=0;i<days;i++){LocalDate d=from.plusDays(i);List<PharmacyOrder> list=group.getOrDefault(d,List.of());BigDecimal amount=list.stream().map(PharmacyOrder::getOrderAmount).reduce(BigDecimal.ZERO,BigDecimal::add);result.add(new TrendPointVO(d.format(DateTimeFormatter.ofPattern("MM-dd")),(long)list.size(),amount));}return result;}
    @Override public List<CategoryDistributionVO> categoryDistribution(int days){
        LocalDateTime start=LocalDate.now().minusDays(Math.max(1,Math.min(days,30))-1L).atStartOfDay();
        List<PharmacyOrder> orders=orderMapper.selectList(new LambdaQueryWrapper<PharmacyOrder>().eq(PharmacyOrder::getOrderStatus,OrderStatus.COMPLETED).ge(PharmacyOrder::getCreateTime,start));
        if(orders.isEmpty())return List.of();
        List<PharmacyOrderItem> items=itemMapper.selectList(new LambdaQueryWrapper<PharmacyOrderItem>().in(PharmacyOrderItem::getOrderId,orders.stream().map(PharmacyOrder::getId).toList()));
        Map<Long,Medicine> medicines=medicineMapper.selectBatchIds(items.stream().map(PharmacyOrderItem::getMedicineId).filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(Medicine::getId,Function.identity()));
        Map<Long,MedicineCategory> categories=categoryMapper.selectBatchIds(medicines.values().stream().map(Medicine::getCategoryId).distinct().toList()).stream().collect(Collectors.toMap(MedicineCategory::getId,Function.identity()));
        record Agg(BigDecimal amount,long qty){};Map<String,Agg> group=new HashMap<>();for(PharmacyOrderItem i:items){Medicine m=medicines.get(i.getMedicineId());String name=m==null?"历史药品":Optional.ofNullable(categories.get(m.getCategoryId())).map(MedicineCategory::getCategoryName).orElse("未分类");Agg a=group.getOrDefault(name,new Agg(BigDecimal.ZERO,0));group.put(name,new Agg(a.amount.add(i.getSubtotalAmount()),a.qty+i.getQuantity()));}return group.entrySet().stream().map(e->new CategoryDistributionVO(e.getKey(),e.getValue().amount,e.getValue().qty)).sorted(Comparator.comparing(CategoryDistributionVO::amount).reversed()).toList();
    }
    @Override public List<HotMedicineVO> hotMedicines(int limit){limit=Math.max(1,Math.min(limit,10));List<PharmacyOrder> orders=orderMapper.selectList(new LambdaQueryWrapper<PharmacyOrder>().eq(PharmacyOrder::getOrderStatus,OrderStatus.COMPLETED));if(orders.isEmpty())return List.of();List<PharmacyOrderItem> items=itemMapper.selectList(new LambdaQueryWrapper<PharmacyOrderItem>().in(PharmacyOrderItem::getOrderId,orders.stream().map(PharmacyOrder::getId).toList()));record Hot(String name,String image,long qty){};Map<String,Hot> group=new HashMap<>();for(PharmacyOrderItem i:items){Hot h=group.getOrDefault(i.getMedicineName(),new Hot(i.getMedicineName(),i.getMedicineImage(),0));group.put(i.getMedicineName(),new Hot(h.name,h.image,h.qty+i.getQuantity()));}return group.values().stream().sorted(Comparator.comparing(Hot::qty).reversed()).limit(limit).map(h->new HotMedicineVO(h.name,h.image,h.qty)).toList();}
    private long countStatus(List<PharmacyOrder> orders,OrderStatus status){return orders.stream().filter(o->o.getOrderStatus()==status).count();}
}
