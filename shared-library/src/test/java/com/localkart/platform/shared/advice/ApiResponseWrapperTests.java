package com.localkart.platform.shared.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.shared.model.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseWrapperTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testApiResponseSuccessEnvelope() throws Exception {
        String testData = "LocalKart Data Payload";
        ApiResponse<String> apiResponse = ApiResponse.success(testData, "Payload retrieved successfully");

        assertTrue(apiResponse.isSuccess());
        assertEquals("Payload retrieved successfully", apiResponse.getMessage());
        assertEquals(testData, apiResponse.getData());
        assertNull(apiResponse.getErrorCode());
        assertTrue(apiResponse.getTimestamp() <= System.currentTimeMillis());

        String serializedJson = objectMapper.writeValueAsString(apiResponse);
        assertTrue(serializedJson.contains("\"success\":true"));
        assertTrue(serializedJson.contains("\"data\":\"LocalKart Data Payload\""));
        assertTrue(serializedJson.contains("\"message\":\"Payload retrieved successfully\""));
    }

    @Test
    void testApiResponseErrorEnvelope() throws Exception {
        ApiResponse<Void> apiResponse = ApiResponse.error("ERR-CODE-500", "Database connection lost");

        assertFalse(apiResponse.isSuccess());
        assertEquals("Database connection lost", apiResponse.getMessage());
        assertEquals("ERR-CODE-500", apiResponse.getErrorCode());
        assertNull(apiResponse.getData());

        String serializedJson = objectMapper.writeValueAsString(apiResponse);
        assertTrue(serializedJson.contains("\"success\":false"));
        assertTrue(serializedJson.contains("\"errorCode\":\"ERR-CODE-500\""));
        assertTrue(serializedJson.contains("\"message\":\"Database connection lost\""));
    }
}
