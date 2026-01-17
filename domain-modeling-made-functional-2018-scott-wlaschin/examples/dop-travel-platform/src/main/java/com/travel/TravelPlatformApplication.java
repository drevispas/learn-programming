package com.travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DOP Travel Platform - 메인 애플리케이션 클래스
 *
 * <h2>목적 (Purpose)</h2>
 * Data-Oriented Programming 원칙을 적용한 여행/숙박/티케팅 플랫폼 예제
 *
 * <h2>학습 포인트 (Learning Points)</h2>
 * <ul>
 *   <li>Ch 1: DOP 4대 원칙 (데이터와 동작 분리)</li>
 *   <li>Ch 2: Value Objects, Wither 패턴</li>
 *   <li>Ch 3: Cardinality, Sum/Product Types</li>
 *   <li>Ch 4: 불가능한 상태 제거, Phantom Types</li>
 *   <li>Ch 5: Total Functions, Result 타입</li>
 *   <li>Ch 6: Functional Core / Imperative Shell</li>
 *   <li>Ch 7: 결합법칙, 멱등성, 항등원</li>
 *   <li>Ch 8: Rule Engine, Interpreter 패턴</li>
 *   <li>Ch 9: JPA Entity와 도메인 Record 공존</li>
 * </ul>
 *
 * @see <a href="../../docs/lectures/data-oriented-programming-java-lecture-v0.2.md">DOP 강의 교재</a>
 */
@SpringBootApplication
public class TravelPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelPlatformApplication.class, args);
    }
}
