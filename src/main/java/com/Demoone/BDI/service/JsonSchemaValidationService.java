package com.Demoone.BDI.service;

import com.networknt.schema.JsonSchemaFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.SpecVersion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;

@Service
public class JsonSchemaValidationService {

    public Set<ValidationMessage> validateJsonAgainstSchema(String jsonPayload, String schemaPath) throws Exception {
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        
        try (InputStream schemaStream = new ClassPathResource(schemaPath).getInputStream()) {
            JsonSchema schema = schemaFactory.getSchema(schemaStream);
            return schema.validate(new ObjectMapper().readTree(jsonPayload));
        }
    }
}

