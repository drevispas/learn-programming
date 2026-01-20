# Chapter 3: 복합 타입 - AND/OR Types

> Part I: 기초 - 도메인과 타입 시스템

---

### 학습 목표
1. Product Type(AND)과 Sum Type(OR)의 개념을 이해한다
2. Java Record로 Product Type을 구현할 수 있다
3. Sealed Interface로 Sum Type을 구현할 수 있다
4. Pattern Matching for Switch를 활용해 모든 케이스를 안전하게 처리할 수 있다

---

### 3.1 타입 대수학: AND와 OR

#### 데이터 모델링의 두 가지 결합 방식

모든 복잡한 데이터 구조는 단 두 가지 방식의 조합으로 만들어집니다:

**그림 3.1**: Algebraic Data Types - Product Type vs Sum Type

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          Algebraic Data Types (ADT)                          │
├──────────────────────────────────────┬───────────────────────────────────────┤
│          Product Type (AND)          │            Sum Type (OR)              │
├──────────────────────────────────────┼───────────────────────────────────────┤
│                                      │                                       │
│    ┌──────────────────────────────┐  │             ┌──────────┐              │
│    │      record Order(           │  │             │  sealed  │              │
│    │        OrderId id,           │  │             │interface │              │
│    │        Money amount,         │  │             │ Payment  │              │
│    │        Status status         │  │             └────┬─────┘              │
│    │      )                       │  │                  │                    │
│    └──────────────────────────────┘  │            ┌─────┼─────┐              │
│                                      │            │     │     │              │
│                                      │          Card Transfer Points         │
│                                      │                                       │
│    id AND amount AND status          │           (하나만 선택)               │
│    (모든 필드 필요)                  │                                       │
│                                      │                                       │
│    상태 공간: N₁ × N₂ × N₃           │       상태 공간: N₁ + N₂ + N₃         │
└──────────────────────────────────────┴───────────────────────────────────────┘
```
*참고 자료: [Domain Modeling Made Functional](https://pragprog.com/titles/swdddf/) - Scott Wlaschin,
[What Are Sum, Product, and Pi Types?](https://manishearth.github.io/blog/2017/03/04/what-are-sum-product-and-pi-types/)*

**표 3.1**: Product Type vs Sum Type

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

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Sum Type (Algebraic Data Types)은 다음에서 사용됩니다:
- Rust의 enum (Result, Option)
- Kotlin의 sealed class
- Scala의 sealed trait
- TypeScript의 discriminated union

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "Sum Type은 도메인의 '또는' 관계를 정확하게 표현한다. 이것이 없으면 null이나 boolean 플래그에 의존하게 된다."
- **Yaron Minsky** (Jane Street): "Make Illegal States Unrepresentable - Sum Type은 이 원칙의 핵심 도구다."

**참고 자료:**
- [Domain Modeling Made Functional](https://pragprog.com/titles/swdddf/) - Scott Wlaschin

---

### 3.2 Product Type: Java Record

#### 주문 정보 모델링

이커머스의 주문(Order)은 전형적인 Product Type입니다:

**코드 3.1**: Product Type - 주문 모델
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

**코드 3.2**: Sum Type - 결제 수단
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

**코드 3.3**: Pattern Matching with Exhaustiveness Check
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

> 💡 `processCreditCard`, `generateReceiptNumber` 등의 전체 구현은
> `examples/functional-domain-modeling/` 프로젝트의 결제 도메인에서 확인할 수 있습니다.

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

**코드 3.4**: 새로운 케이스 추가 시 컴파일 에러
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

### 3.5 Sum Type 설계 원칙: Field vs Method

#### 언제 Field(record 컴포넌트)를 사용하나?

각 구현체마다 **고유한 데이터**가 필요할 때 field를 사용합니다:

**코드 3.5a**: Field - 구현체별 고유 데이터
```java
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points {}

// 카드 결제에만 필요한 데이터
public record CreditCard(CardNumber number, ExpiryDate expiry)
    implements PaymentMethod {}

// 계좌 이체에만 필요한 데이터
public record BankTransfer(BankCode bank, AccountNumber account)
    implements PaymentMethod {}

// 포인트는 추가 데이터 불필요
public record Points() implements PaymentMethod {}
```

#### 언제 Method를 사용하나?

모든 구현체가 공통으로 **대답해야 하는 질문**이나 **수행해야 하는 행위**가 있을 때 method를 사용합니다:

**코드 3.5b**: Method - 공통 행위, 구현체별 다른 결과
```java
public sealed interface MemberGrade permits Bronze, Silver, Gold, Vip {
    // 모든 등급이 대답해야 하는 질문들
    int discountRate();        // "할인율이 얼마야?"
    boolean hasFreeShipping(); // "무료배송 되니?"
}

public record Bronze() implements MemberGrade {
    @Override public int discountRate() { return 0; }
    @Override public boolean hasFreeShipping() { return false; }
}

public record Vip() implements MemberGrade {
    @Override public int discountRate() { return 10; }
    @Override public boolean hasFreeShipping() { return true; }  // VIP만 true
}
```

#### 결정 기준 요약

| 질문 | Field | Method |
|------|-------|--------|
| **특정 구현체에만** 필요한 데이터인가? | ✅ | |
| **모든 구현체가** 대답/수행해야 하는가? | | ✅ |
| 값이 생성 시점에 **외부에서 주입**되는가? | ✅ | |
| 값이 구현체 **내부에서 결정**되는가? | | ✅ |

> 💡 **핵심 원칙**: "이 값을 누가 알고 있는가?"
> - 외부에서 알려줘야 하면 → **Field** (생성자 파라미터)
> - 구현체 자체가 결정하면 → **Method** (다형성 활용)

---

### 3.6 이커머스 복합 타입 예시

#### 쿠폰 타입 모델링

**코드 3.6**: 쿠폰 타입 (Sum Type with Business Logic)
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
public record PercentageDiscount(DiscountRate rate) implements CouponType {
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

**코드 3.7**: 주문 상태 (상태별 데이터가 다른 Sum Type)
```java
// 주문 상태: 각 상태마다 필요한 데이터가 다름!
public sealed interface OrderStatus
    permits Unpaid, Paid, Shipping, Delivered, Cancelled {}

public record Unpaid(LocalDateTime paymentDeadline) implements OrderStatus {}
public record Paid(LocalDateTime paidAt, String transactionId) implements OrderStatus {}
public record Shipping(LocalDateTime shippedAt, String trackingNumber) implements OrderStatus {}
public record Delivered(LocalDateTime deliveredAt) implements OrderStatus {}
public record Cancelled(LocalDateTime cancelledAt, CancelReason reason) implements OrderStatus {}

public enum CancelReason { CUSTOMER_REQUEST, OUT_OF_STOCK, PAYMENT_FAILED }
```

#### 상태별 처리 로직

**코드 3.8**: 상태별 메시지 처리와 Guard Pattern
```java
public class OrderService {
    public String getOrderStatusMessage(Order order) {
        return switch (order.status()) {
            case Unpaid u -> "결제 대기. 기한: " + u.paymentDeadline();
            case Paid p -> "결제 완료 (" + p.paidAt() + ")";
            case Shipping s -> "배송 중. 운송장: " + s.trackingNumber();
            case Delivered d -> "배송 완료 (" + d.deliveredAt() + ")";
            case Cancelled c -> "취소됨. 사유: " + c.reason();
        };
    }

    // Guard Pattern: when 절로 조건 추가
    public boolean canCancel(Order order) {
        return switch (order.status()) {
            case Unpaid u -> true;
            case Paid p when p.paidAt().plusHours(24).isAfter(LocalDateTime.now()) -> true;
            case Paid p, Shipping s, Delivered d, Cancelled c -> false;
        };
    }
}
```

---

### 3.7 중첩된 Pattern Matching

**코드 3.9**: 중첩된 Sum Type과 Pattern Matching
```java
// 결제 결과: Success 또는 Failure
public sealed interface PaymentResult permits Success, Failure {
    record Success(String receiptNumber) implements PaymentResult {}
    record Failure(PaymentError error) implements PaymentResult {}
}

// 결제 에러: 4가지 유형
public sealed interface PaymentError
    permits InsufficientFunds, CardExpired, InvalidAccount, SystemError {}
record InsufficientFunds(Money required) implements PaymentError {}
record CardExpired(ExpiryDate exp) implements PaymentError {}
record InvalidAccount(String reason) implements PaymentError {}
record SystemError(String code) implements PaymentError {}

// 중첩 패턴 매칭: Failure 내부의 에러 타입까지 분기
public String handlePaymentResult(PaymentResult result) {
    return switch (result) {
        case Success s -> "결제 성공! 영수증: " + s.receiptNumber();
        case Failure(InsufficientFunds e) -> "잔액 부족";
        case Failure(CardExpired e) -> "카드 만료: " + e.exp();
        case Failure(InvalidAccount e) -> "잘못된 계좌";
        case Failure(SystemError e) -> "시스템 오류: " + e.code();
    };
}
```

---

### 3.8 Optional의 올바른 사용

> 📝 Optional과 NULL 문제의 심화 내용은 **Chapter 4.1**을 참고하세요.

#### Optional 안티패턴

**코드 3.10**: Optional 안티패턴 모음
```java
// ❌ Record 필드로 Optional 사용 금지
public record Order(OrderId id, Optional<Coupon> coupon) {}

// ✅ 별도 타입으로 분리
public sealed interface CouponChoice permits OrderWithCoupon, OrderWithoutCoupon {}

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

**A.** 결제 수단 (카드 또는 계좌이체 또는 포인트)<br/>
**B.** 배송 정보 (받는 사람, 주소, 연락처) *(정답)*<br/>
**C.** 주문 상태 (미결제 또는 결제완료 또는 배송중)<br/>
**D.** 쿠폰 종류 (정액 할인 또는 정률 할인)

---

#### Q3.2 [코드 분석] Sealed Interface

다음 코드에서 `permits` 절의 역할은?

**코드 3.11**: Sealed Interface permits 예시
```java
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points {}
```

**A.** `PaymentMethod`를 구현할 수 있는 클래스를 제한한다 *(정답)*<br/>
**B.** `PaymentMethod`가 상속받을 인터페이스를 지정한다<br/>
**C.** `PaymentMethod`의 메서드 목록을 정의한다<br/>
**D.** `PaymentMethod`의 접근 제어자를 설정한다

---

#### Q3.3 [코드 분석] Pattern Matching 장점

Sealed Interface와 switch 문을 함께 쓸 때의 장점은?

**A.** default 문을 반드시 작성해야 한다<br/>
**B.** 모든 케이스를 다루지 않으면 컴파일 에러가 발생한다 *(정답)*<br/>
**C.** 실행 속도가 10배 빨라진다<br/>
**D.** 메모리 사용량이 줄어든다

---

#### Q3.4 [설계 문제] 쿠폰 모델링

다음 요구사항을 모델링할 때 가장 적합한 방식은?

> "쿠폰은 정액 할인(5000원 할인), 정률 할인(10% 할인),
> 무료 배송 중 하나입니다."

**A.** `String couponType` 필드에 "FIXED", "PERCENT", "FREE_SHIPPING" 저장<br/>
**B.** `CouponType` enum에 세 가지 값 정의<br/>
**C.** `sealed interface CouponType permits FixedDiscount, PercentDiscount, FreeShipping` *(정답)*<br/>
**D.** 추상 클래스 `Coupon`을 만들고 세 가지 하위 클래스 생성

---

#### Q3.5 [버그 찾기] 불완전한 Pattern Matching

다음 코드의 문제점은?

**코드 3.12**: 불완전한 Pattern Matching 예시
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

**A.** switch 문에 default가 없다<br/>
**B.** 컴파일 에러: Shipped 케이스가 처리되지 않음 *(정답)*<br/>
**C.** 런타임에 NullPointerException 발생<br/>
**D.** 성능 문제가 있다

---

정답은 Appendix D에서 확인할 수 있습니다.
