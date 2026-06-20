package com.localkart.platform.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    VALIDATION_ERROR("VAL-400", "Input validation failed", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("REQ-400", "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("AUTH-401", "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH-403", "Access forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND("RES-404", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("RES-409", "Resource conflict", HttpStatus.CONFLICT),
    BUSINESS_ERROR("BUS-422", "Business rule violation", HttpStatus.UNPROCESSABLE_ENTITY),
    INTERNAL_SERVER_ERROR("SYS-500", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
