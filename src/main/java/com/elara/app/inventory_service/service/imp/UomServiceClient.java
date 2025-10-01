package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.response.UomResponse;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.utils.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class UomServiceClient {

    private static final String NOMENCLATURE = "Uom-service";
    private static final String ENTITY_NAME = "Uom";
    private final RestTemplate restTemplate;
    private final MessageService messageService;

    //    New variables
    @Value("${uom.service.name:unit-of-measure-service}")
    private String uomServiceName;

    public UomServiceClient(RestTemplate restTemplate, MessageService messageService) {
        this.restTemplate = restTemplate;
        this.messageService = messageService;
    }

    public void verifyUomById(Long id) {
        final String methodNomenclature = NOMENCLATURE + "-existsById";
        final String url = "http://" + uomServiceName + "/api/v1/uom/{id}";
        try {
            log.debug("[{}] Searching {} with id: {}", methodNomenclature, ENTITY_NAME, id);
            restTemplate.getForObject(url, UomResponse.class, id);
            String msg = messageService.getMessage("crud.read.success", ENTITY_NAME);
            log.debug("[{}] {}", methodNomenclature, msg);
        } catch (HttpClientErrorException.NotFound e) {
            String msg = messageService.getMessage("crud.not.found", "UOM", "id", id.toString());
            log.warn("[{}] {}", methodNomenclature, msg);
            throw new ResourceNotFoundException(new Object[]{ENTITY_NAME, "id", id.toString()});
        }
    }

}
