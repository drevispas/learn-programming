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
public record DisplayProduct(
    ProductId id, ProductName name, List<String> imageUrls,
    Money price, double rating
) {}

// 재고용 (Inventory Context)
public record InventoryProduct(
    ProductId id, StockQuantity stock, WarehouseLocation location
) {
    public boolean isAvailable(int qty) { return stock.value() >= qty; }
}

// 정산용 (Settlement Context)
public record SettlementProduct(
    ProductId id, SellerId sellerId, Money supplyPrice, FeeRate feeRate
) {}

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
            default -> false;
        };
    }
}
```

---

### 10.4 결제 도메인

```java
package com.ecommerce.domain.payment;

// === 결제 수단 (Sum Type) ===
public sealed interface PaymentMethod
    permits CreditCard, BankTransfer, Points, SimplePay {
    record CreditCard(CardNumber num, ExpiryDate exp) implements PaymentMethod {}
    record BankTransfer(BankCode bank, AccountNumber acc) implements PaymentMethod {}
    record Points(int amount) implements PaymentMethod {}
    record SimplePay(Provider provider, String token) implements PaymentMethod {}
}

public enum Provider { KAKAO, NAVER, TOSS }

// === 결제 결과 (Sum Type) ===
public sealed interface PaymentResult permits Success, Failure {
    record Success(TransactionId txId, Money amount) implements PaymentResult {}
    record Failure(PaymentError error) implements PaymentResult {}
}

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

#### Q10.1 VIP 무료배송을 타입으로 표현하는 방법은?
A. boolean 필드  B. interface 메서드 **정답**  C. 별도 서비스  D. if문

#### Q10.2 컨텍스트별 상품 분리 이유는?
A. 코드량 증가  B. 필요한 데이터만 포함 **정답**  C. 상속 회피  D. 성능

#### Q10.3 Shipping에 cancel()이 없는 이유는?
A. 메모리  B. 타입으로 규칙 강제 **정답**  C. 단순화  D. 성능

#### Q10.4 Mixed 결제를 Sum Type으로 표현하는 이점은?
A. 메모리  B. 타입 안전한 처리 **정답**  C. 속도  D. 가독성 저하

#### Q10.5 쿠폰 use()가 Result를 반환하는 이유는?
A. 복잡성  B. 실패 가능성 **정답**  C. 컨벤션  D. 성능

---

# Appendix A: 흔한 안티패턴

| 안티패턴 | 개선 방법 |
|---------|----------|
| Primitive Obsession | 도메인 타입(Record) 사용 |
| Null 남용 | Optional 또는 타입 분리 |
| String 상태 | Sealed Interface |
| God Class | 컨텍스트별 분리 |
| Exception 흐름제어 | Result 타입 |
| JPA 어노테이션 in 도메인 | Entity/Domain 분리 |
| Optional 필드 | Sealed Interface로 분리 |
| 가변 컬렉션 노출 | List.copyOf() |

---

# Appendix B: Java 25 치트시트

```java
// Record (불변 데이터)
public record Money(BigDecimal amount) {
    public Money { if (amount.signum() < 0) throw new IllegalArgumentException(); }
    public Money add(Money o) { return new Money(amount.add(o.amount)); }
}

// Sealed Interface (Sum Type)
public sealed interface Result<S,F> permits Success, Failure {}
public record Success<S,F>(S value) implements Result<S,F> {}
public record Failure<S,F>(F error) implements Result<S,F> {}

// Pattern Matching
String msg = switch (status) {
    case Unpaid u -> "대기";
    case Paid p when p.paidAt().plusDays(1).isAfter(now) -> "취소가능";
    case Paid p -> "취소불가";
    default -> "기타";
};

// Optional
opt.map(f).flatMap(g).orElseThrow(() -> new NotFoundException());
```

---

# Appendix C: 전체 정답

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | B  | B   |
| 2  | C  | B  | C  | A  | C   |
| 3  | B  | A  | B  | C  | B   |
| 4  | C  | C  | B  | D  | B   |
| 5  | B  | B  | B  | C  | B   |
| 6  | C  | B  | C  | B  | B   |
| 7  | B  | C  | C  | D  | B   |
| 8  | B  | C  | A  | B  | B   |
| 9  | B  | C  | C  | B  | B   |
| 10 | B  | B  | B  | B  | B   |

---

*이 교재는 Scott Wlaschin의 "Domain Modeling Made Functional"을 Java 25와 이커머스 도메인에 맞춰 재구성한 것입니다.*
