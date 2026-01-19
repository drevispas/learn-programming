# Chapter 4: 불가능한 상태를 표현 불가능하게 만들기

## 학습 목표
1. "Make Illegal States Unrepresentable" 원칙의 의미를 이해한다
2. 유효성 검증 없이 안전한 코드를 설계할 수 있다
3. Sealed Interface의 망라성(Exhaustiveness)을 활용할 수 있다
4. 타입 시스템으로 비즈니스 규칙을 강제할 수 있다
5. 상태 전이를 타입으로 모델링할 수 있다

---

## 4.1 유효성 검증이 필요 없는 설계

> **🎯 왜 배우는가?**
>
> if문으로 유효성 검증을 수십 줄씩 작성하느라 지치셨나요?
> 타입 시스템으로 불가능한 상태를 표현 불가능하게 만들면 **런타임 검증 코드가 대폭 줄어들고**,
> 컴파일러가 버그를 미리 잡아줍니다.

### ❌ 안티패턴: 런타임 검증에 의존

**왜 문제인가?**
- **검증 누락 위험**: 개발자가 검증 코드를 빠뜨리면 버그 발생
- **중복 검증**: 여러 곳에서 동일한 검증 로직 반복
- **런타임 오류**: 컴파일은 통과하지만 실행 시 예외 발생

**Code 4.1**: 전통적인 방식 - 런타임 검증 (안티패턴)
```java
// 전통적인 방식
public class OrderService {
    public void processOrder(Order order) {
        // 런타임에 유효성 검증
        if (order.getStatus() == null) {
            throw new IllegalStateException("상태가 없습니다");
        }
        if (order.isPaid() && order.isCanceled()) {
            throw new IllegalStateException("결제됐는데 취소됨?");
        }
        if (order.isShipped() && !order.isPaid()) {
            throw new IllegalStateException("결제 없이 배송됨?");
        }
        // ... 수십 가지 검증 로직

        // 실제 비즈니스 로직
        doProcess(order);
    }
}
```

### ✅ 권장패턴: 타입으로 불가능한 상태 차단

**왜 좋은가?**
- **컴파일 타임 보장**: 잘못된 상태는 컴파일 자체가 안 됨
- **검증 코드 불필요**: 타입이 유효성을 보장하므로 검증 로직 감소
- **자기 문서화**: 타입 정의만 봐도 가능한 상태가 명확함

**Code 4.2**: DOP 방식 - 컴파일 타임 강제
```java
// DOP 방식: 불가능한 상태가 타입으로 표현 불가능
sealed interface OrderStatus {
    record Unpaid() implements OrderStatus {}
    record Paid(PaymentId paymentId) implements OrderStatus {}
    record Shipped(TrackingNumber tracking) implements OrderStatus {}
    record Canceled(CancelReason reason) implements OrderStatus {}
}

public record Order(OrderId id, List<OrderItem> items, OrderStatus status) {}

// 이제 "결제됐는데 취소됨"은 타입으로 표현 불가능!
// Order는 항상 하나의 명확한 상태만 가짐
```

### 비유: 자물쇠와 열쇠

> **타입 시스템은 자물쇠와 같습니다.**
>
> **런타임 검증 (경비원)**:
> - 문이 열려있고, 경비원이 지키고 있습니다
> - 경비원이 자리를 비우면 아무나 들어올 수 있습니다
> - 경비원도 실수할 수 있습니다
>
> **컴파일 타임 강제 (자물쇠)**:
> - 문 자체가 잠겨있고, 열쇠가 맞아야만 열립니다
> - 열쇠 없이는 아무도 들어올 수 없습니다
> - 자물쇠는 실수하지 않습니다
>
> DOP는 "경비원을 더 잘 훈련시키자"가 아니라
> "처음부터 자물쇠를 설치하자"입니다.

---

## 4.2 실전 예제: 이메일 인증 상태

> **🎯 왜 배우는가?**
>
> "이메일이 null인데 인증됨 상태?"라는 논리적 모순을 경험해 본 적 있으신가요?
> 실전 예제를 통해 **boolean 필드의 함정을 피하고 합 타입으로 안전하게 설계**하는 방법을 익힐 수 있습니다.

**Code 4.3**: Boolean으로 모델링 (안티패턴)
```java
// 불가능한 상태가 가능한 설계
class User {
    String email;
    boolean isEmailVerified;
    LocalDateTime emailVerifiedAt;

    // 문제: 이메일이 null인데 verified가 true?
    // 문제: verified가 false인데 verifiedAt이 있음?
    // 문제: verified가 true인데 verifiedAt이 null?
}
```

**Code 4.4**: 합 타입으로 모델링 (DOP 권장)
```java
// 불가능한 상태가 불가능한 설계
sealed interface UserEmail {
    record Unverified(String email) implements UserEmail {}
    record Verified(String email, LocalDateTime verifiedAt) implements UserEmail {}
}

public record User(UserId id, String name, UserEmail email) {}

// Verified 상태에는 반드시 verifiedAt이 존재
// Unverified 상태에는 verifiedAt이 없음
// 이메일 없이 인증됨 상태는 타입으로 표현 불가능!
```

---

## 4.3 Switch Expression과 망라성(Exhaustiveness)

> **🎯 왜 배우는가?**
>
> 새로운 상태를 추가했는데 처리 코드를 깜빡 잊어서 버그가 발생한 적 있으신가요?
> 망라성 검증을 활용하면 **새 상태 추가 시 컴파일러가 누락된 처리를 알려주므로**,
> 휴먼 에러를 원천 차단할 수 있습니다.

Sealed Interface는 컴파일러가 모든 케이스를 처리했는지 검증합니다.

**Code 4.5**: 망라성 검증 - 모든 케이스 처리
```java
public String getStatusMessage(UserEmail email) {
    return switch (email) {
        case Unverified u -> "이메일 인증이 필요합니다: " + u.email();
        case Verified v -> "인증 완료 (" + v.verifiedAt() + ")";
        // default 불필요! 모든 케이스를 처리했으므로
    };
}
```

### 새 상태 추가 시 컴파일러의 도움

**Code 4.6**: 새 상태 추가 시 컴파일 에러 발생
```java
// 새로운 상태 추가
sealed interface UserEmail {
    record Unverified(String email) implements UserEmail {}
    record Verified(String email, LocalDateTime verifiedAt) implements UserEmail {}
    record Banned(String email, String reason) implements UserEmail {}  // 새로 추가!
}

// 기존 코드
public String getStatusMessage(UserEmail email) {
    return switch (email) {
        case Unverified u -> "이메일 인증이 필요합니다: " + u.email();
        case Verified v -> "인증 완료 (" + v.verifiedAt() + ")";
        // 컴파일 에러! 'Banned'를 처리하지 않았습니다!
    };
}
```

---

## 4.4 상태 전이를 타입으로 강제하기

> **🎯 왜 배우는가?**
>
> "결제 전에 배송됨?" 같은 잘못된 상태 전이로 버그가 발생한 적 있으신가요?
> 상태 전이를 타입으로 강제하면 **비즈니스 규칙을 코드로 표현**할 수 있고,
> 잘못된 전이는 컴파일 단계에서 차단됩니다.

### 상태 전이 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                    주문 상태 전이 다이어그램                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                         ┌──────────┐                                │
│                         │  Unpaid  │                                │
│                         │ (미결제) │                                │
│                         └────┬─────┘                                │
│                    pay()│          │cancel()                        │
│                         ↓          ↓                                │
│                  ┌──────────┐  ┌───────────┐                        │
│                  │   Paid   │  │ Canceled  │                        │
│                  │ (결제됨) │  │  (취소됨) │                        │
│                  └────┬─────┘  └───────────┘                        │
│                       │                                             │
│                       │ship()                                       │
│                       ↓                                             │
│                  ┌──────────┐                                       │
│                  │ Shipped  │    ✗ Paid에서 cancel() 불가           │
│                  │ (배송중) │    ✗ Shipped에서 cancel() 불가        │
│                  └────┬─────┘    ✗ Canceled에서 어떤 전이도 불가    │
│                       │                                             │
│                       │deliver()                                    │
│                       ↓                                             │
│                  ┌───────────┐                                      │
│                  │ Delivered │  → 타입 시스템이 잘못된 전이를       │
│                  │ (배송완료)│     컴파일 에러로 차단!               │
│                  └───────────┘                                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Code 4.7**: 상태 전이 메서드 - 가능한 전이만 정의
```java
// 각 상태 타입에서 가능한 전이만 메서드로 정의
sealed interface OrderStatus {

    record Unpaid() implements OrderStatus {
        public Paid pay(PaymentId paymentId) {
            return new Paid(LocalDateTime.now(), paymentId);
        }
        public Canceled cancel(CancelReason reason) {
            return new Canceled(LocalDateTime.now(), reason);
        }
    }

    record Paid(LocalDateTime at, PaymentId paymentId) implements OrderStatus {
        public Shipped ship(TrackingNumber tracking) {
            return new Shipped(LocalDateTime.now(), tracking);
        }
        // cancel은 없음 - Paid 상태에서 취소 불가!
    }

    record Shipped(LocalDateTime at, TrackingNumber tracking)
        implements OrderStatus {
        public Delivered deliver() {
            return new Delivered(LocalDateTime.now());
        }
    }

    record Delivered(LocalDateTime at) implements OrderStatus {}
    record Canceled(LocalDateTime at, CancelReason reason) implements OrderStatus {}
}

// 사용
Unpaid unpaid = new Unpaid();
Paid paid = unpaid.pay(paymentId);     // OK
Shipped shipped = paid.ship(tracking); // OK
// paid.cancel(reason);                // 컴파일 에러! Paid에는 cancel이 없음
```

---

## 퀴즈 Chapter 4

### Q4.1 [개념 확인] 불가능한 상태
"Make Illegal States Unrepresentable"의 의미는?

A. 모든 상태를 enum으로 정의한다<br/>
B. 유효하지 않은 상태를 타입으로 표현할 수 없게 설계한다<br/>
C. 런타임에 철저히 검증한다<br/>
D. 모든 필드를 private으로 숨긴다

---

### Q4.2 [코드 분석] 설계 문제점
다음 설계의 문제점은?

```java
record DeliveryInfo(
    boolean isDelivered,
    LocalDateTime deliveredAt,
    String receiverName
) {}
```

A. Record를 사용한 것이 문제<br/>
B. isDelivered가 false인데 deliveredAt, receiverName이 있을 수 있음<br/>
C. 필드가 너무 적음<br/>
D. 문제없음

### Q4.3 [버그 찾기] 망라성 문제
다음 코드의 잠재적 문제점은?

```java
sealed interface OrderStatus permits Pending, Confirmed, Shipped {}

String getMessage(OrderStatus status) {
    return switch (status) {
        case Pending p -> "대기중";
        case Confirmed c -> "확정됨";
        default -> "알 수 없음";
    };
}
```

A. default가 있어서 Shipped가 처리되지 않음<br/>
B. sealed interface를 잘못 사용함<br/>
C. switch가 아닌 if-else를 써야 함<br/>
D. 문제없음

---

### Q4.4 [설계 문제] 리팩토링
다음을 "불가능한 상태가 불가능한" 설계로 리팩토링하세요.

```java
class Coupon {
    String code;
    boolean isUsed;
    LocalDateTime usedAt;
    String usedByMemberId;
}
```

A. Unused(code)와 Used(code, usedAt, usedByMemberId)로 분리<br/>
B. isUsed를 enum으로 변경<br/>
C. usedAt과 usedByMemberId를 Optional로 변경<br/>
D. 현재 설계가 적절함

---

### Q4.5 [코드 작성] 배송 상태 설계
이커머스의 "배송 상태"를 불가능한 상태가 없도록 설계하세요.
- 준비중 (Preparing)
- 출고완료 (Dispatched) - 출고시각, 택배사 정보 필요
- 배송중 (InTransit) - 운송장 번호, 현재 위치 필요
- 배송완료 (Delivered) - 수령시각, 수령인 이름 필요
- 반송됨 (Returned) - 반송시각, 반송사유 필요

---

정답은 Appendix C에서 확인할 수 있습니다.
