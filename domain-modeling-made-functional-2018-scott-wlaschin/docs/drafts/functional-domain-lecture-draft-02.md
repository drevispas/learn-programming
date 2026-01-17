# [강의명] Java 25로 정복하는 함수형 도메인 모델링 (Functional Domain Modeling)

## 0. 강의 소개 (Introduction)

안녕하세요, 개발 리더님. 이 강의는 "Domain Modeling Made Functional"의 핵심 철학을 귀하의 전문 분야인 **이커머스 도메인**에 적용하여, 견고하고 유지보수하기 쉬운 소프트웨어를 만드는 방법을 다룹니다.

우리는 **Java 25**의 최신 기능(Records, Sealed Interfaces, Pattern Matching, Value Objects)을 적극 활용할 것입니다. 기존의 'Database-Driven' 혹은 'Class-Driven' 방식은 잊으십시오. 우리는 **Type-Driven Design**을 할 것입니다.

---

## 1. 목차 (Table of Contents)

*   **Module 1. 도메인 이해와 타입 시스템 (Understanding Domain & Types)**
    *   1.1 Ubiquitous Language와 Shared Model
    *   1.2 원시 타입 집착(Primitive Obsession) 탈피: Simple Types
    *   1.3 복합 타입 모델링: AND 타입과 OR 타입
*   **Module 2. 워크플로우와 파이프라인 (Workflows as Pipelines)**
    *   2.1 함수로서의 비즈니스 로직
    *   2.2 ROP(Railway Oriented Programming): 에러 핸들링
    *   2.3 전체 파이프라인 조립
*   **Module 3. 도메인 무결성과 영속성 (Integrity & Persistence)**
    *   3.1 불가능한 상태를 불가능하게 만들기 (Make Illegal States Unrepresentable)
    *   3.2 영속성 격리 (Persistence Ignorance)
*   **Chapter 4. 심화 퀴즈 및 해설 (Quizzes & Solutions)**

---

## Module 1. 도메인 이해와 타입 시스템 (Understanding Domain & Types)

### 1.1 Ubiquitous Language와 Shared Model

도메인 주도 설계(DDD)의 핵심은 기획자(Domain Expert)와 개발자가 같은 언어를 쓰는 것입니다. 코드는 번역기가 되어서는 안 됩니다. 코드가 곧 문서가 되어야 합니다.

### 1.2 원시 타입 집착(Primitive Obsession) 탈피: Simple Types

이커머스에서 `Email`, `OrderPrice`, `CouponCode`는 모두 `String`이나 `BigDecimal`로 표현될 수 있습니다. 하지만 이들은 서로 다릅니다. 실수로 `CouponCode` 자리에 `Email`을 넣는 것을 컴파일러가 막아줘야 합니다.

**[Code 1-1] Simple Wrapper Types (Java 25)**

```java
// Java 25의 Record 기능을 사용하여 불변(Immutable) 래퍼 타입을 정의합니다.
// 'value classes' (Project Valhalla)가 적용되어 오버헤드가 거의 없습니다.

package com.ecommerce.domain.types;

// 1. 회원 ID: 단순한 Long이 아니라 MemberId라는 타입을 명시합니다.
public record MemberId(long value) {
    public MemberId {
        // 생성자에서 유효성 검사(Validation)를 수행하여
        // 잘못된 데이터가 생성되는 것을 원천 차단합니다.
        if (value <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }
    }
}

// 2. 이메일: 정규식을 통과해야만 객체가 생성됩니다.
public record Email(String value) {
    public Email {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다.");
        }
    }
}

// 3. 주문 금액: 0원 미만은 존재할 수 없습니다.
public record OrderPrice(java.math.BigDecimal value) {
    public OrderPrice {
        if (value.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("주문 금액은 음수가 될 수 없습니다.");
        }
    }
}
```

### 1.3 복합 타입 모델링: AND 타입과 OR 타입 [핵심]

이 부분이 책의 하이라이트입니다. 데이터 구조는 크게 두 가지로 나뉩니다.

1.  **AND 타입 (Product Type):** A **그리고** B가 있어야 한다. (Java의 `record`)
2.  **OR 타입 (Sum Type):** A **또는** B 중 하나다. (Java의 `sealed interface`)

이커머스의 **결제 수단(PaymentMethod)**을 예로 들어봅시다. 결제는 신용카드 **또는** 카카오페이 **또는** 무통장입금 중 하나입니다.

**[Code 1-2] Modeling Choice Types (OR Types)**

```java
package com.ecommerce.domain.payment;

import com.ecommerce.domain.types.OrderPrice;

// 'sealed interface'는 허용된 구현체 외에는 상속을 금지합니다.
// 이것이 바로 'OR 타입' (Choice Type)입니다.
public sealed interface PaymentMethod permits CreditCard, KakaoPay, BankTransfer {
}

// Case 1: 신용카드 (카드번호와 유효기간이 필요함)
public record CreditCard(
    String cardNumber,
    String expiryDate
) implements PaymentMethod {}

// Case 2: 카카오페이 (토큰만 있으면 됨)
public record KakaoPay(
    String transactionToken
) implements PaymentMethod {}

// Case 3: 무통장입금 (가상계좌번호가 필요함)
public record BankTransfer(
    String virtualAccountNumber
) implements PaymentMethod {}

// 사용 예시: Pattern Matching for switch (Java 21+)
public class PaymentProcessor {
    public void process(PaymentMethod method, OrderPrice price) {
        // 컴파일러가 모든 케이스(CreditCard, KakaoPay, BankTransfer)를
        // 처리했는지 검사해줍니다. (Exhaustiveness checking)
        switch (method) {
            case CreditCard c -> System.out.println("카드 결제: " + c.cardNumber());
            case KakaoPay k -> System.out.println("카카오 결제: " + k.transactionToken());
            case BankTransfer b -> System.out.println("입금 대기: " + b.virtualAccountNumber());
        }
    }
}
```

> **상세 설명:**
> 전통적인 OOP에서는 공통 부모 클래스를 두고 상속을 받지만, 필드가 제각각이라 다루기 어렵습니다. 함수형 모델링에서는 `PaymentMethod`가 가질 수 있는 경우의 수를 `sealed`로 제한하고, `switch` 문을 통해 각 케이스별로 명확하게 로직을 분기합니다. 이는 비즈니스 로직의 누락을 방지합니다.

---

## Module 2. 워크플로우와 파이프라인 (Workflows as Pipelines)

### 2.1 함수로서의 비즈니스 로직

비즈니스 프로세스는 **입력(Input)**을 받아 **출력(Output)**을 내놓는 파이프라인과 같습니다. 이커머스의 '주문(Order)'은 상태가 변하는 데이터가 아니라, 변환되는 과정입니다.

*   `UnvalidatedOrder` (검증 전 주문) -> **[Validate]** -> `ValidatedOrder`
*   `ValidatedOrder` -> **[Price]** -> `PricedOrder` (가격 계산 완료)
*   `PricedOrder` -> **[Pay]** -> `PaidOrder` (결제 완료)

**[Code 2-1] Workflow Modeling**

```java
package com.ecommerce.domain.order;

// 상태별로 타입을 따로 만듭니다. (State Machine)
public record UnvalidatedOrder(...) {}
public record ValidatedOrder(...) {}
public record PricedOrder(...) {}

// 워크플로우의 각 단계는 함수(Functional Interface)로 정의됩니다.
@FunctionalInterface
public interface ValidateOrder {
    ValidatedOrder apply(UnvalidatedOrder input);
}

@FunctionalInterface
public interface PriceOrder {
    PricedOrder apply(ValidatedOrder input);
}
```

### 2.2 ROP(Railway Oriented Programming): 에러 핸들링

함수는 항상 성공하지 않습니다. 재고가 없거나, 쿠폰이 만료될 수 있습니다. 예외(`Exception`)를 던지는 것은 `GOTO`문과 같아서 파이프라인의 흐름을 깹니다. 대신 `Result` 타입을 사용해 성공과 실패를 철길(Railway)처럼 두 갈래로 다룹니다.

**[Code 2-2] The Result Type**

```java
package com.ecommerce.common;

// 성공(S) 또는 실패(F)를 담는 컨테이너
public sealed interface Result<S, F> permits Success, Failure {
    
    // 성공 시 실행할 함수 매핑 (flatMap / bind)
    default <NewS> Result<NewS, F> flatMap(java.util.function.Function<S, Result<NewS, F>> mapper) {
        return switch (this) {
            case Success<S, F> s -> mapper.apply(s.value());
            case Failure<S, F> f -> (Result<NewS, F>) f; // 실패는 그대로 전파 (철길의 실패 라인)
        };
    }
}

public record Success<S, F>(S value) implements Result<S, F> {}
public record Failure<S, F>(F error) implements Result<S, F> {}
```

### 2.3 전체 파이프라인 조립

이제 '주문 생성'이라는 큰 기능을 작은 함수들의 파이프라인으로 조립해 봅시다.

**[Code 2-3] Place Order Pipeline**

```java
package com.ecommerce.usecase;

import com.ecommerce.domain.order.*;
import com.ecommerce.common.*;

public class PlaceOrderWorkflow {

    // 의존성 주입 대신, 함수 자체를 주입받거나 정의합니다.
    // 여기서는 이해를 돕기 위해 메서드로 가정합니다.
    
    public Result<PaidOrder, OrderError> placeOrder(UnvalidatedOrder input) {
        return validate(input)              // 1. 검증: 실패 시 여기서 멈춤
            .flatMap(this::checkInventory)  // 2. 재고 확인
            .flatMap(this::applyCoupon)     // 3. 쿠폰 적용 및 가격 계산
            .flatMap(this::processPayment); // 4. 결제
    }

    // 각 단계의 시그니처(Signature)가 비즈니스 룰을 명확히 보여줍니다.
    private Result<ValidatedOrder, OrderError> validate(UnvalidatedOrder order) { ... }
    private Result<ValidatedOrder, OrderError> checkInventory(ValidatedOrder order) { ... }
    private Result<PricedOrder, OrderError> applyCoupon(ValidatedOrder order) { ... }
    private Result<PaidOrder, OrderError> processPayment(PricedOrder order) { ... }
}

// 도메인 에러도 OR 타입(sealed interface)으로 정의합니다.
sealed interface OrderError permits OutOfStock, InvalidCoupon, PaymentFailed {}
```

> **비유 설명:**
> 이것은 **자동차 조립 라인**과 같습니다. 차체(데이터)가 컨베이어 벨트(파이프라인)를 지나갑니다. 첫 번째 로봇은 도색(Validate)을 하고, 두 번째 로봇은 바퀴를 답니다(Price). 만약 도색 로봇이 "불량!"을 외치면(Failure), 차는 즉시 폐기 라인(Error Track)으로 빠지고 바퀴 조립 단계로 넘어가지 않습니다.

---

## Module 3. 도메인 무결성과 영속성 (Integrity & Persistence)

### 3.1 불가능한 상태를 불가능하게 만들기 (Make Illegal States Unrepresentable)

가장 중요한 개념입니다. 비즈니스 규칙을 `if-else`로 검사하지 말고, **타입 시스템으로 강제**하십시오.

**예제: 이메일 인증**
회원 가입 시 이메일 인증이 완료되지 않으면 계정을 활성화할 수 없어야 합니다.

**[Code 3-1] Enforcing Business Rules with Types**

```java
package com.ecommerce.domain.member;

// 인증되지 않은 이메일
public record UnverifiedEmail(String value) {}

// 인증된 이메일
// 이 객체는 생성자가 private이거나 package-private이라서,
// 오직 '인증 서비스'만이 이 객체를 만들 수 있다고 가정합니다.
public record VerifiedEmail(String value) {
    // 내부 로직 혹은 팩토리 메서드를 통해서만 생성 가능
}

public class MemberService {
    // 이 메서드는 인자로 'VerifiedEmail'을 요구합니다.
    // 즉, 인증하지 않은 이메일로는 '가입 완료' 함수를 호출조차 할 수 없습니다.
    // 컴파일러 레벨에서 비즈니스 룰 위반을 막습니다.
    public Member completeRegistration(VerifiedEmail email, String name) {
        return new Member(email, name);
    }
}
```

### 3.2 영속성 격리 (Persistence Ignorance)

도메인 모델은 DB가 RDB인지 NoSQL인지 몰라야 합니다. 도메인 객체(Entity)와 DB 테이블 매핑 객체(DTO/DAO)를 분리하십시오.

*   **Domain Model:** 비즈니스 로직, 행위 중심, 불변.
*   **Entity(DB):** 상태 저장, ORM 매핑용, 가변 가능.

강의에서는 도메인 로직이 끝난 후, 마지막 경계(Edge)에서 변환하는 과정을 다룹니다.

---

## 4. 퀴즈 및 해설 (Quizzes & Solutions)

다음 퀴즈를 통해 본 강의의 핵심 내용을 확인해 보세요.

---

### Quiz 1: 타입 시스템 (Type System)
**Q1.** 다음 중 "배송 주소(Address)"를 모델링할 때 가장 적절한 방식은 무엇이며, 그 이유는 무엇입니까? (Java 25 기준)

*   A) `public class Address { public String city; public String zipCode; ... }`
*   B) `public record Address(String city, String zipCode) { ... }`
*   C) `public record Address(City city, ZipCode zipCode) { ... }`

### Quiz 2: 불가능한 상태 (Illegal States)
**Q2.** "장바구니(Cart)"에는 '빈 장바구니', '담긴 장바구니', '결제된 장바구니' 상태가 있습니다. '결제하기' 기능은 오직 '담긴 장바구니'에서만 가능해야 합니다. 이를 컴파일 타임에 보장하려면 어떻게 코드를 작성해야 합니까?

### Quiz 3: 에러 핸들링 (Error Handling)
**Q3.** 비즈니스 로직(예: 재고 부족)을 처리할 때 Java의 `Exception`을 사용하는 것보다 `Result` 타입을 사용하는 것이 더 좋은 이유는 무엇입니까? (함수형 프로그래밍 관점에서 서술하시오)

---

## [별첨] 정답 및 상세 해설 (Answers & Explanations)

### A1. 정답: C
**해설:**
*   A는 가변(Mutable) 객체이며, 필드가 원시 타입(String)입니다.
*   B는 불변(Record)이지만, 여전히 원시 타입에 집착(Primitive Obsession)하고 있습니다. `zipCode` 자리에 실수로 `city` 문자열을 넣어도 컴파일러는 모릅니다.
*   **C가 정답입니다.** `City`, `ZipCode`라는 전용 타입(Simple Types)을 정의하여 사용했습니다. 이렇게 하면 데이터의 의미가 명확해지고, 잘못된 데이터를 대입하는 실수를 컴파일러가 잡아줍니다. 또한 `ZipCode`의 생성자 안에서 우편번호 형식 검증 로직을 응집시킬 수 있습니다.

### A2. 정답: 상태를 별도의 타입으로 정의하고, 메서드 시그니처로 제한한다.
**해설:**
```java
// 1. 상태를 sealed interface로 정의
sealed interface Cart permits EmptyCart, ActiveCart, PaidCart {}
record EmptyCart() implements Cart {}
record ActiveCart(List<Item> items) implements Cart {} // 아이템이 있음
record PaidCart(List<Item> items, LocalDateTime paidAt) implements Cart {}

// 2. 결제 함수
// 입력 타입이 'ActiveCart'로 명시되어 있습니다.
// EmptyCart나 PaidCart를 넣으려고 하면 컴파일 에러가 발생합니다.
public PaidCart checkout(ActiveCart cart) {
    return new PaidCart(cart.items(), LocalDateTime.now());
}
```
이렇게 하면 개발자가 실수로 빈 장바구니를 결제하거나, 이미 결제된 장바구니를 중복 결제하는 코드를 작성하는 것 자체가 불가능해집니다(Make Illegal States Unrepresentable).

### A3. 정답: 참조 투명성(Referential Transparency)과 흐름 제어의 명시성 때문입니다.
**해설:**
1.  **시그니처의 정직함:** 함수 시그니처 `Product purchase(Item i)`는 무조건 상품을 반환한다고 거짓말을 합니다(예외가 터질 수 있으므로). 반면 `Result<Product, OutOfStockError> purchase(Item i)`는 "재고가 없어서 실패할 수도 있음"을 명시적으로 알려줍니다.
2.  **GOTO문 방지:** 예외는 호출 스택을 거슬러 올라가며 어디서 잡힐지 모르는 점프(GOTO)와 같습니다. `Result` 패턴(ROP)을 사용하면 에러 처리 흐름이 데이터 흐름과 동일하게 선형적으로 유지되어 파이프라인 구성이 용이합니다.
3.  **타입 안전성:** 컴파일러가 실패 케이스(Failure)를 처리했는지 체크해 줄 수 있습니다.

---

이 문서는 Scott Wlaschin의 철학을 현대적인 Java 문법으로 재구성한 것입니다. 귀하의 팀이 이커머스 도메인의 복잡성을 타입 시스템으로 제어하는 데 큰 도움이 될 것입니다. 질문이 있거나 특정 파트를 더 깊게(예: 정산 로직의 복잡한 타입 모델링) 다루고 싶다면 언제든 말씀해 주십시오.
