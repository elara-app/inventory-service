package com.elara.app.inventory_service.dto.response;

import java.math.BigDecimal;

public record InventoryItemResponse(

    Long id,
    String name,
    String description,
    Long baseUnitOfMeasureId,
    BigDecimal standardCost,
    BigDecimal unitPerPurchaseUom,
    BigDecimal reorderPointQuantity

) {
}
