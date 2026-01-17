package com.travel.domain.member;

import java.util.Optional;

/**
 * 회원 Repository 인터페이스
 */
public interface MemberRepository {

    Optional<Member> findById(MemberId id);

    Optional<Member> findByEmail(String email);

    Member save(Member member);

    boolean existsById(MemberId id);

    boolean existsByEmail(String email);
}
