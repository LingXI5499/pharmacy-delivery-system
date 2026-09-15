package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequestMapping("/api/warehouse/inventory") @RequiredArgsConstructor
public class InventoryQueryController {
    private final MedicineBatchMapper batches; private final InventoryLedgerMapper ledger; private final InventoryService inventory;
    @GetMapping("/batches") @PreAuthorize("@permissionService.has('inventory.read')") public ApiResponse<List<MedicineBatch>> batches(@RequestParam(required=false)Long medicineId,@RequestParam(required=false)Integer expiringDays){LambdaQueryWrapper<MedicineBatch> q=new LambdaQueryWrapper<MedicineBatch>().eq(medicineId!=null,MedicineBatch::getMedicineId,medicineId).le(expiringDays!=null,MedicineBatch::getExpiryDate,LocalDate.now().plusDays(expiringDays==null?0:expiringDays)).orderByAsc(MedicineBatch::getExpiryDate).orderByAsc(MedicineBatch::getId);return ApiResponse.success(batches.selectList(q));}
    @GetMapping("/ledger") @PreAuthorize("@permissionService.has('inventory.read')") public ApiResponse<List<InventoryLedger>> ledger(@RequestParam(required=false)Long batchId,@RequestParam(required=false)String businessId){return ApiResponse.success(ledger.selectList(new LambdaQueryWrapper<InventoryLedger>().eq(batchId!=null,InventoryLedger::getBatchId,batchId).eq(businessId!=null&&!businessId.isBlank(),InventoryLedger::getBusinessId,businessId).orderByDesc(InventoryLedger::getCreateTime).last("LIMIT 500")));}
    @PatchMapping("/batches/{batchId}/stock") @PreAuthorize("@permissionService.has('inventory.write')") public ApiResponse<Void> adjust(@PathVariable Long batchId,@Valid @RequestBody BatchAdjustRequest r){inventory.adjustBatch(batchId,r.adjustType(),r.quantity(),r.reason(),CurrentUser.id());return ApiResponse.success(null);}
}
