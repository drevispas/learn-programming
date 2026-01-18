package com.ecommerce.shared;

import java.util.function.Function;
import java.util.function.Supplier;

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
            // [Key Point] Success 트랙: 함수를 적용하고 새 Success로 감싸기
            case Success<S, F> s -> Result.success(mapper.apply(s.value()));
            // [Key Point] Failure 트랙: 에러를 그대로 전달 (함수 적용 안 함)
            case Failure<S, F> f -> (Result<NewS, F>) f;
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
            // [Key Point] Success 트랙: 함수가 이미 Result를 반환하므로 unwrap만 (이중 감싸기 방지)
            case Success<S, F> s -> mapper.apply(s.value());
            // [Key Point] Failure 트랙: 첫 번째 실패에서 즉시 단락(short-circuit)
            case Failure<S, F> f -> (Result<NewS, F>) f;
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
            // [Key Point] Success 트랙: 값 그대로 유지 (에러 변환 필요 없음)
            case Success<S, F> s -> (Result<S, NewF>) s;
            // [Key Point] Failure 트랙: 에러 타입 변환 (ACL 패턴에서 Bounded Context 간 에러 통합에 활용)
            case Failure<S, F> f -> Result.failure(mapper.apply(f.error()));
        };
    }

    // === 안전한 접근 메서드 (Safe Access Methods) ===

    /**
     * 패턴 매칭 헬퍼: 성공/실패 각각에 대해 함수를 적용하고 단일 결과 반환
     *
     * switch 표현식 대신 사용할 수 있다. 특히 람다에서 유용.
     *
     * @param onSuccess 성공 시 호출될 함수
     * @param onFailure 실패 시 호출될 함수
     * @return 각 함수의 결과
     */
    default <R> R fold(Function<S, R> onSuccess, Function<F, R> onFailure) {
        return switch (this) {
            case Success<S, F> s -> onSuccess.apply(s.value());
            case Failure<S, F> f -> onFailure.apply(f.error());
        };
    }

    /**
     * 성공 값을 반환하거나, 실패 시 기본값 반환
     *
     * value() 메서드와 달리 예외를 던지지 않아 안전하다.
     * 실패 시 즉시 평가되는 기본값이 필요할 때 사용.
     *
     * @param defaultValue 실패 시 반환할 기본값
     * @return 성공 값 또는 기본값
     */
    default S getOrElse(S defaultValue) {
        return fold(s -> s, f -> defaultValue);
    }

    /**
     * 성공 값을 반환하거나, 실패 시 Supplier로 기본값 생성
     *
     * 기본값 생성 비용이 클 때 지연 계산(lazy evaluation)을 위해 사용.
     * 실패 시에만 supplier가 호출된다.
     *
     * @param supplier 실패 시 기본값을 생성할 Supplier
     * @return 성공 값 또는 supplier가 생성한 값
     */
    default S getOrElseGet(Supplier<S> supplier) {
        return fold(s -> s, f -> supplier.get());
    }

    // === 에러 복구 메서드 (Error Recovery Methods) ===

    /**
     * 실패를 성공으로 변환 (복구)
     *
     * 에러 발생 시 대체 값을 제공하여 성공으로 복구할 때 사용.
     * 예: API 호출 실패 시 캐시된 값 사용
     *
     * @param recovery 에러를 받아 성공 값을 반환하는 함수
     * @return 성공 Result 또는 복구된 성공 Result
     */
    default Result<S, F> recover(Function<F, S> recovery) {
        return switch (this) {
            case Success<S, F> s -> s;
            case Failure<S, F> f -> Result.success(recovery.apply(f.error()));
        };
    }

    /**
     * 실패를 다른 Result로 변환 (복구 시도)
     *
     * 에러 발생 시 다른 연산을 시도하고 싶을 때 사용.
     * 예: 첫 번째 API 실패 시 대체 API 호출
     *
     * @param recovery 에러를 받아 새 Result를 반환하는 함수
     * @return 원래 성공 Result 또는 복구 시도 결과
     */
    default Result<S, F> recoverWith(Function<F, Result<S, F>> recovery) {
        return switch (this) {
            case Success<S, F> s -> s;
            case Failure<S, F> f -> recovery.apply(f.error());
        };
    }

    /**
     * 실패 시 대체 Result 반환
     *
     * recoverWith의 간단 버전. 에러 내용을 무시하고 대체 Result 반환.
     *
     * @param alternative 대체 Result를 생성할 Supplier
     * @return 원래 성공 Result 또는 대체 Result
     */
    default Result<S, F> orElse(Supplier<Result<S, F>> alternative) {
        return switch (this) {
            case Success<S, F> s -> s;
            case Failure<S, F> f -> alternative.get();
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
