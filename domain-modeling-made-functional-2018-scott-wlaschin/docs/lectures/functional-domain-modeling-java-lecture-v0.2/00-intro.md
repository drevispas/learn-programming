# Java로 정복하는 함수형 도메인 모델링 V2
## 참조: Domain Modeling Made Functional

**대상**: 백엔드 자바 개발자

**목표**: 컴파일 타임에 버그를 잡는 견고한 시스템 구축

**도구**: Java 25 (Record, Sealed Interface, Pattern Matching, Record Patterns)

---

## 목차

### Part I: 기초 - 도메인과 타입 시스템
- **Chapter 1**: DDD와 함수형 사고의 기초
- **Chapter 2**: 원시 타입의 저주 깨기 - Wrapped Object
- **Chapter 3**: 복합 타입 - AND/OR Types
- **Chapter 4**: 불가능한 상태 제거하기

### Part II: 심화 - 워크플로우와 에러 처리
- **Chapter 5**: 워크플로우를 함수 파이프라인으로
- **Chapter 6**: Railway Oriented Programming
- **Chapter 7**: 파이프라인 조립과 검증

### Part III: 아키텍처
- **Chapter 8**: 도메인과 외부 세계 분리
- **Chapter 9**: 함수형 아키텍처 패턴

### Part IV: 종합
- **Chapter 10**: 종합 프로젝트 - 이커머스 완전 정복

### 부록
- **Appendix A**: 흔한 실수 모음 (⚠️ 경고)
- **Appendix B**: Java 25 함수형 Cheat Sheet
- **Appendix C**: 심화 Q&A
- **Appendix D**: 전체 퀴즈 정답
- **Appendix E**: 컴파일 가능한 샘플 프로젝트

---

## 본 강의의 철학: DDD + 함수형의 조화

본 강의는 전통적인 DDD(Domain-Driven Design)의 핵심 철학을 유지하면서, 함수형 프로그래밍의 장점을 결합합니다.

#### 전통 DDD와 같은 점
- **Bounded Context**: 도메인을 독립적인 경계로 분리
- **Ubiquitous Language**: 도메인 전문가와 개발자가 같은 언어 사용
- **Aggregate**: 일관성 경계를 가진 엔티티 그룹
- **도메인 중심 설계**: 기술보다 비즈니스 로직이 중심

#### 전통 DDD와 다른 점

| 항목 | 전통 DDD (OOP) | 본 강의 (FP + DDD) |
|------|---------------|-------------------|
| **에러 처리** | Exception | `Result<S, F>` 타입 |
| **불변성** | Mutable Entity | Immutable Record |
| **상태 변경** | Setter 메서드 | Wither 패턴 (새 객체 반환) |
| **Null 처리** | Null 허용 | `Optional`, Sealed Type |

#### Behavior 배치 원칙: Rich Model + Domain Service 혼합

본 강의는 "Anemic Model vs Rich Model" 이분법을 따르지 않습니다.
**책임에 따라 적절한 위치에 배치**하는 것이 핵심입니다.

| 로직 종류 | 배치 위치 | 예시 |
|----------|----------|------|
| 단일 Entity 상태 전이 | Entity 내부 | `Order.pay()`, `Coupon.use()` |
| 단일 Entity 검증/조회 | Entity 내부 | `Order.canCancel()` |
| 여러 Entity 협력 로직 | Domain Service | `OrderDomainService.calculatePrice()` |
| 외부 의존성 필요 | Application Service | `PlaceOrderUseCase.execute()` |

**핵심 원칙**:
- Entity는 **자신의 상태 전이 책임**을 가짐 (Rich Model 요소)
- 복잡한 계산이나 **여러 Entity 협력**은 Domain Service로 분리
- **외부 의존성**(DB, API)이 필요한 로직은 Application Service로

#### Domain Service vs Application Service

| 구분 | Domain Service | Application Service |
|------|---------------|---------------------|
| **위치** | Domain Layer | Application Layer |
| **순수성** | Pure Function (Side Effect 없음) | Impure (Side Effect 있음) |
| **의존성** | 도메인 객체만 의존 | Repository, 외부 API 의존 |
| **테스트** | Mock 없이 단위 테스트 | Mock/Stub 필요 |
| **예시** | 가격 계산, 할인 적용, 유효성 판단 | DB 저장, 결제 API 호출, 이벤트 발행 |

```java
// Domain Service: 순수 함수 - 입력만으로 결과 결정
public class OrderDomainService {
    public PricedOrder calculatePrice(ValidatedOrder order, Coupon coupon) {
        Money subtotal = order.lines().stream()...;
        Money discount = coupon.calculateDiscount(subtotal);
        return new PricedOrder(...);  // 외부 호출 없음!
    }
}

// Application Service: 워크플로우 조율 - Side Effect 포함
public class PlaceOrderUseCase {
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
        ValidatedOrder validated = validate(cmd);
        PricedOrder priced = domainService.calculatePrice(validated, coupon);
        paymentGateway.charge(priced.totalAmount());  // 외부 API 호출
        orderRepository.save(order);                   // DB 저장
        return Result.success(new OrderPlaced(...));
    }
}
```

> 💡 이 접근법은 Scott Wlaschin의 "Domain Modeling Made Functional"과
> Vaughn Vernon의 "Implementing Domain-Driven Design"의 실용적 조합입니다.
