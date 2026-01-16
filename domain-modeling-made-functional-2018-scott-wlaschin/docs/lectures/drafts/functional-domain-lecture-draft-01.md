# 📘 [교재] Java 25로 정복하는 도메인 주도 설계와 함수형 프로그래밍

대상: 이커머스 개발 리더 (회원, 주문, 결제, 정산 도메인 담당)

목표: 유지보수하기 쉽고, 버그가 없는 견고한 시스템 구축

도구: Java 25 (Record, Sealed Interface, Pattern Matching, Switch Expressions)

---

## 🔖 목차

1. **[C1] DDD와 함수형 사고의 기초:** 맥락이 왕이다

2. **[C2] 도메인 모델링 I (단순 타입):** 원시 타입의 저주 끊기

3. **[C3] 도메인 모델링 II (복합 타입):** OR 관계와 비즈니스 규칙

4. **[C4] 워크플로우와 에러 처리:** 파이프라인 만들기

5. **[C5] 아키텍처와 진화:** 도메인 격리하기

6. **[Appendix] 퀴즈 정답 및 상세 해설**

---

## [C1] DDD와 함수형 사고의 기초: 맥락이 왕이다

### [C1-S1] Bounded Context (제한된 컨텍스트)

많은 개발팀이 범하는 가장 큰 실수는 **"하나의 통합된 모델"**을 만들려고 하는 것입니다.

예를 들어 User라는 클래스 하나에 로그인 정보, 배송지 주소, 쿠폰 보유량, 정산 계좌 정보를 모두 넣으려 합니다. 이는 재앙의 시작입니다.

**DDD의 핵심:** 단어의 뜻은 **문맥(Context)**에 따라 달라진다.

- **인증 컨텍스트 (Auth Context):** 여기서 `User`는 ID, Password, Role이 중요합니다.

- **주문 컨텍스트 (Order Context):** 여기서 `User`는 `Customer`로 불리며, 배송지와 연락처가 중요합니다.

- **정산 컨텍스트 (Settlement Context):** 여기서 `User`는 `Payee`(지급 대상)이며, 계좌 번호와 사업자 등록 번호가 중요합니다.

이들을 억지로 합치지 말고, 서로 다른 패키지(Context)로 분리해야 합니다.

### [C1-S2] 불변성(Immutability)과 Java Record

함수형 프로그래밍에서 데이터는 **불변**이어야 합니다. 데이터가 변하지 않으면 멀티스레드 환경에서 안전하고, 코드의 흐름을 추적하기 쉬워집니다. Java 14+부터 도입된 `record`는 이를 위한 최고의 도구입니다.

- **기존 Class:** Getter/Setter, `equals`, `hashCode`, `toString`을 모두 작성(혹은 Lombok 사용). 상태가 변할 수 있음.

- **Java Record:** 선언만으로 불변 데이터 객체 완성. 모든 필드는 `final`.

### [Code-1.1] 컨텍스트별 모델 분리 예시

Java

```
// [Context: Auth] 인증을 위한 사용자 모델
package com.ecommerce.auth;

public record AppUser(    long id,    String username,    String passwordHash // 인증에만 필요한 정보) {}

// ---------------------------------------------------------

// [Context: Order] 주문을 위한 구매자 모델
package com.ecommerce.order;

import java.util.List;

public record Customer(    long customerId,    // Auth의 id와 매핑되지만 이름은 다를 수 있음    String shippingAddress,    List<String> couponCodes // 주문 시 사용할 수 있는 쿠폰들) {}

// ---------------------------------------------------------

// [Context: Settlement] 정산을 위한 판매자 모델
package com.ecommerce.settlement;

public record Seller(    long sellerId,    String bankAccountNumber,    double feeRate // 정산 수수료율 (정산팀만 관심 있음)
) {}
```

### 🧠 [Quiz-C1] 개념 확인

**Q1.** 귀하의 이커머스 팀에서 `Product`(상품) 클래스를 설계 중입니다.

- **전시 팀:** 상품의 이미지, 마케팅 문구, 별점이 중요함.

- 물류 팀: 상품의 무게, 부피, 창고 위치가 중요함.
  
  이 상황에서 올바른 DDD 접근법은?
  
  A. Product 클래스에 모든 필드를 넣고 @Nullable을 사용한다.
  
  B. Product 인터페이스를 만들고 DisplayProduct, LogisticsProduct가 상속받는다.
  
  C. context.display.Product와 context.logistics.Product를 각각 정의하고, 서로 다른 모델로 취급한다.
  
  D. 데이터베이스 테이블을 먼저 설계하고 그에 맞춰 클래스를 하나 만든다.

Q2. Java record에 대한 설명으로 틀린 것은?

A. 모든 필드는 기본적으로 private final이다.

B. setter 메서드가 자동으로 생성되어 값을 변경할 수 있다.

C. equals, hashCode, toString이 자동으로 생성된다.

D. 데이터(상태)를 보유하는 불변 객체를 만드는 데 최적화되어 있다.

---

## [C2] 도메인 모델링 I: 원시 타입의 저주 끊기

### [C2-S1] Primitive Obsession (원시 타입 집착)

이커머스 코드에서 흔히 보는 안티 패턴입니다.

Java

```
// 나쁜 예
public void processPayment(String email, double amount) { ... }
```

이 코드는 위험합니다. `email` 자리에 "서울시 강남구..." 같은 문자열이 들어와도 컴파일러는 모릅니다. `amount`에 `-1000`이 들어와도 모릅니다.

### [C2-S2] Value Object (값 객체)와 유효성 검사

의미 있는 단위는 전용 타입(`Wrapper Type`)으로 만들어야 합니다. Java Record의 **Compact Constructor**를 사용하면 생성 시점에 유효성을 강제할 수 있습니다.

### [Code-2.1] 안전한 값 객체 만들기

Java

```
package com.ecommerce.common;

// 1. 이메일 주소 타입
public record EmailAddress(String value) {
    // Compact Constructor: 괄호 없이 작성하며, 생성자 본문 실행 전 검증 로직 삽입 가능
    public EmailAddress {
        if (value == null || !value.contains("@")) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식입니다: " + value);
        }
        // 검증을 통과하면 자동으로 this.value = value; 가 실행됨
    }
}

// 2. 주문 금액 타입
public record OrderAmount(java.math.BigDecimal amount) {
    public OrderAmount {
        if (amount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("주문 금액은 0보다 작을 수 없습니다.");
        }
    }

    // 비즈니스 로직 추가 가능
    public static OrderAmount zero() {
        return new OrderAmount(java.math.BigDecimal.ZERO);
    }
}
```

이제 메서드 시그니처가 바뀝니다.

Java

```
// 좋은 예: 컴파일러가 타입 안전성을 보장함
public void processPayment(EmailAddress email, OrderAmount amount) { ... }
```

### 🧠 [Quiz-C2] 개념 확인

Q3. CouponCode라는 값 객체를 만들려고 합니다. 쿠폰 코드는 반드시 10자리 문자열이어야 합니다. record를 사용하여 이를 구현할 때, 유효성 검사 로직이 들어가기에 가장 적합한 위치는?

A. getCouponCode() 메서드 내부

B. 별도의 Validator 클래스

C. 레코드의 Compact Constructor 내부

D. 데이터베이스에 저장하기 직전

Q4. 다음 중 Value Object(값 객체)의 특징으로 알맞은 것은?

A. 식별자(ID)가 있어서 ID가 같으면 같은 객체다.

B. 속성 값(Value)이 같으면 같은 객체로 취급된다.

C. 내부의 값을 언제든지 변경할 수 있다(Mutable).

D. 보통 데이터베이스 테이블과 1:1로 매핑된다.

---

## [C3] 도메인 모델링 II: OR 관계와 비즈니스 규칙

### [C3-S1] AND 관계와 OR 관계

데이터 모델링은 두 가지 결합으로 이루어집니다.

1. **AND (Product Type):** A **그리고** B (예: 주문 = 상품 목록 `AND` 배송지 `AND` 결제 수단) -> **Java Record** 사용.

2. **OR (Sum Type):** A **또는** B (예: 결제 수단 = 신용카드 `OR` 계좌이체 `OR` 포인트) -> **Sealed Interface** 사용.

Java의 **Sealed Interface**는 상속 가능한 클래스를 엄격하게 제한하여, 컴파일러가 "이 외의 경우는 없다"는 것을 알게 해줍니다.

### [Code-3.1] 결제 수단 모델링 (Sealed Interface)

Java

```
package com.ecommerce.payment;

// 결제 수단은 아래 정의된 3가지(CreditCard, BankTransfer, Point) 중 하나일 수밖에 없음.
public sealed interface PaymentMethod     permits CreditCard, BankTransfer, Point {
}

// 1. 신용카드 (카드 번호 필요)
public record CreditCard(String cardNumber, String cvc) implements PaymentMethod {}

// 2. 계좌 이체 (은행 코드, 계좌 번호 필요)
public record BankTransfer(String bankCode, String accountNum) implements PaymentMethod {}

// 3. 포인트 결제 (별도 정보 필요 없음, 사용자 ID는 컨텍스트에 이미 있다고 가정)
public record Point() implements PaymentMethod {}
```

### [C3-S2] 불가능한 상태를 표현 불가능하게 만들기

주문 상태를 예로 들어봅시다.

- `Unpaid` (미결제) 상태에서는 `PaidDate`가 존재할 수 없습니다.

- 하지만 전통적인 클래스에서는 `Date paidDate` 필드를 두고 `null`로 관리했습니다. 이는 버그의 원상입니다.

- Sealed Interface를 쓰면 **상태별로 필요한 데이터만** 정의할 수 있습니다.

### [Code-3.2] 주문 상태 모델링

Java

```
package com.ecommerce.order;

public sealed interface OrderStatus permits Unpaid, Paid, Shipped, Cancelled {}

// 미결제: 주문 정보만 있음
public record Unpaid(String orderId, OrderAmount total) implements OrderStatus {}

// 결제 완료: 결제 일시 정보가 '추가'됨 (미결제 상태에는 아예 없는 필드)
public record Paid(String orderId, OrderAmount total, java.time.LocalDateTime paidAt) implements OrderStatus {}

// 배송 중: 운송장 번호가 '추가'됨
public record Shipped(String orderId, String trackingNumber) implements OrderStatus {}

// 취소: 취소 사유가 필수
public record Cancelled(String orderId, String reason) implements OrderStatus {}
```

### 🧠 [Quiz-C3] 개념 확인

Q5. 위 PaymentMethod 코드를 바탕으로 로직을 작성할 때, Java 25의 switch 식을 사용하면 어떤 장점이 있나요?

A. default 문을 반드시 작성해야 한다.

B. 모든 케이스(CreditCard, BankTransfer, Point)를 다루지 않으면 컴파일 에러가 발생하여 실수를 방지한다.

C. 코드가 더 길어지지만 가독성이 좋아진다.

D. 실행 속도가 10배 빨라진다.

Q6. "불가능한 상태를 표현 불가능하게 하라"는 원칙에 따를 때, '배송 전' 상태의 주문 객체에 '배송 완료일' 필드가 null로 존재하는 것은 올바른 설계인가요?

A. 예, 나중에 값이 채워지므로 편리하다.

B. 예, 데이터베이스 테이블 컬럼과 일치하므로 좋다.

C. 아니오, 해당 상태에서 존재해서는 안 되는 데이터는 타입 정의에서 아예 배제되어야 한다.

---

## [C4] 워크플로우와 에러 처리: 파이프라인 만들기

### [C4-S1] 워크플로우 = 함수 파이프라인

비즈니스 로직(예: 주문 하기)은 **입력**을 받아 **출력**을 내놓는 함수입니다.

- `Input`: 주문 요청서 (UnvalidatedOrder)

- `Process`: 검증 -> 가격 계산 -> 결제 승인

- `Output`: 주문 완료 이벤트 (OrderPlaced)

### [C4-S2] 예외(Exception) 대신 Result 타입

Java의 try-catch는 로직의 흐름을 끊습니다. 함수형 프로그래밍에서는 에러도 **값(Value)**으로 다룹니다.

(Java 표준에는 없지만, 직접 만들어 쓰거나 라이브러리를 사용합니다. 여기서는 교육용으로 간단히 정의합니다.)

### [Code-4.1] Result 타입과 워크플로우

Java

```
// 성공(Success)하거나 실패(Failure)하는 결과를 담는 컨테이너
sealed interface Result<T> permits Success, Failure {}
record Success<T>(T data) implements Result<T> {}
record Failure<T>(String errorMessage) implements Result<T> {}

class OrderService {

    // 워크플로우: 주문 검증 -> 재고 확인 -> 최종 주문 생성
    // Exception을 던지는 대신 Result를 반환함
    public Result<OrderPlaced> placeOrder(UnvalidatedOrder input) {

        // 1. 검증 단계
        Result<ValidOrder> validOrderResult = validate(input);

        // Java 25의 Switch Pattern Matching 활용 (코드가 평평해짐)
        return switch (validOrderResult) {
            case Failure<ValidOrder> f -> new Failure<>(f.errorMessage()); // 실패하면 즉시 리턴
            case Success<ValidOrder> s -> {
                // 2. 성공 시 다음 단계 (재고 확인) 진행
                // 실제로는 flatMap 등의 함수형 연산자를 쓰면 더 깔끔하지만, 
                // Java 네이티브 문법으로는 이렇게 표현 가능합니다.
                yield checkInventory(s.data()); 
            }
        };
    }

    private Result<ValidOrder> validate(UnvalidatedOrder input) {
        if (input.quantity() <= 0) return new Failure<>("수량 부족");
        return new Success<>(new ValidOrder(input.productId(), input.quantity()));
    }

    private Result<OrderPlaced> checkInventory(ValidOrder order) {
        // ... 재고 로직 ...
        return new Success<>(new OrderPlaced(order));
    }
}
```

### 🧠 [Quiz-C4] 개념 확인

Q7. 비즈니스 로직에서 예외(Exception) 대신 Result 타입을 반환하는 방식의 장점은 무엇인가요?

A. 코드가 짧아진다.

B. 에러 처리를 호출하는 쪽에서 강제로 확인하게 만들어, 예외 상황 누락을 방지한다.

C. 성능이 더 빠르다.

D. 디버깅이 필요 없다.

Q8. 위 placeOrder 메서드에서 validate가 실패(Failure)를 반환하면 어떤 일이 일어나나요?

A. 프로그램이 즉시 종료된다.

B. checkInventory 메서드가 실행된다.

C. Failure 객체가 그대로 반환되어, 호출자가 에러가 발생했음을 알 수 있게 된다.

D. NullPointerException이 발생한다.

---

## [C5] 아키텍처와 진화: 도메인 격리하기

### [C5-S1] 도메인 모델 vs DTO (Data Transfer Object)

우리가 앞서 만든 아름다운 도메인 모델(`Record`, `Sealed Interface`)을 외부(DB, Web API)에 그대로 노출하면 안 됩니다.

- **외부 세계:** JSON, DB Table (평면적, 모든 게 Nullable, 원시 타입 위주)

- **내부 세계(도메인):** Rich Types, 안전함, 계층적 구조

이 둘 사이에는 **변환기(Transformer/Mapper)**가 필요합니다.

### [Code-5.1] DTO와 도메인 변환

Java

```
// [외부 세계] 웹 요청으로 들어오는 DTO (단순하고 멍청한 객체)
public record OrderDto(String email, int count) {}

// [변환기] DTO -> Domain
public class OrderMapper {
    public Result<ValidOrder> toDomain(OrderDto dto) {
        try {
            // 원시 타입(String, int)을 도메인 타입(EmailAddress, Quantity)으로 변환 시도
            EmailAddress email = new EmailAddress(dto.email()); // 여기서 검증 로직 작동

            // ... 추가 로직 ...
            return new Success<>(new ValidOrder(email, ...));
        } catch (IllegalArgumentException e) {
            // 도메인 생성 규칙 위반 시 실패 반환
            return new Failure<>(e.getMessage());
        }
    }
}
```

이렇게 하면 도메인 로직 안쪽에서는 "이메일 형식이 맞을까?" 같은 고민을 다시 할 필요가 없습니다. 입구 컷(Gate Keeping)을 했기 때문입니다.

### 🧠 [Quiz-C5] 개념 확인

Q9. 외부 API 응답을 위해 내부 도메인 객체를 그대로 JSON으로 직렬화하여 내보내는 것은 권장되지 않습니다. 그 이유는?

A. JSON 라이브러리가 도메인 객체를 지원하지 않아서.

B. 도메인 모델을 변경하면 외부 API 명세까지 바뀌어버려 클라이언트가 깨질 수 있기 때문에(결합도 문제).

C. 도메인 객체는 너무 무거워서 전송이 느리기 때문에.

---

## [Appendix] 퀴즈 정답 및 상세 해설 📝

### [C1] DDD와 함수형 기초

- **Q1. 정답: C**
  
  - **해설:** 서로 다른 문맥(전시 vs 물류)에서 '상품'은 관심사가 완전히 다릅니다. 하나로 합치면 복잡도가 폭발합니다. `context.display.Product`와 `context.logistics.Product`로 나누어 관리하는 것이 Bounded Context의 핵심입니다.

- **Q2. 정답: B**
  
  - **해설:** `record`는 **불변(Immutable)** 객체입니다. `setter`는 생성되지 않으며, 한 번 생성된 후에는 값을 바꿀 수 없습니다. 값을 바꾸려면 새로운 객체를 생성해야 합니다.

### [C2] 도메인 모델링 I

- **Q3. 정답: C**
  
  - **해설:** `Compact Constructor`는 레코드가 생성되는 그 순간에 실행됩니다. 여기서 검증하면, 유효하지 않은 `CouponCode` 객체는 이 세상에 절대 존재할 수 없게 됩니다. 이를 "항상 유효한 상태 보장"이라고 합니다.

- **Q4. 정답: B**
  
  - **해설:** Value Object는 고유 식별자가 없습니다. 만원짜리 지폐가 찢어져서 새 지폐로 바꿔도 가치(Value)는 똑같이 만원이듯, 속성 값이 같으면 동등한 객체로 취급합니다.

### [C3] 도메인 모델링 II

- **Q5. 정답: B**
  
  - **해설:** Sealed Interface와 Switch 식을 함께 쓰면 컴파일러가 "모든 경우의 수(Exhaustiveness)"를 체크해줍니다. 만약 `Point` 케이스를 처리 안 하면 컴파일 에러가 나서, 개발자가 실수를 미리 잡을 수 있습니다. 이것이 강력한 타입 시스템의 이점입니다.

- **Q6. 정답: C**
  
  - **해설:** 해당 상태(배송 전)에 필요 없는 데이터(배송 완료일)를 가지고 있는 것은 "불가능한 상태"를 허용하는 것입니다. Sealed Interface를 활용해 상태별로 정확히 필요한 데이터만 가지는 클래스(`Paid`, `Shipped`)를 각각 정의하는 것이 좋습니다.

### [C4] 워크플로우와 에러 처리

- **Q7. 정답: B**
  
  - **해설:** Exception은 "깜빡하고 catch 안 함"이 가능하지만, `Result` 타입을 리턴 받으면 `Success`인지 `Failure`인지 `switch` 문 등을 통해 확인하지 않고서는 내부 데이터를 꺼낼 수 없습니다. 에러 처리를 강제하여 안정성을 높입니다.

- **Q8. 정답: C**
  
  - **해설:** 함수형 에러 처리에서는 예외를 던지지 않습니다. 실패를 의미하는 값(`Failure`)을 리턴합니다. 호출자는 이 값을 받아 "아, 실패했구나"라고 인지하고 그에 맞는 처리(로그 기록, 사용자 알림 등)를 하게 됩니다.

### [C5] 아키텍처와 진화

- **Q9. 정답: B**
  
  - **해설:** 도메인 모델은 비즈니스 로직의 변화에 따라 자주 바뀔 수 있습니다. 반면 외부 API(Contract)는 호환성을 유지해야 합니다. 도메인 모델과 외부 통신용 객체(DTO)를 분리해야 내부 로직을 자유롭게 개선할 수 있습니다.

---

리더님, 이 교재는 **"Domain Modeling Made Functional"**의 정수를 Java 25 환경에 맞춰 압축한 것입니다. 이 내용들을 실제 업무(회원, 주문, 정산 등)에 적용해 보시면, 코드가 놀랍도록 명확해지고 버그가 줄어드는 것을 경험하실 겁니다.

이제 학습 준비가 완벽히 되셨나요? 궁금한 점이 생기면 언제든 첫 번째 챕터부터 질문해 주세요! 🚀
