package com.pharmacy.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InventoryCountRequests {
    private InventoryCountRequests() {
    }

    public record CreateCount(@Size(max = 255) String remark) {
    }

    public record UpsertItem(
            @NotNull Long batchId,
            @NotNull @Min(0) Integer countedQty,
            @NotBlank @Size(max = 255) String reason
    ) {
    }
}
