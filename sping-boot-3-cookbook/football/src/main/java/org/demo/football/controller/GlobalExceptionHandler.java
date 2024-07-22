package org.demo.football.controller;

import org.demo.football.exceptions.AlreadyExistsException;
import org.demo.football.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response<?>> handleNotFoundException(NotFoundException e) {
        return new ResponseEntity<>(
                Response.builder()
                        .error(new ResponseError(e, HttpStatus.NOT_FOUND))
                        .build(),
                HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Player already exists")
    @ExceptionHandler(AlreadyExistsException.class)
    public void handleAlreadyExistsException() {
    }
}
