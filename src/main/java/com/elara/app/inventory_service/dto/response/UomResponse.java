package com.elara.app.inventory_service.dto.response;

import java.math.BigDecimal;

public record UomResponse(

    Long id,
    String name,
    String description,
    BigDecimal conversionFactorToBase,
    Long uomStatusId

) {
}
