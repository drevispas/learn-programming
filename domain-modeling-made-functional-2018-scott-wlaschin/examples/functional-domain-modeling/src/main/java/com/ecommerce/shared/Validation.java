package com.ecommerce.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Applicative Validation 패턴 (Chapter 7)
 *
 * <h2>목적 (Purpose)</h2>
 * 여러 검증을 독립적으로 수행하고, 실패 시 모든 에러를 한 번에 수집한다.
 * 사용자에게 "이름이 비어있습니다"만 보여주고 수정 후 "이메일 형식이 잘못됐습니다"를
 * 다시 보여주는 것보다, 처음부터 모든 오류를 한 번에 보여주는 것이 좋은 UX다.
 *
 * <h2>핵심 개념 (Key Concept): Result vs Validation</h2>
 * <pre>
 * Result.flatMap (Monad):
 *   validate(name).flatMap(n -> validate(email).flatMap(e -> ...))
 *   → 첫 번째 에러에서 중단 (fail-fast)
 *   → 순차적 의존성이 있을 때 사용 (예: 주문 → 재고 확인 → 결제)
 *
 * Validation.combine (Applicative):
 *   combine(validate(name), validate(email), (n, e) -> new User(n, e))
 *   → 모든 검증을 독립적으로 수행하고 에러 누적 (accumulating)
 *   → 폼 입력 검증처럼 독립적인 필드들을 검증할 때 사용
 * </pre>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // 폼 검증 예시: 모든 에러를 한 번에 수집
 * Validation<CreateUserRequest, List<String>> result = Validation.combine3(
 *     validateName(name),      // "이름은 2자 이상이어야 합니다"
 *     validateEmail(email),    // "이메일 형식이 올바르지 않습니다"
 *     validateAge(age),        // "나이는 0 이상이어야 합니다"
 *     CreateUserRequest::new
 * );
 *
 * result.fold(
 *     request -> createUser(request),
 *     errors -> showAllErrors(errors)  // ["이름은 2자 이상", "이메일 형식 오류"] 한번에 표시
 * );
 * }</pre>
 *
 * @param <S> 성공 타입 - 검증 성공 시 반환할 값의 타입
 * @param <E> 에러 타입 - 일반적으로 {@code List<ErrorType>} 사용
 * @see Result 순차적 실패(fail-fast)가 필요하면 Result 사용
 */
public sealed interface Validation<S, E> permits Validation.Valid, Validation.Invalid {

    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    static <S, E> Validation<S, E> invalid(E errors) {
        return new Invalid<>(errors);
    }

    static <S, E> Validation<S, List<E>> invalidOne(E error) {
        return new Invalid<>(List.of(error));
    }

    default <NewS> Validation<NewS, E> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Valid<S, E> v -> new Valid<>(mapper.apply(v.value()));
            case Invalid<S, E> i -> new Invalid<>(i.errors());
        };
    }

    /**
     * 두 개의 독립적인 검증 결과를 결합한다 (Applicative Functor의 핵심 연산)
     *
     * <ul>
     *   <li>둘 다 Valid → combiner 함수로 값 결합</li>
     *   <li>하나만 Invalid → 해당 에러 반환</li>
     *   <li>둘 다 Invalid → 양쪽 에러 리스트 합침 (에러 누적!)</li>
     * </ul>
     *
     * @param combiner 두 성공 값을 결합하는 함수 (예: 생성자 참조)
     */
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
                // 핵심: 두 에러 리스트를 합침 - fail-fast가 아닌 accumulating
                case Invalid<B, List<E>> b -> {
                    List<E> errors = new ArrayList<>(a.errors());
                    errors.addAll(b.errors());
                    yield new Invalid<>(errors);
                }
            };
        };
    }

    /** 3개 검증 결과 결합. 내부적으로 combine을 재귀 사용 */
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

    /** 4개 검증 결과 결합. 더 많은 필드가 필요하면 동일 패턴으로 확장 */
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

    /**
     * 검증 결과를 처리하는 패턴 매칭 헬퍼
     *
     * switch 표현식 대신 사용할 수 있다. 특히 람다에서 유용.
     *
     * @param onValid 성공 시 호출될 함수
     * @param onInvalid 실패 시 호출될 함수 (에러 리스트 받음)
     */
    default <R> R fold(
        Function<S, R> onValid,
        Function<E, R> onInvalid
    ) {
        return switch (this) {
            case Valid<S, E> v -> onValid.apply(v.value());
            case Invalid<S, E> i -> onInvalid.apply(i.errors());
        };
    }

    /** 검증 성공. 유효한 값을 담고 있다 */
    record Valid<S, E>(S value) implements Validation<S, E> {}

    /** 검증 실패. 에러 목록을 담고 있다 (보통 List&lt;E&gt; 타입) */
    record Invalid<S, E>(E errors) implements Validation<S, E> {}

    /** 내부 헬퍼: combine3, combine4에서 중간 결과를 담는 튜플 */
    record Pair<A, B>(A first, B second) {}

    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
}
