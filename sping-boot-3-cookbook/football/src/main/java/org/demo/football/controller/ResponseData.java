package org.demo.football.controller;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResponseData<T> {
    private final T data;
}
