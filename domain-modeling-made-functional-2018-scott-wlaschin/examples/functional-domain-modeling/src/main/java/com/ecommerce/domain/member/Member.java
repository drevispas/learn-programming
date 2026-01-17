package com.ecommerce.domain.member;

import com.ecommerce.shared.Result;
import com.ecommerce.shared.types.Money;

/**
 * 회원 엔티티
 * 불변 객체로 상태 변경 시 새 객체 반환 (Wither 패턴)
 */
public record Member(
    MemberId id,
    String name,
    EmailVerification email,
    MemberGrade grade,
    Points points
) {
    public Member {
        if (id == null) throw new IllegalArgumentException("MemberId required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name required");
        if (email == null) throw new IllegalArgumentException("Email required");
        if (grade == null) throw new IllegalArgumentException("Grade required");
        if (points == null) throw new IllegalArgumentException("Points required");
    }

    // === Wither 패턴 메서드 ===

    public Member withName(String newName) {
        return new Member(id, newName, email, grade, points);
    }

    public Member withEmail(EmailVerification newEmail) {
        return new Member(id, name, newEmail, grade, points);
    }

    public Member withGrade(MemberGrade newGrade) {
        return new Member(id, name, email, newGrade, points);
    }

    public Member withPoints(Points newPoints) {
        return new Member(id, name, email, grade, newPoints);
    }

    // === 비즈니스 로직 ===

    /**
     * 등급 업그레이드
     */
    public Member upgradeGrade(MemberGrade newGrade) {
        return withGrade(newGrade);
    }

    /**
     * 포인트 사용
     * @return 성공 시 갱신된 Member, 실패 시 MemberError
     */
    public Result<Member, MemberError> usePoints(int amount) {
        if (!points.hasEnough(amount)) {
            return Result.failure(new MemberError.InsufficientPoints(amount, points.value()));
        }
        return Result.success(withPoints(points.subtract(amount)));
    }

    /**
     * 포인트 적립
     */
    public Member earnPoints(int amount) {
        return withPoints(points.add(amount));
    }

    /**
     * 주문 금액에 대한 등급 할인 계산
     */
    public Money calculateGradeDiscount(Money orderAmount) {
        if (grade.discountRate() == 0) {
            return Money.ZERO;
        }
        return orderAmount.multiply(grade.discountRate()).divide(100);
    }

    /**
     * 무료배송 가능 여부
     */
    public boolean canGetFreeShipping() {
        return grade.hasFreeShipping();
    }

    /**
     * 이메일 인증 완료 여부
     */
    public boolean isEmailVerified() {
        return email instanceof EmailVerification.VerifiedEmail;
    }

    /**
     * 이메일 주소 반환
     */
    public String emailAddress() {
        return email.email();
    }

    // === 팩토리 메서드 ===

    /**
     * 새 회원 생성 (기본 등급: Bronze, 기본 포인트: 0)
     */
    public static Member create(
        MemberId id,
        String name,
        EmailVerification email
    ) {
        return new Member(id, name, email, new MemberGrade.Bronze(), Points.ZERO);
    }

    /**
     * 새 회원 생성 (인증 필요한 이메일)
     */
    public static Member createWithUnverifiedEmail(
        MemberId id,
        String name,
        String emailAddress,
        String verificationCode
    ) {
        EmailVerification unverified = EmailVerification.createUnverified(emailAddress, verificationCode);
        return new Member(id, name, unverified, new MemberGrade.Bronze(), Points.ZERO);
    }
}
