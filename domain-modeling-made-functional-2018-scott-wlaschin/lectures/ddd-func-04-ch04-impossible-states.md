# Chapter 4: 불가능한 상태 제거하기

> Part I: 기초 - 도메인과 타입 시스템

---

### 학습 목표
1. NULL의 문제점과 Optional의 올바른 사용법을 이해한다
2. State Machine 패턴으로 상태 전이를 안전하게 모델링할 수 있다
3. 타입 시스템으로 비즈니스 규칙을 컴파일 타임에 강제할 수 있다
4. "Make Illegal States Unrepresentable" 원칙을 실무에 적용할 수 있다

---

### 🎯 [핵심 동기 예시] 왜 상태별 Entity가 필요한가?

**코드 4.1**: 타입으로 처리 순서 강제
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

**코드 4.2**: NULL의 위험성
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

**코드 4.3**: 타입으로 이메일 인증 상태 강제
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

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. "Make Illegal States Unrepresentable" 원칙은 다음에서 검증되었습니다:
- Elm 언어의 핵심 설계 원칙
- Rust의 타입 시스템
- Haskell의 모나드 설계

**Expert Opinions:**
- **Yaron Minsky** (Jane Street Capital): "Make Illegal States Unrepresentable - 이 원칙을 따르면 버그의 전체 클래스를 제거할 수 있다."
- **Scott Wlaschin** (원저자): "타입은 문서이자 컴파일러가 검사하는 명세서다. 타입이 허용하지 않는 것은 코드에서 발생할 수 없다."

---

### 4.3 상태별 Entity 패턴 (xxxEntity)

#### 타입으로 상태 전이 강제하기

**그림 4.1**: 주문 상태 전이 다이어그램 (State Machine)

![Order State Machine](https://images.doclify.net/gleek-web/d/420c9e96-63da-4d78-834f-0fc4af7f71a7.png)
*출처: [Online Shopping State Diagram - Gleek](https://www.gleek.io/templates/online-shopping-state)*

> 위 다이어그램은 일반적인 온라인 쇼핑 상태 흐름을 보여줍니다.
> 우리 도메인에서는 `Unpaid → Paid → Shipping → Delivered` 상태 전이를 타입으로 강제합니다.

**코드 4.4**: 상태별 Entity 패턴
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

**코드 4.5**: 타입 시스템이 비즈니스 규칙 강제
```java
UnpaidOrder unpaid = new UnpaidOrder(...);
unpaid.startShipping(tracking);  // 컴파일 에러! 메서드 없음

ShippingOrder shipping = paid.startShipping(tracking);
shipping.cancel();  // 컴파일 에러! 메서드 없음
```

---

### 4.4 Phantom Type 패턴: 보이지 않는 타입 제약

#### 💡 비유: 도장
> **Phantom Type은 문서의 도장과 같습니다.**
> 문서 내용은 바뀌지 않지만, "검토 완료" 도장이 찍히면 다음 단계로 넘어갈 수 있습니다.
> 도장은 문서의 실제 데이터가 아니지만, 프로세스 상태를 표시합니다.

**코드 4.6**: Phantom Type을 사용한 이메일 검증
```java
// Phantom Type: 상태를 제네릭 파라미터로 표시 (런타임 비용 없음)
public sealed interface EmailState {}
public record Unverified() implements EmailState {}
public record Verified() implements EmailState {}

public record Email<S extends EmailState>(String value) {
    public static Email<Unverified> unverified(String value) {
        if (!value.contains("@")) throw new IllegalArgumentException("형식 오류");
        return new Email<>(value);
    }
}

// Unverified → Verified 변환 (검증 통과 시)
public class EmailVerificationService {
    public Email<Verified> verify(Email<Unverified> email, String code) {
        if (verifyCode(email.value(), code))
            return new Email<>(email.value());  // 같은 값, 다른 타입!
        throw new VerificationFailedException();
    }
}

// Verified 이메일만 받음 → 인증 안 된 이메일로 가입 불가!
public class MemberService {
    public Member register(Email<Verified> email, String name) { ... }
}
```

#### 사용 예시

**코드 4.7**: Phantom Type 사용 예시
```java
Email<Unverified> rawEmail = Email.unverified("user@example.com");

// ❌ 컴파일 에러! Unverified로는 회원 가입 불가
memberService.register(rawEmail, "홍길동");

// ✅ 검증 후 사용
Email<Verified> verifiedEmail = verificationService.verify(rawEmail, "123456");
memberService.register(verifiedEmail, "홍길동");  // OK!
```

---

### 4.5 Optional 안티패턴 심화

Chapter 3.7에서 소개한 Optional 안티패턴을 실무 관점에서 더 자세히 살펴봅니다.

#### 안티패턴 1: Optional을 컬렉션처럼 사용

**코드 4.8**: Optional 안티패턴 - isPresent/get 패턴
```java
// ❌ 복잡하고 의도가 불명확
Optional<Customer> customer = findCustomer(id);
if (customer.isPresent()) {
    Customer c = customer.get();
    // ...
}

// ✅ 패턴 매칭 스타일로 명확하게
findCustomer(id)
    .ifPresentOrElse(
        customer -> processCustomer(customer),
        () -> handleNotFound()
    );
```

#### 안티패턴 2: Optional 체이닝 남용

**코드 4.9**: Optional 체이닝 vs 전용 타입
```java
// ❌ 너무 긴 체이닝은 가독성 저하
return order.flatMap(Order::getCustomer)
            .flatMap(Customer::getAddress)
            .flatMap(Address::getCity)
            .orElse("Unknown");

// ✅ 도메인 타입으로 "없음"을 명시적으로 표현
public sealed interface ShippingAddress permits
    KnownAddress, UnknownAddress {}
```

---

### 퀴즈 Chapter 4

#### Q4.1 [개념 확인] `ShippingOrder`에 `cancel()` 메서드가 없으면 어떤 효과가 있나요?

**A.** 예외가 발생한다<br/>
**B.** if문으로 체크한다<br/>
**C.** 호출 자체가 컴파일 에러가 된다 *(정답)*<br/>
**D.** DB 트리거로 막는다

---

#### Q4.2 [설계 문제] "인증되지 않은 이메일로 주문 불가"를 타입으로 강제하려면?

**A.** 생성자에서 if문 체크<br/>
**B.** `createOrder(VerifiedEmail email, ...)` 시그니처 사용 *(정답)*<br/>
**C.** @NotNull 어노테이션<br/>
**D.** 런타임 예외

---

#### Q4.3 [코드 분석] Phantom Type

다음 코드에서 `Email<Verified>`와 `Email<Unverified>`의 런타임 차이는?

**코드 4.10**: Phantom Type 런타임 분석
```java
Email<Unverified> raw = Email.unverified("a@b.com");
Email<Verified> verified = verificationService.verify(raw, "123456");
```

**A.** 내부 데이터 구조가 다르다<br/>
**B.** 런타임에는 차이가 없고 컴파일 타임에만 구분된다 *(정답)*<br/>
**C.** Verified는 추가 검증 데이터를 저장한다<br/>
**D.** 메모리 사용량이 다르다

---

#### Q4.4 [설계 문제] Optional vs 전용 타입

"주문에 쿠폰이 적용될 수도 있고 안 될 수도 있다"를 모델링할 때 가장 적합한 방식은?

**A.** `Optional<Coupon> coupon` 필드 사용<br/>
**B.** `@Nullable Coupon coupon` 어노테이션<br/>
**C.** `sealed interface CouponStatus permits WithCoupon, WithoutCoupon` *(정답)*<br/>
**D.** `boolean hasCoupon` 플래그와 `Coupon coupon` 필드

---

정답은 Appendix D에서 확인할 수 있습니다.
