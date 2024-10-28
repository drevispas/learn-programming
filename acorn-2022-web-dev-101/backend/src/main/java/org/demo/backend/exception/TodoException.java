package org.demo.backend.exception;

// sealed interface of custom exceptions containing sub exceptions
public sealed interface TodoException
        permits TodoException.TodoNotFoundException, TodoException.TodoAlreadyExistsException, TodoException.TodoValidationException {
    // Exception class for when a todo is not found
    final class TodoNotFoundException extends RuntimeException implements TodoException {
        public TodoNotFoundException(String message) {
            super(message);
        }
    }

    // Exception class for when a todo already exists
    final class TodoAlreadyExistsException extends RuntimeException implements TodoException {
        public TodoAlreadyExistsException(String message) {
            super(message);
        }
    }

    // Exception class for when a todo is invalid
    final class TodoValidationException extends RuntimeException implements TodoException {
        public TodoValidationException(String message) {
            super(message);
        }
    }
}
