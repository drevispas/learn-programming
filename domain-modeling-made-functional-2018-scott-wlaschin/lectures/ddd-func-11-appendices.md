# 부록

---

## Appendix A: 흔한 실수 모음

**표 A.1**: 흔한 실수와 올바른 코드
| 실수 | 올바른 코드 | 참고 |
|------|------------|------|
| `quantity < 0` 검증 | `quantity < 1` (0도 막아야) | Ch.2 Compact Constructor |
| `implements` 누락 | `record X() implements Y {}` | Ch.3 Sealed Interface |
| `value` vs `value()` | Record는 메서드 호출 필요 | Ch.3 Pattern Matching |
| 타입 캐스팅 누락 | `(long)(price * rate)` | Ch.2 금액 계산 |
| `new` 키워드 누락 | `new Success<>(...)` | Ch.6 Result 타입 |
| `raw.trim()` | `new SanitizedText(raw.s().trim())` | Ch.5 파이프라인 |

---

## Appendix B: Java 25 함수형 Cheat Sheet

### Record (Immutable Data)

**코드 B.1**: Record 기본 사용법과 Wither 메서드
```java
// 기본 Record
public record Money(BigDecimal amount) {
    public Money {
        if (amount.signum() < 0) throw new IllegalArgumentException();
    }
    public Money add(Money o) { return new Money(amount.add(o.amount)); }
}

// withXxx 메서드로 상태 변경 (JEP 468 미포함으로 수동 구현 필요)
Order oldOrder = new Order(id, status, total);
Order newOrder = oldOrder.withStatus(OrderStatus.PAID);
```

### Sealed Interface (Sum Type)

**코드 B.2**: Sealed Interface로 Sum Type 정의
```java
public sealed interface Result<S,F> permits Success, Failure {}
public record Success<S,F>(S value) implements Result<S,F> {}
public record Failure<S,F>(F error) implements Result<S,F> {}
```

### Pattern Matching

**코드 B.3**: Exhaustive Pattern Matching with Sealed Interface
```java
// sealed interface OrderStatus permits Unpaid, Paid, Shipping, Delivered, Cancelled
String msg = switch (status) {
    case Unpaid u -> "결제 대기";
    case Paid p when p.paidAt().plusDays(1).isAfter(now) -> "취소 가능";
    case Paid p -> "취소 불가";
    case Shipping s -> "배송 중";
    case Delivered d -> "배송 완료";
    case Cancelled c -> "취소됨";
    // sealed interface면 default 불필요!
};
```

### Optional

**코드 B.4**: Optional 체이닝
```java
opt.map(f).flatMap(g).orElseThrow(() -> new NotFoundException());
```

### var 타입 추론

**코드 B.5**: Local Variable Type Inference
```java
var validator = new OrderValidator();
var result = validator.validate(order);
```

---

## Appendix C: 심화 Q&A

### Q1. 객체를 계속 새로 만들면 메모리가 터지지 않나요?

**결론: 전혀 걱정하지 않으셔도 됩니다.**

현대의 JVM(특히 G1GC, ZGC)은 **"짧게 살고 죽는 객체(Short-lived Object)"**를 처리하는 데 극도로 최적화되어 있습니다.

- **에덴(Eden) 영역의 마법**: 대부분의 record나 Immutable 객체는 생성되자마자 잠깐 쓰이고 버려집니다. JVM의 가비지 컬렉터(GC) 입장에서 이런 객체들은 청소 비용이 거의 '0'에 수렴합니다.
- **구조적 공유 (Structural Sharing)**: `new Order(..., newAddress)`를 할 때, 기존 Order의 모든 데이터를 복사하는 게 아닙니다. 기존 데이터는 참조(Reference)만 복사해서 재사용합니다.
- **탈출 분석 (Escape Analysis)**: JIT 컴파일러가 "이 객체는 메서드 밖으로 안 나가네?"라고 판단하면, 힙이 아닌 스택에 할당해버립니다.

### Q2. G1GC와 ZGC가 뭔가요?

**G1GC (Garbage First GC)**: Java 9~18의 기본 GC. 거대한 힙을 작은 Region으로 쪼개서, 쓰레기가 가장 많은 구역부터 청소합니다.

**ZGC (Z Garbage Collector)**: Java 21+의 차세대 GC. 청소를 애플리케이션 실행 중에 몰래 수행합니다. 힙 크기에 상관없이 멈추는 시간이 1ms 미만입니다.

### Q3. toDomain()은 Controller vs Service 어디에?

**정석: Controller에서 변환합니다.**

서비스 메서드 시그니처가 타입(Type) 그 자체로 문서가 되어야 합니다.

**코드 C.1**: toDomain() 위치 - Controller vs Service
```java
// Service가 DTO를 받을 때: 불안함
void register(UserDTO dto)  // dto 안에 쓰레기 값 있을 수도

// Service가 Domain을 받을 때: 안전함
void register(User user)    // User 타입이면 이미 검증 완료
```

### Q4. Repository 인터페이스는 어느 패키지에?

**위치: domain 패키지**

이것이 **Dependency Inversion Principle(DIP)**의 핵심입니다.
- Domain은 인터페이스만 정의 (OrderRepository)
- Infra는 구현체 제공 (JpaOrderRepository)

만약 인터페이스가 infra에 있다면, Domain이 Infra를 import해야 합니다. DB 기술 변경 시 도메인 코드도 수정해야 하는 불상사가 생깁니다.

### Q5. 패키지 상호 참조가 발생하면 무슨 문제?

**스파게티 코드가 되어 유지보수가 불가능해집니다.**

- **컴파일 지옥**: A 컴파일하려니 B 필요, B 하려니 A 필요
- **테스트 불가능**: 단위 테스트하려는데 연쇄적으로 의존성 딸려옴
- **변경의 전파**: DB 스키마 바꿨는데 할인 로직에서 에러 발생

### Q6. Pure Function의 본질은 무엇인가요?

**Referential Transparency (참조 투명성)**: "언제, 어디서, 누가 실행하든 입력이 같으면 결과가 무조건 같아야 한다. 그리고 그 외에는 아무 일도 일어나지 않아야 한다."

`System.out.println`도 Side Effect입니다:
- 모니터라는 외부 세계의 상태를 변경
- 실행 환경에 따라 동작이 달라질 수 있음

### Q7. 팀이 FP를 안 하는데 Pure Function으로 짜면 좋은가요?

**네, 무조건 좋습니다!**

1. **테스트의 천국**: Mock 없이 `assert(f(input) == expected)` 한 줄로 끝
2. **Local Reasoning**: 버그 추적 시 함수 안만 보면 됨
3. **동시성 안전**: 값을 안 바꾸니 락 불필요

### Q8. Pure Function 적용 전략은?

**Functional Core, Imperative Shell**

- **Functional Core**: 비즈니스 로직, 계산, 판단 → Pure Function으로
- **Imperative Shell**: DB 저장, API 호출, 로그 출력 → 바깥쪽으로

복잡한 로직만 Pure Function으로 분리해도 디버깅 시간이 절반으로 줄어듭니다.

---

## Appendix D: 전체 퀴즈 정답

**표 D.1**: 챕터별 퀴즈 정답표

| Ch | Q1 | Q2 | Q3 | Q4 | Q5 |
|----|----|----|----|----|-----|
| 1  | C  | B  | B  | B  | B   |
| 2  | C  | B  | C  | A  | C   |
| 3  | B  | A  | B  | C  | B   |
| 4  | C  | B  | B  | C  | -   |
| 5  | B  | -  | -  | -  | -   |
| 6  | C  | B  | C  | -  | -   |
| 7  | B  | C  | C  | D  | B   |
| 8  | B  | C  | A  | B  | B   |
| 9  | B  | C  | C  | B  | B   |
| 10 | B  | B  | B  | B  | B   |

### 정답 해설

**Chapter 1**
- Q1.1: C - 각 Bounded Context별로 필요한 정보만 담은 별도 모델 정의
- Q1.2: B - Record는 setter가 없음 (Immutable)
- Q1.3: B - 객체가 여러 곳에서 변경되어 상태 추적이 어렵다
- Q1.4: B - 기획서 용어가 그대로 코드에 등장
- Q1.5: B - 주문 Bounded Context에서 인증 정보에 접근

**Chapter 2**
- Q2.1: C - 메모리 사용량 증가는 Primitive Obsession의 문제가 아님
- Q2.2: B - Value Object는 값이 같으면 같은 객체
- Q2.3: C - Compact Constructor에서 검증
- Q2.4: A - double 대신 BigDecimal 사용 (부동소수점 오차)
- Q2.5: C - 주문 상태는 String이 아니라 enum 또는 sealed interface

**Chapter 3**
- Q3.1: B - 배송 정보는 모든 필드가 필요한 AND 타입
- Q3.2: A - permits는 구현 가능한 클래스를 제한
- Q3.3: B - 모든 케이스를 다루지 않으면 컴파일 에러
- Q3.4: C - sealed interface가 가장 적합
- Q3.5: B - Shipped 케이스 누락으로 컴파일 에러

**Chapter 4**
- Q4.1: C - 타입으로 "배송 중엔 취소 불가" 규칙을 강제
- Q4.2: B - VerifiedEmail만 주문할 수 있는 타입 설계
- Q4.3: B - Phantom Type은 컴파일 타임에만 존재하며, 런타임에는 타입 소거로 동일
- Q4.4: C - sealed interface로 "있음/없음"을 명시적인 타입으로 표현

**Chapter 5**
- Q5.1: B - 검증 전후의 데이터가 다른 보장을 가지므로

**Chapter 6**
- Q6.1: C - 메모리 사용량은 Exception의 문제가 아님
- Q6.2: B - 결과가 Result인 함수는 flatMap 사용
- Q6.3: C - Failure가 반환되고 이후 단계는 실행되지 않음
- Q6.X1: 변환 함수의 반환 타입이 `A -> B`이면 **map**, `A -> Result<B, E>`이면 **flatMap** 사용

**Chapter 7**
- Q7.1: B - 폼 검증에서 모든 에러를 한번에 보여줄 때 Validation
- Q7.2: C - Applicative는 모든 에러를 수집 (첫 에러에서 중단 X)
- Q7.3: C - Event는 이미 발생한 Immutable 사실
- Q7.4: D - 두 에러 모두 수집됨
- Q7.5: B - 과거형으로 "이미 일어난 일"을 표현
- Q7.X1: 모든 에러를 한 번에 수집하여 사용자에게 보여줄 수 있어서 (좋은 UX)

**Chapter 8**
- Q8.1: B - 도메인이 인프라에 의존하면 결합도가 높아짐
- Q8.2: C - 코드량 줄이기는 분리 이유가 아님
- Q8.3: A - ACL은 외부 형식을 도메인 형식으로 변환
- Q8.4: B - 외부 API 변경 시 ACL(Adapter)만 수정
- Q8.5: B - Repository의 OrderMapper.toDomain()은 JPA Entity → Domain 변환 (참고: Controller의 toDomain()은 DTO → Domain 변환)

**Chapter 9**
- Q9.1: B - 의존성은 바깥쪽에서 안쪽으로 (Infrastructure → Domain)
- Q9.2: C - 데이터베이스 저장은 Side Effect
- Q9.3: C - Pure Function은 데이터베이스를 조회하지 않음
- Q9.4: B - Repository 인터페이스는 Domain 계층에 정의 (DIP)
- Q9.5: B - Mock 없이 입력/출력만으로 테스트 가능

**Chapter 10**
- Q10.1: B - sealed interface의 메서드로 정의 (hasFreeShipping())
- Q10.2: B - 각 Bounded Context에 필요한 데이터만 포함
- Q10.3: B - 타입으로 "배송 중 취소 불가" 규칙을 강제
- Q10.4: B - 모든 결제 수단을 타입 안전하게 처리 가능
- Q10.5: B - 쿠폰 사용 실패 가능성을 명시적으로 표현

---

*이 교재는 Scott Wlaschin의 "Domain Modeling Made Functional"을 Java 25와 이커머스 도메인에 맞춰 재구성한 것입니다.*

---

## Appendix E: 컴파일 가능한 샘플 프로젝트

실제 컴파일 가능한 예제를 보고 싶다면 아래 샘플 프로젝트를 참고하세요.

- 경로: `examples/functional-domain-modeling`
- 빌드: `mvn -q -f examples/functional-domain-modeling/pom.xml test`
- Java 버전: 21+ (Record, Sealed Interface, Pattern Matching 사용)

> **Java 버전 참고사항**:
> - **Java 21+**: Record, Sealed Interface, Pattern Matching, Record Patterns 정식 지원
> - **Java 22+**: Unnamed Variables & Patterns (`_`) 정식 지원 (JEP 456)
> - **Java 25**: JEP 468 (`with` 구문)은 **포함되지 않았습니다** - 수동 `withXxx()` 메서드 필요
> - 샘플 프로젝트는 Java 21+로 실행 가능하며, 수동 `withXxx()` 메서드를 사용합니다

샘플에는 다음이 포함됩니다:
- `Money`, `OrderLine`, `ValidatedOrder` 등 핵심 도메인 타입
- `Result`, `Validation` 구현체
- 간단한 `PlaceOrder` 워크플로우와 테스트

> 💡 전체 프로젝트 구조와 실행 방법은 `examples/functional-domain-modeling/README.md`를 참조하세요.

---

## Appendix F: 함수형 타입 클래스 이해하기

> 📖 **선택적 심화 학습 자료**
>
> Chapter 6, 7의 내용만으로도 실무에서 충분히 Result와 Validation을 활용할 수 있습니다.
> 이 부록은 "왜 이런 패턴들이 비슷해 보이는가?"에 대한 이론적 배경을 제공합니다.

---

### F.1 타입 클래스란?

#### 공통 패턴 발견하기

다음 코드들을 보세요:

**코드 F.1**: 다양한 타입의 map 연산
```java
// Optional의 map
Optional<String> name = Optional.of("Kim");
Optional<Integer> length = name.map(String::length);

// Stream의 map
Stream<String> names = Stream.of("Kim", "Lee");
Stream<Integer> lengths = names.map(String::length);

// Result의 map
Result<String, Error> name = Result.success("Kim");
Result<Integer, Error> length = name.map(String::length);
```

이 세 가지는 겉보기에 다른 타입이지만, 모두 **동일한 패턴**을 따릅니다:
- "컨테이너" 안에 값이 들어있고
- `map`을 사용하면 안에 든 값만 변환할 수 있음
- 컨테이너의 구조(Optional, Stream, Result)는 유지됨

**이 공통 패턴에 "Functor"라는 이름을 붙였습니다.**

#### 타입 클래스 = 행동의 계약

> 💡 **타입 클래스**는 "이 타입은 이런 연산을 지원합니다"라는 계약입니다.
>
> Java의 `interface`와 비슷하지만, 기존 타입에 구현을 "추가"할 수 있습니다.
> Haskell, Scala, Rust 등에서 널리 사용됩니다.

---

### F.2 Functor (펑터)

#### 정의

> **Functor**는 "컨테이너 안의 값을 변환하는 능력"을 가진 타입입니다.

```java
// Functor의 핵심 연산
<B> F<B> map(Function<A, B> f);
```

#### Functor Laws (법칙)

Functor라고 불리려면 두 가지 법칙을 만족해야 합니다:

**1. Identity Law (항등 법칙)**
```java
// 아무것도 안 하는 함수로 map하면 원본과 같아야 함
container.map(x -> x)  ==  container

// 예: Result
Result.success(42).map(x -> x)  ==  Result.success(42)
```

**2. Composition Law (합성 법칙)**
```java
// f를 map하고 g를 map하는 것 = f.andThen(g)를 한번 map하는 것
container.map(f).map(g)  ==  container.map(f.andThen(g))

// 예: Optional
optional.map(String::length).map(n -> n * 2)
==
optional.map(s -> s.length() * 2)
```

#### 왜 법칙이 중요한가?

> 📌 법칙을 만족하면 **리팩토링이 안전**합니다.
>
> `container.map(f).map(g)`를 `container.map(f.andThen(g))`로 바꿔도
> 동작이 동일함을 보장합니다.

---

### F.3 Monad (모나드)

#### 정의

> **Monad**는 "flatMap을 가진 Functor"입니다.

```java
// Monad의 핵심 연산
<B> M<B> flatMap(Function<A, M<B>> f);  // 또는 bind, >>=
```

#### 왜 Monad가 필요한가?

Functor(map)만으로는 **"실패할 수 있는 연산의 연쇄"**를 표현할 수 없습니다:

**코드 F.2**: Functor vs Monad
```java
// Functor만으로는 중첩됨
Result<Result<Order, E>, E> nested =
    validateInput(input).map(this::processOrder);  // ❌ 중첩!

// Monad(flatMap)를 사용하면 평평하게
Result<Order, E> flat =
    validateInput(input).flatMap(this::processOrder);  // ✅ 단일 레벨
```

#### Monad Laws (법칙)

**1. Left Identity (왼쪽 항등)**
```java
// 순수 값을 컨테이너로 감싼 후 flatMap하면 그냥 f를 적용한 것과 같음
Result.success(a).flatMap(f)  ==  f.apply(a)
```

**2. Right Identity (오른쪽 항등)**
```java
// success로 감싸는 함수로 flatMap하면 원본과 같음
m.flatMap(a -> Result.success(a))  ==  m
```

**3. Associativity (결합 법칙)**
```java
// flatMap 순서를 바꿔도 결과가 같음
m.flatMap(f).flatMap(g)  ==  m.flatMap(a -> f.apply(a).flatMap(g))
```

#### Monad의 직관적 이해

> 💡 **Monad는 "컨텍스트가 있는 계산의 연쇄"입니다.**
>
> - Optional Monad: "값이 없을 수도 있는" 컨텍스트
> - Result Monad: "실패할 수도 있는" 컨텍스트
> - List Monad: "여러 값이 있을 수 있는" 컨텍스트
> - IO Monad: "부수효과가 있는" 컨텍스트

---

### F.4 Applicative (어플리커티브)

#### 정의

> **Applicative**는 "독립적인 컨테이너들을 결합하는 능력"을 가진 타입입니다.

```java
// Applicative의 핵심 연산
static <A, B, C> F<C> combine(F<A> fa, F<B> fb, BiFunction<A, B, C> combiner);
```

#### Monad vs Applicative

**핵심 차이: 의존성**

```
=== Monad (flatMap) - 순차적 의존 ===

  A ─────→ f(A) ─────→ Result<B>
                          │
                          └─→ g(B) ─────→ Result<C>

  "B를 계산하려면 A가 필요하고, C를 계산하려면 B가 필요하다"


=== Applicative (combine) - 독립적 ===

  ┌─→ Result<A> ─────┐
  │                  │
  ├─→ Result<B> ─────┼─→ combine ─→ Result<(A, B, C)>
  │                  │
  └─→ Result<C> ─────┘

  "A, B, C는 서로 독립적으로 계산된다"
```

**코드 F.3**: Monad vs Applicative
```java
// Monad: 순차 의존 (앞 결과가 뒤 계산에 필요)
Result<Order, E> order = getUser(userId)
    .flatMap(user -> getCart(user.cartId()))      // user 필요
    .flatMap(cart -> createOrder(cart));          // cart 필요

// Applicative: 독립 계산 (서로 의존 없음)
Validation<User, List<E>> user = Validation.combine3(
    validateName(input.name()),        // 독립
    validateEmail(input.email()),      // 독립
    validatePhone(input.phone()),      // 독립
    User::new
);
```

---

### F.5 Result는 Monad, Validation은 Applicative인 이유

#### Result: Monad로 구현

Result의 flatMap은 **첫 실패에서 중단**합니다:

```java
// Result.flatMap 구현
public <B> Result<B, E> flatMap(Function<A, Result<B, E>> f) {
    return switch (this) {
        case Success<A, E> s -> f.apply(s.value());  // 성공하면 다음 단계
        case Failure<A, E> fail -> (Result<B, E>) fail;  // 실패하면 중단
    };
}
```

이는 Monad 법칙을 만족하고, **순차적 파이프라인**에 적합합니다.

#### Validation: Applicative로 구현

Validation의 combine은 **모든 에러를 수집**합니다:

```java
// Validation.combine 구현
static <A, B, C> Validation<C, List<E>> combine(
    Validation<A, List<E>> va,
    Validation<B, List<E>> vb,
    BiFunction<A, B, C> combiner
) {
    return switch (va) {
        case Valid<A, List<E>> a -> switch (vb) {
            case Valid<B, List<E>> b -> valid(combiner.apply(a.value(), b.value()));
            case Invalid<B, List<E>> b -> b;
        };
        case Invalid<A, List<E>> a -> switch (vb) {
            case Valid<B, List<E>> b -> a;
            case Invalid<B, List<E>> b -> invalid(concat(a.errors(), b.errors()));  // 에러 합침!
        };
    };
}
```

**Validation은 합법적인 Monad가 될 수 없습니다!**

만약 Validation에 flatMap을 구현하면:
```java
// 가상의 Validation.flatMap
Validation<B, List<E>> flatMap(Function<A, Validation<B, List<E>>> f) {
    return switch (this) {
        case Valid v -> f.apply(v.value());
        case Invalid inv -> inv;  // 여기서 중단됨!
    };
}
```

이렇게 하면 Monad 법칙(결합 법칙)을 위반하게 됩니다.
따라서 Validation은 **Applicative까지만** 구현합니다.

#### 정리표

| 타입 | Functor | Applicative | Monad | 에러 처리 |
|------|---------|-------------|-------|----------|
| Optional | ✅ | ✅ | ✅ | 첫 None에서 중단 |
| Result | ✅ | ✅ | ✅ | 첫 Failure에서 중단 |
| Validation | ✅ | ✅ | ❌ | 모든 에러 수집 |
| List | ✅ | ✅ | ✅ | (해당 없음) |

---

### F.6 더 알아보기

**추천 자료:**

1. **Railway Oriented Programming** (Scott Wlaschin)
   - https://fsharpforfunandprofit.com/rop/
   - Result 패턴의 원전

2. **Functors, Applicatives, and Monads in Pictures** (Aditya Bhargava)
   - https://adit.io/posts/2013-04-17-functors,_applicatives,_and_monads_in_pictures.html
   - 시각적 설명의 명작

3. **Vavr Documentation**
   - https://www.vavr.io/
   - Java에서 함수형 프로그래밍 라이브러리

4. **Arrow (Kotlin) Documentation**
   - https://arrow-kt.io/
   - Kotlin용 함수형 프로그래밍 라이브러리

> 💡 **학습 팁**: 이론을 먼저 이해하려 하기보다, Chapter 6-7의 Result/Validation을
> 실제로 사용해보면서 "왜 이렇게 동작하지?"라는 질문이 생길 때 이 부록을 참고하세요.
