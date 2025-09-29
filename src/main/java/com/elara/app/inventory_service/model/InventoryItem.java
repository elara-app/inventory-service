package com.elara.app.inventory_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity(name = "inventory_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    @Column(name = "id", updatable = false)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @NotNull
    @Positive
    @Column(name = "base_unit_of_measure_id", nullable = false)
    private Long baseUnitOfMeasureId; // Relationship with Uom service

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "standard_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardCost;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "unit_per_purchase_uom", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPerPurchaseUom;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "reorder_point_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderPointQuantity;

}
