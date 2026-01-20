# Chapter 7: 파이프라인 조립과 검증

> Part II: 심화 - 워크플로우와 에러 처리

---

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

**코드 7.1**: Result vs Validation 비교
```java
// Result: 첫 에러에서 중단
Result<Order, Error> result = validateName(input)
    .flatMap(this::validateEmail)   // 이름 실패하면 여기 안 감
    .flatMap(this::validatePhone);  // 이메일 실패하면 여기 안 감

// Validation: 모든 에러 수집
Validation<Order, List<Error>> result = Validation.combine3(
    validateName(input),
    validateEmail(input),
    validatePhone(input),
    Order::new
);  // 모든 검증 결과를 모아서 처리
```

**표 7.1**: Result vs Validation 비교

| 특성 | Result | Validation |
|------|--------|------------|
| 에러 처리 | 첫 에러에서 중단 | 모든 에러 수집 |
| 적합한 경우 | 파이프라인 (순차 처리) | 폼 검증 (병렬 처리) |
| 에러 타입 | 단일 에러 | 에러 목록 |

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

**코드 7.2**: Validation 타입 구현
```java
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Validation 타입 - 에러를 수집하는 Applicative 패턴 구현.
 * E 타입 파라미터는 에러 컬렉션 타입 (보통 List<SomeError>)을 나타냅니다.
 */
public sealed interface Validation<S, E> permits Valid, Invalid {
    // 성공
    static <S, E> Validation<S, E> valid(S value) {
        return new Valid<>(value);
    }

    // 실패 (에러 컬렉션 전달)
    static <S, E> Validation<S, E> invalid(E errors) {
        return new Invalid<>(errors);
    }

    // 단일 에러로 Invalid 생성 (편의 메서드)
    static <S, E> Validation<S, List<E>> invalidOne(E error) {
        return new Invalid<>(List.of(error));
    }

    // 성공 값 변환 (실패면 그대로)
    default <NewS> Validation<NewS, E> map(Function<S, NewS> mapper) {
        return switch (this) {
            case Valid<S, E> v -> new Valid<>(mapper.apply(v.value()));
            case Invalid<S, E> i -> new Invalid<>(i.errors());
        };
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

    // 3개 결합
    static <A, B, C, R, E> Validation<R, List<E>> combine3(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        TriFunction<A, B, C, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            vc,
            (ab, c) -> combiner.apply(ab.first(), ab.second(), c)
        );
    }

    // 4개 결합
    static <A, B, C, D, R, E> Validation<R, List<E>> combine4(
        Validation<A, List<E>> va,
        Validation<B, List<E>> vb,
        Validation<C, List<E>> vc,
        Validation<D, List<E>> vd,
        QuadFunction<A, B, C, D, R> combiner
    ) {
        return combine(
            combine(va, vb, Pair::new),
            combine(vc, vd, Pair::new),
            (ab, cd) -> combiner.apply(
                ab.first(), ab.second(),
                cd.first(), cd.second()
            )
        );
    }
}

public record Valid<S, E>(S value) implements Validation<S, E> {}
public record Invalid<S, E>(E errors) implements Validation<S, E> {}
public record Pair<A, B>(A first, B second) {}

@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}

@FunctionalInterface
public interface QuadFunction<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}
```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. Applicative/Validation 패턴은 다음에서 사용됩니다:
- Vavr의 `Validation<E, T>`
- cats (Scala)의 `Validated`
- Arrow (Kotlin)의 `Validated`

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "Applicative 패턴은 독립적인 검증을 병렬로 수행하여 모든 에러를 수집할 수 있게 한다."

---

### 7.2.1 왜 Applicative가 필요한가?

#### 문제 제시: 폼 검증 UX

회원가입 폼에서 이름, 이메일, 비밀번호가 모두 잘못됐다고 가정해 봅시다.

**flatMap으로 순차 검증하면:**

**코드 7.2.1a**: flatMap으로 폼 검증 (나쁜 UX)
```java
// flatMap은 첫 에러에서 멈춤
Result<User, Error> result = validateName(input)
    .flatMap(name -> validateEmail(input))     // 이름 실패하면 여기 안 감
    .flatMap(email -> validatePassword(input)); // 이메일 실패하면 여기 안 감

// 결과: "이름이 잘못됐습니다" 만 나옴
// 사용자는 3번 수정해야 함 - 나쁜 UX!
```

**Validation.combine으로 병렬 검증하면:**

**코드 7.2.1b**: Validation으로 폼 검증 (좋은 UX)
```java
// Validation은 모든 검증을 수행하고 에러를 수집
Validation<User, List<Error>> result = Validation.combine3(
    validateName(input),
    validateEmail(input),
    validatePassword(input),
    User::new
);

// 결과: "이름이 잘못됐습니다, 이메일 형식이 틀렸습니다, 비밀번호가 너무 짧습니다"
// 사용자가 한 번에 모두 수정 가능 - 좋은 UX!
```

#### 다이어그램: Result vs Validation 흐름 비교

```
=== Result.flatMap: Fail-Fast (빠른 실패) ===

  이름검증 ─────→ 실패! ─────→ [중단]
                  │
                  └─→ 에러: "이름이 필수입니다"
                       (뒤 검증은 실행 안 됨)


=== Validation.combine: Error Accumulation (에러 수집) ===

  이름검증 ──────────────┐
                         │
  이메일검증 ────────────┼──→ combine ──→ 모든 에러 수집
                         │          │
  비밀번호검증 ──────────┘          │
                                    ↓
                        Invalid([이름 에러, 이메일 에러, 비밀번호 에러])
```

#### 💡 비유: 의사 종합 검진

> **Result.flatMap은 "하나씩 검사하고 이상 있으면 중단"입니다.**
>
> 혈압 재고 → 이상 발견 → "고혈압입니다" → 끝
> (당뇨, 콜레스테롤은 검사 안 함)
>
> **Validation.combine은 "종합 검진표"입니다.**
>
> 혈압, 당뇨, 콜레스테롤, 간 수치 모두 검사하고
> "고혈압, 당뇨 전단계, 콜레스테롤 높음" 종합 리포트 제공

#### 결정 트리: 언제 Result vs Validation 사용?

```
                    ┌─────────────────────────────────┐
                    │ 검증들이 서로 의존하는가?       │
                    └───────────────┬─────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                    ▼                               ▼
            ┌───────────┐                   ┌───────────┐
            │ 예 (의존) │                   │ 아니오    │
            └─────┬─────┘                   └─────┬─────┘
                  │                               │
                  ▼                               ▼
        ┌─────────────────┐            ┌─────────────────────┐
        │ Result.flatMap  │            │ 모든 에러를 한번에  │
        │ 순차 처리       │            │ 보여줘야 하는가?    │
        └─────────────────┘            └──────────┬──────────┘
                                                  │
                                    ┌─────────────┴─────────────┐
                                    │                           │
                                    ▼                           ▼
                            ┌───────────┐               ┌───────────┐
                            │ 예        │               │ 아니오    │
                            └─────┬─────┘               └─────┬─────┘
                                  │                           │
                                  ▼                           ▼
                        ┌──────────────────┐        ┌──────────────────┐
                        │ Validation       │        │ Result.flatMap   │
                        │ (에러 수집)      │        │ (빠른 실패)      │
                        └──────────────────┘        └──────────────────┘
```

#### 핵심 요약

| 상황 | 적합한 타입 | 이유 |
|------|-----------|------|
| 폼 검증 (회원가입, 주문) | **Validation** | 모든 에러를 한 번에 보여줘야 |
| 결제 파이프라인 | **Result** | 앞 단계 성공해야 다음 단계 가능 |
| 독립적인 여러 필드 검증 | **Validation** | 필드끼리 의존성 없음 |
| 순차적 비즈니스 로직 | **Result** | 각 단계가 이전 결과에 의존 |

**퀴즈 Q7.X1**: 폼 검증에서 Validation.combine을 사용하는 이유는?
> 모든 에러를 한 번에 수집하여 사용자에게 보여줄 수 있어서 (좋은 UX)

---

### 7.3 이커머스 검증 예시

**코드 7.3**: 이커머스 주문 검증 구현
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
            return Validation.invalidOne(new ValidationError.Required("customerId"));
        }
        try {
            return Validation.valid(new CustomerId(Long.parseLong(input)));
        } catch (NumberFormatException e) {
            return Validation.invalidOne(new ValidationError.InvalidFormat("customerId", "숫자"));
        }
    }

    private Validation<List<ValidatedOrderLine>, List<ValidationError>> validateOrderLines(
        List<UnvalidatedOrderLine> lines
    ) {
        if (lines == null || lines.isEmpty()) {
            return Validation.invalidOne(new ValidationError.Required("orderLines"));
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

    // validateAddress, validateCoupon, validateOrderLine 등은
    // examples/functional-domain-modeling/ 프로젝트에서 확인하세요.
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

**코드 7.4**: Command와 Event 정의
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

**표 7.2**: Command vs Event 비교

| 특성 | Command | Event |
|------|---------|-------|
| 시제 | 미래/명령형 | 과거/완료형 |
| 결과 | 성공 또는 실패 가능 | 이미 발생한 사실 |
| 변경 | 변경 요청 | 불변 |
| 예시 | PlaceOrderCommand | OrderPlaced |

---

### 퀴즈 Chapter 7

#### Q7.1 [개념 확인] Validation vs Result

Validation이 Result보다 적합한 경우는?

**A.** 파이프라인에서 첫 에러에 바로 중단하고 싶을 때<br/>
**B.** 폼 검증에서 모든 에러를 한번에 보여주고 싶을 때 *(정답)*<br/>
**C.** 결제 처리처럼 순차적으로 진행해야 할 때<br/>
**D.** 단일 값 검증

---

#### Q7.2 [코드 분석] Applicative

Applicative 패턴의 특징이 아닌 것은?

**A.** 여러 검증을 독립적으로 수행<br/>
**B.** 모든 에러를 수집<br/>
**C.** 첫 에러에서 중단 *(정답)*<br/>
**D.** 검증 결과를 결합

---

#### Q7.3 [설계 문제] Command vs Event

다음 중 Event의 특징은?

**A.** 미래에 수행할 작업을 나타냄<br/>
**B.** 실패할 수 있음<br/>
**C.** 이미 발생한 불변의 사실 *(정답)*<br/>
**D.** 요청을 나타냄

---

#### Q7.4 [코드 분석] 검증 결합

다음 코드의 결과는?

**코드 7.5**: Validation.combine 동작
```java
Validation.combine(
    Validation.invalidOne(new Error("이름 필수")),
    Validation.invalidOne(new Error("이메일 형식 오류")),
    (name, email) -> new User(name, email)
);
```

**A.** Valid(User)<br/>
**B.** Invalid([이름 필수])<br/>
**C.** Invalid([이메일 형식 오류])<br/>
**D.** Invalid([이름 필수, 이메일 형식 오류]) *(정답)*

---

#### Q7.5 [설계 문제] 이벤트 네이밍

`OrderPlaced` 이벤트의 네이밍이 좋은 이유는?

**A.** 동사 원형을 사용해서<br/>
**B.** 과거형으로 "이미 일어난 일"을 표현해서 *(정답)*<br/>
**C.** 명사를 사용해서<br/>
**D.** 짧아서

---

정답은 Appendix D에서 확인할 수 있습니다.
