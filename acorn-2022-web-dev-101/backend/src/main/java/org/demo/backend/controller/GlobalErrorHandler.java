package org.demo.backend.controller;

import org.demo.backend.exception.TodoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalErrorHandler {

    // IllegalArgumentException handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<TodoResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(TodoResponse.<String>builder().error(e.getMessage()).build());
    }

    // IllegalStateException handler
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<TodoResponse<String>> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity.internalServerError().body(TodoResponse.<String>builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(TodoException.TodoNotFoundException.class)
    public ResponseEntity<TodoResponse<String>> handleTodoNotFoundException(TodoException.TodoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(TodoResponse.<String>builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(TodoException.TodoAlreadyExistsException.class)
    public ResponseEntity<TodoResponse<String>> handleTodoAlreadyExistsException(TodoException.TodoAlreadyExistsException e) {
        return ResponseEntity.badRequest().body(TodoResponse.<String>builder().error(e.getMessage()).build());
    }

    @ExceptionHandler(TodoException.TodoValidationException.class)
    public ResponseEntity<TodoResponse<String>> handleTodoValidationException(TodoException.TodoValidationException e) {
        return ResponseEntity.internalServerError().body(TodoResponse.<String>builder().error(e.getMessage()).build());
    }
}
