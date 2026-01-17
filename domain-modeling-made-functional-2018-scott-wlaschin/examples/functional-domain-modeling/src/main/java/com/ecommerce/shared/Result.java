package com.ecommerce.shared;

import java.util.function.Function;

/**
 * Railway-Oriented Programming (ROP) 패턴의 핵심 타입 (Chapter 6)
 *
 * <h2>목적 (Purpose)</h2>
 * 예외(Exception) 대신 타입으로 에러를 표현하여 함수 합성(composition)을 가능하게 한다.
 * 함수의 시그니처만 보고도 실패 가능성을 알 수 있어 명시적인 에러 처리를 강제한다.
 *
 * <h2>핵심 개념 (Key Concept): Two-Track Model</h2>
 * <pre>
 * Success 트랙: ────[ A ]────[ B ]────[ C ]────▶ 최종 성공 결과
 *                    │        │        │
 *                    ▼        ▼        ▼
 * Failure 트랙: ─────────────────────────────▶ 첫 번째 실패 지점에서 중단
 * </pre>
 *
 * - Success: 값을 담고 있으며, 다음 연산으로 진행
 * - Failure: 에러를 담고 있으며, 이후 연산을 건너뛰고 에러 전파
 *
 * <h2>주요 연산</h2>
 * <ul>
 *   <li>{@link #map}: 성공 값 변환 (A → B). Failure면 그대로 통과</li>
 *   <li>{@link #flatMap}: 다른 Result 반환 함수 연결 (A → Result&lt;B, F&gt;).
 *       실패 가능한 연산 체이닝에 사용</li>
 *   <li>{@link #mapError}: 에러 타입 변환. Bounded Context 간 에러 변환에 유용</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // Before: try-catch 중첩으로 가독성 저하
 * try {
 *     var validated = validate(order);
 *     try {
 *         var priced = price(validated);
 *         // ...
 *     } catch (PricingException e) { ... }
 * } catch (ValidationException e) { ... }
 *
 * // After: flatMap 체인으로 선형적 흐름
 * return validate(order)
 *     .flatMap(this::checkInventory)
 *     .flatMap(this::calculatePrice)
 *     .flatMap(this::applyDiscount)
 *     .map(this::createReceipt);
 * }</pre>
 *
 * @param <S> Success 타입 - 성공 시 반환할 값의 타입
 * @param <F> Failure 타입 - 실패 시 반환할 에러의 타입 (sealed interface 권장)
 * @see Validation 모든 에러를 수집하려면 Validation 사용
 */
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

    /**
     * 성공 값을 변환하는 함수 (Functor의 fmap)
     *
     * 실패하지 않는 변환에 사용. mapper 함수가 예외를 던지지 않아야 한다.
     * 예: Result&lt;Order, Error&gt; → Result&lt;OrderDto, Error&gt;
     */
    default <NewS> Result<NewS, F> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Success<S, F> s -> Result.success(mapper.apply(s.value()));
            case Failure<S, F> f -> (Result<NewS, F>) f;  // Failure는 그대로 전파
        };
    }

    /**
     * 실패 가능한 연산을 연결하는 함수 (Monad의 bind)
     *
     * Railway의 핵심 연산. 각 단계가 실패할 수 있을 때 사용.
     * 첫 번째 실패 시점에서 체인이 중단되고 Failure가 전파된다 (fail-fast).
     *
     * @param mapper 성공 값을 받아 새로운 Result를 반환하는 함수
     * @see Validation#combine 모든 에러를 수집하려면 Validation 사용
     */
    default <NewS> Result<NewS, F> flatMap(Function<S, Result<NewS, F>> mapper) {
        return switch (this) {
            case Success<S, F> s -> mapper.apply(s.value());  // 다음 단계 실행
            case Failure<S, F> f -> (Result<NewS, F>) f;       // 바로 Failure 전파
        };
    }

    /**
     * 에러 타입을 변환하는 함수
     *
     * Bounded Context 간 에러 통합에 유용.
     * 예: OrderError → PlaceOrderError (상위 에러 타입으로 변환)
     */
    default <NewF> Result<S, NewF> mapError(Function<F, NewF> mapper) {
        return switch (this) {
            case Success<S, F> s -> (Result<S, NewF>) s;
            case Failure<S, F> f -> Result.failure(mapper.apply(f.error()));
        };
    }

    /**
     * 성공을 나타내는 타입. 값을 담고 있다.
     */
    record Success<S, F>(S value) implements Result<S, F> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public F error() { throw new IllegalStateException("Success has no error"); }
    }

    /**
     * 실패를 나타내는 타입. 에러를 담고 있다.
     * 이후 map/flatMap 호출 시 연산을 건너뛰고 에러가 전파된다.
     */
    record Failure<S, F>(F error) implements Result<S, F> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public S value() { throw new IllegalStateException("Failure has no value"); }
    }
}
