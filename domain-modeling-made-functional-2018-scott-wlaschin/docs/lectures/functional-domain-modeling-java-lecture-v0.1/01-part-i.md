# Part I: 기초 - 도메인과 타입 시스템

---

## Chapter 1: DDD와 함수형 사고의 기초

### 학습 목표
1. Bounded Context의 개념과 필요성을 이해한다
2. Ubiquitous Language가 코드 품질에 미치는 영향을 설명할 수 있다
3. 불변성(Immutability)의 장점을 이해하고 Java Record로 구현할 수 있다
4. 함수형 사고방식이 도메인 모델링에 주는 이점을 파악한다
5. Event Storming을 통해 도메인을 탐험하는 방법을 익힌다

---

### 1.1 Bounded Context: 맥락이 왕이다

#### 왜 하나의 통합 모델은 실패하는가?

많은 개발팀이 범하는 가장 큰 실수는 **"하나의 통합된 모델"**을 만들려는 것입니다. 예를 들어 `User`라는 클래스 하나에 로그인 정보, 배송지 주소, 쿠폰 보유량, 정산 계좌 정보를 모두 넣으려 합니다.

```java
// 안티패턴: 모든 것을 담은 God Class
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

#### 비유: 나라와 언어

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

#### 기획자와 개발자의 언어 통일

DDD의 또 다른 핵심은 **유비쿼터스 언어(Ubiquitous Language)**입니다. 기획자(Domain Expert)와 개발자가 같은 용어를 사용해야 합니다.

```java
// 나쁜 예: 기획서와 코드의 용어가 다름
// 기획서: "쿠폰을 적용하면 할인된 금액이 계산됩니다"
public class Util {
    public static double calc(double a, String c) {
        // a가 뭐지? c가 뭐지?
        return a * 0.9;
    }
}

// 좋은 예: 기획서의 용어가 그대로 코드에 등장
public class CouponService {
    public DiscountedPrice applyCoupon(OriginalPrice price, Coupon coupon) {
        // 기획서를 읽은 사람이라면 이 코드를 바로 이해할 수 있음
        return coupon.applyTo(price);
    }
}
```

#### 비유: 통역 없는 직접 대화

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

#### 왜 데이터는 불변이어야 하는가?

함수형 프로그래밍의 핵심 원칙 중 하나는 **불변성(Immutability)**입니다. 데이터가 한번 생성되면 변경되지 않습니다.

**가변 객체의 문제점:**

```java
// 가변 객체: 언제 어디서 값이 바뀔지 모름
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

#### 비유: 공증된 계약서

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
// 기존 방식: 보일러플레이트 코드가 많음
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

// Java Record: 한 줄로 동일한 기능
public record OrderAmount(BigDecimal value) {}
```

**Record의 특징:**
- 모든 필드는 `private final` (불변)
- `getter`, `equals`, `hashCode`, `toString` 자동 생성
- Setter는 없음 (값을 바꾸려면 새 객체 생성)

#### 값을 변경하고 싶다면? with 패턴

```java
public record Order(String orderId, OrderAmount amount, OrderStatus status) {
    // 상태를 변경한 "새로운" 주문 객체 반환
    public Order withStatus(OrderStatus newStatus) {
        return new Order(this.orderId, this.amount, newStatus);
    }
}

// 사용
Order unpaidOrder = new Order("ORD-001", amount, OrderStatus.UNPAID);
Order paidOrder = unpaidOrder.withStatus(OrderStatus.PAID);

// unpaidOrder는 여전히 UNPAID (불변!)
// paidOrder는 새로운 객체로 PAID
```

---

### 1.4 함수형 사고방식

#### 명령형 vs 선언형

**명령형(Imperative)**: "어떻게(How)" 할지를 단계별로 지시

```java
// 명령형: 장바구니 총액 계산
int total = 0;
for (Item item : cart.getItems()) {
    if (item.isAvailable()) {
        total = total + item.getPrice();
    }
}
```

**선언형(Declarative)**: "무엇을(What)" 원하는지를 표현

```java
// 선언형: 장바구니 총액 계산
int total = cart.getItems().stream()
    .filter(Item::isAvailable)
    .mapToInt(Item::getPrice)
    .sum();
```

#### 함수형 도메인 모델링의 핵심 아이디어

1. **타입이 문서다**: 함수 시그니처만 봐도 무슨 일을 하는지 알 수 있어야 함
2. **불가능한 상태는 표현 불가능하게**: 잘못된 데이터는 타입 레벨에서 차단
3. **부수효과 격리**: 순수한 비즈니스 로직과 I/O를 분리
4. **파이프라인 사고**: 데이터 변환의 연속으로 워크플로우 구성

```java
// 함수형 사고: 주문 처리를 데이터 변환으로 표현
// UnvalidatedOrder -> ValidatedOrder -> PricedOrder -> PaidOrder

public Result<PaidOrder, OrderError> processOrder(UnvalidatedOrder input) {
    return validate(input)           // 검증
        .flatMap(this::calculatePrice)  // 가격 계산
        .flatMap(this::processPayment); // 결제 처리
}
```

---

### 1.5 도메인 탐험: Event Storming

#### 코딩보다 먼저 해야 할 일

도메인 모델링은 클래스 다이어그램을 그리는 것에서 시작하지 않습니다. **이벤트 스토밍(Event Storming)** 이라는 협업 워크숍을 통해 비즈니스 흐름을 파악하는 것이 먼저입니다.

**핵심 질문**: "우리 시스템에서 어떤 흥미로운 일이 발생합니까?"

#### 도메인 이벤트(Domain Event)

도메인 이벤트는 비즈니스적으로 의미 있는 사건을 과거형으로 기술합니다.

- **주문됨 (OrderPlaced)**
- **결제됨 (PaymentReceived)**
- **배송 시작됨 (ShippingStarted)**
- **배송 완료됨 (ItemDelivered)**

#### 워크플로우 발견

이벤트를 시간 순서대로 나열하면 자연스럽게 워크플로우가 드러납니다.

> `주문됨` → (결제 프로세스) → `결제됨` → (배송 프로세스) → `배송 시작됨`

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
        // 주문 생성 로직
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

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 2: 원시 타입의 저주 끊기 - Simple Types

### 학습 목표
1. Primitive Obsession 안티패턴을 인식하고 그 위험성을 설명할 수 있다
2. Value Object와 Entity의 차이를 이해한다
3. Compact Constructor를 활용해 생성 시점 검증을 구현할 수 있다
4. 도메인 타입으로 코드의 의도를 명확히 전달할 수 있다

---

### 2.1 Primitive Obsession: 원시 타입 집착

#### 흔히 보는 위험한 코드

이커머스 코드에서 가장 흔히 보는 안티패턴입니다:

```java
// 위험한 코드: 모든 것이 String과 숫자
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

#### 비유: 라벨 없는 약병

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
| 불변성 | 항상 불변 | 상태가 변할 수 있음 |
| 수명 | 독립적 | 영속적 (DB에 저장) |
| 예시 | 금액, 이메일, 주소 | 주문, 회원, 상품 |

#### 비유: 지폐와 은행 계좌

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
> - 계좌의 잔액은 변할 수 있습니다 (가변)
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

// 3. 쿠폰 코드 (정확히 10자리 영숫자)
public record CouponCode(String value) {
    private static final Pattern PATTERN = Pattern.compile("^[A-Z0-9]{10}$");

    public CouponCode {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "쿠폰 코드는 10자리 영숫자여야 합니다: " + value
            );
        }
    }
}

// 4. 회원 ID (양수)
public record MemberId(long value) {
    public MemberId {
        if (value <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다: " + value);
        }
    }
}

// 5. 수량 (1 이상)
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

#### 비유: 공항 입국 심사

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

#### 달라진 메서드 시그니처

```java
// Before: 원시 타입 - 아무거나 넣을 수 있음
public void processPayment(String email, double amount) { ... }

// After: 도메인 타입 - 잘못된 타입은 컴파일 에러
public void processPayment(EmailAddress email, OrderAmount amount) { ... }

// 호출 시도
processPayment(
    new EmailAddress("invalid"),     // 런타임 에러: @ 없음
    OrderAmount.won(-1000)           // 런타임 에러: 음수
);

// 올바른 호출
processPayment(
    new EmailAddress("user@example.com"),  // OK
    OrderAmount.won(50000)                  // OK
);
```

---

### 2.4 도메인 타입의 추가 이점

#### 1. 자동 완성과 IDE 지원

```java
// Before: amount가 뭐였더라?
public void process(double amount) {
    // amount * ??? 뭘 해야 하지?
}

// After: IDE가 도와줌
public void process(OrderAmount amount) {
    amount.  // IDE: add(), multiply(), value() 등 제안
}
```

#### 2. 비즈니스 로직 응집

```java
public record Percentage(int value) {
    public Percentage {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("퍼센트는 0-100 사이여야 합니다");
        }
    }

    // 할인율 계산 로직이 타입 안에 응집
    public OrderAmount calculateDiscount(OrderAmount original) {
        BigDecimal discountRate = BigDecimal.valueOf(value).divide(BigDecimal.valueOf(100));
        BigDecimal discounted = original.value().multiply(BigDecimal.ONE.subtract(discountRate));
        return new OrderAmount(discounted);
    }
}

// 사용
Percentage discountRate = new Percentage(10);  // 10% 할인
OrderAmount discounted = discountRate.calculateDiscount(originalPrice);
```

#### 3. 디버깅 용이성

```java
// Before: 로그에서 의미 파악 어려움
logger.info("Processing: " + email + ", " + amount);
// 출력: Processing: user@test.com, 50000.0

// After: 타입 정보가 명확
logger.info("Processing: " + email + ", " + amount);
// 출력: Processing: EmailAddress[value=user@test.com], OrderAmount[value=50000]
```

---

### 2.5 이커머스 Simple Types 종합 예시

```java
package com.ecommerce.domain.types;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

public record PhoneNumber(String value) {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[0-9]-[0-9]{4}-[0-9]{4}$");

    public PhoneNumber {
        if (value == null || !PHONE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("유효하지 않은 전화번호: " + value);
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

public record ProductName(String value) {
    public ProductName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("상품명은 비어있을 수 없습니다");
        }
        if (value.length() > 100) {
            throw new IllegalArgumentException("상품명은 100자를 초과할 수 없습니다");
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

정답은 Appendix C에서 확인할 수 있습니다.

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

#### 비유: 햄버거 세트와 메뉴판

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

#### 비유: 신분증

> **Record는 신분증처럼 여러 정보를 하나로 묶습니다.**
>
> 주민등록증에는 이름 **AND** 주민번호 **AND** 주소 **AND** 사진이 있습니다.
> 이 중 하나라도 빠지면 유효한 신분증이 아닙니다.
> 그리고 한번 발급되면 마음대로 수정할 수 없습니다 (불변).

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

#### 왜 Enum이 아니라 Sealed Interface인가?

```java
// Enum의 한계: 각 케이스마다 다른 데이터를 가질 수 없음
public enum PaymentType {
    CREDIT_CARD,    // 카드번호, 유효기간은 어디에?
    BANK_TRANSFER,  // 계좌번호는 어디에?
    POINTS          // 이건 추가 데이터 없음
}

// Sealed Interface: 각 케이스가 다른 구조를 가질 수 있음
sealed interface PaymentMethod permits CreditCard, BankTransfer, Points {}
record CreditCard(CardNumber number, ExpiryDate expiry) implements PaymentMethod {}
record BankTransfer(AccountNumber account) implements PaymentMethod {}
record Points() implements PaymentMethod {}  // 데이터 없음도 OK
```

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

#### 비유: 은행 창구

> **Pattern Matching은 은행의 업무별 창구와 같습니다.**
>
> 은행에 가면 창구가 나뉘어 있습니다:
> - 1번 창구: 예금/출금
> - 2번 창구: 대출 상담
> - 3번 창구: 환전
> - 4번 창구: 카드 발급
>
> 손님(데이터)이 오면 업무 종류에 따라 해당 창구로 갑니다.
> 모든 업무 유형에 대해 창구가 준비되어 있어야 합니다.
> "5번: 기타 업무" 같은 애매한 창구(default)는 필요 없습니다.
>
> Pattern Matching은 "모든 창구가 준비되었는지" 컴파일러가 확인해줍니다.

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

**이것이 Sum Type의 핵심 이점입니다.** 새로운 케이스를 추가하면 처리하지 않은 모든 곳에서 컴파일 에러가 발생합니다. 런타임에 "처리되지 않은 케이스" 버그가 발생할 수 없습니다.

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

정답은 Appendix C에서 확인할 수 있습니다.

---

## Chapter 4: 불가능한 상태를 불가능하게

### 학습 목표
1. NULL의 문제점과 Optional의 올바른 사용법을 이해한다
2. State Machine 패턴으로 상태 전이를 안전하게 모델링할 수 있다
3. 타입 시스템으로 비즈니스 규칙을 컴파일 타임에 강제할 수 있다
4. "Make Illegal States Unrepresentable" 원칙을 실무에 적용할 수 있다

---

### 4.1 NULL의 문제: 10억 달러짜리 실수

#### Tony Hoare의 고백

> "I call it my billion-dollar mistake. It was the invention of the null reference in 1965."
> - Tony Hoare (NULL을 발명한 컴퓨터 과학자)

NULL은 "값이 없음"을 표현하지만, 타입 시스템에서 이를 구분할 수 없습니다.

```java
// 위험한 코드: customer가 null일 수 있는지 시그니처만 봐서는 알 수 없음
public void sendEmail(Customer customer) {
    String email = customer.getEmail();  // NullPointerException 가능!
}

// 호출하는 쪽
Customer customer = findCustomer(id);  // null일 수 있음
sendEmail(customer);  // 컴파일 OK, 런타임에 폭발
```

#### 비유: 지뢰밭

> **NULL은 코드에 숨겨진 지뢰입니다.**
>
> 지뢰밭을 걸을 때는 모든 발걸음이 위험합니다.
> 어디에 지뢰가 있는지 표시가 없기 때문입니다.
>
> NULL도 마찬가지입니다. 어떤 변수가 null일 수 있는지
> 코드만 봐서는 알 수 없습니다.
> 그래서 모든 변수를 의심하며 `if (x != null)` 검사를 해야 합니다.
>
> 결국 코드는 null 체크로 도배되고,
> 하나라도 빠뜨리면 런타임에 폭발합니다.

---

### 4.2 Optional: 없을 수도 있음을 명시

#### Optional의 올바른 사용

```java
// Before: null 반환 (위험)
public Customer findCustomer(CustomerId id) {
    return customerMap.get(id);  // 없으면 null
}

// After: Optional로 명시
public Optional<Customer> findCustomer(CustomerId id) {
    return Optional.ofNullable(customerMap.get(id));
}

// 호출하는 쪽
findCustomer(id)
    .map(Customer::email)
    .ifPresentOrElse(
        this::sendWelcomeEmail,
        () -> log.warn("Customer not found: " + id)
    );
```

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

### 4.3 State Machine: 상태 전이를 타입으로

#### 비유: 지하철 개찰구

> **State Machine은 지하철 개찰구입니다.**
>
> 개찰구 상태:
> - **잠김**: 카드를 찍어야 열림
> - **열림**: 통과하면 다시 잠김
>
> 허용된 전이만 가능합니다.
> 잘못된 전이(잠긴 상태에서 통과)는 물리적으로 막힙니다.
>
> 타입으로 State Machine을 구현하면
> 컴파일러가 "잘못된 전이"를 막아줍니다.

#### 타입으로 상태 전이 강제하기

```java
// 각 상태를 별도 타입으로
public sealed interface OrderState
    permits UnpaidOrder, PaidOrder, ShippingOrder, DeliveredOrder {}

public record UnpaidOrder(OrderId id, Money total) implements OrderState {
    // 결제하면 PaidOrder로 전이
    public PaidOrder pay(PaymentInfo payment) {
        return new PaidOrder(id, total, LocalDateTime.now(), payment);
    }
}

public record PaidOrder(OrderId id, Money total, LocalDateTime paidAt, PaymentInfo payment)
    implements OrderState {
    // 배송 시작하면 ShippingOrder로 전이
    public ShippingOrder startShipping(TrackingNumber tracking) {
        return new ShippingOrder(id, paidAt, tracking, LocalDateTime.now());
    }
}

public record ShippingOrder(OrderId id, LocalDateTime paidAt,
    TrackingNumber tracking, LocalDateTime shippedAt) implements OrderState {
    // 배송 완료
    public DeliveredOrder complete() {
        return new DeliveredOrder(id, paidAt, tracking, LocalDateTime.now());
    }
    // cancel() 메서드 없음 - 배송 중 취소 불가!
}

public record DeliveredOrder(OrderId id, LocalDateTime paidAt,
    TrackingNumber tracking, LocalDateTime deliveredAt) implements OrderState {
    // 전이 메서드 없음 - 최종 상태
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

### 4.4 타입으로 비즈니스 규칙 강제

#### 비유: USB 포트

> **타입 제약은 USB 포트입니다.**
>
> USB-C에 USB-A를 꽂으면 물리적으로 안 들어갑니다.
> 타입도 마찬가지입니다. `VerifiedEmail`을 요구하는 곳에
> `UnverifiedEmail`을 넣으면 컴파일러가 거부합니다.

```java
// 인증되지 않은 이메일
public record UnverifiedEmail(String value) {}

// 인증된 이메일 (이메일 인증 서비스만 생성 가능)
public record VerifiedEmail(String value) {}

public class MemberService {
    // VerifiedEmail만 받음 - 인증 안 된 이메일로는 호출 불가!
    public Member completeRegistration(VerifiedEmail email, String name) {
        return new Member(MemberId.generate(), email, name);
    }
}
```

---

### 퀴즈 Chapter 4

#### Q4.1 [개념 확인] NULL의 문제
NULL이 "10억 달러짜리 실수"라고 불리는 이유는?

A. 메모리를 많이 사용해서
B. 개발 시간이 많이 들어서
C. NULL 가능 여부를 타입으로 표현할 수 없어 런타임 에러 발생
D. Java에만 있어서

---

#### Q4.2 [코드 분석] State Machine
`ShippingOrder`에 `cancel()` 메서드가 없으면 어떤 효과가 있나요?

A. 예외가 발생한다
B. if문으로 체크한다
C. 호출 자체가 컴파일 에러가 된다
D. DB 트리거로 막는다

---

#### Q4.3 [설계 문제] 이메일 인증 규칙
"인증되지 않은 이메일로 주문 불가"를 타입으로 강제하려면?

A. 생성자에서 if문 체크
B. `createOrder(VerifiedEmail email, ...)` 시그니처 사용
C. @NotNull 어노테이션
D. 런타임 예외

---

#### Q4.4 [코드 분석] Optional
올바른 Optional 사용법은?

A. `record Order(Optional<Coupon> coupon)`
B. `void process(Optional<String> name)`
C. `Optional.get()`
D. `findById(id).orElseThrow(() -> new NotFoundException())`

---

#### Q4.5 [설계 문제] 불가능한 상태
"배송 전에는 운송장 번호 없음"을 보장하는 방법은?

A. nullable 필드 + null 체크
B. UnshippedOrder와 ShippedOrder 별도 타입
C. @NotBlank 어노테이션
D. DB 제약조건

---

정답은 Appendix C에서 확인할 수 있습니다.

---

