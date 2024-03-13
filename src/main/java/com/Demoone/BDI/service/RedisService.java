package com.Demoone.BDI.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public String savePlanAndGetETag(JsonNode planNode) throws NoSuchAlgorithmException, JsonProcessingException {
        String id = planNode.get("objectId").asText();
        String json = planNode.toString();
        
        // Generate ETag
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(json.getBytes());
        String eTag = bytesToHex(hash);
        
        // Combine plan JSON and ETag into a single JSON object
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode combinedNode = mapper.createObjectNode();
        combinedNode.put("data", json);
        combinedNode.put("eTag", eTag);

        // Save combined JSON in Redis
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(id, combinedNode.toString());

        return eTag;
    }


    public String getETagById(String id) throws JsonProcessingException {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String combinedJson = ops.get(id);
        if (combinedJson == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode combinedNode = mapper.readTree(combinedJson);
        return combinedNode.get("eTag").asText();
    }
    
    public JsonNode getPlanById(String id) throws JsonProcessingException {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String combinedJson = ops.get(id);
        if (combinedJson == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode combinedNode = mapper.readTree(combinedJson);
        return combinedNode.get("data");
    }
    
    public void deletePlanById(String id) {
        redisTemplate.delete(id);
    }
    

    // Helper method to convert byte array to hex string
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}