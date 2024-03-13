package com.Demoone.BDI.controller;

import com.Demoone.BDI.service.JsonSchemaValidationService;
import com.Demoone.BDI.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final RedisService redisService;
    private final JsonSchemaValidationService validationService;

    // Assuming you have an ObjectMapper bean defined elsewhere
    private final ObjectMapper objectMapper;

    public PlanController(RedisService redisService, JsonSchemaValidationService validationService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.validationService = validationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody String planJson) {
        try {
            Set<ValidationMessage> errors = validationService.validateJsonAgainstSchema(planJson, "plan-schema.json");
            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(errors);
            }
            JsonNode planNode = objectMapper.readTree(planJson);
            String eTag = redisService.savePlanAndGetETag(planNode);
            return ResponseEntity.ok().eTag(eTag).body("Plan saved with ID: " + planNode.get("objectId").asText());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable String id, @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        try {
            String eTag = redisService.getETagById(id);
            if (eTag != null && eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            JsonNode planNode = redisService.getPlanById(id);
            if (planNode != null) {
                return ResponseEntity.ok().eTag(eTag).body(planNode.toString());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving plan: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlanById(@PathVariable String id) {
        try {
            redisService.deletePlanById(id);
            return ResponseEntity.ok().body("Plan deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting plan: " + e.getMessage());
        }
    }
}
