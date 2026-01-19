# Chapter 2: 원시 타입의 저주 깨기 - Wrapped Object

> Part I: 기초 - 도메인과 타입 시스템

---

### 학습 목표
1. Primitive Obsession 안티패턴을 인식하고 그 위험성을 설명할 수 있다
2. Value Object와 Entity의 차이를 이해한다
3. Compact Constructor를 활용해 생성 시점 검증을 구현할 수 있다
4. 도메인 타입으로 코드의 의도를 명확히 전달할 수 있다

---

### 🎯 [핵심 동기 예시] 왜 Wrapped Object가 필요한가?

**코드 2.1**: Primitive Obsession vs Type-Safe Design
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

**코드 2.2**: Anti-pattern - Primitive Obsession
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

#### ❌ Anti-pattern: Primitive Obsession

**왜 나쁜가?**
1. **타입 안전성 없음**: 순서 바꿔도 컴파일 성공
2. **Validation 분산**: 검증 로직이 여러 곳에 흩어짐
3. **자기 문서화 실패**: `String`이 무엇을 의미하는지 알 수 없음

**실제 버그 사례:**
```java
// AWS S3 SDK 버그 (2018) - 버킷명과 키 순서 혼동
// 수백만 달러 손실 야기
s3.putObject(key, bucket, data);  // 순서 실수!
```

**반박 예상 질문:**
> "Value Object 너무 많으면 boilerplate 아닌가요?"

**답변:** Java 25의 Record와 IDE 지원으로 boilerplate 최소화.
타입 안전성으로 얻는 버그 방지 효과가 훨씬 큼.

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

**코드 2.3**: Primitive Obsession으로 인한 실제 버그들
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

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Value Object 패턴은 다음에서 검증되었습니다:
- Vavr (Java FP 라이브러리)의 타입 안전 컬렉션
- 대부분의 금융 시스템 (Money 타입)
- DDD를 적용한 모든 성공적인 프로젝트

**Expert Opinions:**
- **Martin Fowler**: "Value Objects를 사용하면 Primitive Obsession을 피할 수 있다. 이것은 코드를 더 명확하게 만들고 버그를 줄인다."
- **Eric Evans**: "Value Object는 DDD의 빌딩 블록 중 하나로, 도메인의 개념을 명시적으로 표현한다."

---

### 2.2 Value Object vs Entity

#### 두 개념의 핵심 차이

**표 2.1**: Value Object vs Entity 비교

| 구분 | Value Object | Entity |
|-----|----------------------|----------------|
| 동등성 | 값이 같으면 같은 것 | ID가 같으면 같은 것 |
| Immutability | 항상 불변 | 상태가 변할 수 있음 (함수형에서는 불변으로 처리) |
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

**코드 2.4**: Value Object vs Entity
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

**코드 2.5**: Compact Constructor를 활용한 Value Object
```java
// 이메일: Compact Constructor로 생성 시점 검증
public record EmailAddress(String value) {
    public EmailAddress {  // 괄호 없이 작성
        if (value == null || !value.contains("@"))
            throw new IllegalArgumentException("유효하지 않은 이메일");
        // 검증 통과 후 this.value = value; 자동 실행
    }
}

// 금액: 음수 방지 + 비즈니스 메서드 포함
public record OrderAmount(BigDecimal value) {
    public OrderAmount {
        Objects.requireNonNull(value);
        if (value.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("음수 불가");
    }
    public OrderAmount add(OrderAmount other) {
        return new OrderAmount(this.value.add(other.value));
    }
}

// 수량: 1 이상 강제
public record Quantity(int value) {
    public Quantity { if (value < 1) throw new IllegalArgumentException("1 이상 필요"); }
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

**코드 2.6**: 이커머스 도메인 타입 (핵심 예시)
```java
// === 핵심: Money 타입 (금액 계산의 정밀도 보장) ===
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

    public Money add(Money other) { /* 통화 확인 후 덧셈 */ }
    public Money subtract(Money other) { /* 통화 확인 후 뺄셈 */ }
    public Money multiply(int factor) { /* 곱셈 */ }
    public boolean isLessThan(Money other) { /* 비교 */ }
}

public enum Currency { KRW, USD, EUR }

// === ID 타입들: 생성 시점 검증 ===
public record MemberId(long value) {
    public MemberId { if (value <= 0) throw new IllegalArgumentException("양수 필요"); }
}

public record OrderId(String value) {
    public OrderId {
        if (value == null || !value.startsWith("ORD-"))
            throw new IllegalArgumentException("ORD-로 시작 필요");
    }
    public static OrderId generate() { return new OrderId("ORD-" + System.currentTimeMillis()); }
}

// === 검증 타입들: 정규식 또는 범위 검증 ===
public record EmailAddress(String value) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    public EmailAddress {
        if (value == null || !EMAIL_PATTERN.matcher(value).matches())
            throw new IllegalArgumentException("유효하지 않은 이메일");
    }
}

public record OrderLineQuantity(int value) {
    public OrderLineQuantity {
        if (value < 1 || value > 99) throw new IllegalArgumentException("1-99 사이 필요");
    }
}
```

> 💡 전체 동작 코드는 `examples/functional-domain-modeling/` 프로젝트에서 확인하세요.
> 특히 `src/main/java/com/ecommerce/shared/` 디렉토리에서 공통 타입들을 볼 수 있습니다.

---

### 퀴즈 Chapter 2

#### Q2.1 [개념 확인] Primitive Obsession

다음 중 Primitive Obsession의 문제점이 **아닌** 것은?

**A.** 컴파일러가 타입 실수를 잡아주지 못한다<br/>
**B.** 코드의 의도를 파악하기 어렵다<br/>
**C.** 메모리 사용량이 증가한다 *(정답)*<br/>
**D.** 유효하지 않은 값이 시스템에 들어올 수 있다

---

#### Q2.2 [개념 확인] Value Object

Value Object의 특징으로 올바른 것은?

**A.** 식별자(ID)가 있어서 ID가 같으면 같은 객체다<br/>
**B.** 속성 값(Value)이 같으면 같은 객체로 취급된다 *(정답)*<br/>
**C.** 내부의 값을 언제든지 변경할 수 있다<br/>
**D.** 보통 데이터베이스 테이블과 1:1로 매핑된다

---

#### Q2.3 [코드 작성] Compact Constructor

`CouponCode`라는 Value Object를 만들려고 합니다. 요구사항:
- 반드시 10자리 영문 대문자+숫자 조합
- null이면 안 됨

유효성 검사 로직이 들어가기에 가장 적합한 위치는?

**A.** `getCouponCode()` 메서드 내부<br/>
**B.** 별도의 `CouponCodeValidator` 클래스<br/>
**C.** 레코드의 Compact Constructor 내부 *(정답)*<br/>
**D.** 데이터베이스에 저장하기 직전

---

#### Q2.4 [코드 분석] 도메인 타입 설계

다음 코드의 문제점은?

**코드 2.7**: 부동소수점 문제 예시
```java
public record OrderAmount(double value) {
    public OrderAmount {
        if (value < 0) {
            throw new IllegalArgumentException("음수 불가");
        }
    }
}
```

**A.** `double` 대신 `BigDecimal`을 사용해야 한다 (부동소수점 오차 문제) *(정답)*<br/>
**B.** 예외 메시지가 너무 짧다<br/>
**C.** Compact Constructor를 사용하면 안 된다<br/>
**D.** 문제없다

---

#### Q2.5 [설계 문제] 도메인 타입 식별

이커머스 시스템에서 다음 데이터를 Simple Type으로 만들 때, 가장 적절하지 **않은** 것은?

**A.** 배송 주소 → `ShippingAddress(String value)`<br/>
**B.** 할인율 → `DiscountRate(int percentage)`<br/>
**C.** 주문 상태 → `OrderStatus(String value)` *(정답 - enum 또는 sealed interface 사용)*<br/>
**D.** 상품 가격 → `ProductPrice(BigDecimal value)`


---

정답은 Appendix D에서 확인할 수 있습니다.
