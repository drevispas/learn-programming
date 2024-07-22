package org.demo.football.controller;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Arrays;

@Getter
public class ResponseError {
    private final String phrase;
    private final String message;
    private final String status;
    private final LocalDateTime timestamp;
    private final String trace;

    public ResponseError(RuntimeException e, HttpStatus httpStatus) {
        this.phrase = httpStatus.getReasonPhrase();
        this.message = e.getMessage();
        this.status = httpStatus.toString();
        this.timestamp = LocalDateTime.now();
        this.trace = Arrays.toString(e.getStackTrace());
    }
}
