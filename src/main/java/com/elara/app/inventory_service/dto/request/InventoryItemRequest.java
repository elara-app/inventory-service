package com.elara.app.inventory_service.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record InventoryItemRequest(

    @NotBlank
    @Size(max = 100)
    String name,

    @Size(max = 200)
    String description,

    @NotNull
    @Positive
    Long baseUnitOfMeasureId,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    BigDecimal standardCost,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    BigDecimal unitPerPurchaseUom,

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    BigDecimal reorderPointQuantity

) {
}
