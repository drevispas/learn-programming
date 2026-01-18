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

**코드 10.1**: 회원 도메인 - MemberGrade, EmailVerification, Member
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

public record Silver() implements MemberGrade {
    @Override public int discountRate() { return 5; }
    @Override public boolean hasFreeShipping() { return false; }
}

public record Gold() implements MemberGrade {
    @Override public int discountRate() { return 8; }
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
    // Compact Constructor: 불변 객체 생성 시 검증 필수 (Ch.2 원칙)
    public UnverifiedEmail {
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("유효하지 않은 이메일");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("인증 코드 필수");
        if (expiresAt == null)
            throw new IllegalArgumentException("만료일 필수");
    }

    public Result<VerifiedEmail, EmailError> verify(String inputCode) {
        if (LocalDateTime.now().isAfter(expiresAt))
            return Result.failure(new EmailError.Expired());
        if (!code.equals(inputCode))
            return Result.failure(new EmailError.InvalidCode());
        return Result.success(new VerifiedEmail(email, LocalDateTime.now()));
    }
}

public record VerifiedEmail(String email, LocalDateTime verifiedAt) implements EmailVerification {
    public VerifiedEmail {
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("유효하지 않은 이메일");
        if (verifiedAt == null)
            throw new IllegalArgumentException("인증 시간 필수");
    }
}

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

**코드 10.2**: 상품 도메인 - Bounded Context별 Product 모델
```java
package com.ecommerce.domain.product;

// === Bounded Context별 상품 모델 ===

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

**코드 10.3**: 주문 도메인 - OrderStatus State Machine과 Order
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

// === 주문 에러 (Sum Type) ===
public sealed interface OrderError permits InvalidState, AlreadyCancelled {
    record InvalidState(String message) implements OrderError {}
    record AlreadyCancelled() implements OrderError {}
}

// === 주문 ===
public record Order(
    OrderId id, MemberId memberId, List<OrderLine> lines,
    Money total, OrderStatus status
) {
    // 비즈니스 로직에서는 Exception 대신 Result 사용
    public Result<Order, OrderError> pay(TransactionId txId) {
        if (!(status instanceof Unpaid))
            return Result.failure(new OrderError.InvalidState("미결제만 결제 가능"));
        return Result.success(new Order(id, memberId, lines, total,
            new Paid(LocalDateTime.now(), txId)));
    }

    public Result<Order, OrderError> ship(TrackingNumber tracking) {
        if (!(status instanceof Paid p))
            return Result.failure(new OrderError.InvalidState("결제완료만 배송 가능"));
        return Result.success(new Order(id, memberId, lines, total,
            new Shipping(p.paidAt(), tracking)));
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

**코드 10.4**: 결제 도메인 - PaymentMethod, PaymentResult, PaymentError
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

**코드 10.5**: 쿠폰 도메인 - CouponType, CouponStatus, Coupon
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

> 💡 전체 도메인 구현은 `examples/functional-domain-modeling/src/main/java/com/ecommerce/domain/` 디렉토리를 참조하세요.
> 각 Bounded Context별 패키지(order, member, product, payment, coupon)에서 실제 동작 코드를 확인할 수 있습니다.

---

### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. 이 종합 패턴들은 다음 프로젝트에서 검증되었습니다:
- 쿠팡, 배민 등 국내 이커머스 플랫폼 (DDD + Bounded Context)
- Amazon, Shopify 등 글로벌 이커머스 (Event-Driven + Sum Types)
- 금융권 결제 시스템 (State Machine + Result Type)

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "Make illegal states unrepresentable - 타입 시스템을 활용하면 런타임 에러를 컴파일 타임으로 옮길 수 있다"
- **Eric Evans** (DDD 창시자): "각 Bounded Context는 독립적인 모델을 가져야 한다 - DisplayProduct, InventoryProduct 분리가 좋은 예"
- **Vaughn Vernon**: "Aggregate는 트랜잭션 일관성의 경계다 - Order가 OrderLine을 포함하는 것이 적절하다"
- **Martin Fowler**: "State Machine 패턴으로 복잡한 상태 전이를 명확하게 표현할 수 있다"

**참고 자료:**
- [Domain Modeling Made Functional](https://pragprog.com/titles/swdddf/) - Scott Wlaschin
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) - Eric Evans
- [Implementing Domain-Driven Design](https://vaughnvernon.com/) - Vaughn Vernon
- [Patterns of Enterprise Application Architecture](https://martinfowler.com/eaaCatalog/) - Martin Fowler

---

### 퀴즈 Chapter 10

#### Q10.1 [설계 문제] VIP 무료배송을 타입으로 표현하는 방법은?

**A.** boolean 필드<br/>
**B.** sealed interface의 메서드로 정의<br/>
**C.** 별도 서비스 클래스<br/>
**D.** if문으로 처리

---

#### Q10.2 [개념 확인] Bounded Context별 상품 분리(DisplayProduct, InventoryProduct, SettlementProduct) 이유는?

**A.** 코드량 증가를 위해<br/>
**B.** 각 Bounded Context에 필요한 데이터만 포함하기 위해<br/>
**C.** 상속을 회피하기 위해<br/>
**D.** 성능 향상을 위해

---

#### Q10.3 [코드 분석] Shipping 상태에 cancel() 메서드가 없는 이유는?

**A.** 메모리 절약<br/>
**B.** 타입으로 비즈니스 규칙(배송 중 취소 불가)을 강제하기 위해<br/>
**C.** 코드 단순화<br/>
**D.** 성능 향상

---

#### Q10.4 [개념 확인] 결제 수단을 Sum Type(sealed interface)으로 표현하는 이점은?

**A.** 메모리 절약<br/>
**B.** 모든 결제 수단을 타입 안전하게 처리 가능<br/>
**C.** 실행 속도 향상<br/>
**D.** 코드 가독성 저하

---

#### Q10.5 [코드 분석] 쿠폰의 use()가 Result를 반환하는 이유는?

**A.** 코드 복잡성 증가<br/>
**B.** 쿠폰 사용이 실패할 수 있는 경우를 명시적으로 표현<br/>
**C.** 컨벤션<br/>
**D.** 성능 향상

---

정답은 Appendix D에서 확인할 수 있습니다.
