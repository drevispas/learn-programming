package com.travel.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Validation 타입 - 모든 오류를 수집하는 Applicative Functor
 *
 * <h2>목적 (Purpose)</h2>
 * 여러 검증을 실행하고 모든 오류를 수집 (fail-fast가 아닌 fail-slow)
 *
 * <h2>핵심 개념 (Key Concept): Ch 5 Applicative vs Monad</h2>
 * <pre>
 * [Key Point] Result(Monad) vs Validation(Applicative) 차이:
 *
 * Result (flatMap): 첫 번째 오류에서 중단 (fail-fast)
 *   validateName(input)        → Failure("이름 오류")
 *     .flatMap(validateEmail)  → 실행 안 됨
 *     .flatMap(validateAge)    → 실행 안 됨
 *
 * Validation (ap): 모든 검증 실행 후 오류 수집 (fail-slow)
 *   validateName(input)        → Invalid("이름 오류")
 *   validateEmail(input)       → Invalid("이메일 오류")
 *   validateAge(input)         → Invalid("나이 오류")
 *   → 최종: Invalid(["이름 오류", "이메일 오류", "나이 오류"])
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 7 결합법칙</h2>
 * <pre>
 * 오류 목록 결합은 결합법칙을 만족:
 *   (errors1 ++ errors2) ++ errors3 = errors1 ++ (errors2 ++ errors3)
 * </pre>
 *
 * <h2>놓치기 쉬운 부분 (Common Mistakes)</h2>
 * <ul>
 *   <li>[Trap] 의존적 검증에 Validation 사용 → 이전 검증 결과가 필요하면 Result 사용</li>
 *   <li>[Why List] 단일 오류가 아닌 리스트 → 여러 오류 누적 가능</li>
 * </ul>
 *
 * @param <S> 성공 시 값의 타입
 * @param <F> 실패 오류의 타입
 */
public sealed interface Validation<S, F> permits Validation.Valid, Validation.Invalid {

    // ============================================
    // Sum Type 구현
    // ============================================

    /**
     * 유효한 상태 - 검증 통과 값 보유
     */
    record Valid<S, F>(S value) implements Validation<S, F> {
        public Valid {
            Objects.requireNonNull(value, "Valid 값은 null일 수 없습니다");
        }
    }

    /**
     * 무효한 상태 - 오류 목록 보유
     *
     * <p>[Key Point] 단일 오류가 아닌 리스트로 여러 오류 누적</p>
     */
    record Invalid<S, F>(List<F> errors) implements Validation<S, F> {
        public Invalid {
            Objects.requireNonNull(errors, "오류 목록은 null일 수 없습니다");
            if (errors.isEmpty()) {
                throw new IllegalArgumentException("Invalid는 최소 하나의 오류가 필요합니다");
            }
            errors = List.copyOf(errors); // 불변 복사
        }

        /**
         * 단일 오류로 Invalid 생성 (편의 생성자)
         */
        public Invalid(F error) {
            this(List.of(error));
        }
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 유효한 Validation 생성
     */
    static <S, F> Validation<S, F> valid(S value) {
        return new Valid<>(value);
    }

    /**
     * 무효한 Validation 생성 (단일 오류)
     */
    static <S, F> Validation<S, F> invalid(F error) {
        return new Invalid<>(error);
    }

    /**
     * 무효한 Validation 생성 (오류 목록)
     */
    static <S, F> Validation<S, F> invalid(List<F> errors) {
        return new Invalid<>(errors);
    }

    // ============================================
    // [Key Point] Functor - map
    // ============================================

    /**
     * 유효한 값 변환
     */
    default <T> Validation<T, F> map(Function<? super S, ? extends T> mapper) {
        return switch (this) {
            case Valid<S, F>(var value) -> valid(mapper.apply(value));
            case Invalid<S, F>(var errors) -> invalid(errors);
        };
    }

    /**
     * 오류 변환
     */
    @SuppressWarnings("unchecked")
    default <G> Validation<S, G> mapErrors(Function<? super F, ? extends G> mapper) {
        if (this instanceof Valid<S, F>(var value)) {
            return valid(value);
        } else if (this instanceof Invalid<S, F>(var errors)) {
            List<G> mappedErrors = (List<G>) errors.stream().map(mapper).toList();
            return invalid(mappedErrors);
        }
        throw new IllegalStateException("Exhaustive match failed");
    }

    // ============================================
    // [Key Point] Applicative - ap (apply)
    // 여러 검증을 독립적으로 실행하고 오류 수집
    // ============================================

    /**
     * 두 Validation 결합 (Applicative apply)
     *
     * <pre>
     * [Key Point] Applicative의 핵심 연산
     *
     * Valid(f) ap Valid(x)     = Valid(f(x))
     * Valid(f) ap Invalid(e)   = Invalid(e)
     * Invalid(e1) ap Valid(x)  = Invalid(e1)
     * Invalid(e1) ap Invalid(e2) = Invalid(e1 ++ e2)  // 오류 결합!
     * </pre>
     *
     * @param vf 함수를 포함한 Validation
     * @param va 값을 포함한 Validation
     * @return 적용 결과
     */
    static <A, B, F> Validation<B, F> ap(
            Validation<Function<A, B>, F> vf,
            Validation<A, F> va
    ) {
        return switch (vf) {
            case Valid<Function<A, B>, F>(var f) -> switch (va) {
                case Valid<A, F>(var a) -> valid(f.apply(a));
                case Invalid<A, F>(var errors) -> invalid(errors);
            };
            case Invalid<Function<A, B>, F>(var errors1) -> switch (va) {
                case Valid<A, F> v -> invalid(errors1);
                case Invalid<A, F>(var errors2) -> {
                    // [Key Point] 양쪽 모두 Invalid면 오류 결합
                    var combined = new ArrayList<F>(errors1.size() + errors2.size());
                    combined.addAll(errors1);
                    combined.addAll(errors2);
                    yield invalid(combined);
                }
            };
        };
    }

    // ============================================
    // [Key Point] 편의 결합 메서드
    // 2~4개 Validation을 결합하는 헬퍼
    // ============================================

    /**
     * 2개 Validation 결합
     *
     * <pre>
     * Validation.combine(
     *     validateName(name),
     *     validateEmail(email),
     *     (name, email) -> new User(name, email)
     * );
     * </pre>
     */
    static <A, B, R, F> Validation<R, F> combine(
            Validation<A, F> va,
            Validation<B, F> vb,
            Function2<A, B, R> combiner
    ) {
        return ap(va.map(a -> b -> combiner.apply(a, b)), vb);
    }

    /**
     * 3개 Validation 결합
     */
    static <A, B, C, R, F> Validation<R, F> combine(
            Validation<A, F> va,
            Validation<B, F> vb,
            Validation<C, F> vc,
            Function3<A, B, C, R> combiner
    ) {
        Validation<Function<B, Function<C, R>>, F> step1 = va.map(a -> b -> c -> combiner.apply(a, b, c));
        Validation<Function<C, R>, F> step2 = ap(step1, vb);
        return ap(step2, vc);
    }

    /**
     * 4개 Validation 결합
     */
    static <A, B, C, D, R, F> Validation<R, F> combine(
            Validation<A, F> va,
            Validation<B, F> vb,
            Validation<C, F> vc,
            Validation<D, F> vd,
            Function4<A, B, C, D, R> combiner
    ) {
        Validation<Function<B, Function<C, Function<D, R>>>, F> step1 =
                va.map(a -> b -> c -> d -> combiner.apply(a, b, c, d));
        Validation<Function<C, Function<D, R>>, F> step2 = ap(step1, vb);
        Validation<Function<D, R>, F> step3 = ap(step2, vc);
        return ap(step3, vd);
    }

    // ============================================
    // 상태 확인 및 값 추출
    // ============================================

    default boolean isValid() {
        return this instanceof Valid;
    }

    default boolean isInvalid() {
        return this instanceof Invalid;
    }

    /**
     * 양쪽 케이스를 처리하여 값 추출
     */
    default <T> T fold(
            Function<? super S, ? extends T> onValid,
            Function<? super List<F>, ? extends T> onInvalid
    ) {
        return switch (this) {
            case Valid<S, F>(var value) -> onValid.apply(value);
            case Invalid<S, F>(var errors) -> onInvalid.apply(errors);
        };
    }

    /**
     * 유효한 값 추출 (무효 시 예외)
     */
    default S getOrThrow() {
        return switch (this) {
            case Valid<S, F>(var value) -> value;
            case Invalid<S, F>(var errors) ->
                    throw new IllegalStateException("Validation is Invalid: " + errors);
        };
    }

    /**
     * 오류 목록 추출 (유효 시 빈 목록)
     */
    default List<F> errors() {
        return switch (this) {
            case Valid<S, F> v -> Collections.emptyList();
            case Invalid<S, F>(var errors) -> errors;
        };
    }

    /**
     * Result로 변환
     *
     * <p>[Key Point] 오류 목록을 단일 오류로 변환 필요</p>
     */
    default Result<S, List<F>> toResult() {
        return switch (this) {
            case Valid<S, F>(var value) -> Result.success(value);
            case Invalid<S, F>(var errors) -> Result.failure(errors);
        };
    }

    // ============================================
    // 함수형 인터페이스
    // ============================================

    @FunctionalInterface
    interface Function2<A, B, R> {
        R apply(A a, B b);
    }

    @FunctionalInterface
    interface Function3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    interface Function4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
}
