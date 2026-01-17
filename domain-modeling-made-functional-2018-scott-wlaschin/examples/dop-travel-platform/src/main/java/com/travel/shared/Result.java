package com.travel.shared;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result 타입 - 성공 또는 실패를 표현하는 Sum Type
 *
 * <h2>목적 (Purpose)</h2>
 * 예외 대신 값으로 오류를 표현하여 함수의 전체성(totality) 보장
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Total Functions</h2>
 * <pre>
 * [Key Point] Total Function이란?
 * - 모든 입력에 대해 정의된 출력을 반환하는 함수
 * - 예외를 던지지 않음 (부분 함수의 문제)
 * - 호출자가 실패 케이스를 명시적으로 처리하도록 강제
 *
 * [Before] 예외 사용:
 *   User findUser(String id) throws UserNotFoundException
 *   → 호출자가 예외 처리를 잊을 수 있음
 *
 * [After] Result 사용:
 *   Result&lt;User, UserNotFound&gt; findUser(String id)
 *   → 반환값 처리 시 Success/Failure 모두 다뤄야 함
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Railway-Oriented Programming</h2>
 * <pre>
 *      Success Track ─────────────────────────────→
 *           │               │               │
 *         map()          flatMap()        map()
 *           │               │               │
 *      Failure Track ─────────────────────────────→
 *
 * [Key Point] 한 번 실패하면 이후 연산은 건너뛰고 실패 결과만 전달
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] get() 직접 호출 → Failure인 경우 예외 발생</li>
 *   <li>[Trap] isSuccess() 체크 후 get() → 더 좋은 방법: fold() 또는 패턴 매칭</li>
 *   <li>[Why sealed] sealed interface로 Success/Failure만 허용</li>
 * </ul>
 *
 * @param <S> 성공 시 값의 타입
 * @param <F> 실패 시 오류의 타입
 */
public sealed interface Result<S, F> permits Result.Success, Result.Failure {

    // ============================================
    // [Key Point] Sum Type 구현
    // 정확히 두 가지 상태만 존재
    // ============================================

    /**
     * 성공 상태 - 값을 보유
     *
     * @param value 성공 값
     * @param <S>   성공 값의 타입
     * @param <F>   실패 오류의 타입 (사용되지 않음)
     */
    record Success<S, F>(S value) implements Result<S, F> {
        public Success {
            Objects.requireNonNull(value, "Success 값은 null일 수 없습니다");
        }
    }

    /**
     * 실패 상태 - 오류를 보유
     *
     * @param error 실패 오류
     * @param <S>   성공 값의 타입 (사용되지 않음)
     * @param <F>   실패 오류의 타입
     */
    record Failure<S, F>(F error) implements Result<S, F> {
        public Failure {
            Objects.requireNonNull(error, "Failure 오류는 null일 수 없습니다");
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 성공 Result 생성
     */
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    /**
     * 실패 Result 생성
     */
    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }

    /**
     * 예외를 던질 수 있는 Supplier를 Result로 변환
     *
     * <p>[Key Point] 기존 예외 기반 코드를 Result 기반으로 래핑</p>
     *
     * @param supplier   값을 공급하는 함수
     * @param errorMapper 예외를 오류 타입으로 변환하는 함수
     */
    static <S, F> Result<S, F> fromThrowing(
            ThrowingSupplier<S> supplier,
            Function<Exception, F> errorMapper
    ) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(errorMapper.apply(e));
        }
    }

    // ============================================
    // [Key Point] Functor - map
    // 성공 값을 변환, 실패는 그대로 통과
    // ============================================

    /**
     * 성공 값 변환 (Functor map)
     *
     * <pre>
     * [Key Point]
     * Success: f 적용하여 새 Success 반환
     * Failure: 그대로 Failure 반환 (f 실행 안 함)
     * </pre>
     *
     * @param mapper 변환 함수
     * @param <T>    새로운 성공 타입
     * @return 변환된 Result
     */
    default <T> Result<T, F> map(Function<? super S, ? extends T> mapper) {
        return switch (this) {
            case Success<S, F>(var value) -> success(mapper.apply(value));
            case Failure<S, F>(var error) -> failure(error);
        };
    }

    /**
     * 실패 오류 변환
     *
     * @param mapper 오류 변환 함수
     * @param <G>    새로운 실패 타입
     * @return 변환된 Result
     */
    default <G> Result<S, G> mapFailure(Function<? super F, ? extends G> mapper) {
        return switch (this) {
            case Success<S, F>(var value) -> success(value);
            case Failure<S, F>(var error) -> failure(mapper.apply(error));
        };
    }

    // ============================================
    // [Key Point] Monad - flatMap
    // Result를 반환하는 함수 체이닝
    // ============================================

    /**
     * Result 반환 함수 체이닝 (Monad flatMap/bind)
     *
     * <pre>
     * [Key Point] Railway-Oriented Programming의 핵심
     *
     * validateAge(user)
     *     .flatMap(this::validateEmail)
     *     .flatMap(this::validateAddress)
     *     .flatMap(this::createAccount)
     *
     * 중간에 실패하면 이후 단계는 모두 건너뜀
     * </pre>
     *
     * @param mapper Result를 반환하는 변환 함수
     * @param <T>    새로운 성공 타입
     * @return 체이닝된 Result
     */
    default <T> Result<T, F> flatMap(Function<? super S, ? extends Result<T, F>> mapper) {
        return switch (this) {
            case Success<S, F>(var value) -> mapper.apply(value);
            case Failure<S, F>(var error) -> failure(error);
        };
    }

    // ============================================
    // 상태 확인 및 값 추출
    // ============================================

    /**
     * 성공 여부 확인
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * 실패 여부 확인
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * 양쪽 케이스를 처리하여 값 추출 (권장 방식)
     *
     * <pre>
     * [Key Point] fold는 패턴 매칭의 함수형 버전
     * 두 케이스를 모두 처리해야 하므로 안전
     *
     * result.fold(
     *     success -> "Created: " + success.name(),
     *     failure -> "Error: " + failure.getMessage()
     * );
     * </pre>
     *
     * @param onSuccess 성공 시 처리 함수
     * @param onFailure 실패 시 처리 함수
     * @param <T>       결과 타입
     * @return 처리 결과
     */
    default <T> T fold(
            Function<? super S, ? extends T> onSuccess,
            Function<? super F, ? extends T> onFailure
    ) {
        return switch (this) {
            case Success<S, F>(var value) -> onSuccess.apply(value);
            case Failure<S, F>(var error) -> onFailure.apply(error);
        };
    }

    /**
     * 성공 값 추출 (실패 시 기본값)
     *
     * @param defaultValue 실패 시 반환할 기본값
     * @return 성공 값 또는 기본값
     */
    default S getOrElse(S defaultValue) {
        return switch (this) {
            case Success<S, F>(var value) -> value;
            case Failure<S, F> f -> defaultValue;
        };
    }

    /**
     * 성공 값 추출 (실패 시 Supplier로 기본값 생성)
     *
     * @param defaultSupplier 기본값 생성 Supplier
     * @return 성공 값 또는 생성된 기본값
     */
    default S getOrElseGet(Supplier<? extends S> defaultSupplier) {
        return switch (this) {
            case Success<S, F>(var value) -> value;
            case Failure<S, F> f -> defaultSupplier.get();
        };
    }

    /**
     * 성공 값 추출 (실패 시 예외 던짐)
     *
     * <p>[Trap] 가급적 fold()나 getOrElse() 사용 권장.
     * 이 메서드는 반드시 성공임을 확신할 때만 사용.</p>
     *
     * @return 성공 값
     * @throws IllegalStateException 실패인 경우
     */
    default S getOrThrow() {
        return switch (this) {
            case Success<S, F>(var value) -> value;
            case Failure<S, F>(var error) ->
                    throw new IllegalStateException("Result is Failure: " + error);
        };
    }

    /**
     * 실패 오류 추출 (성공 시 null)
     *
     * @return 실패 오류 또는 null
     */
    default F errorOrNull() {
        return switch (this) {
            case Success<S, F> s -> null;
            case Failure<S, F>(var error) -> error;
        };
    }

    // ============================================
    // 유틸리티 인터페이스
    // ============================================

    /**
     * 예외를 던질 수 있는 Supplier
     */
    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
