package com.ecommerce.domain.member;

import static org.junit.jupiter.api.Assertions.*;

import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Currency;
import com.ecommerce.shared.types.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@DisplayName("회원 도메인 테스트")
class MemberDomainTest {

    @Nested
    @DisplayName("MemberGrade 테스트")
    class MemberGradeTests {

        @Test
        @DisplayName("Bronze 등급: 할인율 0%, 무료배송 없음")
        void bronze_noDiscount_noFreeShipping() {
            MemberGrade grade = new MemberGrade.Bronze();
            assertEquals(0, grade.discountRate());
            assertFalse(grade.hasFreeShipping());
            assertEquals("브론즈", grade.gradeName());
        }

        @Test
        @DisplayName("Silver 등급: 할인율 5%, 무료배송 없음")
        void silver_5percentDiscount_noFreeShipping() {
            MemberGrade grade = new MemberGrade.Silver();
            assertEquals(5, grade.discountRate());
            assertFalse(grade.hasFreeShipping());
        }

        @Test
        @DisplayName("Gold 등급: 할인율 8%, 무료배송 없음")
        void gold_8percentDiscount_noFreeShipping() {
            MemberGrade grade = new MemberGrade.Gold();
            assertEquals(8, grade.discountRate());
            assertFalse(grade.hasFreeShipping());
        }

        @Test
        @DisplayName("VIP 등급: 할인율 10%, 무료배송 있음")
        void vip_10percentDiscount_hasFreeShipping() {
            MemberGrade grade = new MemberGrade.Vip();
            assertEquals(10, grade.discountRate());
            assertTrue(grade.hasFreeShipping());
        }

        @Test
        @DisplayName("등급별 패턴 매칭 (exhaustive)")
        void exhaustivePatternMatching() {
            MemberGrade grade = new MemberGrade.Gold();

            String result = switch (grade) {
                case MemberGrade.Bronze b -> "브론즈";
                case MemberGrade.Silver s -> "실버";
                case MemberGrade.Gold g -> "골드";
                case MemberGrade.Vip v -> "VIP";
            };

            assertEquals("골드", result);
        }
    }

    @Nested
    @DisplayName("EmailVerification 테스트")
    class EmailVerificationTests {

        @Test
        @DisplayName("미검증 이메일 생성")
        void createUnverifiedEmail() {
            EmailVerification.UnverifiedEmail email =
                EmailVerification.createUnverified("test@example.com", "123456");

            assertEquals("test@example.com", email.email());
            assertEquals("123456", email.verificationCode());
            assertFalse(email.isExpired());
        }

        @Test
        @DisplayName("이메일 검증 성공")
        void verifyEmail_success() {
            EmailVerification.UnverifiedEmail email =
                EmailVerification.createUnverified("test@example.com", "123456");

            Result<EmailVerification.VerifiedEmail, EmailError> result = email.verify("123456");

            assertTrue(result.isSuccess());
            assertEquals("test@example.com", result.value().email());
        }

        @Test
        @DisplayName("이메일 검증 실패 - 코드 불일치")
        void verifyEmail_invalidCode() {
            EmailVerification.UnverifiedEmail email =
                EmailVerification.createUnverified("test@example.com", "123456");

            Result<EmailVerification.VerifiedEmail, EmailError> result = email.verify("wrong");

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof EmailError.InvalidCode);
        }

        @Test
        @DisplayName("이메일 검증 실패 - 만료됨")
        void verifyEmail_expired() {
            EmailVerification.UnverifiedEmail email =
                new EmailVerification.UnverifiedEmail(
                    "test@example.com",
                    "123456",
                    LocalDateTime.now().minusMinutes(1) // 과거 시간
                );

            Result<EmailVerification.VerifiedEmail, EmailError> result = email.verify("123456");

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof EmailError.CodeExpired);
        }
    }

    @Nested
    @DisplayName("Member 테스트")
    class MemberTests {

        @Test
        @DisplayName("회원 생성")
        void createMember() {
            Member member = Member.createWithUnverifiedEmail(
                new MemberId(1L),
                "홍길동",
                "hong@example.com",
                "123456"
            );

            assertEquals(1L, member.id().value());
            assertEquals("홍길동", member.name());
            assertFalse(member.isEmailVerified());
            assertTrue(member.grade() instanceof MemberGrade.Bronze);
            assertEquals(0, member.points().value());
        }

        @Test
        @DisplayName("Wither 패턴: 이름 변경")
        void witherPattern_changeName() {
            Member original = Member.createWithUnverifiedEmail(
                new MemberId(1L), "홍길동", "hong@example.com", "123456"
            );

            Member updated = original.withName("김철수");

            assertEquals("홍길동", original.name()); // 원본 유지
            assertEquals("김철수", updated.name());
            assertEquals(original.id(), updated.id()); // ID는 동일
        }

        @Test
        @DisplayName("등급 업그레이드")
        void upgradeGrade() {
            Member member = Member.createWithUnverifiedEmail(
                new MemberId(1L), "홍길동", "hong@example.com", "123456"
            );

            Member upgraded = member.upgradeGrade(new MemberGrade.Vip());

            assertTrue(upgraded.grade() instanceof MemberGrade.Vip);
            assertTrue(upgraded.canGetFreeShipping());
        }

        @Test
        @DisplayName("포인트 사용 성공")
        void usePoints_success() {
            Member member = Member.createWithUnverifiedEmail(
                new MemberId(1L), "홍길동", "hong@example.com", "123456"
            ).earnPoints(1000);

            Result<Member, MemberError> result = member.usePoints(500);

            assertTrue(result.isSuccess());
            assertEquals(500, result.value().points().value());
        }

        @Test
        @DisplayName("포인트 사용 실패 - 잔액 부족")
        void usePoints_insufficientPoints() {
            Member member = Member.createWithUnverifiedEmail(
                new MemberId(1L), "홍길동", "hong@example.com", "123456"
            ).earnPoints(100);

            Result<Member, MemberError> result = member.usePoints(500);

            assertTrue(result.isFailure());
            assertTrue(result.error() instanceof MemberError.InsufficientPoints);
        }

        @Test
        @DisplayName("등급 할인 계산")
        void calculateGradeDiscount() {
            Member vipMember = Member.createWithUnverifiedEmail(
                new MemberId(1L), "홍길동", "hong@example.com", "123456"
            ).upgradeGrade(new MemberGrade.Vip());

            Money orderAmount = new Money(BigDecimal.valueOf(100000), Currency.KRW);
            Money discount = vipMember.calculateGradeDiscount(orderAmount);

            assertEquals(0, BigDecimal.valueOf(10000).compareTo(discount.amount())); // 10% 할인
        }
    }

    @Nested
    @DisplayName("Points 테스트")
    class PointsTests {

        @Test
        @DisplayName("포인트 추가")
        void addPoints() {
            Points points = new Points(100);
            Points result = points.add(50);
            assertEquals(150, result.value());
        }

        @Test
        @DisplayName("포인트 차감")
        void subtractPoints() {
            Points points = new Points(100);
            Points result = points.subtract(30);
            assertEquals(70, result.value());
        }

        @Test
        @DisplayName("포인트 부족 시 예외")
        void subtractPoints_insufficient() {
            Points points = new Points(100);
            assertThrows(IllegalArgumentException.class, () -> points.subtract(200));
        }
    }
}
