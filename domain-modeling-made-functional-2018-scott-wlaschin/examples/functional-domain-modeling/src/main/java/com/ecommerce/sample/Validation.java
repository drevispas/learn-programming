package com.ecommerce.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Validation 타입 - 에러를 수집하는 Applicative 패턴 구현.
 * E 타입 파라미터는 에러 컬렉션 타입 (보통 List<SomeError>)을 나타냅니다.
 */
public sealed interface Validation<S, E> permits Valid, Invalid {

    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    static <S, E> Validation<S, E> invalid(E errors) {
        return new Invalid<>(errors);
    }

    // 단일 에러로 Invalid 생성 (편의 메서드)
    static <S, E> Validation<S, List<E>> invalidOne(E error) {
        return new Invalid<>(List.of(error));
    }

    default <NewS> Validation<NewS, E> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Valid<S, E> v -> new Valid<>(mapper.apply(v.value()));
            case Invalid<S, E> i -> new Invalid<>(i.errors());
        };
    }

    // 여러 Validation 결합 (에러는 List<E>로 수집)
    static <A, B, C, E> Validation<C, List<E>> combine(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        BiFunction<A, B, C> combiner
    ) {
        return switch (va) {
            case Valid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> new Valid<>(combiner.apply(a.value(), b.value()));
                case Invalid<B, List<E>> b -> new Invalid<>(b.errors());
            };
            case Invalid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> new Invalid<>(a.errors());
                case Invalid<B, List<E>> b -> {
                    List<E> errors = new ArrayList<>(a.errors());
                    errors.addAll(b.errors());
                    yield new Invalid<>(errors);
                }
            };
        };
    }

    // 3개 결합
    static <A, B, C, R, E> Validation<R, List<E>> combine3(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        TriFunction<A, B, C, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            vc,
            (ab, c) -> combiner.apply(ab.first(), ab.second(), c)
        );
    }

    // 4개 결합
    static <A, B, C, D, R, E> Validation<R, List<E>> combine4(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        Validation<D, List<E>> vd,
        QuadFunction<A, B, C, D, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            combine(vc, vd, Pair::new),
            (ab, cd) -> combiner.apply(ab.first(), ab.second(), cd.first(), cd.second())
        );
    }

    // fold: Validation을 Result로 변환
    default <R> R fold(
        java.util.function.Function<S, R> onValid,
        java.util.function.Function<E, R> onInvalid
    ) {
        return switch (this) {
            case Valid<S, E> v -> onValid.apply(v.value());
            case Invalid<S, E> i -> onInvalid.apply(i.errors());
        };
    }
}

record Valid<S, E>(S value) implements Validation<S, E> {}
record Invalid<S, E>(E errors) implements Validation<S, E> {}
record Pair<A, B>(A first, B second) {}

@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

@FunctionalInterface
interface QuadFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}
