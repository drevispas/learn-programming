package com.travel.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 설정
 *
 * <h2>핵심 개념 (Key Concept): Ch 9 인프라 설정</h2>
 * <pre>
 * [Key Point] JPA 관련 설정을 인프라 레이어에 격리:
 * - 도메인 레이어는 JPA 의존성 없음
 * - Repository 인터페이스만 도메인에 정의
 * </pre>
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.travel.infrastructure.persistence.repository")
public class JpaConfig {
    // 추가 JPA 설정이 필요한 경우 여기에 정의
}
