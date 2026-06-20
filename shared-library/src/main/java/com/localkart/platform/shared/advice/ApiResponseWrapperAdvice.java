package com.localkart.platform.shared.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.shared.annotation.IgnoreResponseWrapping;
import com.localkart.platform.shared.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip wrapping if the method or class is annotated with IgnoreResponseWrapping
        if (returnType.hasMethodAnnotation(IgnoreResponseWrapping.class) ||
            Objects.requireNonNull(returnType.getDeclaringClass()).isAnnotationPresent(IgnoreResponseWrapping.class)) {
            return false;
        }

        // Avoid wrapping Actuator, Swagger / OpenAPI documentation endpoints
        String className = returnType.getDeclaringClass().getName();
        if (className.contains("org.springdoc") || 
            className.contains("swagger") || 
            className.contains("org.springframework.boot.actuate")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // If it's already wrapped in ApiResponse, do nothing
        if (body instanceof ApiResponse) {
            return body;
        }

        ApiResponse<Object> wrappedResponse = ApiResponse.success(body);

        // String response requires manual serialization to prevent ClassCastException in StringHttpMessageConverter
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(wrappedResponse);
            } catch (JsonProcessingException e) {
                log.error("Failed to wrap String response in ApiResponse", e);
                return "{\"success\":false,\"message\":\"Serialization Error\",\"errorCode\":\"SERIALIZATION_ERROR\"}";
            }
        }

        return wrappedResponse;
    }
}
