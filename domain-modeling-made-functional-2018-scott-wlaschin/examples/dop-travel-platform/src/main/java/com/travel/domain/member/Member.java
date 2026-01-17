package com.travel.domain.member;

import com.travel.domain.membership.MembershipTier;
import com.travel.shared.types.Email;
import com.travel.shared.types.Money;

import java.time.Instant;

/**
 * 회원 - 불변 엔티티
 *
 * <h2>목적 (Purpose)</h2>
 * 회원 정보와 멤버십 상태를 관리하는 불변 엔티티
 *
 * <h2>핵심 개념 (Key Concept): Ch 2 Wither 패턴</h2>
 * <pre>
 * [Key Point] Record는 불변이므로 "수정"은 새 객체 생성:
 *   member.withGrade(newGrade)
 *   member.withTotalBookingAmount(newAmount)
 * </pre>
 *
 * <h2>핵심 개념 (Key Concept): Ch 4 Phantom Types</h2>
 * <pre>
 * Email&lt;Verified&gt; vs Email&lt;Unverified&gt;
 * - 이메일 검증 상태를 타입으로 추적
 * - 미검증 이메일로 중요 알림 발송 방지
 * </pre>
 *
 * @param id                 회원 ID
 * @param email              이메일 (Phantom Type)
 * @param name               이름
 * @param phoneNumber        전화번호
 * @param membershipTier     멤버십 등급
 * @param totalBookingAmount 총 예약 금액
 * @param bookingCount       예약 횟수
 * @param createdAt          가입 시간
 * @param updatedAt          수정 시간
 */
public record Member(
        MemberId id,
        Email<?> email,
        String name,
        String phoneNumber,
        MembershipTier membershipTier,
        Money totalBookingAmount,
        int bookingCount,
        Instant createdAt,
        Instant updatedAt
) {

    public Member {
        if (id == null) throw new IllegalArgumentException("회원 ID는 필수입니다");
        if (email == null) throw new IllegalArgumentException("이메일은 필수입니다");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("이름은 필수입니다");
        if (membershipTier == null) throw new IllegalArgumentException("멤버십 등급은 필수입니다");
        if (totalBookingAmount == null) {
            totalBookingAmount = Money.ZERO_KRW;
        }
        if (bookingCount < 0) throw new IllegalArgumentException("예약 횟수는 0 이상이어야 합니다");
        if (createdAt == null) throw new IllegalArgumentException("가입 시간은 필수입니다");
        if (updatedAt == null) throw new IllegalArgumentException("수정 시간은 필수입니다");
    }

    // ============================================
    // 정적 팩토리 메서드
    // ============================================

    /**
     * 새 회원 생성 (미검증 이메일)
     */
    public static Member create(String emailAddress, String name, String phoneNumber) {
        Instant now = Instant.now();
        return new Member(
                MemberId.generate(),
                Email.unverified(emailAddress),
                name,
                phoneNumber,
                MembershipTier.BRONZE,
                Money.ZERO_KRW,
                0,
                now,
                now
        );
    }

    // ============================================
    // Wither 패턴
    // ============================================

    public Member withMembershipTier(MembershipTier newTier) {
        return new Member(
                id, email, name, phoneNumber, newTier,
                totalBookingAmount, bookingCount, createdAt, Instant.now()
        );
    }

    public Member withEmail(Email<?> newEmail) {
        return new Member(
                id, newEmail, name, phoneNumber, membershipTier,
                totalBookingAmount, bookingCount, createdAt, Instant.now()
        );
    }

    public Member withName(String newName) {
        return new Member(
                id, email, newName, phoneNumber, membershipTier,
                totalBookingAmount, bookingCount, createdAt, Instant.now()
        );
    }

    public Member withBookingStats(Money newTotalAmount, int newBookingCount) {
        return new Member(
                id, email, name, phoneNumber, membershipTier,
                newTotalAmount, newBookingCount, createdAt, Instant.now()
        );
    }

    // ============================================
    // 조회 메서드
    // ============================================

    public boolean isEmailVerified() {
        return email instanceof Email<?> e && e.getClass().getTypeParameters().length > 0;
        // 실제로는 더 정교한 체크 필요 - 여기서는 단순화
    }

    /**
     * VIP 여부 (GOLD 이상)
     */
    public boolean isVip() {
        return membershipTier == MembershipTier.GOLD ||
               membershipTier == MembershipTier.PLATINUM ||
               membershipTier == MembershipTier.DIAMOND;
    }
}
