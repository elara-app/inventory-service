package com.elara.app.inventory_service.mapper;

import com.elara.app.inventory_service.dto.request.InventoryItemRequest;
import com.elara.app.inventory_service.dto.response.InventoryItemResponse;
import com.elara.app.inventory_service.dto.update.InventoryItemUpdate;
import com.elara.app.inventory_service.model.InventoryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface InventoryItemMapper {

    InventoryItem toEntity(InventoryItemRequest request);

    InventoryItemResponse toResponse(InventoryItem entity);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(@MappingTarget InventoryItem existing, InventoryItemUpdate update);

}
