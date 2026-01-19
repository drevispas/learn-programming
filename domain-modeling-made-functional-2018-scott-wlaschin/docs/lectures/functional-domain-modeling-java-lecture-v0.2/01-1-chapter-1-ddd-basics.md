# Chapter 1: DDD와 함수형 사고의 기초

> Part I: 기초 - 도메인과 타입 시스템

---

### 학습 목표
1. Bounded Context의 개념과 필요성을 이해한다
2. Ubiquitous Language가 코드 품질에 미치는 영향을 설명할 수 있다
3. Immutability (불변성)의 장점을 이해하고 Java Record로 구현할 수 있다
4. Pure Function (순수 함수)의 본질을 파악한다
5. Event Storming을 통해 도메인을 탐험하는 방법을 익힌다

---

### 1.1 Bounded Context: 맥락이 왕이다

#### 🎯 WHY: 하나의 통합 모델은 왜 실패하는가?

많은 개발팀이 범하는 가장 큰 실수는 **"하나의 통합된 모델"**을 만들려는 것입니다. 예를 들어 `User`라는 클래스 하나에 로그인 정보, 배송지 주소, 쿠폰 보유량, 정산 계좌 정보를 모두 넣으려 합니다.

**코드 1.1**: Anti-pattern - God Class
```java
// ❌ 안티패턴: 모든 것을 담은 God Class
public class User {
    private Long id;
    private String username;
    private String passwordHash;        // 인증팀만 관심
    private String shippingAddress;     // 주문팀만 관심
    private List<String> couponCodes;   // 마케팅팀만 관심
    private String bankAccount;         // 정산팀만 관심
    private Double feeRate;             // 정산팀만 관심
    private Integer membershipLevel;    // 멤버십팀만 관심
    // ... 필드가 계속 늘어남
}
```

#### ❌ Anti-pattern: God Class

**왜 나쁜가?**
1. **결합도 폭발**: 인증 로직을 수정하려는데 정산 관련 테스트가 깨짐
2. **인지 부하**: 개발자가 50개 필드 중 자신에게 필요한 5개를 찾아야 함
3. **성능 저하**: 단순 로그인에도 불필요한 정산 정보까지 로드
4. **팀 간 충돌**: 여러 팀이 같은 클래스를 동시에 수정하려다 Git 충돌

**반박 예상 질문:**
> "클래스 하나로 관리하면 편하지 않나요?"

**답변:** 초기에는 편해 보이지만, 시스템이 성장하면서 변경 비용이 기하급수적으로 증가합니다. 각 팀이 독립적으로 개발하고 배포할 수 없게 되어 조직 전체의 생산성이 저하됩니다.

#### 💡 비유: 나라와 언어
> **Bounded Context는 나라와 같습니다.**
>
> "Football"이라는 단어를 생각해보세요:
> - **미국**에서는 타원형 공을 들고 뛰는 스포츠 (American Football)
> - **영국**에서는 둥근 공을 발로 차는 스포츠 (Soccer)
>
> 같은 단어지만 **맥락(나라)**에 따라 의미가 완전히 다릅니다.
> 두 나라 사람이 "Football 경기 보러 가자"고 약속하면 서로 다른 경기장에 도착할 것입니다.
>
> 소프트웨어도 마찬가지입니다. "User"라는 단어가 인증 컨텍스트에서는 "로그인할 수 있는 주체"를,
> 정산 컨텍스트에서는 "돈을 받을 수 있는 계좌 소유자"를 의미합니다.
> 이를 억지로 하나로 합치면 소통의 혼란이 발생합니다.

#### DDD의 핵심 원칙

**단어의 의미는 문맥(Context)에 따라 달라집니다.**

**표 1.1**: Bounded Context별 "사용자" 의미

| 컨텍스트 | "사용자"의 의미 | 핵심 관심사 |
|---------|---------------|-----------|
| 인증 (Auth) | AppUser | ID, Password, Role |
| 주문 (Order) | Customer | 배송지, 연락처, 주문 이력 |
| 정산 (Settlement) | Payee | 계좌번호, 사업자번호, 수수료율 |
| 마케팅 (Marketing) | Target | 구매 패턴, 선호 카테고리 |

이들을 억지로 합치지 말고, **서로 다른 패키지(Context)**로 분리해야 합니다.

#### 코드 예시: 컨텍스트별 모델 분리

**코드 1.2**: Bounded Context별 모델 분리
```java
// ============================================
// [Context: Auth] 인증을 위한 사용자 모델
// ============================================
package com.ecommerce.auth;

public record AppUser(
    long id,
    String username,
    String passwordHash,
    Set<Role> roles
) {}

public enum Role { BUYER, SELLER, ADMIN }

// ============================================
// [Context: Order] 주문을 위한 구매자 모델
// ============================================
package com.ecommerce.order;

public record Customer(
    CustomerId id,           // Auth의 id와 매핑되지만 타입이 다름
    ShippingAddress address,
    ContactInfo contact
) {}

// ============================================
// [Context: Settlement] 정산을 위한 판매자 모델
// ============================================
package com.ecommerce.settlement;

public record Payee(
    PayeeId id,
    BankAccount bankAccount,
    BusinessNumber businessNumber,
    FeeRate feeRate
) {}
```

**핵심 포인트**: 각 컨텍스트는 자신만의 언어(타입)를 가집니다. `Auth.AppUser`와 `Order.Customer`는 같은 사람을 가리킬 수 있지만, 각 컨텍스트에서 필요한 정보만 담고 있습니다.

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Bounded Context는 다음 프로젝트에서 검증되었습니다:
- Netflix, Amazon의 마이크로서비스 아키텍처
- Uber의 도메인 중심 설계
- 대부분의 성공적인 대규모 시스템

**Expert Opinions:**
- **Eric Evans** (DDD 창시자): "Bounded Context는 DDD의 핵심 패턴이다. 큰 모델을 분리하는 것이 복잡성을 관리하는 핵심이다."
- **Vaughn Vernon** (Implementing DDD 저자): "Bounded Context는 팀 자율성과 독립적 배포를 가능하게 하는 조직적 경계이기도 하다."

#### Bounded Context의 Java 투영

BC는 **개념적 경계**입니다. Java 프로젝트에서는 규모에 따라 다르게 구현할 수 있습니다:

**표 1.1b**: BC 구현 방식 비교

| 규모 | 구현 방식 | 예시 |
|-----|----------|------|
| 대규모 (마이크로서비스) | 별도 서비스 | `display-service`, `inventory-service` |
| 중규모 (모듈러 모놀리스) | Java 모듈 | `module display`, `module inventory` |
| 소규모 (단일 프로젝트) | 패키지 분리 | `com.ecommerce.display`, `com.ecommerce.inventory` |
| 소규모 (단순화) | **타입 분리만** | 같은 패키지 내 `DisplayProduct`, `InventoryProduct` |

> ⚠️ **교육용 코드의 단순화**
>
> 이 강의의 예제 코드는 학습 편의를 위해 **타입 분리만** 적용했습니다:
> ```java
> package com.ecommerce.domain.product;  // 같은 패키지
>
> public record DisplayProduct(...) {}     // 전시 맥락
> public record InventoryProduct(...) {}   // 재고 맥락
> public record SettlementProduct(...) {}  // 정산 맥락
> ```
>
> 실무에서 팀/서비스가 분리된다면 패키지나 모듈로 물리적 경계를 추가하세요:
> ```java
> package com.ecommerce.display;   // 전시팀 담당
> package com.ecommerce.inventory; // 물류팀 담당
> package com.ecommerce.settlement; // 정산팀 담당
> ```

**핵심**: BC의 본질은 **God Class 방지**입니다. 물리적 분리 여부보다 "각 맥락에 필요한 데이터만 포함하는 별도 타입"을 만드는 것이 더 중요합니다.

**참고 자료:**
- [Domain-Driven Design Reference](https://www.domainlanguage.com/ddd/reference/) - Eric Evans
- [Implementing Domain-Driven Design](https://vaughnvernon.com/) - Vaughn Vernon

---

### 1.2 Ubiquitous Language: 코드가 곧 문서다

#### 🎯 WHY: 개발자-기획자 간 "전화놀이" 효과 방지

DDD의 또 다른 핵심은 **Ubiquitous Language**입니다. 기획자(Domain Expert)와 개발자가 같은 용어를 사용해야 합니다.

**코드 1.3**: Ubiquitous Language 위반 vs 준수
```java
// ❌ 나쁜 예: 기획서와 코드의 용어가 다름
// 기획서: "쿠폰을 적용하면 할인된 금액이 계산됩니다"
public class Util {
    public static double calc(double a, String c) {
        // a가 뭐지? c가 뭐지?
        return a * 0.9;
    }
}

// ✅ 좋은 예: 기획서의 용어가 그대로 코드에 등장
public class CouponService {
    public DiscountedPrice applyCoupon(OriginalPrice price, Coupon coupon) {
        // 기획서를 읽은 사람이라면 이 코드를 바로 이해할 수 있음
        return coupon.applyTo(price);
    }
}
```

> 💡 `coupon.applyTo(price)`의 전체 구현은 `examples/functional-domain-modeling/` 프로젝트의 쿠폰 도메인에서 확인할 수 있습니다.

#### 💡 비유: 통역 없는 직접 대화
> **코드는 번역기가 되어서는 안 됩니다. 코드가 곧 문서가 되어야 합니다.**
>
> 기획자가 "회원 등급이 VIP면 무료 배송이에요"라고 말했을 때,
> 코드에서 `if (user.level == 3)`이라고 쓰면 나중에 아무도 3이 VIP인지 모릅니다.
>
> 대신 `if (member.grade() == MemberGrade.VIP)`라고 쓰면
> 기획자도 코드를 읽고 "맞아, 이게 내가 말한 거야"라고 확인할 수 있습니다.

#### 이커머스 도메인 용어 사전 예시

**표 1.2**: 이커머스 도메인 용어 사전

| 기획 용어 | 코드 타입 | 설명 |
|----------|---------|------|
| 회원 등급 | `MemberGrade` | BRONZE, SILVER, GOLD, VIP |
| 상품 가격 | `ProductPrice` | 0원 이상의 금액 |
| 할인 쿠폰 | `DiscountCoupon` | 정액/정률 할인 |
| 주문 상태 | `OrderStatus` | 미결제, 결제완료, 배송중, 배송완료 |
| 결제 수단 | `PaymentMethod` | 카드, 계좌이체, 포인트 |

---

### 1.3 Immutability와 Java Record

#### 🎯 WHY: 상태 추적 불가능 문제

함수형 프로그래밍의 핵심 원칙 중 하나는 **Immutability (불변성)**입니다. 데이터가 한번 생성되면 변경되지 않습니다.

**가변 객체의 문제점:**

**코드 1.4**: Anti-pattern - Mutable Object
```java
// ❌ 가변 객체: 언제 어디서 값이 바뀔지 모름
public class MutableOrder {
    private String status;
    private int amount;

    public void setStatus(String status) { this.status = status; }
    public void setAmount(int amount) { this.amount = amount; }
}

// 문제 상황
MutableOrder order = new MutableOrder();
order.setStatus("PAID");
order.setAmount(10000);

// ... 100줄의 코드 ...

processOrder(order);  // 이 시점에 order의 상태가 뭐지?
                      // 중간에 누가 바꿨을 수도 있음!

// 멀티스레드 환경에서는 더 심각
// Thread A: order.setStatus("CANCELLED");
// Thread B: order.setAmount(20000);
// 결과: 취소된 주문인데 금액이 변경됨 (일관성 파괴)
```

#### ❌ Anti-pattern: Mutable State

**왜 나쁜가?**
1. **상태 추적 불가**: 100줄 뒤에서 객체 상태가 무엇인지 확신할 수 없음
2. **동시성 버그**: 멀티스레드 환경에서 레이스 컨디션 발생
3. **디버깅 지옥**: "누가 이 값을 바꿨지?" 추적이 어려움

**실제 버그 사례:**
```java
// 2019년 모 쇼핑몰 장애
// 동시에 접근하는 여러 스레드가 같은 Order 객체의 상태를 변경
// 결과: 결제된 주문이 미결제로 표시되거나, 취소된 주문이 배송됨
```

#### 💡 비유: 공증된 계약서
> **불변 객체는 공증된 계약서와 같습니다.**
>
> 일반 문서(가변 객체)는 누군가 몰래 수정할 수 있습니다.
> "10만원을 지급한다"가 어느새 "100만원을 지급한다"로 바뀔 수 있죠.
>
> 하지만 공증된 계약서(불변 객체)는 한번 작성되면 수정이 불가능합니다.
> 내용을 바꾸려면 새로운 계약서를 작성해야 합니다.
> 이렇게 하면 "이 계약서는 처음부터 끝까지 동일한 내용"이라고 확신할 수 있습니다.
>
> 코드에서도 마찬가지입니다. 불변 객체를 받으면
> "이 객체는 내가 받은 그 순간부터 끝까지 동일하다"고 확신할 수 있습니다.

#### Java Record: 불변 객체의 최고의 도구

Java 14+부터 도입된 `record`는 불변 데이터 객체를 위한 최고의 도구입니다.

**기존 클래스 vs Record:**

**코드 1.5**: 기존 클래스 vs Record 비교
```java
// ❌ 기존 방식: 30줄의 보일러플레이트
public final class OrderAmount {
    private final BigDecimal value;
    public OrderAmount(BigDecimal value) { this.value = value; }
    public BigDecimal getValue() { return value; }
    // equals(), hashCode(), toString() 직접 구현 필요...
}

// ✅ Java Record: 한 줄로 동일한 기능
public record OrderAmount(BigDecimal value) {}
```

**Record의 특징:**
- 모든 필드는 `private final` (불변)
- `getter`, `equals`, `hashCode`, `toString` 자동 생성
- Setter는 없음 (값을 바꾸려면 새 객체 생성)

#### `withXxx` 메서드로 상태 변경 (Wither 패턴)

불변 Record에서 특정 필드만 변경하려면 모든 필드를 복사하여 새로운 객체를 생성해야 합니다. 이를 편리하게 하기 위해 `withXxx()` 메서드를 직접 구현하는 **Wither 패턴**을 사용합니다.

> 💡 **현황 공유: JEP 468 (Derived Record Creation)**
>
> 많은 기대를 모았던 `with` 표현식은 Java 25 LTS 버전에서도 아직 **기본 제공되지 않거나 문법이 확정되지 않았습니다**.
>
> ```java
> // ❌ [Java 25 기준 실패] 컴파일 에러 발생
> var updated = user with { age = 31; };
> ```
>
> 따라서 현재는 아래와 같이 수동으로 **Wither 메서드**를 작성하는 것이 가장 안전하고 표준적인 방법입니다.

**코드 1.6**: Wither 패턴 구현
```java
public record User(String name, int age, String email) {
    // 각 필드별 wither 메서드 - 새 객체를 반환
    public User withAge(int age) { return new User(this.name, age, this.email); }
    public User withName(String name) { return new User(name, this.age, this.email); }
}

// 사용 예시
User user1 = new User("Alice", 30, "alice@example.com");
User user2 = user1.withAge(31);                    // age만 변경
User user3 = user1.withName("Bob").withAge(25);    // 체이닝
```

**핵심 포인트**:
- **Immutability 유지**: `user1`은 절대 변하지 않습니다.
- **명시적 의도**: `withAge`라는 이름을 통해 "나이가 변경된 새로운 상태"임을 명확히 드러냅니다.
- **컴파일 타임 안전성**: 필드 이름이 바뀌면 `withXxx` 메서드에서 즉시 컴파일 에러가 발생하여 안전합니다.

> ⚠️ **흔한 실수**: "Entity는 상태가 변하니까 mutable이어야 하지 않나?"
>
> **아닙니다!** 함수형 DDD에서는 Entity조차도 불변으로 다룹니다.
> Entity의 상태가 변한다는 것은 "어제의 나와 다른 객체"를 만드는 것입니다.
> 하지만 **ID(주민등록번호)**가 같으니 같은 사람으로 취급합니다.
>
> ```java
> Person olderMe = youngMe.withAge(20);  // withAge() 메서드로 새 객체 생성
> // youngMe와 olderMe가 동시에 존재! 시간 여행 가능!
> ```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Immutability는 다음에서 핵심 원칙으로 사용됩니다:
- Clojure, Haskell, Scala의 기본 데이터 구조
- React의 상태 관리 (useState, Redux)
- Kafka, Event Sourcing 시스템

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "불변성은 'Make Illegal States Unrepresentable' 원칙의 기초다."
- **Martin Fowler**: "Value Objects는 가능하면 항상 사용하라. 불변성은 버그를 줄이는 가장 효과적인 방법 중 하나다."

---

### 1.4 Pure Function (순수 함수)

#### 🎯 WHY: Side Effect로 인한 예측 불가능

함수형 프로그래밍에서 함수는 **파이프(Pipe)**입니다.
- 입력(Input)이 들어가면
- 항상 똑같은 출력(Output)이 나옵니다.
- **Side Effect (부수 효과)**가 없어야 합니다.

**코드 1.7**: 순수 함수 vs 비순수 함수
```java
// ❌ 나쁜 예 (부수 효과 있음):
int globalCount = 0;

public int add(int a, int b) {
    globalCount++; // ⚠️ 범인! 외부 상태를 몰래 바꿈
    return a + b;
}

// ✅ 좋은 예 (순수 함수):
public int add(int a, int b) {
    return a + b; // 오직 입력만으로 결과를 만듦. 언제 실행해도 결과가 같음.
}
```

#### 순수 함수의 본질: Referential Transparency (참조 투명성)

> **"언제, 어디서, 누가 실행하든 입력이 같으면 결과가 무조건 같아야 한다.
> 그리고 그 외에는 아무 일도 일어나지 않아야 한다."**

예시: `add(2, 3) -> 5`
- 이 함수는 내일 실행하든, 100년 뒤에 실행하든, 우주 정거장에서 실행하든 항상 5입니다.
- 세상에 아무런 흔적을 남기지 않습니다.
- `add(2, 3)`을 그냥 숫자 `5`로 바꿔쳐도 프로그램은 똑같이 동작합니다.

> ⚠️ **흔한 실수**: `System.out.println("Hello")`도 부수 효과입니다!
>
> **왜?**
> - **세상을 바꿈**: 모니터(콘솔)라는 외부 세계의 상태를 변경했습니다. 픽셀이 바뀌었죠.
> - **결과가 보장 안 됨**: 모니터가 꺼져 있거나, 파이프가 깨지면 동작이 달라질 수 있습니다.
> - **대체 불가능**:
>   ```java
>   public int impure(int x) {
>       System.out.println("Firing Missile!"); // 부수 효과
>       return x + 1;
>   }
>   ```
>   위 코드에서 `impure(1)`을 결과값인 `2`로 바꿔버리면? 미사일은 발사되지 않습니다!

#### 순수 함수의 실질적 이점

**표 1.3**: 순수 함수의 이점

| 이점 | 설명 |
|------|------|
| **테스트의 천국** | Mock 없이 `assert(f(input) == expected)` 한 줄로 테스트 끝 |
| **Local Reasoning** | 버그 추적 시 함수 안만 보면 됨 |
| **동시성 안전** | 값을 바꾸지 않으므로 락(Lock) 불필요 |

---

### 1.5 도메인 탐험: Event Storming

#### 코딩보다 먼저 해야 할 일

도메인 모델링은 클래스 다이어그램을 그리는 것에서 시작하지 않습니다. **Event Storming**이라는 협업 워크숍을 통해 비즈니스 흐름을 파악하는 것이 먼저입니다.

**그림 1.1**: Event Storming 워크플로우

![Event Storming Process](https://corporate-assets.lucid.co/co/63567f11-f099-44cd-8c96-ec0650ec3c5b.png)
*출처: [8 Steps in the Event Storming Process - Lucidspark](https://lucid.co/blog/8-steps-in-the-event-storming-process)*

> 색깔별 포스트잇: **주황색** = Domain Event (과거형), **파란색** = Command,
> **노란색** = Aggregate, **분홍색** = 외부 시스템, **빨간색** = 문제점/핫스팟

**핵심 질문**: "우리 시스템에서 어떤 흥미로운 일이 발생합니까?"

#### Domain Event

Domain Event는 비즈니스적으로 의미 있는 사건을 **과거형**으로 기술합니다.

- **주문됨 (OrderPlaced)**
- **결제됨 (PaymentReceived)**
- **배송 시작됨 (ShippingStarted)**
- **배송 완료됨 (ItemDelivered)**

#### 워크플로우 발견

이벤트를 시간 순서대로 나열하면 자연스럽게 워크플로우가 드러납니다.

**코드 1.8**: 워크플로우 다이어그램
```
[PlaceOrderCommand] → 주문 프로세스 → [OrderPlaced]
                           ↓
[PayOrderCommand]  → 결제 프로세스 → [PaymentReceived]
                           ↓
[ShipOrderCommand] → 배송 프로세스 → [ShippingStarted]
```

이 흐름이 바로 우리가 구현할 파이프라인의 청사진이 됩니다. 각 단계(프로세스)는 입력을 받아 이벤트를 발생시키는 함수로 모델링할 수 있습니다.

---

### 퀴즈 Chapter 1

#### Q1.1 [개념 확인] Bounded Context

귀하의 이커머스 팀에서 `Product`(상품) 클래스를 설계 중입니다.
- **전시팀**: 상품의 이미지 URL, 마케팅 문구, 평균 별점이 중요
- **물류팀**: 상품의 무게, 부피, 창고 위치가 중요
- **정산팀**: 상품의 공급가, 수수료율, 정산 주기가 중요

이 상황에서 올바른 DDD 접근법은?

**A.** `Product` 클래스에 모든 필드를 넣고 `@Nullable`을 사용한다<br/>
**B.** `Product` 인터페이스를 만들고 `DisplayProduct`, `LogisticsProduct`가 상속받는다<br/>
**C.** `display.Product`, `logistics.Product`, `settlement.Product`를 각각 정의한다 *(정답)*<br/>
**D.** 데이터베이스 테이블을 먼저 설계하고 그에 맞춰 클래스를 하나 만든다

---

#### Q1.2 [개념 확인] Java Record

Java Record에 대한 설명으로 **틀린** 것은?

**A.** 모든 필드는 기본적으로 `private final`이다<br/>
**B.** `setter` 메서드가 자동으로 생성되어 값을 변경할 수 있다 *(정답)*<br/>
**C.** `equals`, `hashCode`, `toString`이 자동으로 생성된다<br/>
**D.** 데이터를 보유하는 불변 객체를 만드는 데 최적화되어 있다

---

#### Q1.3 [코드 분석] Immutability의 이점

다음 코드의 문제점은 무엇인가요?

**코드 1.9**: 가변 객체 문제 예시
```java
public class OrderService {
    public void processOrder(Order order) {
        validateOrder(order);
        calculateDiscount(order);
        saveToDatabase(order);
    }

    private void validateOrder(Order order) {
        if (order.getAmount() < 0) {
            order.setAmount(0);  // 음수면 0으로 수정
        }
    }

    private void calculateDiscount(Order order) {
        order.setAmount(order.getAmount() * 0.9);  // 10% 할인
    }
}
```

**A.** 메서드 이름이 불명확하다<br/>
**B.** 객체가 여러 곳에서 변경되어 상태 추적이 어렵다 *(정답)*<br/>
**C.** 예외 처리가 없다<br/>
**D.** 주석이 없다

---

#### Q1.4 [설계 문제] Ubiquitous Language

기획팀에서 다음과 같이 요구사항을 전달했습니다:

> "회원이 VIP 등급이면 모든 주문에 무료 배송을 적용해주세요"

이를 가장 잘 표현한 코드는?

**A.**
```java
if (user.level >= 4) {
    order.shipping = 0;
}
```

**B.**
```java
if (member.getGrade() == MemberGrade.VIP) {
    order = order.withShippingFee(ShippingFee.FREE);
}
```
*(정답)*

**C.**
```java
if (checkVIP(userId)) {
    updateShipping(orderId, 0);
}
```

**D.**
```java
if (data.get("grade").equals("vip")) {
    data.put("shipping", "0");
}
```

---

#### Q1.5 [버그 찾기] Context 혼용

다음 코드에서 Bounded Context 원칙을 위반한 부분은?

**코드 1.10**: Bounded Context 위반 예시
```java
package com.ecommerce.order;

public class OrderService {
    public void createOrder(Customer customer, List<Product> products) {
        Order order = new Order(customer, products);

        // 문제: 주문 서비스에서 인증 로직 직접 호출
        if (!customer.getPasswordHash().isEmpty()) {
            // 로그인된 사용자만 주문 가능
            orderRepository.save(order);
        }
    }
}
```

**A.** `Customer` 클래스가 Record가 아니다<br/>
**B.** 주문 컨텍스트에서 인증 정보(`passwordHash`)에 접근하고 있다 *(정답)*<br/>
**C.** 예외를 던지지 않고 있다<br/>
**D.** 트랜잭션 처리가 없다

---

정답은 Appendix D에서 확인할 수 있습니다.
