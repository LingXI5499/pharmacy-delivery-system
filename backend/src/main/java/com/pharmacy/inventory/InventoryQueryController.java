package com.pharmacy.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pharmacy.common.ApiResponse;
import com.pharmacy.common.ErrorCode;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/warehouse/inventory")
@RequiredArgsConstructor
public class InventoryQueryController {
    private static final Set<Integer> NEAR_EXPIRY_WINDOWS = Set.of(30, 60, 90);

    private final MedicineBatchMapper batches;
    private final InventoryLedgerMapper ledger;
    private final InventoryService inventory;
    private final InventoryCountService countService;

    @GetMapping("/batches")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<List<MedicineBatch>> batches(@RequestParam(required = false) Long medicineId,
                                                    @RequestParam(required = false) Integer expiringDays) {
        LambdaQueryWrapper<MedicineBatch> q = new LambdaQueryWrapper<MedicineBatch>()
                .eq(medicineId != null, MedicineBatch::getMedicineId, medicineId)
                .orderByAsc(MedicineBatch::getExpiryDate)
                .orderByAsc(MedicineBatch::getId);
        if (expiringDays != null) {
            if (!NEAR_EXPIRY_WINDOWS.contains(expiringDays)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "近效期仅支持 30/60/90 天");
            }
            LocalDate today = LocalDate.now();
            q.eq(MedicineBatch::getSellable, 1)
                    .eq(MedicineBatch::getQualityStatus, "QUALIFIED")
                    .gt(MedicineBatch::getExpiryDate, today)
                    .le(MedicineBatch::getExpiryDate, today.plusDays(expiringDays));
        }
        return ApiResponse.success(batches.selectList(q));
    }

    @GetMapping("/ledger")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<List<InventoryLedger>> ledger(@RequestParam(required = false) Long batchId,
                                                     @RequestParam(required = false) String businessId) {
        return ApiResponse.success(ledger.selectList(new LambdaQueryWrapper<InventoryLedger>()
                .eq(batchId != null, InventoryLedger::getBatchId, batchId)
                .eq(businessId != null && !businessId.isBlank(), InventoryLedger::getBusinessId, businessId)
                .orderByDesc(InventoryLedger::getCreateTime)
                .last("LIMIT 500")));
    }

    @PatchMapping("/batches/{batchId}/stock")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<Void> adjust(@PathVariable Long batchId, @Valid @RequestBody BatchAdjustRequest r) {
        inventory.adjustBatch(batchId, r.adjustType(), r.quantity(), r.reason(), CurrentUser.id());
        return ApiResponse.success(null);
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<List<Map<String, Object>>> reconciliation() {
        return ApiResponse.success(countService.reconciliation());
    }

    @GetMapping("/counts")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<List<InventoryCount>> counts(@RequestParam(required = false) String status) {
        return ApiResponse.success(countService.list(status));
    }

    @GetMapping("/counts/{id}")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<Map<String, Object>> countDetail(@PathVariable Long id) {
        return ApiResponse.success(countService.detail(id));
    }

    @PostMapping("/counts")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<InventoryCount> createCount(@RequestBody(required = false) InventoryCountRequests.CreateCount body) {
        return ApiResponse.success(countService.create(CurrentUser.id(), body == null ? null : body.remark()));
    }

    @PostMapping("/counts/{id}/start")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<Void> startCount(@PathVariable Long id) {
        countService.startCounting(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/counts/{id}/items")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<InventoryCountItem> upsertItem(@PathVariable Long id,
                                                      @Valid @RequestBody InventoryCountRequests.UpsertItem body) {
        return ApiResponse.success(countService.upsertItem(id, body.batchId(), body.countedQty(), body.reason(), CurrentUser.id()));
    }

    @PostMapping("/counts/{id}/complete")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<Void> completeCount(@PathVariable Long id) {
        countService.complete(id, CurrentUser.id());
        return ApiResponse.success(null);
    }

    @PostMapping("/counts/{id}/cancel")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<Void> cancelCount(@PathVariable Long id) {
        countService.cancel(id);
        return ApiResponse.success(null);
    }
}
