# Java 25로 정복하는 함수형 도메인 모델링 V2
## Domain Modeling Made Functional - 이커머스 실전 교재

**대상**: 이커머스 개발자 (회원, 상품, 주문, 결제, 쿠폰 도메인)

**목표**: 컴파일 타임에 버그를 잡는 견고한 시스템 구축

**도구**: Java 25 (Record, Sealed Interface, Pattern Matching, `with` 구문)

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

---

# Part I: 기초 - 도메인과 타입 시스템

---

## Chapter 1: DDD와 함수형 사고의 기초

### 학습 목표
1. Bounded Context의 개념과 필요성을 이해한다
2. Ubiquitous Language가 코드 품질에 미치는 영향을 설명할 수 있다
3. 불변성(Immutability)의 장점을 이해하고 Java Record로 구현할 수 있다
4. 순수 함수(Pure Function)의 본질을 파악한다
5. Event Storming을 통해 도메인을 탐험하는 방법을 익힌다

---

### 1.1 Bounded Context: 맥락이 왕이다

#### 🎯 WHY: 하나의 통합 모델은 왜 실패하는가?

많은 개발팀이 범하는 가장 큰 실수는 **"하나의 통합된 모델"**을 만들려는 것입니다. 예를 들어 `User`라는 클래스 하나에 로그인 정보, 배송지 주소, 쿠폰 보유량, 정산 계좌 정보를 모두 넣으려 합니다.

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

이 접근법의 문제점:
- **결합도 폭발**: 인증 로직을 수정하려는데 정산 관련 테스트가 깨짐
- **인지 부하**: 개발자가 50개 필드 중 자신에게 필요한 5개를 찾아야 함
- **성능 저하**: 단순 로그인에도 불필요한 정산 정보까지 로드
- **팀 간 충돌**: 여러 팀이 같은 클래스를 동시에 수정하려다 Git 충돌

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

| 컨텍스트 | "사용자"의 의미 | 핵심 관심사 |
|---------|---------------|-----------|
| 인증 (Auth) | AppUser | ID, Password, Role |
| 주문 (Order) | Customer | 배송지, 연락처, 주문 이력 |
| 정산 (Settlement) | Payee | 계좌번호, 사업자번호, 수수료율 |
| 마케팅 (Marketing) | Target | 구매 패턴, 선호 카테고리 |

이들을 억지로 합치지 말고, **서로 다른 패키지(Context)**로 분리해야 합니다.

#### 코드 예시: 컨텍스트별 모델 분리

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

---

### 1.2 Ubiquitous Language: 코드가 곧 문서다

#### 🎯 WHY: 개발자-기획자 간 "전화놀이" 효과 방지

DDD의 또 다른 핵심은 **유비쿼터스 언어(Ubiquitous Language)**입니다. 기획자(Domain Expert)와 개발자가 같은 용어를 사용해야 합니다.

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

#### 💡 비유: 통역 없는 직접 대화

> **코드는 번역기가 되어서는 안 됩니다. 코드가 곧 문서가 되어야 합니다.**
>
> 기획자가 "회원 등급이 VIP면 무료 배송이에요"라고 말했을 때,
> 코드에서 `if (user.level == 3)`이라고 쓰면 나중에 아무도 3이 VIP인지 모릅니다.
>
> 대신 `if (member.grade() == MemberGrade.VIP)`라고 쓰면
> 기획자도 코드를 읽고 "맞아, 이게 내가 말한 거야"라고 확인할 수 있습니다.

#### 이커머스 도메인 용어 사전 예시

| 기획 용어 | 코드 타입 | 설명 |
|----------|---------|------|
| 회원 등급 | `MemberGrade` | BRONZE, SILVER, GOLD, VIP |
| 상품 가격 | `ProductPrice` | 0원 이상의 금액 |
| 할인 쿠폰 | `DiscountCoupon` | 정액/정률 할인 |
| 주문 상태 | `OrderStatus` | 미결제, 결제완료, 배송중, 배송완료 |
| 결제 수단 | `PaymentMethod` | 카드, 계좌이체, 포인트 |

---

### 1.3 불변성(Immutability)과 Java Record

#### 🎯 WHY: 상태 추적 불가능 문제

함수형 프로그래밍의 핵심 원칙 중 하나는 **불변성(Immutability)**입니다. 데이터가 한번 생성되면 변경되지 않습니다.

**가변 객체의 문제점:**

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

```java
// ❌ 기존 방식: 보일러플레이트 코드가 많음
public final class OrderAmount {
    private final BigDecimal value;

    public OrderAmount(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderAmount that = (OrderAmount) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "OrderAmount[value=" + value + "]";
    }
}

// ✅ Java Record: 한 줄로 동일한 기능
public record OrderAmount(BigDecimal value) {}
```

**Record의 특징:**
- 모든 필드는 `private final` (불변)
- `getter`, `equals`, `hashCode`, `toString` 자동 생성
- Setter는 없음 (값을 바꾸려면 새 객체 생성)

#### `with` 구문으로 상태 변경 (Java 25)

Java 25의 **Derived Record Creation (JEP 468)** `with` 구문을 사용하면 불변성을 유지하면서도 직관적으로 "상태를 변경"할 수 있습니다.

```java
public record Order(OrderId id, OrderStatus status, Money total) {
    // 상태를 변경한 "새로운" 주문 객체 반환
    public Order withStatus(OrderStatus newStatus) {
        return this with { status = newStatus; };
    }
}

// 사용
Order unpaidOrder = new Order(orderId, OrderStatus.UNPAID, amount);
Order paidOrder = unpaidOrder with { status = OrderStatus.PAID; };

// unpaidOrder는 여전히 UNPAID (불변!)
// paidOrder는 새로운 객체로 PAID
```

> ⚠️ **흔한 실수**: "Entity는 상태가 변하니까 mutable이어야 하지 않나?"
>
> **아닙니다!** 함수형 DDD에서는 Entity조차도 불변으로 다룹니다.
> Entity의 상태가 변한다는 것은 "어제의 나와 다른 객체"를 만드는 것입니다.
> 하지만 **ID(주민등록번호)**가 같으니 같은 사람으로 취급합니다.
>
> ```java
> Person olderMe = youngMe with { age = 20; };
> // youngMe와 olderMe가 동시에 존재! 시간 여행 가능!
> ```

---

### 1.4 순수 함수(Pure Functions)

#### 🎯 WHY: 부수 효과로 인한 예측 불가능

함수형 프로그래밍에서 함수는 **파이프(Pipe)**입니다.
- 입력(Input)이 들어가면
- 항상 똑같은 출력(Output)이 나옵니다.
- **부수 효과(Side Effect)**가 없어야 합니다.

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

#### 순수 함수의 본질: 참조 투명성(Referential Transparency)

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

1. **테스트의 천국**: Mock 없이 `assert(f(input) == expected)` 한 줄로 테스트 끝
2. **Local Reasoning**: 버그 추적 시 함수 안만 보면 됨
3. **동시성 안전**: 값을 바꾸지 않으므로 락(Lock) 불필요

---

### 1.5 도메인 탐험: Event Storming

#### 코딩보다 먼저 해야 할 일

도메인 모델링은 클래스 다이어그램을 그리는 것에서 시작하지 않습니다. **이벤트 스토밍(Event Storming)**이라는 협업 워크숍을 통해 비즈니스 흐름을 파악하는 것이 먼저입니다.

**핵심 질문**: "우리 시스템에서 어떤 흥미로운 일이 발생합니까?"

#### 도메인 이벤트(Domain Event)

도메인 이벤트는 비즈니스적으로 의미 있는 사건을 **과거형**으로 기술합니다.

- **주문됨 (OrderPlaced)**
- **결제됨 (PaymentReceived)**
- **배송 시작됨 (ShippingStarted)**
- **배송 완료됨 (ItemDelivered)**

#### 워크플로우 발견

이벤트를 시간 순서대로 나열하면 자연스럽게 워크플로우가 드러납니다.

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

A. `Product` 클래스에 모든 필드를 넣고 `@Nullable`을 사용한다
B. `Product` 인터페이스를 만들고 `DisplayProduct`, `LogisticsProduct`가 상속받는다
C. `display.Product`, `logistics.Product`, `settlement.Product`를 각각 정의한다
D. 데이터베이스 테이블을 먼저 설계하고 그에 맞춰 클래스를 하나 만든다

---

#### Q1.2 [개념 확인] Java Record
Java Record에 대한 설명으로 **틀린** 것은?

A. 모든 필드는 기본적으로 `private final`이다
B. `setter` 메서드가 자동으로 생성되어 값을 변경할 수 있다
C. `equals`, `hashCode`, `toString`이 자동으로 생성된다
D. 데이터를 보유하는 불변 객체를 만드는 데 최적화되어 있다

---

#### Q1.3 [코드 분석] 불변성의 이점
다음 코드의 문제점은 무엇인가요?

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

A. 메서드 이름이 불명확하다
B. 객체가 여러 곳에서 변경되어 상태 추적이 어렵다
C. 예외 처리가 없다
D. 주석이 없다

---

#### Q1.4 [설계 문제] Ubiquitous Language
기획팀에서 다음과 같이 요구사항을 전달했습니다:

> "회원이 VIP 등급이면 모든 주문에 무료 배송을 적용해주세요"

이를 가장 잘 표현한 코드는?

A.
```java
if (user.level >= 4) {
    order.shipping = 0;
}
```

B.
```java
if (member.getGrade() == MemberGrade.VIP) {
    order = order.withShippingFee(ShippingFee.FREE);
}
```

C.
```java
if (checkVIP(userId)) {
    updateShipping(orderId, 0);
}
```

D.
```java
if (data.get("grade").equals("vip")) {
    data.put("shipping", "0");
}
```

---

#### Q1.5 [버그 찾기] Context 혼용
다음 코드에서 Bounded Context 원칙을 위반한 부분은?

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

A. `Customer` 클래스가 Record가 아니다
B. 주문 컨텍스트에서 인증 정보(`passwordHash`)에 접근하고 있다
C. 예외를 던지지 않고 있다
D. 트랜잭션 처리가 없다

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 2: 원시 타입의 저주 깨기 - Wrapped Object

### 학습 목표
1. Primitive Obsession 안티패턴을 인식하고 그 위험성을 설명할 수 있다
2. Value Object와 Entity의 차이를 이해한다
3. Compact Constructor를 활용해 생성 시점 검증을 구현할 수 있다
4. 도메인 타입으로 코드의 의도를 명확히 전달할 수 있다

---

### 🎯 [핵심 동기 예시] 왜 Wrapped Object가 필요한가?

```java
// ❌ 위험: 순서 바뀌어도 컴파일 성공
new Customer("홍길동", "서울시 강남구", "hong@test.com");
new Customer("홍길동", "hong@test.com", "서울시 강남구"); // 버그! 컴파일 OK

// ✅ 안전: 순서 바뀌면 컴파일 실패
new Customer(
    new CustomerName("홍길동"),
    new PostalAddress("서울시 강남구"),
    new EmailAddress("hong@test.com")
);
// PostalAddress 자리에 EmailAddress를 넣으면 컴파일 에러!
```

**컴파일러가 우리의 첫 번째 테스터가 됩니다.**

---

### 2.1 Primitive Obsession: 원시 타입 집착

#### 흔히 보는 위험한 코드

이커머스 코드에서 가장 흔히 보는 안티패턴입니다:

```java
// ❌ 위험한 코드: 모든 것이 String과 숫자
public class PaymentService {
    public void processPayment(
        String customerId,
        String email,
        String productId,
        double amount,
        String couponCode
    ) {
        // ...
    }
}

// 호출하는 쪽
paymentService.processPayment(
    "user@email.com",    // customerId 자리에 email을!
    "COUPON-2024",       // email 자리에 couponCode를!
    "12345",             // 이게 productId? customerId?
    -500.0,              // 음수 금액??
    null                 // 쿠폰 코드
);
```

컴파일러는 이 코드에서 아무 문제도 발견하지 못합니다. 모두 `String`이고 `double`이니까요.
하지만 런타임에 예상치 못한 버그가 발생합니다.

#### 💡 비유: 라벨 없는 약병

> **Primitive Obsession은 라벨 없는 약병과 같습니다.**
>
> 병원 약국에 하얀 가루가 든 투명한 병이 100개 있다고 상상해보세요.
> 어떤 병이 진통제이고, 어떤 병이 수면제이고, 어떤 병이 독약인지
> 라벨 없이는 알 수 없습니다.
>
> 약사가 "아, 세 번째 줄 다섯 번째가 진통제야"라고 기억에 의존하면
> 언젠가 반드시 사고가 납니다.
>
> 코드에서 `String email`, `String customerId`, `String couponCode`는
> 모두 "하얀 가루가 든 투명한 병"입니다.
> 컴파일러(약사)는 이들을 구분할 수 없습니다.

#### Primitive Obsession의 실제 피해 사례

```java
// 실제 발생할 수 있는 버그들

// 1. 파라미터 순서 실수
createOrder(productId, customerId);  // 순서가 바뀜, 컴파일 OK

// 2. 잘못된 값 할당
String orderId = email;  // 컴파일 OK, 런타임에 이상한 동작

// 3. 유효하지 않은 값
String email = "이것은 이메일이 아닙니다";  // 컴파일 OK

// 4. 의미 없는 연산
double totalPrice = quantity + price;  // 수량 + 가격? 컴파일 OK
```

---

### 2.2 Value Object vs Entity

#### 두 개념의 핵심 차이

| 구분 | Value Object (값 객체) | Entity (엔티티) |
|-----|----------------------|----------------|
| 동등성 | 값이 같으면 같은 것 | ID가 같으면 같은 것 |
| 불변성 | 항상 불변 | 상태가 변할 수 있음 (함수형에서는 불변으로 처리) |
| 수명 | 독립적 | 영속적 (DB에 저장) |
| 예시 | 금액, 이메일, 주소 | 주문, 회원, 상품 |

#### 💡 비유: 지폐와 은행 계좌

> **Value Object는 지폐, Entity는 은행 계좌입니다.**
>
> **지폐(Value Object)**:
> - 만원짜리 두 장이 있으면 "같은" 만원입니다
> - 지폐가 찢어져서 새 지폐로 바꿔도 가치는 동일합니다
> - 지폐에는 "이 지폐의 주인"이라는 개념이 없습니다
> - 지폐 자체를 수정할 수 없습니다 (불변)
>
> **은행 계좌(Entity)**:
> - 같은 100만원이 들어있어도 "내 계좌"와 "네 계좌"는 다릅니다
> - 계좌번호(ID)가 같으면 같은 계좌입니다
> - 계좌의 잔액은 변할 수 있습니다 (가변 → 함수형에서는 새 객체로)
> - 계좌는 "생성 → 사용 → 폐쇄"의 생명주기가 있습니다

#### 코드로 보는 차이

```java
// Value Object: 값이 같으면 동일
public record Money(BigDecimal amount, Currency currency) {}

Money money1 = new Money(BigDecimal.valueOf(10000), Currency.KRW);
Money money2 = new Money(BigDecimal.valueOf(10000), Currency.KRW);
money1.equals(money2);  // true - 값이 같으니까!

// Entity: ID가 같아야 동일
public record Order(OrderId id, Money totalAmount, OrderStatus status) {}

Order order1 = new Order(new OrderId("ORD-001"), money1, OrderStatus.UNPAID);
Order order2 = new Order(new OrderId("ORD-001"), money2, OrderStatus.PAID);
// order1과 order2는 같은 주문? -> ID가 같으니 "같은 주문"
// 하지만 상태가 다름 -> 시간에 따라 변한 것
```

---

### 2.3 Simple Types: 래퍼 타입 만들기

#### Compact Constructor로 생성 시점 검증

Java Record의 **Compact Constructor**를 사용하면 객체가 생성되는 순간 유효성을 검증할 수 있습니다.

```java
package com.ecommerce.domain.types;

// 1. 이메일 주소
public record EmailAddress(String value) {
    // Compact Constructor: 파라미터 괄호 없이 작성
    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("이메일은 비어있을 수 없습니다");
        }
        if (!value.contains("@")) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식: " + value);
        }
        // 검증 통과 후 this.value = value; 가 자동 실행됨
    }
}

// 2. 주문 금액 (0원 이상)
public record OrderAmount(BigDecimal value) {
    public OrderAmount {
        Objects.requireNonNull(value, "금액은 null일 수 없습니다");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("주문 금액은 음수가 될 수 없습니다: " + value);
        }
    }

    // 유용한 팩토리 메서드
    public static OrderAmount zero() {
        return new OrderAmount(BigDecimal.ZERO);
    }

    public static OrderAmount won(long amount) {
        return new OrderAmount(BigDecimal.valueOf(amount));
    }

    // 비즈니스 로직도 포함 가능
    public OrderAmount add(OrderAmount other) {
        return new OrderAmount(this.value.add(other.value));
    }

    public OrderAmount multiply(int quantity) {
        return new OrderAmount(this.value.multiply(BigDecimal.valueOf(quantity)));
    }
}

// 3. 수량 (1 이상)
public record Quantity(int value) {
    public Quantity {
        if (value < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }
}
```

> ⚠️ **흔한 실수**: `if (quantity < 0)` → 0도 막아야 합니다!
>
> ```java
> // ❌ 잘못된 검증: 0은 허용됨
> public record OrderQuantity(int quantity) {
>     public OrderQuantity {
>         if (quantity < 0 || quantity > 100) {  // 0은 통과!
>             throw new IllegalArgumentException("Invalid quantity");
>         }
>     }
> }
>
> // ✅ 올바른 검증: 1 이상이어야 함
> public record OrderQuantity(int quantity) {
>     public OrderQuantity {
>         if (quantity < 1 || quantity > 100) {  // 0도 막음!
>             throw new IllegalArgumentException("수량은 1개에서 100개 사이여야 합니다");
>         }
>     }
> }
> ```

#### 💡 비유: 공항 입국 심사

> **Compact Constructor는 공항 입국 심사와 같습니다.**
>
> 공항에서 입국하려면 반드시 입국 심사대를 통과해야 합니다.
> - 여권이 유효한지 확인
> - 비자가 있는지 확인
> - 금지 품목을 소지하고 있지 않은지 확인
>
> 심사를 통과하지 못하면 나라에 들어올 수 없습니다.
>
> Compact Constructor도 마찬가지입니다.
> 유효성 검사를 통과하지 못한 데이터는 우리 시스템(도메인)에 들어올 수 없습니다.
> 일단 입국(생성)이 허용되면, 그 이후로는 "이 데이터는 유효하다"고 확신할 수 있습니다.

---

### 2.4 도메인별 Simple Type 설계

```java
package com.ecommerce.domain.types;

import java.math.BigDecimal;
import java.util.regex.Pattern;

// === 공통 타입 ===

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("금액은 음수가 될 수 없습니다");
        }
    }

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.KRW);

    public static Money krw(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.KRW);
    }

    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화가 다릅니다");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

public enum Currency { KRW, USD, EUR }

// === 회원 도메인 ===

public record MemberId(long value) {
    public MemberId {
        if (value <= 0) throw new IllegalArgumentException("회원 ID는 양수여야 합니다");
    }
}

public record EmailAddress(String value) {
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public EmailAddress {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 이메일: " + value);
        }
    }
}

// === 상품 도메인 ===

public record ProductId(String value) {
    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("상품 ID는 비어있을 수 없습니다");
        }
    }
}

public record StockQuantity(int value) {
    public StockQuantity {
        if (value < 0) {
            throw new IllegalArgumentException("재고는 음수가 될 수 없습니다");
        }
    }

    public static StockQuantity zero() {
        return new StockQuantity(0);
    }

    public boolean isOutOfStock() {
        return value == 0;
    }

    public StockQuantity decrease(int amount) {
        return new StockQuantity(value - amount);
    }
}

// === 주문 도메인 ===

public record OrderId(String value) {
    public OrderId {
        if (value == null || !value.startsWith("ORD-")) {
            throw new IllegalArgumentException("주문 ID는 'ORD-'로 시작해야 합니다: " + value);
        }
    }

    public static OrderId generate() {
        return new OrderId("ORD-" + System.currentTimeMillis());
    }
}

public record OrderLineQuantity(int value) {
    public OrderLineQuantity {
        if (value < 1) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다");
        }
        if (value > 99) {
            throw new IllegalArgumentException("주문 수량은 99개를 초과할 수 없습니다");
        }
    }
}

// === 쿠폰 도메인 ===

public record CouponCode(String value) {
    private static final Pattern COUPON_PATTERN = Pattern.compile("^[A-Z0-9]{8,12}$");

    public CouponCode {
        if (value == null || !COUPON_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("쿠폰 코드는 8-12자리 영숫자여야 합니다: " + value);
        }
    }
}

public record DiscountRate(int value) {
    public DiscountRate {
        if (value < 1 || value > 100) {
            throw new IllegalArgumentException("할인율은 1-100 사이여야 합니다: " + value);
        }
    }

    public Money applyTo(Money original) {
        BigDecimal rate = BigDecimal.valueOf(100 - value).divide(BigDecimal.valueOf(100));
        return new Money(original.amount().multiply(rate), original.currency());
    }
}
```

---

### 퀴즈 Chapter 2

#### Q2.1 [개념 확인] Primitive Obsession
다음 중 Primitive Obsession의 문제점이 **아닌** 것은?

A. 컴파일러가 타입 실수를 잡아주지 못한다
B. 코드의 의도를 파악하기 어렵다
C. 메모리 사용량이 증가한다
D. 유효하지 않은 값이 시스템에 들어올 수 있다

---

#### Q2.2 [개념 확인] Value Object
Value Object의 특징으로 올바른 것은?

A. 식별자(ID)가 있어서 ID가 같으면 같은 객체다
B. 속성 값(Value)이 같으면 같은 객체로 취급된다
C. 내부의 값을 언제든지 변경할 수 있다
D. 보통 데이터베이스 테이블과 1:1로 매핑된다

---

#### Q2.3 [코드 작성] Compact Constructor
`CouponCode`라는 Value Object를 만들려고 합니다. 요구사항:
- 반드시 10자리 영문 대문자+숫자 조합
- null이면 안 됨

유효성 검사 로직이 들어가기에 가장 적합한 위치는?

A. `getCouponCode()` 메서드 내부
B. 별도의 `CouponCodeValidator` 클래스
C. 레코드의 Compact Constructor 내부
D. 데이터베이스에 저장하기 직전

---

#### Q2.4 [코드 분석] 도메인 타입 설계
다음 코드의 문제점은?

```java
public record OrderAmount(double value) {
    public OrderAmount {
        if (value < 0) {
            throw new IllegalArgumentException("음수 불가");
        }
    }
}
```

A. `double` 대신 `BigDecimal`을 사용해야 한다 (부동소수점 오차 문제)
B. 예외 메시지가 너무 짧다
C. Compact Constructor를 사용하면 안 된다
D. 문제없다

---

#### Q2.5 [설계 문제] 도메인 타입 식별
이커머스 시스템에서 다음 데이터를 Simple Type으로 만들 때, 가장 적절하지 **않은** 것은?

A. 배송 주소 → `ShippingAddress(String value)`
B. 할인율 → `DiscountRate(int percentage)`
C. 주문 상태 → `OrderStatus(String value)`
D. 상품 가격 → `ProductPrice(BigDecimal value)`

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 3: 복합 타입 - AND/OR Types

### 학습 목표
1. Product Type(AND)과 Sum Type(OR)의 개념을 이해한다
2. Java Record로 Product Type을 구현할 수 있다
3. Sealed Interface로 Sum Type을 구현할 수 있다
4. Pattern Matching for Switch를 활용해 모든 케이스를 안전하게 처리할 수 있다

---

### 3.1 타입 대수학: AND와 OR

#### 데이터 모델링의 두 가지 결합 방식

모든 복잡한 데이터 구조는 단 두 가지 방식의 조합으로 만들어집니다:

| 결합 방식 | 의미 | Java 도구 | 예시 |
|----------|-----|----------|------|
| **AND (Product Type)** | A **그리고** B | `record` | 주문 = 상품목록 AND 배송지 AND 결제정보 |
| **OR (Sum Type)** | A **또는** B | `sealed interface` | 결제수단 = 카드 OR 계좌이체 OR 포인트 |

#### 💡 비유: 햄버거 세트와 메뉴판

> **Product Type(AND)은 햄버거 세트와 같습니다.**
>
> 세트를 주문하면 버거 **AND** 감자튀김 **AND** 음료가 함께 옵니다.
> 세 가지가 모두 있어야 "세트"가 완성됩니다.
> 감자튀김 없이 세트라고 할 수 없습니다.
>
> ```java
> // Product Type: 모든 구성요소가 필요
> record HamburgerSet(Burger burger, Fries fries, Drink drink) {}
> ```

> **Sum Type(OR)은 선택 메뉴판과 같습니다.**
>
> "음료를 선택하세요: 콜라 **OR** 사이다 **OR** 커피"
> 하나만 선택해야 합니다. 동시에 두 개를 고를 수 없습니다.
>
> ```java
> // Sum Type: 하나만 선택
> sealed interface Drink permits Cola, Cider, Coffee {}
> ```

---

### 3.2 Product Type: Java Record

#### 주문 정보 모델링

이커머스의 주문(Order)은 전형적인 Product Type입니다:

```java
// 주문 = 주문ID AND 고객정보 AND 상품목록 AND 배송지 AND 결제정보
public record Order(
    OrderId id,
    Customer customer,
    List<OrderLine> orderLines,
    ShippingAddress shippingAddress,
    PaymentInfo paymentInfo
) {
    // Compact Constructor로 불변식(invariant) 검증
    public Order {
        Objects.requireNonNull(id, "주문 ID는 필수입니다");
        Objects.requireNonNull(customer, "고객 정보는 필수입니다");
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다");
        }
        // 방어적 복사: 외부에서 리스트를 수정해도 영향 없음
        orderLines = List.copyOf(orderLines);
    }

    // 총 금액 계산 (비즈니스 로직)
    public Money totalAmount() {
        return orderLines.stream()
            .map(OrderLine::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// 주문 상세 = 상품 AND 수량 AND 단가
public record OrderLine(
    Product product,
    Quantity quantity,
    Money unitPrice
) {
    public Money subtotal() {
        return unitPrice.multiply(quantity.value());
    }
}
```

---

### 3.3 Sum Type: Sealed Interface

#### 결제 수단 모델링

이커머스의 결제 수단은 전형적인 Sum Type입니다:

```java
package com.ecommerce.domain.payment;

// 결제 수단은 딱 4가지 중 하나!
// sealed: 허용된 구현체 외에는 상속 불가
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay {
}

// Case 1: 신용카드
public record CreditCard(
    CardNumber cardNumber,
    ExpiryDate expiryDate,
    Cvc cvc
) implements PaymentMethod {}

// Case 2: 계좌 이체
public record BankTransfer(
    BankCode bankCode,
    AccountNumber accountNumber,
    AccountHolder holderName
) implements PaymentMethod {}

// Case 3: 포인트 결제 (추가 정보 불필요)
public record Points() implements PaymentMethod {}

// Case 4: 간편 결제 (카카오페이, 네이버페이 등)
public record SimplePay(
    SimplePayProvider provider,
    String transactionToken
) implements PaymentMethod {}

public enum SimplePayProvider { KAKAO, NAVER, TOSS }
```

> ⚠️ **흔한 실수**: `implements` 빠뜨림
>
> Sealed Interface는 양방향 계약입니다. 부모가 `permits`로 자식을 지정했다면,
> 자식도 `implements`로 부모를 명시해야 합니다.
>
> ```java
> // ❌ 컴파일 에러!
> public sealed interface Discount permits PercentageOff, FixedAmountOff {}
> public record PercentageOff(int value) {}  // implements Discount 빠짐!
> public record FixedAmountOff(int value) {} // implements Discount 빠짐!
>
> // ✅ 올바른 코드
> public sealed interface Discount permits PercentageOff, FixedAmountOff {}
> public record PercentageOff(int percent) implements Discount {}
> public record FixedAmountOff(int amount) implements Discount {}
> ```

---

### 3.4 Pattern Matching for Switch

#### 모든 케이스 안전하게 처리하기

Java 21+의 Pattern Matching을 사용하면 컴파일러가 모든 케이스를 처리했는지 검증합니다:

```java
public class PaymentProcessor {

    public PaymentResult process(PaymentMethod method, Money amount) {
        // 컴파일러가 모든 케이스(4가지)를 처리했는지 확인!
        // 하나라도 빠뜨리면 컴파일 에러
        return switch (method) {
            case CreditCard card -> processCreditCard(card, amount);
            case BankTransfer transfer -> processBankTransfer(transfer, amount);
            case Points points -> processPoints(amount);
            case SimplePay pay -> processSimplePay(pay, amount);
            // default 불필요! sealed interface라서 4가지가 전부임
        };
    }

    private PaymentResult processCreditCard(CreditCard card, Money amount) {
        // 카드 결제 로직
        // card.cardNumber(), card.expiryDate() 등 타입 안전하게 접근
        return new PaymentResult.Success(generateReceiptNumber());
    }
    // ... 나머지 메서드들
}
```

> ⚠️ **흔한 실수**: `email.value` vs `email.value()` (메서드 호출!)
>
> Record의 필드 접근은 **메서드 호출**입니다. 괄호를 빠뜨리면 컴파일 에러!
>
> ```java
> public void printContact(ContactInfo contact) {
>     String s = switch (contact) {
>         case EmailOnly email -> email.value;   // ❌ 컴파일 에러!
>         case PostOnly post -> post.value();    // ✅ 올바름
>     };
> }
>
> // 올바른 코드
> public void printContact(ContactInfo contact) {
>     String s = switch (contact) {
>         case EmailOnly email -> email.value();  // ✅ 메서드 호출
>         case PostOnly post -> post.value();     // ✅ 메서드 호출
>     };
> }
> ```

#### Exhaustiveness Check (완전성 검사)

```java
// 새로운 결제 수단 추가
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay, Crypto {}  // Crypto 추가!

public record Crypto(WalletAddress address) implements PaymentMethod {}

// 이제 기존 코드에서 컴파일 에러 발생!
return switch (method) {
    case CreditCard card -> ...
    case BankTransfer transfer -> ...
    case Points points -> ...
    case SimplePay pay -> ...
    // 컴파일 에러: Crypto case가 없습니다!
};
```

**이것이 Sum Type의 핵심 이점입니다.** 새로운 케이스를 추가하면 처리하지 않은 모든 곳에서 컴파일 에러가 발생합니다.

---

### 3.5 이커머스 복합 타입 예시

#### 쿠폰 타입 모델링

```java
package com.ecommerce.domain.coupon;

// 쿠폰 종류: 정액 할인 OR 정률 할인 OR 무료 배송
public sealed interface CouponType
    permits FixedAmountDiscount, PercentageDiscount, FreeShipping {

    // 공통 메서드: 각 구현체가 자신만의 방식으로 계산
    Money calculateDiscount(Money originalPrice);
}

// Case 1: 정액 할인 (예: 5000원 할인)
public record FixedAmountDiscount(Money discountAmount) implements CouponType {
    public FixedAmountDiscount {
        if (discountAmount.isNegativeOrZero()) {
            throw new IllegalArgumentException("할인 금액은 양수여야 합니다");
        }
    }

    @Override
    public Money calculateDiscount(Money originalPrice) {
        // 원래 가격보다 할인이 크면 0원까지만 할인
        return originalPrice.isLessThan(discountAmount)
            ? originalPrice
            : discountAmount;
    }
}

// Case 2: 정률 할인 (예: 10% 할인)
public record PercentageDiscount(Percentage rate) implements CouponType {
    @Override
    public Money calculateDiscount(Money originalPrice) {
        return originalPrice.multiply(rate.value()).divide(100);
    }
}

// Case 3: 무료 배송 (할인 금액 = 배송비)
public record FreeShipping(Money shippingFee) implements CouponType {
    @Override
    public Money calculateDiscount(Money originalPrice) {
        return shippingFee;  // 배송비만큼 할인
    }
}
```

#### 주문 상태 모델링

```java
package com.ecommerce.domain.order;

// 주문 상태: 각 상태마다 필요한 데이터가 다름
public sealed interface OrderStatus
    permits Unpaid, Paid, Shipping, Delivered, Cancelled {
}

// 미결제: 결제 기한만 있음
public record Unpaid(
    LocalDateTime paymentDeadline
) implements OrderStatus {}

// 결제 완료: 결제 일시와 결제 정보 추가
public record Paid(
    LocalDateTime paidAt,
    PaymentMethod paymentMethod,
    String transactionId
) implements OrderStatus {}

// 배송 중: 운송장 번호와 배송 시작일 추가
public record Shipping(
    LocalDateTime paidAt,
    String trackingNumber,
    LocalDateTime shippedAt
) implements OrderStatus {}

// 배송 완료: 수령 확인 일시 추가
public record Delivered(
    LocalDateTime paidAt,
    String trackingNumber,
    LocalDateTime deliveredAt
) implements OrderStatus {}

// 취소됨: 취소 사유와 환불 정보
public record Cancelled(
    LocalDateTime cancelledAt,
    CancelReason reason,
    RefundInfo refundInfo  // null일 수 있음 (환불 전)
) implements OrderStatus {}

public enum CancelReason {
    CUSTOMER_REQUEST, OUT_OF_STOCK, PAYMENT_FAILED, FRAUD_SUSPECTED
}
```

#### 상태별 처리 로직

```java
public class OrderService {

    public String getOrderStatusMessage(Order order) {
        return switch (order.status()) {
            case Unpaid u ->
                "결제 대기 중입니다. 기한: " + u.paymentDeadline();

            case Paid p ->
                "결제가 완료되었습니다. (" + p.paidAt() + ")";

            case Shipping s ->
                "배송 중입니다. 운송장: " + s.trackingNumber();

            case Delivered d ->
                "배송이 완료되었습니다. (" + d.deliveredAt() + ")";

            case Cancelled c ->
                "주문이 취소되었습니다. 사유: " + c.reason();
        };
    }

    // Guard Pattern: 조건부 매칭
    public boolean canCancel(Order order) {
        return switch (order.status()) {
            case Unpaid u -> true;  // 미결제는 항상 취소 가능
            case Paid p when isWithin24Hours(p.paidAt()) -> true;  // 24시간 내 취소 가능
            case Paid p -> false;   // 24시간 이후 취소 불가
            case Shipping s -> false;  // 배송 시작 후 취소 불가
            case Delivered d -> false;
            case Cancelled c -> false;  // 이미 취소됨
        };
    }

    private boolean isWithin24Hours(LocalDateTime time) {
        return time.plusHours(24).isAfter(LocalDateTime.now());
    }
}
```

---

### 3.6 중첩된 Pattern Matching

```java
// 결제 결과도 Sum Type
public sealed interface PaymentResult
    permits PaymentResult.Success, PaymentResult.Failure {

    record Success(String receiptNumber, LocalDateTime processedAt) implements PaymentResult {}

    record Failure(PaymentError error) implements PaymentResult {}
}

public sealed interface PaymentError
    permits InsufficientFunds, CardExpired, InvalidAccount, SystemError {
}

record InsufficientFunds(Money required, Money available) implements PaymentError {}
record CardExpired(ExpiryDate expiryDate) implements PaymentError {}
record InvalidAccount(String reason) implements PaymentError {}
record SystemError(String errorCode, String message) implements PaymentError {}

// 중첩 패턴 매칭으로 상세 처리
public String handlePaymentResult(PaymentResult result) {
    return switch (result) {
        case PaymentResult.Success s ->
            "결제 성공! 영수증: " + s.receiptNumber();

        case PaymentResult.Failure(InsufficientFunds e) ->
            "잔액 부족: " + e.available() + " / " + e.required() + " 필요";

        case PaymentResult.Failure(CardExpired e) ->
            "카드 만료: " + e.expiryDate();

        case PaymentResult.Failure(InvalidAccount e) ->
            "잘못된 계좌: " + e.reason();

        case PaymentResult.Failure(SystemError e) ->
            "시스템 오류 [" + e.errorCode() + "]: " + e.message();
    };
}
```

---

### 3.7 Optional의 올바른 사용

#### Optional 안티패턴

```java
// ❌ Record 필드로 Optional 사용 금지
public record Order(OrderId id, Optional<Coupon> coupon) {}

// ✅ 별도 타입으로 분리
public sealed interface Order permits OrderWithCoupon, OrderWithoutCoupon {}

// ❌ 파라미터로 Optional 사용 금지
public void process(Optional<Coupon> coupon) {}

// ✅ 메서드 오버로딩
public void process(Coupon coupon) {}
public void processWithoutCoupon() {}

// ❌ Optional.get() 직접 호출 금지
customer.get();  // NoSuchElementException 가능!

// ✅ orElse, orElseThrow 사용
customer.orElseThrow(() -> new NotFoundException());
```

---

### 퀴즈 Chapter 3

#### Q3.1 [개념 확인] Product Type vs Sum Type
다음 중 **Product Type(AND)**으로 모델링하기 적합한 것은?

A. 결제 수단 (카드 또는 계좌이체 또는 포인트)
B. 배송 정보 (받는 사람, 주소, 연락처)
C. 주문 상태 (미결제 또는 결제완료 또는 배송중)
D. 쿠폰 종류 (정액 할인 또는 정률 할인)

---

#### Q3.2 [코드 분석] Sealed Interface
다음 코드에서 `permits` 절의 역할은?

```java
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points {}
```

A. `PaymentMethod`를 구현할 수 있는 클래스를 제한한다
B. `PaymentMethod`가 상속받을 인터페이스를 지정한다
C. `PaymentMethod`의 메서드 목록을 정의한다
D. `PaymentMethod`의 접근 제어자를 설정한다

---

#### Q3.3 [코드 분석] Pattern Matching 장점
Sealed Interface와 switch 문을 함께 쓸 때의 장점은?

A. default 문을 반드시 작성해야 한다
B. 모든 케이스를 다루지 않으면 컴파일 에러가 발생한다
C. 실행 속도가 10배 빨라진다
D. 메모리 사용량이 줄어든다

---

#### Q3.4 [설계 문제] 쿠폰 모델링
다음 요구사항을 모델링할 때 가장 적합한 방식은?

> "쿠폰은 정액 할인(5000원 할인), 정률 할인(10% 할인),
> 무료 배송 중 하나입니다."

A. `String couponType` 필드에 "FIXED", "PERCENT", "FREE_SHIPPING" 저장
B. `CouponType` enum에 세 가지 값 정의
C. `sealed interface CouponType permits FixedDiscount, PercentDiscount, FreeShipping`
D. 추상 클래스 `Coupon`을 만들고 세 가지 하위 클래스 생성

---

#### Q3.5 [버그 찾기] 불완전한 Pattern Matching
다음 코드의 문제점은?

```java
sealed interface OrderStatus permits Unpaid, Paid, Shipped {}

String getMessage(OrderStatus status) {
    return switch (status) {
        case Unpaid u -> "결제 대기";
        case Paid p -> "결제 완료";
        // Shipped 케이스 누락
    };
}
```

A. switch 문에 default가 없다
B. 컴파일 에러: Shipped 케이스가 처리되지 않음
C. 런타임에 NullPointerException 발생
D. 성능 문제가 있다

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 4: 불가능한 상태 제거하기

### 학습 목표
1. NULL의 문제점과 Optional의 올바른 사용법을 이해한다
2. State Machine 패턴으로 상태 전이를 안전하게 모델링할 수 있다
3. 타입 시스템으로 비즈니스 규칙을 컴파일 타임에 강제할 수 있다
4. "Make Illegal States Unrepresentable" 원칙을 실무에 적용할 수 있다

---

### 🎯 [핵심 동기 예시] 왜 상태별 Entity가 필요한가?

```java
// ❌ 위험: 순서 강제 안 됨
order.confirm();  // 검증 안 거쳤을 수도?
order.ship();     // 확정 안 됐을 수도?

// ✅ 안전: 타입으로 순서 강제
ValidatedOrder validated = validateOrder(raw);      // UnvalidatedOrder → ValidatedOrder
PricedOrder priced = priceOrder(validated);         // ValidatedOrder만 받음
PlacedOrder placed = acknowledgeOrder(priced);      // PricedOrder만 받음
// 검증 안 한 주문은 가격 계산 함수에 넣을 수 없음! 컴파일 에러!
```

**타입이 처리 순서를 강제합니다. 실수로 단계를 건너뛸 수 없습니다.**

---

### 4.1 NULL의 문제: 10억 달러짜리 실수

#### Tony Hoare의 고백

> "I call it my billion-dollar mistake. It was the invention of the null reference in 1965."
> - Tony Hoare (NULL을 발명한 컴퓨터 과학자)

NULL은 "값이 없음"을 표현하지만, 타입 시스템에서 이를 구분할 수 없습니다.

```java
// ❌ 위험한 코드: customer가 null일 수 있는지 시그니처만 봐서는 알 수 없음
public void sendEmail(Customer customer) {
    String email = customer.getEmail();  // NullPointerException 가능!
}
```

#### 💡 비유: 지뢰밭

> **NULL은 코드에 숨겨진 지뢰입니다.**
> 어디에 지뢰가 있는지 표시가 없기 때문에 모든 발걸음이 위험합니다.
> 결국 코드는 null 체크로 도배되고, 하나라도 빠뜨리면 런타임에 폭발합니다.

---

### 4.2 타입으로 비즈니스 규칙 표현

#### 💡 비유: USB 포트

> **타입 제약은 USB 포트입니다.**
> USB-C에 USB-A를 꽂으면 물리적으로 안 들어갑니다.
> `VerifiedEmail`을 요구하는 곳에 `UnverifiedEmail`을 넣으면 컴파일러가 거부합니다.

```java
public record UnverifiedEmail(String value) {}
public record VerifiedEmail(String value) {}

public class MemberService {
    // VerifiedEmail만 받음 - 인증 안 된 이메일로는 호출 불가!
    public Member completeRegistration(VerifiedEmail email, String name) {
        return new Member(MemberId.generate(), email, name);
    }
}
```

---

### 4.3 상태별 Entity 패턴 (xxxEntity)

#### 타입으로 상태 전이 강제하기

```java
public sealed interface OrderState
    permits UnpaidOrder, PaidOrder, ShippingOrder, DeliveredOrder {}

public record UnpaidOrder(OrderId id, Money total) implements OrderState {
    public PaidOrder pay(PaymentInfo payment) {
        return new PaidOrder(id, total, LocalDateTime.now(), payment);
    }
}

public record PaidOrder(OrderId id, Money total, LocalDateTime paidAt, PaymentInfo payment)
    implements OrderState {
    public ShippingOrder startShipping(TrackingNumber tracking) {
        return new ShippingOrder(id, paidAt, tracking, LocalDateTime.now());
    }
}

public record ShippingOrder(OrderId id, LocalDateTime paidAt,
    TrackingNumber tracking, LocalDateTime shippedAt) implements OrderState {
    public DeliveredOrder complete() {
        return new DeliveredOrder(id, paidAt, tracking, LocalDateTime.now());
    }
    // cancel() 메서드 없음 - 배송 중 취소 불가!
}
```

#### 컴파일러가 규칙 강제

```java
UnpaidOrder unpaid = new UnpaidOrder(...);
unpaid.startShipping(tracking);  // 컴파일 에러! 메서드 없음

ShippingOrder shipping = paid.startShipping(tracking);
shipping.cancel();  // 컴파일 에러! 메서드 없음
```

---

### 퀴즈 Chapter 4

#### Q4.1 [개념 확인] `ShippingOrder`에 `cancel()` 메서드가 없으면 어떤 효과가 있나요?

A. 예외가 발생한다
B. if문으로 체크한다
C. 호출 자체가 컴파일 에러가 된다
D. DB 트리거로 막는다

---

#### Q4.2 [설계 문제] "인증되지 않은 이메일로 주문 불가"를 타입으로 강제하려면?

A. 생성자에서 if문 체크
B. `createOrder(VerifiedEmail email, ...)` 시그니처 사용
C. @NotNull 어노테이션
D. 런타임 예외

---

정답은 Appendix D에서 확인할 수 있습니다.

---

# Part II: 심화 - 워크플로우와 에러 처리

---

## Chapter 5: 워크플로우를 함수 파이프라인으로

#### 🎯 WHY: 비즈니스 프로세스 = 함수

#### 💡 비유: 공장 조립 라인

> **비즈니스 프로세스는 공장 조립 라인입니다.**
> - **철판** → 프레스 → **차체**
> - **차체** → 도색 → **도색된 차체**
> - **도색된 차체** → 조립 → **완성차**

```
주문 프로세스:
  PlaceOrderCommand → [Validate] → ValidatedOrder
                           ↓
                      [Price] → PricedOrder
                           ↓
                      [Pay] → PaidOrder
                           ↓
                   OrderPlacedEvent
```

---

### 5.1 중간 타입으로 데이터 품질 표현

```java
// 1. 명령 (Command) - 사용자의 의도 (검증 전)
public record PlaceOrderCommand(String customerId, List<UnvalidatedOrderLine> lines) {}

// 2. 검증 후 - 유효한 상태
public record ValidatedOrder(CustomerId customerId, List<ValidatedOrderLine> lines) {}

// 3. 가격 계산 후
public record PricedOrder(CustomerId customerId, Money totalAmount) {}

// 4. 이벤트 - 확정된 과거
public record OrderPlacedEvent(OrderId orderId, Money totalAmount, LocalDateTime occurredAt) {}
```

> ⚠️ **흔한 실수**: Record 알맹이 꺼내기/포장 미흡
>
> ```java
> // ❌ raw.trim() - raw는 RawText, trim()은 String 메서드
> return raw.trim();
>
> // ✅ new SanitizedText(raw.s().trim())
> return new SanitizedText(raw.s().trim());
> ```

---

### 5.2 함수 합성

#### 💡 비유: 레고 블록

> 함수도 출력 타입 = 다음 입력 타입이면 조립됩니다.
> ```
> f: A → B
> g: B → C
> 합성: A → B → C
> ```

```java
public class PlaceOrderWorkflow {
    public Result<OrderPlacedEvent, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder.apply(command)
            .map(priceOrder::apply)
            .flatMap(processPayment::apply)
            .map(this::createOrderPlacedEvent);
    }
}
```

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] `PlaceOrderCommand`와 `ValidatedOrder`를 분리하는 이유는?

A. 메모리 절약
B. 검증 전후의 데이터가 다른 보장을 가지므로
C. Java 문법 제약
D. 디버깅 용이

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 6: Railway Oriented Programming

### 학습 목표
1. Exception의 문제점을 이해한다
2. Result 타입으로 성공/실패를 명시적으로 표현할 수 있다
3. map, flatMap 연산의 의미와 사용법을 익힌다
4. 에러를 값으로 다루는 함수형 에러 처리를 구현할 수 있다

---

### 6.1 Exception의 문제점

#### 💡 비유: 비상 탈출구 vs 두 갈래 길

> **Exception은 비상 탈출구입니다.**
>
> 비상 탈출구는 건물 어디서든 뛰어내릴 수 있습니다.
> 하지만 어디로 떨어질지, 누가 받아줄지 알 수 없습니다.
>
> Exception도 마찬가지입니다:
> - 어디서든 throw 가능
> - 어디서 catch될지 예측 불가
> - 중간 코드들을 모두 건너뜀
>
> **Result 타입은 두 갈래 길입니다.**
>
> 기차역에서 "성공행"과 "실패행" 두 플랫폼이 있습니다.
> 어떤 열차를 타든 목적지가 명확합니다.
> 중간에 건너뛰거나 예상치 못한 곳으로 가지 않습니다.

#### Exception의 구체적 문제점

```java
// 문제 1: 시그니처가 거짓말을 함
public Order createOrder(OrderRequest request) {
    // 시그니처: "항상 Order를 반환합니다"
    // 현실: 재고 부족, 잘못된 쿠폰 등으로 예외 발생 가능
    if (outOfStock) throw new OutOfStockException();
    if (invalidCoupon) throw new InvalidCouponException();
    return new Order(...);
}

// 문제 2: 어디서 catch될지 모름
try {
    Order order = createOrder(request);
    Payment payment = processPayment(order);  // 여기서도 예외 가능
    sendNotification(order);  // 여기서도 예외 가능
} catch (Exception e) {
    // 어떤 단계에서 실패했는지 알기 어려움
}

// 문제 3: 흐름 제어가 GOTO와 유사
void methodA() { methodB(); }
void methodB() { methodC(); }
void methodC() { throw new SomeException(); }  // 바로 catch로 점프!
```

---

### 6.2 Result 타입: 철도 분기점

#### 💡 비유: 철도 분기점

> **Result 타입은 철도 분기점입니다.**
>
> 기차가 분기점에 도착하면:
> - **성공**: 녹색 선로로 계속 진행
> - **실패**: 빨간 선로로 분기
>
> 한번 빨간 선로에 들어가면 녹색 역들을 모두 건너뜁니다.
> 하지만 선로를 벗어나지 않고 안전하게 종착역에 도착합니다.

#### Result 타입 정의

```java
package com.ecommerce.common;

// 성공(S) 또는 실패(F)를 담는 컨테이너
public sealed interface Result<S, F> permits Success, Failure {

    // 성공 여부 확인
    boolean isSuccess();
    boolean isFailure();

    // 값 추출 (사용 주의)
    S value();
    F error();

    // 팩토리 메서드
    static <S, F> Result<S, F> success(S value) {
        return new Success<>(value);
    }

    static <S, F> Result<S, F> failure(F error) {
        return new Failure<>(error);
    }
}

public record Success<S, F>(S value) implements Result<S, F> {
    @Override public boolean isSuccess() { return true; }
    @Override public boolean isFailure() { return false; }
    @Override public F error() { throw new IllegalStateException("Success has no error"); }
}

public record Failure<S, F>(F error) implements Result<S, F> {
    @Override public boolean isSuccess() { return false; }
    @Override public boolean isFailure() { return true; }
    @Override public S value() { throw new IllegalStateException("Failure has no value"); }
}
```

> ⚠️ **흔한 실수**: `new` 키워드 누락
>
> ```java
> // ❌ return Success<>(new User(...));
> // ✅ return new Success<>(new User(...));
> ```

---

### 6.3 map과 flatMap: 역 환승

#### 💡 비유: 역 환승

> **map은 같은 노선 내 이동입니다.**
>
> 성공 선로에서 값을 변환하지만 선로는 그대로입니다.
> `A → B` 형태의 순수 함수를 적용합니다.
>
> **flatMap은 환승입니다.**
>
> 다른 노선(다른 Result)으로 갈아탈 수 있습니다.
> `A → Result<B, E>` 형태의 함수를 적용합니다.

#### map 연산

```java
// map: 성공 값 변환 (실패면 그대로 통과)
public <NewS> Result<NewS, F> map(Function<S, NewS> mapper) {
    return switch (this) {
        case Success<S, F> s -> Result.success(mapper.apply(s.value()));
        case Failure<S, F> f -> (Result<NewS, F>) f;  // 실패는 그대로
    };
}

// 사용 예
Result<ValidatedOrder, OrderError> validated = validateOrder(input);
Result<PricedOrder, OrderError> priced = validated.map(order -> priceOrder(order));
// validateOrder가 실패하면 priceOrder는 실행되지 않음
```

#### flatMap 연산

```java
// flatMap: 결과가 Result인 함수 적용
public <NewS> Result<NewS, F> flatMap(Function<S, Result<NewS, F>> mapper) {
    return switch (this) {
        case Success<S, F> s -> mapper.apply(s.value());
        case Failure<S, F> f -> (Result<NewS, F>) f;
    };
}

// 사용 예: 각 단계가 실패할 수 있을 때
Result<PaidOrder, OrderError> result = validateOrder(input)
    .flatMap(this::checkInventory)      // 재고 확인 (실패 가능)
    .flatMap(this::processPayment);     // 결제 (실패 가능)
```

> ⚠️ **흔한 실수**: 타입 불일치
>
> ```java
> String r = switch (result) {
>     case Success<Integer> s -> s.value();  // ❌ Integer인데 String 기대
>     case Failure<Integer> f -> f.message();
> };
>
> // ✅ String.valueOf(s.value())로 변환 필요
> ```

---

### 6.4 ROP 패턴 적용

```java
public class PlaceOrderWorkflow {

    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder(command)
            .flatMap(this::checkInventory)
            .flatMap(this::applyCoupon)
            .flatMap(this::processPayment)
            .map(this::createEvent);  // 최종 변환은 map
    }

    // 각 단계가 Result 반환
    private Result<ValidatedOrder, OrderError> validateOrder(PlaceOrderCommand command) {
        if (command.lines().isEmpty()) {
            return Result.failure(new OrderError.EmptyOrder());
        }
        // ... 검증 로직
        return Result.success(new ValidatedOrder(...));
    }

    private Result<ValidatedOrder, OrderError> checkInventory(ValidatedOrder order) {
        for (var line : order.lines()) {
            if (inventoryService.getStock(line.productId()) < line.quantity()) {
                return Result.failure(new OrderError.OutOfStock(line.productId()));
            }
        }
        return Result.success(order);
    }

    private Result<PricedOrder, OrderError> applyCoupon(ValidatedOrder order) {
        return order.couponCode()
            .map(code -> couponService.validate(code))
            .orElse(Result.success(null))
            .map(coupon -> priceOrder(order, coupon));
    }

    private Result<PaidOrder, OrderError> processPayment(PricedOrder order) {
        return paymentGateway.charge(order.totalAmount())
            .mapError(e -> new OrderError.PaymentFailed(e.message()));
    }
}
```

---

### 6.5 에러 타입도 Sum Type으로

```java
// 도메인 에러를 sealed interface로 정의
public sealed interface OrderError permits
    OrderError.EmptyOrder,
    OrderError.InvalidCustomer,
    OrderError.OutOfStock,
    OrderError.InvalidCoupon,
    OrderError.PaymentFailed {

    record EmptyOrder() implements OrderError {}
    record InvalidCustomer(String customerId) implements OrderError {}
    record OutOfStock(ProductId productId) implements OrderError {}
    record InvalidCoupon(CouponCode code, String reason) implements OrderError {}
    record PaymentFailed(String reason) implements OrderError {}
}

// 에러 처리 - 모든 케이스 처리 강제
String handleError(OrderError error) {
    return switch (error) {
        case OrderError.EmptyOrder e -> "주문 상품이 없습니다";
        case OrderError.InvalidCustomer e -> "존재하지 않는 고객: " + e.customerId();
        case OrderError.OutOfStock e -> "품절된 상품: " + e.productId();
        case OrderError.InvalidCoupon e -> "쿠폰 오류: " + e.reason();
        case OrderError.PaymentFailed e -> "결제 실패: " + e.reason();
    };
}
```

---

### 퀴즈 Chapter 6

#### Q6.1 [개념 확인] Exception 문제
Exception의 문제점이 아닌 것은?

A. 함수 시그니처가 실패 가능성을 표현하지 못함
B. 흐름 제어가 GOTO와 유사함
C. 메모리를 많이 사용함
D. 어디서 catch될지 예측하기 어려움

---

#### Q6.2 [코드 분석] map vs flatMap
다음 중 `flatMap`을 사용해야 하는 경우는?

A. `String` → `UppercaseString` 변환
B. `ValidatedOrder` → `Result<PricedOrder, Error>` 변환
C. `Money` → `String` 변환
D. `List<Order>` → `Stream<Order>` 변환

---

#### Q6.3 [코드 분석] ROP 파이프라인
다음 코드에서 `checkInventory`가 실패하면?

```java
validateOrder(input)
    .flatMap(this::checkInventory)
    .flatMap(this::processPayment)
    .map(this::createEvent);
```

A. processPayment가 실행된다
B. createEvent가 실행된다
C. Failure가 반환되고 이후 단계는 실행되지 않는다
D. NullPointerException이 발생한다

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 7: 파이프라인 조립과 검증

### 학습 목표
1. Validation과 Result의 차이를 이해한다
2. Applicative 패턴으로 여러 검증을 병렬로 수행할 수 있다
3. Command와 Event 패턴을 이해한다
4. 실제 이커머스 검증 로직을 구현할 수 있다

---

### 7.1 Validation vs Result

#### 💡 비유: 시험 채점

> **Result는 첫 번째 오답에서 멈추는 채점입니다.**
>
> 10문제 중 3번에서 틀리면 바로 "불합격" 처리.
> 4번~10번이 맞는지 틀린지 알 수 없습니다.
>
> **Validation은 모든 문제를 채점합니다.**
>
> 10문제 모두 채점하고 "3번, 7번, 9번 오답" 리포트 제공.
> 학생이 자신의 모든 실수를 한번에 알 수 있습니다.

```java
// Result: 첫 에러에서 중단
Result<Order, Error> result = validateName(input)
    .flatMap(this::validateEmail)   // 이름 실패하면 여기 안 감
    .flatMap(this::validatePhone);  // 이메일 실패하면 여기 안 감

// Validation: 모든 에러 수집
Validation<Order, List<Error>> result = Validation.combine(
    validateName(input),
    validateEmail(input),
    validatePhone(input)
).map(Order::new);  // 모든 검증 결과를 모아서 처리
```

---

### 7.2 Applicative 패턴: 여러 창구 동시 처리

#### 💡 비유: 은행 여러 창구

> **Applicative는 여러 창구에서 동시에 처리하는 것입니다.**
>
> 대출 신청 시:
> - 1번 창구: 신분증 확인
> - 2번 창구: 재직 증명
> - 3번 창구: 소득 증명
>
> 순차 처리(flatMap): 1번 끝나야 2번 시작
> 병렬 처리(Applicative): 세 창구 동시 처리, 결과 모아서 판단

```java
// Validation 타입 정의
public sealed interface Validation<S, E> permits Valid, Invalid {
    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    static <S, E> Validation<S, List<E>> invalid(E error) {
        return new Invalid<>(List.of(error));
    }

    // 여러 Validation 결합
    static <A, B, C, E> Validation<C, List<E>> combine(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        BiFunction<A, B, C> combiner
    ) {
        return switch (va) {
            case Valid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> Validation.valid(combiner.apply(a.value(), b.value()));
                case Invalid<B, List<E>> b -> (Validation<C, List<E>>) b;
            };
            case Invalid<A, List<E>> a -> switch (vb) {
                case Valid<B, List<E>> b -> (Validation<C, List<E>>) a;
                case Invalid<B, List<E>> b -> {
                    List<E> errors = new ArrayList<>(a.errors());
                    errors.addAll(b.errors());
                    yield new Invalid<>(errors);
                }
            };
        };
    }
}

public record Valid<S, E>(S value) implements Validation<S, E> {}
public record Invalid<S, E>(List<E> errors) implements Validation<S, E> {}
```

---

### 7.3 이커머스 검증 예시

```java
public class OrderValidationService {

    public Validation<ValidatedOrder, List<ValidationError>> validateOrder(
        PlaceOrderCommand command
    ) {
        // 각 필드 독립적으로 검증
        var customerValidation = validateCustomerId(command.customerId());
        var linesValidation = validateOrderLines(command.lines());
        var addressValidation = validateAddress(command.shippingAddress());
        var couponValidation = validateCoupon(command.couponCode());

        // 모든 검증 결과 결합
        return Validation.combine4(
            customerValidation,
            linesValidation,
            addressValidation,
            couponValidation,
            ValidatedOrder::new
        );
    }

    private Validation<CustomerId, List<ValidationError>> validateCustomerId(String input) {
        if (input == null || input.isBlank()) {
            return Validation.invalid(new ValidationError.Required("customerId"));
        }
        try {
            return Validation.valid(new CustomerId(Long.parseLong(input)));
        } catch (NumberFormatException e) {
            return Validation.invalid(new ValidationError.InvalidFormat("customerId", "숫자"));
        }
    }

    private Validation<List<ValidatedOrderLine>, List<ValidationError>> validateOrderLines(
        List<UnvalidatedOrderLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalid(new ValidationError.Required("orderLines"));
        }

        List<ValidationError> errors = new ArrayList<>();
        List<ValidatedOrderLine> validatedLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var lineValidation = validateOrderLine(line, i);
            switch (lineValidation) {
                case Valid<ValidatedOrderLine, List<ValidationError>> v ->
                    validatedLines.add(v.value());
                case Invalid<ValidatedOrderLine, List<ValidationError>> inv ->
                    errors.addAll(inv.errors());
            }
        }

        return errors.isEmpty()
            ? Validation.valid(validatedLines)
            : new Invalid<>(errors);
    }
}

// 검증 에러 타입
public sealed interface ValidationError permits
    ValidationError.Required,
    ValidationError.InvalidFormat,
    ValidationError.TooLong,
    ValidationError.OutOfRange,
    ValidationError.NotFound {

    record Required(String field) implements ValidationError {}
    record InvalidFormat(String field, String expectedFormat) implements ValidationError {}
    record TooLong(String field, int maxLength) implements ValidationError {}
    record OutOfRange(String field, int min, int max) implements ValidationError {}
    record NotFound(String field, String value) implements ValidationError {}
}
```

---

### 7.4 Command와 Event

#### 💡 비유: 주문서와 영수증

> **Command는 주문서입니다.**
>
> "라떼 한 잔 주세요" - 요청하는 것
> 성공할지 실패할지는 아직 모릅니다.
>
> **Event는 영수증입니다.**
>
> "라떼 1잔 결제 완료" - 이미 일어난 사실
> 과거의 불변 기록입니다.

```java
// Command: "이렇게 해주세요" (미래, 실패 가능)
public sealed interface OrderCommand permits
    PlaceOrderCommand, CancelOrderCommand, ShipOrderCommand {

    record PlaceOrderCommand(
        String customerId,
        List<OrderLineCommand> lines,
        String shippingAddress,
        String paymentMethodId
    ) implements OrderCommand {}

    record CancelOrderCommand(
        OrderId orderId,
        String reason
    ) implements OrderCommand {}

    record ShipOrderCommand(
        OrderId orderId,
        String trackingNumber
    ) implements OrderCommand {}
}

// Event: "이렇게 됐습니다" (과거, 불변)
public sealed interface OrderEvent permits
    OrderPlaced, OrderCancelled, OrderShipped, OrderDelivered {

    record OrderPlaced(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderCancelled(
        OrderId orderId,
        String reason,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderShipped(
        OrderId orderId,
        String trackingNumber,
        LocalDateTime occurredAt
    ) implements OrderEvent {}

    record OrderDelivered(
        OrderId orderId,
        LocalDateTime occurredAt
    ) implements OrderEvent {}
}
```

---

### 퀴즈 Chapter 7

#### Q7.1 [개념 확인] Validation vs Result
Validation이 Result보다 적합한 경우는?

A. 파이프라인에서 첫 에러에 바로 중단하고 싶을 때
B. 폼 검증에서 모든 에러를 한번에 보여주고 싶을 때
C. 결제 처리처럼 순차적으로 진행해야 할 때
D. 단일 값 검증

---

#### Q7.2 [코드 분석] Applicative
Applicative 패턴의 특징이 아닌 것은?

A. 여러 검증을 독립적으로 수행
B. 모든 에러를 수집
C. 첫 에러에서 중단
D. 검증 결과를 결합

---

#### Q7.3 [설계 문제] Command vs Event
다음 중 Event의 특징은?

A. 미래에 수행할 작업을 나타냄
B. 실패할 수 있음
C. 이미 발생한 불변의 사실
D. 요청을 나타냄

---

#### Q7.4 [코드 분석] 검증 결합
다음 코드의 결과는?

```java
Validation.combine(
    Validation.invalid(new Error("이름 필수")),
    Validation.invalid(new Error("이메일 형식 오류")),
    (name, email) -> new User(name, email)
);
```

A. Valid(User)
B. Invalid([이름 필수])
C. Invalid([이메일 형식 오류])
D. Invalid([이름 필수, 이메일 형식 오류])

---

#### Q7.5 [설계 문제] 이벤트 네이밍
`OrderPlaced` 이벤트의 네이밍이 좋은 이유는?

A. 동사 원형을 사용해서
B. 과거형으로 "이미 일어난 일"을 표현해서
C. 명사를 사용해서
D. 짧아서

---

정답은 Appendix D에서 확인할 수 있습니다.

---

# Part III: 아키텍처

---

## Chapter 8: 도메인과 외부 세계 분리

### 학습 목표
1. Persistence Ignorance 원칙을 이해한다
2. DTO와 Domain 모델 분리의 필요성을 설명할 수 있다
3. Anti-Corruption Layer를 구현할 수 있다
4. 외부 시스템과의 경계를 명확히 정의할 수 있다

---

### 8.1 Persistence Ignorance

#### 💡 비유: VIP와 매니저

> **도메인 모델은 VIP입니다.**
> VIP는 자신의 전문 분야(비즈니스 로직)에만 집중합니다.
> "데이터가 어디에 저장되는지", "어떤 DB를 쓰는지" 신경 쓰지 않습니다.
>
> **Repository는 매니저입니다.**
> 매니저가 VIP의 일정, 이동, 숙소를 모두 관리합니다.
> VIP는 그냥 업무(비즈니스 로직)만 하면 됩니다.

```java
// 도메인 모델: DB를 전혀 모름 (JPA 어노테이션 없음!)
public record Order(
    OrderId id,
    CustomerId customerId,
    List<OrderLine> lines,
    Money totalAmount,
    OrderStatus status
) {
    // 순수한 비즈니스 로직만
    public Order cancel(CancelReason reason) {
        if (!(status instanceof Unpaid || status instanceof Paid)) {
            throw new IllegalStateException("취소 불가능한 상태");
        }
        return this with { status = new Cancelled(LocalDateTime.now(), reason); };
    }
}

// Repository 인터페이스: 도메인 레이어에 정의
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    Order save(Order order);
    void delete(OrderId id);
}

// Repository 구현: 인프라 레이어에 정의
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(mapper::toDomain);  // Entity → Domain 변환
    }
}
```

---

### 8.2 Trust Boundary와 DTO

#### 도메인 코어를 보호하라

**신뢰 경계**:
- **외부**: 신뢰할 수 없는 데이터 (JSON, String, Raw Data)
- **경계**: 유효성 검사 및 변환 (DTO -> Domain Object)
- **내부**: 신뢰할 수 있는 도메인 객체 (불변, 유효함)

```java
// DTO: 외부 통신용
public class OrderDto {
    public String orderId;
    public BigDecimal amount;
}

// 변환: DTO -> Domain (입력)
public Result<Order, Error> toDomain(OrderDto dto) {
    return OrderId.create(dto.orderId)
        .combine(Money.create(dto.amount))
        .map(Order::new);
}

// 변환: Domain -> DTO (출력)
public OrderDto toDto(Order order) {
    OrderDto dto = new OrderDto();
    dto.orderId = order.id().value();
    dto.amount = order.total().amount();
    return dto;
}
```

---

### 8.3 Anti-Corruption Layer

#### 💡 비유: 통역사

> **ACL은 통역사입니다.**
> 외국 손님(외부 시스템)이 오면 통역사가 번역합니다.
> 우리 팀(도메인)은 우리 언어(도메인 타입)만 사용합니다.

```java
// 외부 결제 API 응답
public record ExternalPaymentResponse(
    String result_code,      // "0000" = 성공
    String result_msg,
    String transaction_id
) {}

// ACL: 외부 형식 → 도메인 형식 변환
public class PaymentGatewayAdapter {
    public Result<PaymentApproval, PaymentError> processPayment(PaymentRequest request) {
        try {
            ExternalPaymentResponse response = externalClient.pay(request);
            return translateResponse(response);
        } catch (ExternalApiException e) {
            return Result.failure(new PaymentError.SystemError(e.getMessage()));
        }
    }

    private Result<PaymentApproval, PaymentError> translateResponse(
        ExternalPaymentResponse response
    ) {
        if (!"0000".equals(response.result_code())) {
            return Result.failure(translateErrorCode(response.result_code()));
        }
        return Result.success(new PaymentApproval(
            new TransactionId(response.transaction_id())
        ));
    }
}
```

---

### 8.4 toDomain() 위치: Controller가 변환 (문지기 역할)

서비스 메서드 시그니처가 **타입(Type) 그 자체로 문서**가 되어야 합니다.

```java
// [Controller] - 문지기 역할
@PostMapping("/users")
public Result<Void> registerUser(@RequestBody UserDTO dto) {
    // 1. 여기서 변환 및 1차 검증
    User user = UserMapper.toDomain(dto);

    // 2. 서비스에는 '순수한 도메인 객체'만 넘김
    return userService.register(user);
}

// [Service] - 비즈니스 로직 전담
public Result<Void> register(User user) {
    // 이미 'User' 타입이므로 이름이 비었거나 나이가 음수일 확률 0%
    // 중복 가입 여부 등 '비즈니스 로직'에만 집중!
    if (userRepository.exists(user.username())) {
        return new Failure<>("이미 존재하는 유저입니다.");
    }
    userRepository.save(user);
    return new Success<>(null);
}
```

---

### 퀴즈 Chapter 8

#### Q8.1 도메인 모델이 JPA 어노테이션을 직접 가지면 안 되는 이유는?

A. 성능이 느려져서
B. 도메인이 인프라(DB)에 의존하게 되어 결합도가 높아짐
C. 코드가 길어져서
D. 테스트가 어려워져서

---

#### Q8.2 [설계 문제] DTO 분리
Domain 모델과 API Response를 분리하는 이유가 아닌 것은?

A. 도메인 변경이 API에 영향주지 않도록
B. API 응답에 추가 정보(statusDescription 등) 포함 가능
C. 코드량을 줄이기 위해
D. 보안 민감 정보(password 등) 노출 방지

---

#### Q8.3 [코드 분석] ACL
Anti-Corruption Layer의 역할은?

A. 외부 시스템의 형식을 도메인 형식으로 변환
B. 데이터베이스 트랜잭션 관리
C. 로깅
D. 캐싱

---

#### Q8.4 [설계 문제] 경계 정의
외부 결제 API가 응답 형식을 바꾸면 수정해야 하는 곳은?

A. 도메인 모델
B. Anti-Corruption Layer (Adapter)
C. 컨트롤러
D. 모든 곳

---

#### Q8.5 [코드 분석] Mapper
OrderMapper.toDomain()의 역할은?

A. Domain → Entity 변환
B. Entity → Domain 변환
C. Domain → Response 변환
D. Request → Domain 변환

---

정답은 Appendix D에서 확인할 수 있습니다.

---

## Chapter 9: 함수형 아키텍처 패턴

### 학습 목표
1. Onion Architecture의 계층 구조를 이해한다
2. 부수효과를 도메인 로직과 분리하는 방법을 익힌다
3. 순수 함수의 테스트 용이성을 활용할 수 있다
4. 의존성 주입을 함수형 스타일로 적용할 수 있다

---

### 9.1 Onion Architecture

#### 💡 비유: 양파 껍질

> **아키텍처는 양파입니다.**
>
> - **가장 안쪽(Core)**: 도메인 모델 - 순수한 비즈니스 규칙
> - **중간층**: 애플리케이션 서비스 - 유스케이스 조율
> - **바깥층**: 인프라 - DB, 외부 API, 웹 프레임워크
>
> **의존성은 항상 안쪽으로 향합니다.** 도메인은 아무것도 의존하지 않습니다.

```
┌─────────────────────────────────────────────────────────┐
│                    Infrastructure                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Application Services               │   │
│  │  ┌───────────────────────────────────────────┐ │   │
│  │  │              Domain Model                 │ │   │
│  │  │  - Entities (Order, Customer)            │ │   │
│  │  │  - Value Objects (Money, Email)          │ │   │
│  │  │  - Domain Services                        │ │   │
│  │  └───────────────────────────────────────────┘ │   │
│  │  - Use Cases (PlaceOrder, CancelOrder)        │   │
│  │  - Repository Interfaces                       │   │
│  └─────────────────────────────────────────────────┘   │
│  - Controllers, JPA Repositories, External APIs       │
│  - Mappers, Adapters                                   │
└─────────────────────────────────────────────────────────┘
```

> **중요**: Repository Interface는 **domain 패키지**에 위치합니다! (DIP)
>
> Domain: "나는 저장을 하고 싶어. 하지만 DB가 Oracle인지 MySQL인지, 파일인지 알 바 아니야."
> → 인터페이스 정의 (OrderRepository)
>
> Infra: "도메인 주인님, 제가 그 인터페이스에 맞춰서 JPA로 구현해 왔습니다."
> → 구현체 (OrderRepositoryImpl)

---

### 9.2 함수형 의존성 주입 (Dependency Injection)

#### 인터페이스 없는 DI

전통적인 방식은 인터페이스를 만들고 `@Autowired`로 구현체를 주입받습니다. 함수형 프로그래밍에서는 **함수를 파라미터로 전달**하는 것만으로 충분합니다.

```java
// 의존성: 환율 계산 함수 (인터페이스가 아닌 함수형 인터페이스)
public interface GetExchangeRate {
    BigDecimal get(Currency from, Currency to);
}

// 비즈니스 로직
public class PriceService {
    // 의존성을 메서드 파라미터로 받음
    public Money convertPrice(Money price, Currency to, GetExchangeRate getRate) {
        BigDecimal rate = getRate.get(price.currency(), to);
        return new Money(price.amount().multiply(rate), to);
    }
}
```

#### 커링(Currying)과 부분 적용(Partial Application)

매번 의존성을 넘기는 것이 귀찮다면, 함수를 리턴하는 함수(고차 함수)를 사용해 의존성을 미리 주입(설정)해둘 수 있습니다.

```java
// 설정 단계 (Composition Root)
GetExchangeRate realExchangeRate = new RealExchangeRateApi();

// 의존성 주입: 함수를 부분 적용하여 새로운 함수 생성
Function<Money, Money> krwConverter =
    price -> priceService.convertPrice(price, Currency.KRW, realExchangeRate);

// 사용 단계: 의존성을 몰라도 됨
Money krw = krwConverter.apply(usd100);
```

---

### 9.3 부수효과 격리

#### 💡 비유: 회계사와 금고

> **순수 함수는 회계사입니다.**
>
> 회계사는 장부(입력)를 보고 계산(로직)만 합니다.
> 직접 금고(DB)를 열거나 돈을 옮기지 않습니다.
>
> **부수효과는 금고 관리인입니다.**
>
> 회계사의 지시(결과)에 따라
> 금고 관리인이 실제로 돈을 옮깁니다.

---

### 9.4 Functional Core, Imperative Shell

> "우리 팀은 Spring 쓰고 JPA 쓰는데요?" 100% 순수하게 짤 수는 없습니다.
> 현실적인 타협점: **Functional Core, Imperative Shell**

**Functional Core (순수 영역)**:
- 비즈니스 로직, 계산, 판단, 데이터 변환 등 **"머리 쓰는 일"**은 순수 함수로
- Mock 없이 빡세게 테스트

**Imperative Shell (불순 영역)**:
- DB 저장, API 호출, 로그 출력 등 **"몸 쓰는 일"**은 바깥쪽으로
- 로직이 거의 없게 만들어서 테스트 부담 감소

```java
// 순수한 도메인 로직 (부수효과 없음)
public class OrderDomainService {
    // 입력 → 출력, 외부 의존성 없음
    // Optional을 파라미터로 받지 않고 메서드 오버로딩 사용
    public PricedOrder calculatePrice(ValidatedOrder order) {
        return calculatePrice(order, Money.ZERO);
    }

    public PricedOrder calculatePrice(ValidatedOrder order, Coupon coupon) {
        Money subtotal = order.lines().stream()
            .map(line -> line.price().multiply(line.quantity()))
            .reduce(Money.ZERO, Money::add);

        Money discount = coupon.calculateDiscount(subtotal);

        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, discount, subtotal.subtract(discount));
    }

    // 내부에서만 사용하는 private 메서드
    private PricedOrder calculatePrice(ValidatedOrder order, Money discount) {
        Money subtotal = order.lines().stream()
            .map(line -> line.price().multiply(line.quantity()))
            .reduce(Money.ZERO, Money::add);

        return new PricedOrder(order.customerId(), order.lines(),
            subtotal, discount, subtotal.subtract(discount));
    }
}

// 부수효과를 가진 애플리케이션 서비스
public class PlaceOrderUseCase {
    private final OrderRepository orderRepository;      // 부수효과: DB
    private final PaymentGateway paymentGateway;        // 부수효과: 외부 API
    private final OrderDomainService domainService;     // 순수 로직

    @Transactional
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand cmd) {
        // 1. 부수효과: DB에서 데이터 조회
        Customer customer = customerRepository.findById(cmd.customerId()).orElseThrow();

        // 2. 순수 로직: 가격 계산 (테스트 쉬움)
        PricedOrder priced = domainService.calculatePrice(order, coupon);

        // 3. 부수효과: 결제
        PaymentResult payment = paymentGateway.charge(priced.totalAmount());

        // 4. 부수효과: 저장
        Order savedOrder = orderRepository.save(order);

        return Result.success(new OrderPlaced(savedOrder.id()));
    }
}
```

---

### 9.5 순수 함수와 테스트

#### 💡 비유: 계산기

> **순수 함수는 계산기입니다.**
> 같은 버튼을 누르면 항상 같은 결과. 언제 어디서 눌러도 1 + 1 = 2.
>
> 테스트하기 매우 쉽습니다:
> - 입력 준비
> - 함수 호출
> - 결과 확인
>
> DB 연결, 네트워크, 시간 등 외부 요소 불필요.

```java
class OrderDomainServiceTest {
    private final OrderDomainService service = new OrderDomainService();

    @Test
    void calculatePrice_withCoupon_appliesDiscount() {
        // Given: 순수한 입력 데이터
        var order = new ValidatedOrder(
            new CustomerId(1L),
            List.of(new ValidatedOrderLine(productId, quantity(2), Money.krw(10000)))
        );
        var coupon = new PercentageCoupon(10);  // 10% 할인

        // When: 순수 함수 호출
        PricedOrder result = service.calculatePrice(order, coupon);

        // Then: 결과 검증 (외부 의존성 없음!)
        assertThat(result.subtotal()).isEqualTo(Money.krw(20000));
        assertThat(result.discount()).isEqualTo(Money.krw(2000));
        assertThat(result.totalAmount()).isEqualTo(Money.krw(18000));
    }

    @Test
    void canCancel_unpaidOrder_returnsTrue() {
        var order = new Order(orderId, customerId, lines, new Unpaid());
        assertThat(service.canCancel(order)).isTrue();
    }

    @Test
    void canCancel_shippingOrder_returnsFalse() {
        var order = new Order(orderId, customerId, lines, new Shipping(tracking));
        assertThat(service.canCancel(order)).isFalse();
    }
}
```

---

### 퀴즈 Chapter 9

#### Q9.1 [개념 확인] Onion Architecture
Onion Architecture에서 의존성 방향은?

A. 안쪽 → 바깥쪽 (Domain → Infrastructure)
B. 바깥쪽 → 안쪽 (Infrastructure → Domain)
C. 양방향
D. 의존성 없음

---

#### Q9.2 [설계 문제] 부수효과
다음 중 부수효과가 있는 작업은?

A. 주문 금액 계산
B. 할인율 적용
C. 데이터베이스에 저장
D. 취소 가능 여부 판단

---

#### Q9.3 [코드 분석] 순수 함수
순수 함수의 특징이 아닌 것은?

A. 같은 입력에 항상 같은 출력
B. 외부 상태를 변경하지 않음
C. 데이터베이스를 조회함
D. 테스트하기 쉬움

---

#### Q9.4 [설계 문제] 계층 분리
OrderRepository 인터페이스는 어느 계층에 정의해야 하나요?

A. Infrastructure
B. Domain
C. Application
D. Presentation

---

#### Q9.5 [코드 분석] 테스트 용이성
순수한 도메인 로직을 분리하면 테스트가 쉬워지는 이유는?

A. 코드가 짧아져서
B. DB 연결, Mock 없이 입력/출력만으로 테스트 가능
C. 실행 속도가 빨라서
D. IDE 지원이 좋아서

---

정답은 Appendix D에서 확인할 수 있습니다.

---

# Part IV: 종합

---

## Chapter 10: 종합 프로젝트 - 이커머스 완전 정복

### 학습 목표
1. 5개 이커머스 도메인(회원, 상품, 주문, 결제, 쿠폰)을 함수형으로 모델링할 수 있다
2. 도메인 간 상호작용을 타입 안전하게 설계할 수 있다
3. 전체 주문 워크플로우를 파이프라인으로 구현할 수 있다
4. 학습한 모든 패턴을 실제 코드에 적용할 수 있다

---

### 10.1 회원 도메인

```java
package com.ecommerce.domain.member;

// === 회원 등급 (Sum Type) ===
public sealed interface MemberGrade permits Bronze, Silver, Gold, Vip {
    int discountRate();
    boolean hasFreeShipping();
}

public record Bronze() implements MemberGrade {
    @Override public int discountRate() { return 0; }
    @Override public boolean hasFreeShipping() { return false; }
}

public record Vip() implements MemberGrade {
    @Override public int discountRate() { return 10; }
    @Override public boolean hasFreeShipping() { return true; }
}

// === 이메일 인증 상태 (State Machine) ===
public sealed interface EmailVerification permits UnverifiedEmail, VerifiedEmail {}

public record UnverifiedEmail(String email, String code, LocalDateTime expiresAt)
    implements EmailVerification {
    public Result<VerifiedEmail, EmailError> verify(String inputCode) {
        if (LocalDateTime.now().isAfter(expiresAt))
            return Result.failure(new EmailError.Expired());
        if (!code.equals(inputCode))
            return Result.failure(new EmailError.InvalidCode());
        return Result.success(new VerifiedEmail(email, LocalDateTime.now()));
    }
}

public record VerifiedEmail(String email, LocalDateTime verifiedAt) implements EmailVerification {}

// === 회원 ===
public record Member(
    MemberId id, String name, EmailVerification email,
    MemberGrade grade, Points points
) {
    public Member upgradeGrade(MemberGrade newGrade) {
        return new Member(id, name, email, newGrade, points);
    }
}
```

---

### 10.2 상품 도메인

```java
package com.ecommerce.domain.product;

// === 컨텍스트별 상품 모델 ===

// 전시용 (Display Context)
public record DisplayProduct(ProductId id, ProductName name, List<String> imageUrls, Money price) {}

// 재고용 (Inventory Context)
public record InventoryProduct(ProductId id, StockQuantity stock, WarehouseLocation location) {
    public boolean isAvailable(int qty) { return stock.value() >= qty; }
}

// 정산용 (Settlement Context)
public record SettlementProduct(ProductId id, SellerId sellerId, Money supplyPrice, FeeRate feeRate) {}

// === 상품 상태 (Sum Type) ===
public sealed interface ProductStatus permits Draft, OnSale, SoldOut, Discontinued {
    record Draft() implements ProductStatus {}
    record OnSale(LocalDateTime listedAt) implements ProductStatus {}
    record SoldOut(LocalDateTime soldOutAt) implements ProductStatus {}
    record Discontinued(LocalDateTime at, String reason) implements ProductStatus {}
}
```

---

### 10.3 주문 도메인

```java
package com.ecommerce.domain.order;

// === 주문 상태 (State Machine) ===
public sealed interface OrderStatus
    permits Unpaid, Paid, Shipping, Delivered, Cancelled {
    record Unpaid(LocalDateTime deadline) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, TransactionId txId) implements OrderStatus {}
    record Shipping(LocalDateTime paidAt, TrackingNumber tracking) implements OrderStatus {}
    record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
    record Cancelled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}

// === 주문 ===
public record Order(
    OrderId id, MemberId memberId, List<OrderLine> lines,
    Money total, OrderStatus status
) {
    public Order pay(TransactionId txId) {
        if (!(status instanceof Unpaid))
            throw new IllegalStateException("미결제만 결제 가능");
        return new Order(id, memberId, lines, total,
            new Paid(LocalDateTime.now(), txId));
    }

    public Order ship(TrackingNumber tracking) {
        if (!(status instanceof Paid p))
            throw new IllegalStateException("결제완료만 배송 가능");
        return new Order(id, memberId, lines, total,
            new Shipping(p.paidAt(), tracking));
    }

    public boolean canCancel() {
        return switch (status) {
            case Unpaid u -> true;
            case Paid p -> p.paidAt().plusHours(24).isAfter(LocalDateTime.now());
            case Shipping s -> false;
            case Delivered d -> false;
            case Cancelled c -> false;
        };
    }
}
```

---

### 10.4 결제 도메인

```java
package com.ecommerce.domain.payment;

// === 결제 수단 (Sum Type) ===
public sealed interface PaymentMethod permits CreditCard, BankTransfer, Points, SimplePay {
    record CreditCard(CardNumber num, ExpiryDate exp) implements PaymentMethod {}
    record BankTransfer(BankCode bank, AccountNumber acc) implements PaymentMethod {}
    record Points(int amount) implements PaymentMethod {}
    record SimplePay(Provider provider, String token) implements PaymentMethod {}
}

// === 결제 결과 (Sum Type) ===
public sealed interface PaymentResult permits Success, Failure {
    record Success(TransactionId txId, Money amount) implements PaymentResult {}
    record Failure(PaymentError error) implements PaymentResult {}
}

public enum Provider { KAKAO, NAVER, TOSS }

// === 결제 에러 (Sum Type) ===
public sealed interface PaymentError
    permits InsufficientFunds, CardExpired, SystemError {
    record InsufficientFunds(Money required) implements PaymentError {}
    record CardExpired(ExpiryDate exp) implements PaymentError {}
    record SystemError(String msg) implements PaymentError {}
}
```

---

### 10.5 쿠폰 도메인

```java
package com.ecommerce.domain.coupon;

// === 쿠폰 종류 (Sum Type) ===
public sealed interface CouponType permits FixedAmount, Percentage, FreeShipping {
    Money calculateDiscount(Money orderAmount);

    record FixedAmount(Money amount) implements CouponType {
        public Money calculateDiscount(Money order) {
            return order.isLessThan(amount) ? order : amount;
        }
    }

    record Percentage(int rate, Money max) implements CouponType {
        public Money calculateDiscount(Money order) {
            Money calc = order.multiply(rate).divide(100);
            return calc.isGreaterThan(max) ? max : calc;
        }
    }

    record FreeShipping(Money fee) implements CouponType {
        public Money calculateDiscount(Money order) { return fee; }
    }
}

// === 쿠폰 상태 (State Machine) ===
public sealed interface CouponStatus permits Issued, Used, Expired {
    record Issued(LocalDateTime expiresAt) implements CouponStatus {
        boolean isValid() { return LocalDateTime.now().isBefore(expiresAt); }
    }
    record Used(LocalDateTime usedAt, OrderId orderId) implements CouponStatus {}
    record Expired(LocalDateTime at) implements CouponStatus {}
}

// === 쿠폰 ===
public record Coupon(CouponId id, CouponType type, CouponStatus status, Money minOrder) {
    public Result<UsedCoupon, CouponError> use(OrderId orderId, Money orderAmount) {
        if (!(status instanceof Issued i) || !i.isValid())
            return Result.failure(new CouponError.NotAvailable());
        if (orderAmount.isLessThan(minOrder))
            return Result.failure(new CouponError.MinNotMet(minOrder));
        Money discount = type.calculateDiscount(orderAmount);
        return Result.success(new UsedCoupon(
            new Coupon(id, type, new Used(LocalDateTime.now(), orderId), minOrder),
            discount
        ));
    }
}
```

---

### 퀴즈 Chapter 10

#### Q10.1 [설계 문제] VIP 무료배송을 타입으로 표현하는 방법은?
A. boolean 필드
B. sealed interface의 메서드로 정의
C. 별도 서비스 클래스
D. if문으로 처리

---

#### Q10.2 [개념 확인] 컨텍스트별 상품 분리(DisplayProduct, InventoryProduct, SettlementProduct) 이유는?
A. 코드량 증가를 위해
B. 각 컨텍스트에 필요한 데이터만 포함하기 위해
C. 상속을 회피하기 위해
D. 성능 향상을 위해

---

#### Q10.3 [코드 분석] Shipping 상태에 cancel() 메서드가 없는 이유는?
A. 메모리 절약
B. 타입으로 비즈니스 규칙(배송 중 취소 불가)을 강제하기 위해
C. 코드 단순화
D. 성능 향상

---

#### Q10.4 [개념 확인] 결제 수단을 Sum Type(sealed interface)으로 표현하는 이점은?
A. 메모리 절약
B. 모든 결제 수단을 타입 안전하게 처리 가능
C. 실행 속도 향상
D. 코드 가독성 저하

---

#### Q10.5 [코드 분석] 쿠폰의 use()가 Result를 반환하는 이유는?
A. 코드 복잡성 증가
B. 쿠폰 사용이 실패할 수 있는 경우를 명시적으로 표현
C. 컨벤션
D. 성능 향상

---

정답은 Appendix D에서 확인할 수 있습니다.

---

# 부록

---

## Appendix A: 흔한 실수 모음 (⚠️ 경고)

| 실수 | 올바른 코드 | 참고 |
|------|------------|------|
| `quantity < 0` 검증 | `quantity < 1` (0도 막아야) | Ch.2 Compact Constructor |
| `implements` 누락 | `record X() implements Y {}` | Ch.3 Sealed Interface |
| `value` vs `value()` | Record는 메서드 호출 필요 | Ch.3 Pattern Matching |
| 타입 캐스팅 누락 | `(long)(price * rate)` | Ch.2 금액 계산 |
| `new` 키워드 누락 | `new Success<>(...)` | Ch.6 Result 타입 |
| `raw.trim()` | `new SanitizedText(raw.s().trim())` | Ch.5 파이프라인 |

---

## Appendix B: Java 25 함수형 Cheat Sheet

### Record (불변 데이터)

```java
// 기본 Record
public record Money(BigDecimal amount) {
    public Money {
        if (amount.signum() < 0) throw new IllegalArgumentException();
    }
    public Money add(Money o) { return new Money(amount.add(o.amount)); }
}

// Java 25 with 구문 (Derived Record Creation)
Order oldOrder = new Order(id, status, total);
Order newOrder = oldOrder with { status = OrderStatus.PAID; };
```

### Sealed Interface (Sum Type)

```java
public sealed interface Result<S,F> permits Success, Failure {}
public record Success<S,F>(S value) implements Result<S,F> {}
public record Failure<S,F>(F error) implements Result<S,F> {}
```

### Pattern Matching

```java
// sealed interface OrderStatus permits Unpaid, Paid, Shipping, Delivered, Cancelled
String msg = switch (status) {
    case Unpaid u -> "결제 대기";
    case Paid p when p.paidAt().plusDays(1).isAfter(now) -> "취소 가능";
    case Paid p -> "취소 불가";
    case Shipping s -> "배송 중";
    case Delivered d -> "배송 완료";
    case Cancelled c -> "취소됨";
    // sealed interface면 default 불필요!
};
```

### Optional

```java
opt.map(f).flatMap(g).orElseThrow(() -> new NotFoundException());
```

### var 타입 추론

```java
var validator = new OrderValidator();
var result = validator.validate(order);
```

---

## Appendix C: 심화 Q&A

### Q1. 객체를 계속 새로 만들면 메모리가 터지지 않나요?

**결론: 전혀 걱정하지 않으셔도 됩니다.**

현대의 JVM(특히 G1GC, ZGC)은 **"짧게 살고 죽는 객체(Short-lived Object)"**를 처리하는 데 극도로 최적화되어 있습니다.

- **에덴(Eden) 영역의 마법**: 대부분의 record나 불변 객체는 생성되자마자 잠깐 쓰이고 버려집니다. JVM의 가비지 컬렉터(GC) 입장에서 이런 객체들은 청소 비용이 거의 '0'에 수렴합니다.
- **구조적 공유 (Structural Sharing)**: `new Order(..., newAddress)`를 할 때, 기존 Order의 모든 데이터를 복사하는 게 아닙니다. 기존 데이터는 참조(Reference)만 복사해서 재사용합니다.
- **탈출 분석 (Escape Analysis)**: JIT 컴파일러가 "이 객체는 메서드 밖으로 안 나가네?"라고 판단하면, 힙이 아닌 스택에 할당해버립니다.

### Q2. G1GC와 ZGC가 뭔가요?

**G1GC (Garbage First GC)**: Java 9~18의 기본 GC. 거대한 힙을 작은 Region으로 쪼개서, 쓰레기가 가장 많은 구역부터 청소합니다.

**ZGC (Z Garbage Collector)**: Java 21+의 차세대 GC. 청소를 애플리케이션 실행 중에 몰래 수행합니다. 힙 크기에 상관없이 멈추는 시간이 1ms 미만입니다.

### Q3. toDomain()은 Controller vs Service 어디에?

**정석: Controller에서 변환합니다.**

서비스 메서드 시그니처가 타입(Type) 그 자체로 문서가 되어야 합니다.

```java
// Service가 DTO를 받을 때: 불안함
void register(UserDTO dto)  // dto 안에 쓰레기 값 있을 수도

// Service가 Domain을 받을 때: 안전함
void register(User user)    // User 타입이면 이미 검증 완료
```

### Q4. Repository 인터페이스는 어느 패키지에?

**위치: domain 패키지**

이것이 **의존성 역전 원칙(DIP)**의 핵심입니다.
- Domain은 인터페이스만 정의 (OrderRepository)
- Infra는 구현체 제공 (JpaOrderRepository)

만약 인터페이스가 infra에 있다면, Domain이 Infra를 import해야 합니다. DB 기술 변경 시 도메인 코드도 수정해야 하는 불상사가 생깁니다.

### Q5. 패키지 상호 참조가 발생하면 무슨 문제?

**스파게티 코드가 되어 유지보수가 불가능해집니다.**

- **컴파일 지옥**: A 컴파일하려니 B 필요, B 하려니 A 필요
- **테스트 불가능**: 단위 테스트하려는데 연쇄적으로 의존성 딸려옴
- **변경의 전파**: DB 스키마 바꿨는데 할인 로직에서 에러 발생

### Q6. 순수 함수의 본질은 무엇인가요?

**참조 투명성(Referential Transparency)**: "언제, 어디서, 누가 실행하든 입력이 같으면 결과가 무조건 같아야 한다. 그리고 그 외에는 아무 일도 일어나지 않아야 한다."

`System.out.println`도 부수 효과입니다:
- 모니터라는 외부 세계의 상태를 변경
- 실행 환경에 따라 동작이 달라질 수 있음

### Q7. 팀이 FP를 안 하는데 순수 함수로 짜면 좋은가요?

**네, 무조건 좋습니다!**

1. **테스트의 천국**: Mock 없이 `assert(f(input) == expected)` 한 줄로 끝
2. **Local Reasoning**: 버그 추적 시 함수 안만 보면 됨
3. **동시성 안전**: 값을 안 바꾸니 락 불필요

### Q8. 순수 함수 적용 전략은?

**Functional Core, Imperative Shell**

- **Functional Core**: 비즈니스 로직, 계산, 판단 → 순수 함수로
- **Imperative Shell**: DB 저장, API 호출, 로그 출력 → 바깥쪽으로

복잡한 로직만 순수 함수로 분리해도 디버깅 시간이 절반으로 줄어듭니다.

---

## Appendix D: 전체 퀴즈 정답

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | B  | B   |
| 2  | C  | B  | C  | A  | C   |
| 3  | B  | A  | B  | C  | B   |
| 4  | C  | B  | -  | -  | -   |
| 5  | B  | -  | -  | -  | -   |
| 6  | C  | B  | C  | -  | -   |
| 7  | B  | C  | C  | D  | B   |
| 8  | B  | C  | A  | B  | B   |
| 9  | B  | C  | C  | B  | B   |
| 10 | B  | B  | B  | B  | B   |

### 정답 해설

**Chapter 1**
- Q1.1: C - 각 컨텍스트별로 필요한 정보만 담은 별도 모델 정의
- Q1.2: B - Record는 setter가 없음 (불변)
- Q1.3: B - 객체가 여러 곳에서 변경되어 상태 추적이 어렵다
- Q1.4: B - 기획서 용어가 그대로 코드에 등장
- Q1.5: B - 주문 컨텍스트에서 인증 정보에 접근

**Chapter 2**
- Q2.1: C - 메모리 사용량 증가는 Primitive Obsession의 문제가 아님
- Q2.2: B - Value Object는 값이 같으면 같은 객체
- Q2.3: C - Compact Constructor에서 검증
- Q2.4: A - double 대신 BigDecimal 사용 (부동소수점 오차)
- Q2.5: C - 주문 상태는 String이 아니라 enum 또는 sealed interface

**Chapter 3**
- Q3.1: B - 배송 정보는 모든 필드가 필요한 AND 타입
- Q3.2: A - permits는 구현 가능한 클래스를 제한
- Q3.3: B - 모든 케이스를 다루지 않으면 컴파일 에러
- Q3.4: C - sealed interface가 가장 적합
- Q3.5: B - Shipped 케이스 누락으로 컴파일 에러

**Chapter 4**
- Q4.1: C - 타입으로 "배송 중엔 취소 불가" 규칙을 강제
- Q4.2: B - VerifiedEmail만 주문할 수 있는 타입 설계

**Chapter 5**
- Q5.1: B - 검증 전후의 데이터가 다른 보장을 가지므로

**Chapter 6**
- Q6.1: C - 메모리 사용량은 Exception의 문제가 아님
- Q6.2: B - 결과가 Result인 함수는 flatMap 사용
- Q6.3: C - Failure가 반환되고 이후 단계는 실행되지 않음

**Chapter 7**
- Q7.1: B - 폼 검증에서 모든 에러를 한번에 보여줄 때 Validation
- Q7.2: C - Applicative는 모든 에러를 수집 (첫 에러에서 중단 X)
- Q7.3: C - Event는 이미 발생한 불변의 사실
- Q7.4: D - 두 에러 모두 수집됨
- Q7.5: B - 과거형으로 "이미 일어난 일"을 표현

**Chapter 8**
- Q8.1: B - 도메인이 인프라에 의존하면 결합도가 높아짐
- Q8.2: C - 코드량 줄이기는 분리 이유가 아님
- Q8.3: A - ACL은 외부 형식을 도메인 형식으로 변환
- Q8.4: B - 외부 API 변경 시 ACL(Adapter)만 수정
- Q8.5: B - Repository의 OrderMapper.toDomain()은 JPA Entity → Domain 변환 (참고: Controller의 toDomain()은 DTO → Domain 변환)

**Chapter 9**
- Q9.1: B - 의존성은 바깥쪽에서 안쪽으로 (Infrastructure → Domain)
- Q9.2: C - 데이터베이스 저장은 부수효과
- Q9.3: C - 순수 함수는 데이터베이스를 조회하지 않음
- Q9.4: B - Repository 인터페이스는 Domain 계층에 정의 (DIP)
- Q9.5: B - Mock 없이 입력/출력만으로 테스트 가능

**Chapter 10**
- Q10.1: B - sealed interface의 메서드로 정의 (hasFreeShipping())
- Q10.2: B - 각 컨텍스트에 필요한 데이터만 포함
- Q10.3: B - 타입으로 "배송 중 취소 불가" 규칙을 강제
- Q10.4: B - 모든 결제 수단을 타입 안전하게 처리 가능
- Q10.5: B - 쿠폰 사용 실패 가능성을 명시적으로 표현

---

*이 교재는 Scott Wlaschin의 "Domain Modeling Made Functional"을 Java 25와 이커머스 도메인에 맞춰 재구성한 것입니다.*
