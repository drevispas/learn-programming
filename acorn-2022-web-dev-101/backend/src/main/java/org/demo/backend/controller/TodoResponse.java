package org.demo.backend.controller;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// Response entity class containing error and data fields. Data field is a generic type.
@Getter
@Setter
@Builder
public class TodoResponse<T> {

    private String error;
    private T data;

    public TodoResponse(String errorMessage, T data) {
        this.error = errorMessage;
        this.data = data;
    }

    public TodoResponse(Throwable throwable) {
        this.error = throwable.getMessage();
    }

    public TodoResponse(T data) {
        this.data = data;
    }
}
