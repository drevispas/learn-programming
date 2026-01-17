package com.ecommerce.domain.member;

import java.util.Optional;

/**
 * 회원 레포지토리 인터페이스
 * 도메인 레이어에 정의되어 인프라 레이어에서 구현
 */
public interface MemberRepository {
    Optional<Member> findById(MemberId id);
    Optional<Member> findByEmail(String email);
    Member save(Member member);
    void delete(MemberId id);
    boolean existsByEmail(String email);
}
