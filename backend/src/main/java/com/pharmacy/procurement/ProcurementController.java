package com.pharmacy.procurement;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequiredArgsConstructor
public class ProcurementController {
    private final ProcurementService service;
    @GetMapping("/api/purchaser/suppliers") @PreAuthorize("@permissionService.has('procurement.write')") public ApiResponse<List<Supplier>> suppliers(){return ApiResponse.success(service.suppliers());}
    @PostMapping("/api/purchaser/suppliers") @PreAuthorize("@permissionService.has('procurement.write')") public ApiResponse<Supplier> supplier(@Valid @RequestBody SupplierRequest r){return ApiResponse.success(service.createSupplier(r));}
    @PostMapping("/api/purchaser/purchase-orders") @PreAuthorize("@permissionService.has('procurement.write')") public ApiResponse<PurchaseOrder> create(@Valid @RequestBody PurchaseCreateRequest r){return ApiResponse.success(service.create(CurrentUser.id(),r));}
    @PostMapping("/api/admin/purchase-orders/{id}/approve") @PreAuthorize("@permissionService.has('procurement.write')") public ApiResponse<Void> approve(@PathVariable Long id){service.approve(CurrentUser.id(),id);return ApiResponse.success(null);}
    @PostMapping("/api/warehouse/purchase-receipts") @PreAuthorize("@permissionService.has('inventory.write')") public ApiResponse<PurchaseReceipt> receive(@Valid @RequestBody ReceiptRequest r){return ApiResponse.success(service.receive(CurrentUser.id(),r));}
}
