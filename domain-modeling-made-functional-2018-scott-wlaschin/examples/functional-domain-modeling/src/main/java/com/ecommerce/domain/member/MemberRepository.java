package com.ecommerce.domain.member;

import java.util.Optional;

/**
 * 회원 Repository 인터페이스 - Persistence Ignorance (Chapter 8)
 *
 * 도메인 레이어에서 정의, 인프라 레이어에서 구현.
 * Member 엔티티는 저장소 기술을 모르고 순수한 도메인 객체로 유지된다.
 */
public interface MemberRepository {
    Optional<Member> findById(MemberId id);
    Optional<Member> findByEmail(String email);
    Member save(Member member);
    void delete(MemberId id);
    boolean existsByEmail(String email);
}
