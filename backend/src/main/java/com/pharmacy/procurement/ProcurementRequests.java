package com.pharmacy.procurement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class ProcurementRequests { private ProcurementRequests() {} }

record SupplierRequest(@NotBlank @Size(max=32) String supplierCode,
                       @NotBlank @Size(max=100) String supplierName,
                       @Size(max=50) String contactName,@Size(max=20) String phone) {}
record PurchaseItemRequest(@NotNull Long medicineId,@NotNull @Positive Integer quantity,
                           @NotNull @DecimalMin("0.01") BigDecimal purchasePrice) {}
record PurchaseCreateRequest(@NotNull Long supplierId,@NotEmpty List<@Valid PurchaseItemRequest> items,
                             @Size(max=255) String remark) {}
record ReceiptItemRequest(@NotNull Long purchaseOrderItemId,@NotBlank @Size(max=64) String batchNo,
                          @NotNull LocalDate productionDate,@NotNull LocalDate expiryDate,
                          @NotNull @PositiveOrZero Integer qualifiedQty,
                          @NotNull @PositiveOrZero Integer rejectedQty) {
    @AssertTrue(message="生产日期必须早于失效日期") boolean validDates(){return productionDate!=null&&expiryDate!=null&&productionDate.isBefore(expiryDate);}
    @AssertTrue(message="合格数和拒收数之和必须大于零") boolean hasQuantity(){return qualifiedQty!=null&&rejectedQty!=null&&qualifiedQty+rejectedQty>0;}
}
record ReceiptRequest(@NotNull Long purchaseOrderId,@NotEmpty List<@Valid ReceiptItemRequest> items,
                      @Size(max=255) String remark) {}
