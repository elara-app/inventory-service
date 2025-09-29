package com.elara.app.inventory_service.service.imp;

import com.elara.app.inventory_service.dto.response.UomResponse;
import com.elara.app.inventory_service.exceptions.ResourceNotFoundException;
import com.elara.app.inventory_service.utils.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class UomServiceClient {

    private static final String NOMENCLATURE = "Uom-service";
    private static final String ENTITY_NAME = "Uom";
    private final RestTemplate restTemplate;
    private final String uomServiceBaseUrl = "http://localhost:8080/api/v1/uom/";
    private final MessageService messageService;

    public UomServiceClient(RestTemplate restTemplate, MessageService messageService) {
        this.restTemplate = restTemplate;
        this.messageService = messageService;
    }

    public void verifyUomById(Long id) {
        try {
            log.debug("[" + NOMENCLATURE + "-findById] Searching {} with id: {}", ENTITY_NAME, id);
            restTemplate.getForObject(uomServiceBaseUrl + id, UomResponse.class);
            log.debug("[Uom-service-findById] {}", messageService.getMessage("crud.read.success", ENTITY_NAME));
        } catch (HttpClientErrorException.NotFound e) {
            String msg = messageService.getMessage("crud.not.found", "UOM", "id", id.toString());
            log.warn("[" + NOMENCLATURE + "-findById] {}", msg);
            throw new ResourceNotFoundException(new Object[]{"id", id.toString()});
        }
    }

}
