package com.ecommerce.domain.order;

import java.util.List;
import java.util.Optional;

/**
 * 주문 Repository 인터페이스 - Persistence Ignorance (Chapter 8)
 *
 * <h2>목적 (Purpose)</h2>
 * 도메인 레이어에서 정의하고, 인프라 레이어에서 구현한다.
 * DIP(의존성 역전 원칙)로 도메인 로직이 저장소 기술에 의존하지 않게 한다.
 *
 * <h2>핵심 개념: Persistence Ignorance</h2>
 * <ul>
 *   <li>도메인 객체(Order)는 JPA 어노테이션이나 ORM 관련 코드가 없음</li>
 *   <li>Repository 인터페이스만 정의하고, 구현은 인프라 계층이 담당</li>
 *   <li>저장소 기술(JPA, MyBatis, Redis 등) 교체 시 도메인 코드 변경 없음</li>
 * </ul>
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * 테스트 시 인메모리 구현체나 Mock을 주입하여 DB 없이 테스트 가능.
 */
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId id);
}
