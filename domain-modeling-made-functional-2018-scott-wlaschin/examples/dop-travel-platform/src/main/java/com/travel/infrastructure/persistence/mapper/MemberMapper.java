package com.travel.infrastructure.persistence.mapper;

import com.travel.domain.member.Member;
import com.travel.domain.member.MemberId;
import com.travel.domain.membership.MembershipTier;
import com.travel.infrastructure.persistence.entity.MemberEntity;
import com.travel.shared.types.Currency;
import com.travel.shared.types.Email;
import com.travel.shared.types.Money;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 회원 Mapper - Domain Record ↔ JPA Entity 변환
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 Mapper 패턴</h2>
 * <pre>
 * [Key Point] 주요 변환 포인트:
 * - Email&lt;Verified&gt; ↔ String + boolean
 * - MembershipTier (enum) ↔ String
 * - Money ↔ BigDecimal + Currency
 * </pre>
 */
@Component
public class MemberMapper {

    // ============================================
    // Domain → Entity
    // ============================================

    /**
     * Member → MemberEntity 변환
     */
    public MemberEntity toEntity(Member member) {
        MemberEntity entity = new MemberEntity();

        entity.setId(member.id().value().toString());
        entity.setEmail(member.email().value());
        entity.setEmailVerified(member.isEmailVerified());
        entity.setName(member.name());
        entity.setPhoneNumber(member.phoneNumber());
        entity.setMembershipTier(member.membershipTier().name());
        entity.setTotalBookingAmount(member.totalBookingAmount().amount());
        entity.setBookingCount(member.bookingCount());
        entity.setCreatedAt(member.createdAt());
        entity.setUpdatedAt(member.updatedAt());

        return entity;
    }

    // ============================================
    // Entity → Domain
    // ============================================

    /**
     * MemberEntity → Member 변환
     */
    public Member toDomain(MemberEntity entity) {
        // [Ch 4] Email Phantom Type 복원
        Email<?> email;
        if (entity.isEmailVerified()) {
            email = Email.alreadyVerified(entity.getEmail());
        } else {
            email = Email.unverified(entity.getEmail());
        }

        return new Member(
                new MemberId(UUID.fromString(entity.getId())),
                email,
                entity.getName(),
                entity.getPhoneNumber(),
                MembershipTier.valueOf(entity.getMembershipTier()),
                new Money(entity.getTotalBookingAmount(), Currency.KRW),
                entity.getBookingCount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
