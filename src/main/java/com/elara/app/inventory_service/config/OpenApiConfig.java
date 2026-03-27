package com.elara.app.inventory_service.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public OpenAPI customOpenAPI() throws IOException {
        Map<String, Example> examples = loadExamples();
        
        return new OpenAPI()
            .info(new Info()
                .title("Inventory Service API")
                .version("1.0.0")
                .description("""
                        RESTful API for managing Inventory Items in the Elara platform.
                        
                        ## Features
                        - **CRUD Operations**: Create, read, update, and delete Inventory Item records
                        - **Pagination & Sorting**: Browse large datasets with configurable page size and sorting
                        - **Search**: Find items by name using case-insensitive partial matching
                        - **Validation**: Name uniqueness check and comprehensive input validation
                        - **Cost Management**: Track standard costs and reorder points for inventory items
                        - **UOM Integration**: Associate items with base units of measure
                        
                        ## Resource Paths
                        - `/item/**` for Inventory Item operations
                        
                        ## Response Format
                        All responses use JSON format with consistent error handling and HTTP status codes.
                        """)
                .contact(new Contact()
                    .name("Elara Development Team")
                    .url("https://github.com/elara-app")
                    .email("dev@elara-app.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/license/mit")))
            .components(new Components()
                .addResponses("BadRequest", createErrorResponse("Bad Request - Invalid input data, validation errors, or missing required parameters"))
                .addResponses("NotFound", createErrorResponse("Not Found - The requested resource does not exist"))
                .addResponses("Conflict", createErrorResponse("Conflict - Resource already exists or operation conflicts with existing data"))
                .addResponses("InternalServerError", createErrorResponse("Internal Server Error - Unexpected error occurred on the server"))
                .addSchemas("ErrorResponse", createErrorResponseSchema())
                .addSchemas("InventoryItemResponse", createInventoryItemResponseSchema())
                .addSchemas("InventoryItemRequest", createInventoryItemRequestSchema())
                .addSchemas("InventoryItemUpdate", createInventoryItemUpdateSchema())
                .addSchemas("InventoryItemPageResponse", createInventoryItemPageResponseSchema())
                .addExamples("InventoryItemCreated", examples.get("inventory-item-created"))
                .addExamples("InventoryItemUpdated", examples.get("inventory-item-updated"))
                .addExamples("InventoryItemPage", examples.get("inventory-item-page"))
                .addExamples("ErrorBadRequestInventoryItem", examples.get("error-bad-request-inventory-item"))
                .addExamples("ErrorInventoryItemNotFound", examples.get("error-inventory-item-not-found"))
                .addExamples("ErrorInventoryItemConflict", examples.get("error-inventory-item-conflict"))
                .addExamples("ErrorBadRequest", examples.get("error-bad-request"))
                .addExamples("ErrorNotFound", examples.get("error-not-found"))
                .addExamples("ErrorConflict", examples.get("error-conflict"))
                .addExamples("ErrorDeleteConflict", examples.get("error-delete-conflict"))
                .addExamples("ErrorServer", examples.get("error-server")));
    }

    private Map<String, Example> loadExamples() throws IOException {
        Map<String, Example> examples = new HashMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        
        String[] exampleFiles = {
            "inventory-item-created.json",
            "inventory-item-updated.json",
            "inventory-item-page.json",
            "error-bad-request-inventory-item.json",
            "error-inventory-item-not-found.json",
            "error-inventory-item-conflict.json",
            "error-bad-request.json",
            "error-not-found.json",
            "error-conflict.json",
            "error-delete-conflict.json",
            "error-server.json"
        };
        
        for (String fileName : exampleFiles) {
            Resource resource = resolver.getResource("classpath:examples/" + fileName);
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes());
                Map<String, Object> jsonMap = objectMapper.readValue(content, new TypeReference<>() {});
                Example example = new Example();
                example.setSummary(fileName.replace(".json", "").replace("-", " "));
                example.setValue(jsonMap);
                String key = fileName.replace(".json", "");
                examples.put(key, example);
            }
        }
        
        return examples;
    }

    private ApiResponse createErrorResponse(String description) {
        return new ApiResponse()
            .description(description)
            .content(new Content()
                .addMediaType("application/json", new MediaType()
                    .schema(new Schema<>()
                        .$ref("#/components/schemas/ErrorResponse"))));
    }

    private Schema<?> createErrorResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.type("object");
        schema.addProperty("code", new Schema<>().type("integer").description("Error code").example(1002));
        schema.addProperty("value", new Schema<>().type("string").description("Error code name").example("INVALID_DATA"));
        schema.addProperty("message", new Schema<>().type("string").description("Descriptive error message").example("Validation failed: Name must not be blank"));
        schema.addProperty("timestamp", new Schema<>().type("string").format("date-time").description("Error timestamp").example("2025-08-06T10:30:15"));
        schema.addProperty("path", new Schema<>().type("string").description("Request path").example("/item"));
        schema.addRequiredItem("code");
        schema.addRequiredItem("value");
        schema.addRequiredItem("message");
        schema.addRequiredItem("timestamp");
        schema.addRequiredItem("path");
        return schema;
    }

    private Schema<?> createInventoryItemResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.type("object");
        schema.description("Inventory Item response object");
        schema.addProperty("id", new Schema<>().type("integer").format("int64").description("Unique identifier").example(1));
        schema.addProperty("name", new Schema<>().type("string").description("Item name (unique, max 100 chars)").example("Steel Bolt M10"));
        schema.addProperty("description", new Schema<>().type("string").description("Item description (max 200 chars)").example("High-strength steel bolt, metric M10 x 50mm"));
        schema.addProperty("baseUnitOfMeasureId", new Schema<>().type("integer").format("int64").description("Associated base UOM identifier").example(1));
        schema.addProperty("standardCost", new Schema<>().type("number").description("Standard cost per unit").example(2.50));
        schema.addProperty("unitPerPurchaseUom", new Schema<>().type("number").description("Units per purchase UOM").example(100.00));
        schema.addProperty("reorderPointQuantity", new Schema<>().type("number").description("Reorder point quantity").example(500.00));
        schema.addRequiredItem("id");
        schema.addRequiredItem("name");
        schema.addRequiredItem("baseUnitOfMeasureId");
        schema.addRequiredItem("standardCost");
        schema.addRequiredItem("unitPerPurchaseUom");
        schema.addRequiredItem("reorderPointQuantity");
        return schema;
    }

    private Schema<?> createInventoryItemRequestSchema() {
        Schema<?> schema = new Schema<>();
        schema.type("object");
        schema.description("Inventory Item creation request");
        schema.addProperty("name", new Schema<>().type("string").description("Item name (max 100 chars, required, must be unique)").example("Steel Bolt M10"));
        schema.addProperty("description", new Schema<>().type("string").description("Item description (max 200 chars, optional)").example("High-strength steel bolt, metric M10 x 50mm"));
        schema.addProperty("baseUnitOfMeasureId", new Schema<>().type("integer").format("int64").description("Base UOM identifier (required, must exist)").example(1));
        schema.addProperty("standardCost", new Schema<>().type("number").description("Standard cost per unit (required, positive)").example(2.50));
        schema.addProperty("unitPerPurchaseUom", new Schema<>().type("number").description("Units per purchase UOM (required, positive)").example(100.00));
        schema.addProperty("reorderPointQuantity", new Schema<>().type("number").description("Reorder point quantity (required, positive)").example(500.00));
        schema.addRequiredItem("name");
        schema.addRequiredItem("baseUnitOfMeasureId");
        schema.addRequiredItem("standardCost");
        schema.addRequiredItem("unitPerPurchaseUom");
        schema.addRequiredItem("reorderPointQuantity");
        return schema;
    }

    private Schema<?> createInventoryItemUpdateSchema() {
        Schema<?> schema = new Schema<>();
        schema.type("object");
        schema.description("Inventory Item update request");
        schema.addProperty("name", new Schema<>().type("string").description("Item name (max 100 chars, required)").example("Steel Bolt M12"));
        schema.addProperty("description", new Schema<>().type("string").description("Item description (max 200 chars, optional)").example("High-strength steel bolt, metric M12 x 50mm"));
        schema.addProperty("baseUnitOfMeasureId", new Schema<>().type("integer").format("int64").description("Base UOM identifier (required)").example(1));
        schema.addProperty("standardCost", new Schema<>().type("number").description("Standard cost per unit (required, positive)").example(3.00));
        schema.addProperty("unitPerPurchaseUom", new Schema<>().type("number").description("Units per purchase UOM (required, positive)").example(100.00));
        schema.addProperty("reorderPointQuantity", new Schema<>().type("number").description("Reorder point quantity (required, positive)").example(400.00));
        schema.addRequiredItem("name");
        schema.addRequiredItem("baseUnitOfMeasureId");
        schema.addRequiredItem("standardCost");
        schema.addRequiredItem("unitPerPurchaseUom");
        schema.addRequiredItem("reorderPointQuantity");
        return schema;
    }

    private Schema<?> createInventoryItemPageResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.type("object");
        schema.description("Paginated response for Inventory Item resources");
        schema.addProperty("content", new ArraySchema()
            .items(new Schema<>().$ref("#/components/schemas/InventoryItemResponse"))
            .description("Page content"));
        schema.addProperty("totalElements", new Schema<>().type("integer").format("int64").example(10));
        schema.addProperty("totalPages", new Schema<>().type("integer").example(1));
        schema.addProperty("number", new Schema<>().type("integer").example(0));
        schema.addProperty("size", new Schema<>().type("integer").example(20));
        schema.addProperty("numberOfElements", new Schema<>().type("integer").example(10));
        schema.addProperty("first", new Schema<>().type("boolean").example(true));
        schema.addProperty("last", new Schema<>().type("boolean").example(true));
        schema.addProperty("empty", new Schema<>().type("boolean").example(false));
        return schema;
    }
}
