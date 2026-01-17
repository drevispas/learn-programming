package com.travel.sample;

import com.travel.domain.booking.BookingError;
import com.travel.shared.Result;
import com.travel.shared.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chapter 5: Total Functions와 Result 타입 학습 테스트
 *
 * <h2>학습 목표</h2>
 * <ul>
 *   <li>Total Function vs Partial Function 이해</li>
 *   <li>Result 타입으로 오류 처리</li>
 *   <li>Railway-Oriented Programming 패턴</li>
 *   <li>Validation (Applicative)으로 오류 수집</li>
 * </ul>
 */
@DisplayName("[Ch 5] Total Functions & Result 타입")
class Chapter5_TotalFunctionsTest {

    @Nested
    @DisplayName("[Ch 5.1] Total vs Partial Function")
    class TotalVsPartialFunction {

        @Test
        @DisplayName("Partial Function은 일부 입력에서 예외를 던진다 (안티패턴)")
        void partial_function_throws_for_some_inputs() {
            // [안티패턴] Partial Function - 일부 입력에서 예외 발생
            assertThrows(ArithmeticException.class, () -> {
                int result = 10 / 0; // 0으로 나누기 → 예외!
            });

            assertThrows(IllegalArgumentException.class, () -> {
                // 음수 입력 → 예외!
                parseDaysUntilTrip(-5);
            });
        }

        @Test
        @DisplayName("Total Function은 모든 입력에 대해 정의된 출력을 반환한다")
        void total_function_returns_for_all_inputs() {
            // [권장] Total Function - Result로 실패 표현
            Result<Integer, String> result1 = safeDivide(10, 2);
            Result<Integer, String> result2 = safeDivide(10, 0);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isFailure());
            assertEquals(5, result1.getOrThrow());
            assertEquals("0으로 나눌 수 없습니다", result2.errorOrNull());
        }

        // Partial Function (안티패턴)
        private int parseDaysUntilTrip(int days) {
            if (days < 0) throw new IllegalArgumentException("음수 불가");
            return days;
        }

        // Total Function (권장)
        private Result<Integer, String> safeDivide(int a, int b) {
            if (b == 0) return Result.failure("0으로 나눌 수 없습니다");
            return Result.success(a / b);
        }
    }

    @Nested
    @DisplayName("[Ch 5.2] Result 타입 사용")
    class ResultTypeUsage {

        @Test
        @DisplayName("Result.success()는 성공 값을 감싼다")
        void result_success_wraps_value() {
            Result<String, BookingError> result = Result.success("성공 데이터");

            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertEquals("성공 데이터", result.getOrThrow());
        }

        @Test
        @DisplayName("Result.failure()는 오류를 감싼다")
        void result_failure_wraps_error() {
            var bookingId = com.travel.domain.booking.BookingId.generate();
            var error = new BookingError.NotFound(bookingId);
            Result<String, BookingError> result = Result.failure(error);

            assertTrue(result.isFailure());
            assertFalse(result.isSuccess());
            assertInstanceOf(BookingError.NotFound.class, result.errorOrNull());
        }

        @Test
        @DisplayName("fold()로 양쪽 케이스를 처리한다")
        void fold_handles_both_cases() {
            Result<Integer, String> success = Result.success(42);
            Result<Integer, String> failure = Result.failure("오류 발생");

            String successMessage = success.fold(
                    value -> "값: " + value,
                    error -> "에러: " + error
            );

            String failureMessage = failure.fold(
                    value -> "값: " + value,
                    error -> "에러: " + error
            );

            assertEquals("값: 42", successMessage);
            assertEquals("에러: 오류 발생", failureMessage);
        }
    }

    @Nested
    @DisplayName("[Ch 5.3] Railway-Oriented Programming")
    class RailwayOrientedProgramming {

        @Test
        @DisplayName("flatMap으로 성공 케이스를 체이닝한다")
        void flatMap_chains_success_cases() {
            // Given
            Result<Integer, String> start = Result.success(10);

            // When: 체이닝
            Result<Integer, String> result = start
                    .flatMap(x -> Result.success(x * 2))    // 20
                    .flatMap(x -> Result.success(x + 5))    // 25
                    .flatMap(x -> Result.success(x / 5));   // 5

            // Then
            assertTrue(result.isSuccess());
            assertEquals(5, result.getOrThrow());
        }

        @Test
        @DisplayName("체인 중간에 실패하면 이후는 건너뛴다")
        void failure_short_circuits_the_chain() {
            // Given
            Result<Integer, String> start = Result.success(10);

            // When: 중간에 실패
            Result<Integer, String> result = start
                    .flatMap(x -> Result.success(x * 2))                    // 20
                    .flatMap(x -> Result.<Integer, String>failure("오류!"))  // 실패!
                    .flatMap(x -> Result.success(x + 100))  // 실행 안 됨
                    .flatMap(x -> Result.success(x * 1000)); // 실행 안 됨

            // Then: 최종 결과는 실패
            assertTrue(result.isFailure());
            assertEquals("오류!", result.errorOrNull());
        }

        @Test
        @DisplayName("map으로 성공 값을 변환한다")
        void map_transforms_success_value() {
            // Given
            Result<Integer, String> success = Result.success(42);

            // When
            Result<String, String> result = success.map(x -> "Answer: " + x);

            // Then
            assertEquals("Answer: 42", result.getOrThrow());
        }
    }

    @Nested
    @DisplayName("[Ch 5.4] Validation (Applicative)")
    class ValidationApplicative {

        @Test
        @DisplayName("Validation.valid()는 유효한 값을 감싼다")
        void validation_valid_wraps_value() {
            Validation<String, String> valid = Validation.valid("유효한 데이터");

            assertTrue(valid.isValid());
            assertEquals("유효한 데이터", valid.getOrThrow());
        }

        @Test
        @DisplayName("Validation.invalid()는 오류를 감싼다")
        void validation_invalid_wraps_error() {
            Validation<String, String> invalid = Validation.invalid("오류 메시지");

            assertTrue(invalid.isInvalid());
            assertEquals(List.of("오류 메시지"), invalid.errors());
        }

        @Test
        @DisplayName("Validation은 모든 오류를 수집한다 (fail-slow)")
        void validation_collects_all_errors() {
            // Given: 여러 검증 (모두 실패)
            Validation<String, String> v1 = Validation.invalid("이름 오류");
            Validation<String, String> v2 = Validation.invalid("이메일 오류");

            // When: combine (모든 오류 수집)
            Validation<String, String> result = Validation.combine(
                    v1, v2,
                    (name, email) -> name + "/" + email
            );

            // Then: 모든 오류가 수집됨
            assertTrue(result.isInvalid());
            List<String> errors = result.errors();
            assertEquals(2, errors.size());
            assertTrue(errors.contains("이름 오류"));
            assertTrue(errors.contains("이메일 오류"));
        }

        @Test
        @DisplayName("Result(Monad)는 첫 번째 오류에서 멈춘다 (fail-fast)")
        void result_stops_at_first_error() {
            // Given: flatMap 체인
            Result<String, String> result = Result.<String, String>failure("첫 번째 오류")
                    .flatMap(x -> Result.<String, String>failure("두 번째 오류"))
                    .flatMap(x -> Result.<String, String>failure("세 번째 오류"));

            // Then: 첫 번째 오류만 반환
            assertTrue(result.isFailure());
            assertEquals("첫 번째 오류", result.errorOrNull());
            // "두 번째 오류", "세 번째 오류"는 실행되지 않음
        }

        @Test
        @DisplayName("Validation vs Result: 적합한 상황")
        void validation_vs_result_use_cases() {
            // [Validation 사용 시]
            // - 폼 입력 검증 (모든 오류를 사용자에게 보여줘야 함)
            // - 독립적인 검증 규칙들 (이전 결과에 의존하지 않음)

            // [Result 사용 시]
            // - 의존적인 연산 체인 (이전 결과가 다음에 필요)
            // - 첫 실패에서 즉시 중단해야 하는 경우

            // 예: 폼 검증 → Validation
            var nameValidation = validateName("");           // Invalid
            var emailValidation = validateEmail("invalid");  // Invalid
            var ageValidation = validateAge(-1);             // Invalid

            var formResult = Validation.combine(
                    nameValidation,
                    emailValidation,
                    ageValidation,
                    (name, email, age) -> "User: " + name + ", " + email + ", " + age
            );

            // 3개의 오류 모두 수집됨
            assertEquals(3, formResult.errors().size());
        }

        private Validation<String, String> validateName(String name) {
            if (name == null || name.isBlank()) {
                return Validation.invalid("이름은 필수입니다");
            }
            return Validation.valid(name);
        }

        private Validation<String, String> validateEmail(String email) {
            if (email == null || !email.contains("@")) {
                return Validation.invalid("유효한 이메일이 아닙니다");
            }
            return Validation.valid(email);
        }

        private Validation<Integer, String> validateAge(int age) {
            if (age < 0 || age > 150) {
                return Validation.invalid("유효한 나이가 아닙니다");
            }
            return Validation.valid(age);
        }
    }
}
