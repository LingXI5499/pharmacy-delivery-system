package com.pharmacy.procurement;

import com.pharmacy.common.ApiResponse;
import com.pharmacy.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProcurementController {
    private final ProcurementService service;

    @GetMapping("/api/purchaser/suppliers")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<List<Supplier>> suppliers() {
        return ApiResponse.success(service.suppliers());
    }

    @PostMapping("/api/purchaser/suppliers")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<Supplier> supplier(@Valid @RequestBody SupplierRequest request) {
        return ApiResponse.success(service.createSupplier(request));
    }

    @PostMapping("/api/purchaser/purchase-orders")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<PurchaseOrder> create(@Valid @RequestBody PurchaseCreateRequest request) {
        return ApiResponse.success(service.create(CurrentUser.id(), request));
    }

    @GetMapping("/api/purchaser/purchase-orders")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<List<PurchaseOrderDetail>> purchaserOrders(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(status));
    }

    @GetMapping("/api/purchaser/purchase-orders/{id}")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<PurchaseOrderDetail> purchaserDetail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @GetMapping("/api/admin/purchase-orders")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<List<PurchaseOrderDetail>> adminOrders(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(status));
    }

    @GetMapping("/api/admin/purchase-orders/{id}")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<PurchaseOrderDetail> adminDetail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping("/api/admin/purchase-orders/{id}/approve")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        service.approve(CurrentUser.id(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/api/admin/purchase-orders/{id}/reject")
    @PreAuthorize("@permissionService.has('procurement.write')")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        service.reject(CurrentUser.id(), id, request.reason());
        return ApiResponse.success(null);
    }

    @GetMapping("/api/warehouse/purchase-orders")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<List<PurchaseOrderDetail>> warehouseOrders() {
        return ApiResponse.success(service.listReceivable());
    }

    @GetMapping("/api/warehouse/purchase-orders/{id}")
    @PreAuthorize("@permissionService.has('inventory.read')")
    public ApiResponse<PurchaseOrderDetail> warehouseDetail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    @PostMapping("/api/warehouse/purchase-receipts")
    @PreAuthorize("@permissionService.has('inventory.write')")
    public ApiResponse<PurchaseReceipt> receive(@Valid @RequestBody ReceiptRequest request) {
        return ApiResponse.success(service.receive(CurrentUser.id(), request));
    }

    public record RejectRequest(@NotBlank @Size(max = 255) String reason) {
    }
}
