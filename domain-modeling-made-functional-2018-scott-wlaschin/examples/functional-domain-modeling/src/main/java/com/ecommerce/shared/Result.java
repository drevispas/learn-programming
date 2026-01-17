package com.ecommerce.shared;

import java.util.function.Function;

public sealed interface Result<S, F> permits Result.Success, Result.Failure {
    boolean isSuccess();
    boolean isFailure();
    S value();
    F error();

    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    default <NewS> Result<NewS, F> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Success<S, F> s -> Result.success(mapper.apply(s.value()));
            case Failure<S, F> f -> (Result<NewS, F>) f;
        };
    }

    default <NewS> Result<NewS, F> flatMap(Function<S, Result<NewS, F>> mapper) {
        return switch (this) {
            case Success<S, F> s -> mapper.apply(s.value());
            case Failure<S, F> f -> (Result<NewS, F>) f;
        };
    }

    default <NewF> Result<S, NewF> mapError(Function<F, NewF> mapper) {
        return switch (this) {
            case Success<S, F> s -> (Result<S, NewF>) s;
            case Failure<S, F> f -> Result.failure(mapper.apply(f.error()));
        };
    }

    record Success<S, F>(S value) implements Result<S, F> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public F error() { throw new IllegalStateException("Success has no error"); }
    }

    record Failure<S, F>(F error) implements Result<S, F> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public S value() { throw new IllegalStateException("Failure has no value"); }
    }
}
