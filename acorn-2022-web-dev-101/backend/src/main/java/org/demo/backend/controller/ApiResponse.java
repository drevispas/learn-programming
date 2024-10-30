package org.demo.backend.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Response entity class containing error and data fields. Data field is a generic type.
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String error;
    private T data;
    private HttpStatus status;

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.<T>builder()
                .data(data)
                .status(status)
                .build());
    }

    public static ResponseEntity<ApiResponse<Void>> error(String error, HttpStatus status) {
        return ResponseEntity.status(status).body(ApiResponse.<Void>builder()
                .error(error)
                .status(status)
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return success(data, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return success(data, HttpStatus.CREATED);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    public static ResponseEntity<ApiResponse<Void>> notFound(String message) {
        return error(message, HttpStatus.NOT_FOUND);
    }

    public static ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return error(message, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    public static ResponseEntity<Void> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    public static ResponseEntity<ApiResponse<Void>> conflict(String message) {
        return error(message, HttpStatus.CONFLICT);
    }

    public static ResponseEntity<ApiResponse<Void>> internalServerError(String message) {
        return error(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ResponseEntity<ApiResponse<Void>> serviceUnavailable(String message) {
        return error(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
