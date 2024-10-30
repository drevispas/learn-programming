package org.demo.todo.controller;

import org.demo.todo.exception.TodoException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalErrorHandler {

    // IllegalArgumentException handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResponse.badRequest(e.getMessage());
    }

    // IllegalStateException handler
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        return ApiResponse.internalServerError(e.getMessage());
    }

    @ExceptionHandler(TodoException.TodoNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTodoNotFoundException(TodoException.TodoNotFoundException e) {
        return ApiResponse.notFound(e.getMessage());
    }

    @ExceptionHandler(TodoException.TodoAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleTodoAlreadyExistsException(TodoException.TodoAlreadyExistsException e) {
        return ApiResponse.badRequest(e.getMessage());
    }

    @ExceptionHandler(TodoException.TodoValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleTodoValidationException(TodoException.TodoValidationException e) {
        return ApiResponse.badRequest(e.getMessage());
    }
}
