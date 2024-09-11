package org.demo.football.controller;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Response<T> {

    private final ResponseData<T> data;
    private final ResponseError error;
}
