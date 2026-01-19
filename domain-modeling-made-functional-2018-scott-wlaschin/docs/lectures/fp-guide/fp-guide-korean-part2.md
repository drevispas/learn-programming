# 자바/코틀린 개발자를 위한 함수형 프로그래밍 가이드 - Part 2: 컨테이너/ADT

> **"OOP는 움직이는 부품을 캡슐화해서 코드를 이해하기 쉽게 만들고, FP는 움직이는 부품 자체를 줄여서 코드를 이해하기 쉽게 만든다."** — Michael Feathers

---

## 이 파트의 목차

4. [컨테이너 패밀리: 펑터에서 모나드까지](#4-컨테이너-패밀리-펑터에서-모나드까지)
   - Functor (map의 정체)
   - Monad (flatMap의 정체)
   - Applicative (독립 계산 결합)
5. [대수적 구조](#5-대수적-구조)
   - 세미그룹과 모노이드
6. [대수적 데이터 타입 (ADT)](#6-대수적-데이터-타입-adt)
   - 곱 타입 (Product Type)
   - 합 타입 (Sum Type)

### 전체 가이드 네비게이션

- [Part 1: 기초](fp-guide-korean-part1.md) - 순수함수, 불변성, 고차함수, 합성, 커링
- **Part 2: 컨테이너/ADT** (현재 문서) - 펑터, 모나드, 어플리커티브, 대수적 구조, ADT
- [Part 3: 평가/실전](fp-guide-korean-part3.md) - 지연 평가, 실전 예제, 퀴즈 정답

---

## 4. 컨테이너 패밀리: 펑터에서 모나드까지

### 전체 그림

여기서부터가 함수형 프로그래밍의 꽃입니다. 겁먹지 마세요, 생각보다 별거 아닙니다.

**그림 4.1: 컨테이너 타입 계층 구조**
```
┌─────────────────────────────────────────────────────────────────┐
│                     컨테이너 계층 구조                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                    ┌──────────────┐                             │
│                    │   Functor    │  "map 할 수 있음"          │
│                    │   map(f)     │                             │
│                    └──────┬───────┘                             │
│                           │                                     │
│                           ↓                                     │
│                    ┌──────────────┐                             │
│                    │ Applicative  │  "감싸진 함수 적용 가능"   │
│                    │   ap(F<f>)   │                             │
│                    └──────┬───────┘                             │
│                           │                                     │
│                           ↓                                     │
│                    ┌──────────────┐                             │
│                    │    Monad     │  "flatMap 할 수 있음"      │
│                    │  flatMap(f)  │                             │
│                    └──────────────┘                             │
│                                                                 │
│  위로 갈수록 기본, 아래로 갈수록 강력함                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.1 펑터 (Functor)

#### 비유: 선물 포장

선물 상자 안에 뭔가 들어있어요. 펑터는 **상자를 열지 않고 안의 내용물을 변환**할 수 있게 해줍니다.

**그림 4.2: 펑터 개념도**
```
┌─────────────────────────────────────────────────────────────────┐
│                펑터 = 변환 가능한 선물 상자                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   펑터는 "상자를 열지 않고" 내용물을 바꿀 수 있는 컨테이너     │
│                                                                 │
│   ┌─────────┐      map(색칠)       ┌─────────┐                 │
│   │ Box     │  ─────────────────→  │ Box     │                 │
│   │  ┌───┐  │      내용물만        │  ┌───┐  │                 │
│   │  │ A │  │      변환됨          │  │ B │  │                 │
│   │  └───┘  │                      │  └───┘  │                 │
│   └─────────┘                      └─────────┘                  │
│   Box<자동차>                      Box<색칠된자동차>            │
│                                                                 │
│   핵심: 상자 구조는 그대로! 내용만 바뀜                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 왜 펑터라는 추상화가 필요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│                   펑터의 목적과 이점                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 통일된 인터페이스 (Unified Interface)                       │
│     Optional, List, Stream 모두 map() 사용                      │
│     컨테이너 종류와 관계없이 "변환"이라는 동일한 개념            │
│                                                                 │
│  2. 제네릭 알고리즘 (Generic Algorithms)                        │
│     "컨테이너 종류 상관없이 변환" 코드 작성 가능                │
│     펑터이기만 하면 같은 패턴 적용                              │
│                                                                 │
│  3. 법칙의 보장 (Guaranteed Laws)                               │
│     법칙을 만족하면 동작 예측 가능                              │
│     리팩토링 안전 - 법칙 덕분에 치환 가능                       │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  법칙이 왜 중요한가?                                            │
│  ─────────────────────────────────────────────────────────────  │
│  Identity법칙:  map(x -> x)가 원본과 같음                       │
│                 → 불필요한 변환 제거 가능 (최적화)              │
│                                                                 │
│  Composition법칙: map(f).map(g) == map(f.andThen(g))           │
│                   → 성능 최적화 (한 번만 순회)                  │
│                   → 리팩토링 안전 (동치 보장)                   │
│                                                                 │
│  실제 활용:                                                     │
│  ─────────────────────────────────────────────────────────────  │
│  • Optional, Stream, CompletableFuture 모두 "같은 방식"으로    │
│  • 내용물 변환 가능 → 학습 곡선 감소, 코드 일관성              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 펑터 법칙

펑터라고 불리려면 이 두 가지 법칙을 만족해야 합니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                        펑터 법칙                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 항등 법칙:                                                  │
│     container.map(x -> x) == container                         │
│     "아무것도 안 하는 함수로 map하면 원래 그대로"               │
│                                                                 │
│  2. 합성 법칙:                                                  │
│     container.map(f).map(g) == container.map(f.andThen(g))     │
│     "두 번 map하나 합성해서 한 번 map하나 결과는 같음"          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```java
// src/main/java/com/example/fp/FunctorDemo.java

// 주의: map() 메서드 자체가 펑터가 아닙니다!
// 펑터 = 타입 생성자(Optional, Stream 등) + 그에 맞는 map 구현 + 펑터 법칙 만족
//
// Optional<T>가 펑터인 이유:
// 1. 타입 생성자: Optional<T> (T를 받아 Optional<T> 타입 생성)
// 2. map 연산: Optional.map(f)
// 3. 펑터 법칙 만족 (항등, 합성)
//
// 자바의 Optional, List, Stream 전부 펑터입니다!
public class FunctorDemo {

    // Optional = 펑터
    public Optional<Integer> parseAndDouble(String input) {
        return Optional.ofNullable(input)
                .map(String::trim)        // map: String -> String
                .map(Integer::parseInt)   // map: String -> Integer
                .map(n -> n * 2);         // map: Integer -> Integer
    }

    // List = 펑터
    public List<String> getUserEmails(List<User> users) {
        return users.stream()
                .map(User::email)  // map: User -> String
                .toList();
    }

    // CompletableFuture = 펑터
    public CompletableFuture<String> fetchAndProcess(String url) {
        return fetchAsync(url)              // Future<Response>
                .thenApply(Response::body)  // map: Response -> String
                .thenApply(String::trim);   // map: String -> String
    }
}
```

---

### 4.2 모나드 (Monad)

드디어 왔습니다, 모나드. 인터넷에 "모나드 설명" 치면 온갖 복잡한 글이 나오는데요, 사실 핵심은 간단합니다.

#### 비유: 기차 선로 전환기

`map`의 문제가 뭐냐면, **함수 결과가 또 컨테이너일 때 중첩**이 됩니다.

**그림 4.3: 모나드 개념도 (map vs flatMap)**
```
┌─────────────────────────────────────────────────────────────────┐
│                  모나드 = 선로 전환기                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  map의 문제:                                                    │
│                                                                 │
│  Optional<String> name = ...;                                   │
│  Optional<Optional<Address>> nested = name.map(findAddress);   │
│  // 중첩됨! Optional 안에 Optional                              │
│                                                                 │
│  해결책 - flatMap이 중첩을 풀어줌:                              │
│                                                                 │
│     map:                              flatMap:                  │
│     ──────────────────────            ──────────────────────    │
│     ┌───┐    f    ┌─────────┐         ┌───┐    f    ┌───┐      │
│     │ A │ ──────→ │ M< M<B> >│         │ A │ ──────→ │ B │      │
│     └───┘         └─────────┘         └───┘         └───┘       │
│                   중첩!               평평!                      │
│                                                                 │
│  ═══════════════════════════════════════════════════════════   │
│                                                                 │
│  철도 비유:                                                      │
│                                                                 │
│  ════╦════════════════════════════════════╦════→ 성공 선로    │
│      ║                                    ║                     │
│      ╚══════════════════╦═════════════════╝                     │
│         실패 선로       ╚══════════════════════════════════→   │
│                                                                 │
│  flatMap은 선로를 중첩 없이 연결해줌!                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 왜 flatMap이 필요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│                   모나드의 목적과 이점                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 중첩 방지 (Avoiding Nesting)                                │
│     map만 쓰면 Optional<Optional<T>> 발생                       │
│     flatMap이 자동으로 평탄화                                   │
│                                                                 │
│  2. 순차 의존성 표현 (Sequential Dependencies)                  │
│     "A 성공하면 B, B 성공하면 C" 파이프라인                     │
│     각 단계가 이전 단계 결과에 의존                             │
│                                                                 │
│  3. 실패 자동 전파 (Automatic Failure Propagation)              │
│     중간에 실패(null, Empty, Error) 시 이후 단계 건너뜀         │
│     명시적 null 체크 없이 깔끔한 체이닝                         │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  실제 문제 해결:                                                │
│  ─────────────────────────────────────────────────────────────  │
│  Before (null 지옥):                                            │
│  if (a != null) {                                               │
│      if (b != null) {                                           │
│          if (c != null) {                                       │
│              // 드디어 로직!                                    │
│          }                                                      │
│      }                                                          │
│  }                                                              │
│                                                                 │
│  After (모나드 체이닝):                                         │
│  a.flatMap(b).flatMap(c)                                        │
│  → 선형적, 읽기 쉬움, null 체크 자동화                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 모나드가 되려면?

```
┌─────────────────────────────────────────────────────────────────┐
│                      모나드 요구사항                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 타입 생성자: M<T>                                           │
│     예: Optional<T>, List<T>, CompletableFuture<T>             │
│                                                                 │
│  2. 값을 넣는 방법: of(value) / pure(value)                    │
│     Optional.of(5), List.of(1,2,3), CompletableFuture.completedFuture(x)│
│                                                                 │
│  3. 연산 연결하는 방법: flatMap (또는 bind)                     │
│     .flatMap(a -> M<B>)                                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 모나드 법칙 (Monad Laws)

모나드라고 불리려면 이 세 가지 법칙을 만족해야 합니다:

```
┌─────────────────────────────────────────────────────────────────┐
│                        모나드 법칙                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Left Identity (왼쪽 항등 법칙):                             │
│     of(a).flatMap(f) == f(a)                                   │
│     "값을 감싸서 바로 flatMap 하면, 그냥 함수 적용한 것과 같음" │
│                                                                 │
│  2. Right Identity (오른쪽 항등 법칙):                          │
│     m.flatMap(of) == m                                         │
│     "flatMap으로 다시 감싸기만 하면 원래 그대로"                │
│                                                                 │
│  3. Associativity (결합 법칙):                                  │
│     m.flatMap(f).flatMap(g) == m.flatMap(a -> f(a).flatMap(g)) │
│     "flatMap 체이닝 순서를 바꿔 묶어도 결과는 같음"             │
│                                                                 │
│  예시 (Optional):                                               │
│  ─────────────────────────────────────────────────              │
│                                                                 │
│  // 먼저 f를 정의 (값을 받아 모나드 반환하는 함수)              │
│  Function<Integer, Optional<Integer>> f = x -> Optional.of(x * 2);│
│                                                                 │
│  // Left Identity: of(a).flatMap(f) == f(a)                    │
│  Optional.of(5).flatMap(f)  // Optional.of(10)                 │
│      .equals(f.apply(5));   // Optional.of(10) → true!         │
│  // "값을 감싸서 바로 flatMap하면 그냥 함수 적용한 것과 같음"   │
│                                                                 │
│  // Right Identity: m.flatMap(of) == m                         │
│  Optional.of(5).flatMap(Optional::of)  // Optional.of(5)       │
│      .equals(Optional.of(5));          // true!                │
│  // "flatMap으로 다시 감싸기만 하면 원래 그대로"                │
│  // 여기서 Optional::of가 항등 역할!                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 실전 코드: null 지옥 탈출

```java
// src/main/java/com/example/fp/MonadDemo.java

public class MonadDemo {

    record User(Long id, String name, Optional<Long> addressId) {}
    record Address(Long id, String street, String city) {}

    // 실패할 수 있는 조회 메서드들
    Optional<User> findUser(Long id) { /* ... */ return Optional.empty(); }
    Optional<Address> findAddress(Long id) { /* ... */ return Optional.empty(); }

    // flatMap 없이: 중첩 지옥
    public Optional<Optional<Optional<String>>> getCityBad(Long userId) {
        return findUser(userId)                              // Optional<User>
            .map(user -> user.addressId())                   // Optional<Optional<Long>>
            .map(optId -> optId.flatMap(this::findAddress))  // 점점 복잡
            .map(optAddr -> optAddr.map(Address::city));     //
    }

    // flatMap 사용: 깔끔한 선로
    public Optional<String> getCityGood(Long userId) {
        return findUser(userId)                    // Optional<User>
            .flatMap(user -> user.addressId()      // Optional<Long>
                .flatMap(this::findAddress))       // Optional<Address>
            .map(Address::city);                   // Optional<String>
    }

    // 더 깔끔하게
    public Optional<String> getCityCleanest(Long userId) {
        return findUser(userId)
            .flatMap(User::addressId)     // Optional을 0-1개 컬렉션으로 취급
            .flatMap(this::findAddress)
            .map(Address::city);
    }
}
```

#### 자바/코틀린의 주요 모나드들

```
┌─────────────────────────────────────────────────────────────────┐
│                      자주 쓰는 모나드                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  모나드            │ 의미                  │ flatMap 이름      │
│  ─────────────────┼──────────────────────┼──────────────────── │
│  Optional<T>      │ 값이 있거나 없거나   │ .flatMap()         │
│  List<T>          │ 여러 값              │ .flatMap()         │
│  Stream<T>        │ 지연 시퀀스          │ .flatMap()         │
│  CompletableFuture│ 비동기 계산          │ .thenCompose()     │
│  Result<T,E>      │ 성공 또는 에러       │ (Kotlin/Vavr)      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 모나드 체이닝의 위력

```kotlin
// src/main/kotlin/com/example/fp/MonadChaining.kt

// 여러 단계 실패를 우아하게 처리
sealed class Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>()
    data class Failure<E>(val error: E) : Result<Nothing, E>()

    inline fun <R> map(f: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(f(value))
        is Failure -> this
    }

    inline fun <R> flatMap(f: (T) -> Result<R, @UnsafeVariance E>): Result<R, E> =
        when (this) {
            is Success -> f(value)
            is Failure -> this
        }
}

// 실전: 주문 처리 파이프라인
fun processOrder(orderId: String): Result<Receipt, OrderError> {
    return validateOrderId(orderId)           // Result<OrderId, OrderError>
        .flatMap { findOrder(it) }            // Result<Order, OrderError>
        .flatMap { validateInventory(it) }    // Result<ValidatedOrder, OrderError>
        .flatMap { processPayment(it) }       // Result<PaidOrder, OrderError>
        .map { createReceipt(it) }            // Result<Receipt, OrderError>
}
// 어느 단계에서든 실패하면 에러가 자동으로 전파됨!
```

---

### 4.3 어플리커티브 (Applicative)

드디어 계층 구조의 중간 단계인 어플리커티브입니다. Functor와 Monad 사이에 있는데, 실무에서 **폼 검증**할 때 정말 유용합니다.

#### 비유: 은행 창구

모나드는 **한 줄로 늘어선 ATM**처럼 순차적입니다. 앞 사람이 끝나야 뒷 사람이 할 수 있어요.

반면 어플리커티브는 **여러 개의 은행 창구**와 같습니다. 각 창구에서 독립적으로 업무를 보고, 마지막에 결과를 합치면 됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│          Applicative = 병렬 은행 창구                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Monad (순차적 - ATM 줄):                                       │
│  ┌─────┐    ┌─────┐    ┌─────┐                                 │
│  │ 1번 │───→│ 2번 │───→│ 3번 │   앞 사람 끝나야 다음!         │
│  └─────┘    └─────┘    └─────┘                                  │
│  이전 결과가 다음 입력으로 필요함                               │
│                                                                 │
│  Applicative (독립적 - 여러 창구):                              │
│  ┌─────┐                                                        │
│  │ 1번 │──┐                                                     │
│  └─────┘  │                                                     │
│  ┌─────┐  │    ┌──────────┐                                    │
│  │ 2번 │──┼───→│ 결과 합침 │   각자 독립! 마지막에 합침        │
│  └─────┘  │    └──────────┘                                    │
│  ┌─────┐  │                                                     │
│  │ 3번 │──┘                                                     │
│  └─────┘                                                        │
│  서로 의존하지 않음                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 왜 Result 대신 Validation을 쓰는가?

```
┌─────────────────────────────────────────────────────────────────┐
│              어플리커티브/Validation의 목적과 이점               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. UX 개선 (Better User Experience)                            │
│     모든 오류를 한 번에 표시                                    │
│     사용자 재시도 최소화 (한 번에 다 고치게)                    │
│                                                                 │
│  2. 독립 검증 (Independent Validations)                         │
│     각 필드 검증이 서로 의존하지 않음                           │
│     이름 검증 ≠ 이메일 검증 ≠ 나이 검증                        │
│                                                                 │
│  3. 병렬화 가능 (Parallelizable)                                │
│     검증들이 독립적 → 동시 실행 가능                           │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  언제 무엇을:                                                   │
│  ─────────────────────────────────────────────────────────────  │
│  Result (Monad):                                                │
│  • 검증 간 의존성 있음 (A 실패 → B 실행 불가)                  │
│  • 결제 흐름: 재고 → 결제 → 배송 (순차적 의존)                │
│                                                                 │
│  Validation (Applicative):                                      │
│  • 검증 간 독립적 (이름, 이메일, 나이 각각 검증)               │
│  • 회원가입 폼: 모든 에러 한 번에 표시                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Monad vs Applicative: 핵심 차이

```
┌─────────────────────────────────────────────────────────────────┐
│              Monad vs Applicative 차이                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Monad (flatMap):                                               │
│  ────────────────                                               │
│  • 이전 결과를 보고 다음 계산 결정                              │
│  • 실패하면 즉시 중단 (fail-fast)                               │
│  • 순차적 의존 관계                                             │
│                                                                 │
│  userId.flatMap(id ->                                          │
│      findUser(id).flatMap(user ->      // user 있어야 다음 가능│
│          findAddress(user.addressId()) // addressId가 필요!    │
│      )                                                          │
│  )                                                              │
│                                                                 │
│  Applicative (combine / ap):                                    │
│  ─────────────────────────                                      │
│  • 계산들이 서로 독립적                                         │
│  • 모든 에러 수집 가능 (error accumulation)                     │
│  • 병렬 실행 가능                                               │
│                                                                 │
│  combine(                                                       │
│      validateName(input.name),      // 독립!                   │
│      validateEmail(input.email),    // 독립!                   │
│      validateAge(input.age)         // 독립!                   │
│  ).map(User::new)                   // 전부 성공하면 합침      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Validation: 어플리커티브의 꽃

폼 검증을 생각해보세요. 이름, 이메일, 나이 검증은 서로 독립적입니다. Monad(Result)를 쓰면 첫 번째 에러에서 멈추지만, **Validation**을 쓰면 모든 에러를 한 번에 수집할 수 있습니다.

```java
// src/main/java/com/example/fp/ValidationDemo.java

// Validation: 에러를 수집하는 타입
public sealed interface Validation<E, A>
        permits Valid, Invalid {

    // 검증 성공
    record Valid<E, A>(A value) implements Validation<E, A> {}

    // 검증 실패 (에러 목록 - 하나 이상!)
    record Invalid<E, A>(List<E> errors) implements Validation<E, A> {}

    // map은 펑터처럼 동작
    default <B> Validation<E, B> map(Function<A, B> f) {
        return switch (this) {
            case Valid<E, A> v -> new Valid<>(f.apply(v.value()));
            case Invalid<E, A> i -> new Invalid<>(i.errors());
        };
    }

    // combine: 어플리커티브의 핵심!
    static <E, A, B, C> Validation<E, C> combine(
            Validation<E, A> va,
            Validation<E, B> vb,
            BiFunction<A, B, C> f
    ) {
        return switch (va) {
            case Valid<E, A> a -> switch (vb) {
                case Valid<E, B> b -> new Valid<>(f.apply(a.value(), b.value()));
                case Invalid<E, B> ib -> new Invalid<>(ib.errors());
            };
            case Invalid<E, A> ia -> switch (vb) {
                case Valid<E, B> b -> new Invalid<>(ia.errors());
                case Invalid<E, B> ib -> {
                    // 에러 합치기! (모노이드 사용)
                    var combined = new ArrayList<>(ia.errors());
                    combined.addAll(ib.errors());
                    yield new Invalid<>(combined);
                }
            };
        };
    }
}
```

#### 실전: 폼 검증 예제

```java
// src/main/java/com/example/fp/FormValidation.java

public class FormValidation {

    record CreateUserRequest(String name, String email, int age) {}
    record User(String name, Email email, Age age) {}

    // 각 필드별 검증 함수 (서로 독립!)
    Validation<String, String> validateName(String name) {
        if (name == null || name.isBlank()) {
            return new Invalid<>(List.of("이름은 필수입니다"));
        }
        if (name.length() < 2) {
            return new Invalid<>(List.of("이름은 2자 이상이어야 합니다"));
        }
        return new Valid<>(name.trim());
    }

    Validation<String, Email> validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            return new Invalid<>(List.of("유효한 이메일을 입력하세요"));
        }
        return new Valid<>(new Email(email));
    }

    Validation<String, Age> validateAge(int age) {
        if (age < 0 || age > 150) {
            return new Invalid<>(List.of("나이는 0~150 사이여야 합니다"));
        }
        return new Valid<>(new Age(age));
    }

    // Result(Monad) 사용 시: 첫 에러에서 중단!
    Result<String, User> validateWithResult(CreateUserRequest req) {
        return validateName(req.name())
            .flatMap(name -> validateEmail(req.email())
                .flatMap(email -> validateAge(req.age())
                    .map(age -> new User(name, email, age))));
        // 이름이 잘못되면 이메일/나이 에러는 못 봄!
    }

    // Validation(Applicative) 사용 시: 모든 에러 수집!
    Validation<String, User> validateWithValidation(CreateUserRequest req) {
        return Validation.combine3(
            validateName(req.name()),
            validateEmail(req.email()),
            validateAge(req.age()),
            User::new   // 세 검증 모두 성공하면 User 생성
            // User::new는 TriFunction<String, Email, Age, User>로 변환됨!
            //
            // combine3 내부 동작:
            // - 세 Validation 모두 Valid → combiner.apply(name, email, age) 호출
            // - 하나라도 Invalid → 모든 에러 수집하여 반환
            //
            // Java 컴파일러가 User(String, Email, Age) 생성자를
            // TriFunction 타입에 맞게 자동 매칭해줍니다.
        );
        // 모든 필드의 에러를 한 번에 반환!
    }
}

// 사용 예시
var request = new CreateUserRequest("", "bad-email", -5);
var result = formValidation.validateWithValidation(request);
// Invalid([
//   "이름은 필수입니다",
//   "유효한 이메일을 입력하세요",
//   "나이는 0~150 사이여야 합니다"
// ])
// 세 개의 에러가 한 번에!
```

#### 실전 예제: 숙박 예약 검증

회원가입 폼뿐만 아니라 숙박 예약에서도 Validation은 유용합니다.

```java
// 예약 요청 검증
public class BookingValidation {

    record BookingRequest(
        String guestName,
        String email,
        LocalDate checkIn,
        LocalDate checkOut,
        int guestCount,
        String roomType
    ) {}

    record ValidatedBooking(
        GuestName name,
        Email email,
        DateRange stay,
        GuestCount guests,
        RoomType room
    ) {}

    // 각 필드별 독립 검증
    Validation<String, GuestName> validateGuestName(String name) {
        if (name == null || name.isBlank())
            return new Invalid<>(List.of("예약자명을 입력하세요"));
        return new Valid<>(new GuestName(name.trim()));
    }

    Validation<String, DateRange> validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        var errors = new ArrayList<String>();

        if (checkIn == null)
            errors.add("체크인 날짜를 선택하세요");
        if (checkOut == null)
            errors.add("체크아웃 날짜를 선택하세요");
        if (checkIn != null && checkOut != null) {
            if (!checkIn.isBefore(checkOut))
                errors.add("체크아웃은 체크인 이후여야 합니다");
            if (checkIn.isBefore(LocalDate.now()))
                errors.add("과거 날짜는 예약할 수 없습니다");
        }

        return errors.isEmpty()
            ? new Valid<>(new DateRange(checkIn, checkOut))
            : new Invalid<>(errors);
    }

    Validation<String, GuestCount> validateGuestCount(int count, String roomType) {
        int maxGuests = getMaxGuests(roomType);  // 룸 타입별 최대 인원
        if (count < 1)
            return new Invalid<>(List.of("최소 1명 이상이어야 합니다"));
        if (count > maxGuests)
            return new Invalid<>(List.of(
                String.format("%s 객실 최대 인원은 %d명입니다", roomType, maxGuests)
            ));
        return new Valid<>(new GuestCount(count));
    }

    // Applicative 합성: 모든 에러 수집!
    Validation<String, ValidatedBooking> validateBooking(BookingRequest req) {
        return Validation.combine5(
            validateGuestName(req.guestName()),
            validateEmail(req.email()),
            validateDateRange(req.checkIn(), req.checkOut()),
            validateGuestCount(req.guestCount(), req.roomType()),
            validateRoomType(req.roomType()),
            ValidatedBooking::new
        );
    }
}

// 사용 예
var badRequest = new BookingRequest(
    "",                              // 빈 이름
    "invalid-email",                 // 잘못된 이메일
    LocalDate.of(2023, 1, 1),       // 과거 날짜
    LocalDate.of(2023, 1, 1),       // 체크인과 같은 날
    10,                              // 인원 초과
    "STANDARD"
);

var result = validation.validateBooking(badRequest);
// Invalid([
//   "예약자명을 입력하세요",
//   "유효한 이메일을 입력하세요",
//   "과거 날짜는 예약할 수 없습니다",
//   "체크아웃은 체크인 이후여야 합니다",
//   "STANDARD 객실 최대 인원은 4명입니다"
// ])
// 모든 문제를 한 번에 사용자에게 알려줄 수 있음!
```

#### 왜 Validation은 Monad가 될 수 없는가?

이 질문은 면접에서도 종종 나옵니다. 핵심은 **모나드 법칙**을 위반하기 때문입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│          Validation이 Monad가 될 수 없는 이유                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Monad의 flatMap은 "앞 결과를 보고 다음을 결정"해야 함:        │
│                                                                 │
│  Invalid([에러1]).flatMap(a ->                                  │
│      // 여기서 'a'가 없는데 어떻게 다음 검증을 호출하지?        │
│      validateSomething(a)  // a가 없음!                         │
│  )                                                              │
│                                                                 │
│  만약 억지로 flatMap을 구현하면?                                │
│  - 앞이 Invalid면 뒤는 실행 불가 → 에러 수집 불가!             │
│  - 이러면 그냥 Result와 같아짐                                  │
│                                                                 │
│  결론:                                                          │
│  ─────                                                          │
│  • 에러 수집을 위해선 모든 검증이 "독립적"이어야 함             │
│  • flatMap은 "순차적 의존"을 전제로 함                         │
│  • 이 둘은 본질적으로 충돌!                                     │
│                                                                 │
│  Validation은 Applicative이지만 Monad는 아님                   │
│  Result/Either는 Monad이자 Applicative (하지만 에러 수집 불가) │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Result vs Validation: 언제 무엇을 쓸까?

```
┌─────────────────────────────────────────────────────────────────┐
│              Result vs Validation 선택 가이드                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Result<T, E> (Monad) 사용:                                    │
│  ───────────────────────────                                    │
│  • 순차적 파이프라인 (앞 단계 성공해야 다음 가능)              │
│  • 외부 API 호출 체이닝                                        │
│  • 결제 처리 같은 트랜잭션                                     │
│  • 첫 실패에서 중단해도 되는 경우                              │
│                                                                 │
│  예: 결제 흐름                                                  │
│  validateOrder → reserveStock → processPayment → sendEmail    │
│  (앞 단계 실패하면 뒤는 의미 없음)                             │
│                                                                 │
│  ═══════════════════════════════════════════════════════════   │
│                                                                 │
│  Validation<E, T> (Applicative) 사용:                          │
│  ────────────────────────────────────                           │
│  • 폼 검증 (모든 필드 독립적)                                  │
│  • 설정 파일 파싱                                              │
│  • 여러 독립적 검증 규칙 적용                                  │
│  • 사용자에게 모든 에러를 한 번에 보여줘야 할 때               │
│                                                                 │
│  예: 회원가입 폼                                                │
│  validateName + validateEmail + validatePassword               │
│  (세 필드가 서로 영향 안 줌, 에러 전부 보여줘야 함)            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    결정 플로우차트                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Q: 단계들이 서로 의존하나요?                                    │
│     │                                                            │
│     ├── YES → Result (Monad) 사용                               │
│     │         "앞 단계 결과가 뒷 단계 입력으로 필요"             │
│     │                                                            │
│     └── NO → Q: 모든 에러를 수집해야 하나요?                    │
│               │                                                  │
│               ├── YES → Validation (Applicative) 사용           │
│               │         "폼 검증, 설정 파싱 등"                  │
│               │                                                  │
│               └── NO → Result도 OK (더 간단)                    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

#### Railway-Oriented Programming (ROP)과의 연결

여기서 배운 Result/Validation 패턴을 **Railway-Oriented Programming**이라고도 부릅니다. 철도 분기점처럼 성공/실패 두 트랙을 달리는 거죠.

**Result (Fail-Fast) - 순차적 철도**:
```
┌─────────────────────────────────────────────────────────────────┐
│                    Result: Fail-Fast 패턴                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  성공 트랙:                                                     │
│  ════[검증1]═══════[검증2]═══════[검증3]══════════→ Success   │
│         │                                                       │
│         ↓ (실패시 즉시 탈선)                                   │
│  ───────+───────────────────────────────────────────→ Failure │
│  실패 트랙                                                      │
│                                                                 │
│  특징: 첫 번째 실패에서 즉시 중단! 뒤 검증은 실행 안 됨       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Validation (Error Accumulation) - 병렬 검증**:
```
┌─────────────────────────────────────────────────────────────────┐
│               Validation: Error Accumulation 패턴               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│     [검증1]         [검증2]         [검증3]                    │
│        │               │               │                        │
│        ↓               ↓               ↓                        │
│     Valid/          Valid/          Valid/       ← 모두 독립   │
│     Invalid         Invalid         Invalid         실행!      │
│        │               │               │                        │
│        └───────────────┼───────────────┘                        │
│                        ↓                                        │
│                 ┌─────────────┐                                 │
│                 │  결과 병합   │                                 │
│                 └─────────────┘                                 │
│                        │                                        │
│            ┌───────────┴───────────┐                            │
│            ↓                       ↓                            │
│    모두 Valid → Valid      하나라도 Invalid →                  │
│                            Invalid(["에러1", "에러2", ...])    │
│                                                                 │
│  특징: 모든 검증 실행! 에러를 수집하여 한 번에 반환            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4장 퀴즈

**퀴즈 4.1 (초급)**: 다음 중 올바른 설명을 고르세요.
- A: map()은 컨테이너를 평탄화한다
- B: flatMap()은 중첩된 컨테이너를 평탄화한다
- C: 펑터는 flatMap()을 제공하는 타입이다
- D: 모나드는 map()만 제공하면 된다

**퀴즈 4.2 (중급)**: 다음 코드의 차이점을 설명하세요.

```java
Optional.of("5")
    .map(s -> Optional.of(Integer.parseInt(s)));
// vs
Optional.of("5")
    .flatMap(s -> Optional.of(Integer.parseInt(s)));
```

**퀴즈 4.3 (고급)**: 폼 검증에서 Result(Monad) 대신 Validation(Applicative)을 사용해야 하는 이유를 설명하세요. Validation이 Monad가 될 수 없는 이유도 함께 설명하세요.

> 정답은 [Part 3: 퀴즈 정답](fp-guide-korean-part3.md#퀴즈-정답) 섹션에서 확인하세요.

---

## 5. 대수적 구조

왜 "대수적"이라고 부를까요? 대수학(Algebra)은 **연산과 그 법칙**을 연구하는 수학 분야입니다. 프로그래밍에서는 값들을 결합하는 연산의 "계약(Contract)"을 정의하는 데 사용됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                   왜 대수적 구조를 배우는가?                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  목적 1: 추상화                                                 │
│  ─────────────────────────────────────────────────────────────  │
│  다양한 타입을 일관된 방식으로 다룰 수 있음                     │
│  String, Integer, List 모두 "결합 가능한 것"으로 추상화        │
│                                                                 │
│  목적 2: 합성 가능성                                            │
│  ─────────────────────────────────────────────────────────────  │
│  작은 조각을 조립해서 큰 것을 만들 수 있음                      │
│  모노이드가 있으면 어떤 컬렉션이든 reduce 가능!                 │
│                                                                 │
│  목적 3: 검증 가능성                                            │
│  ─────────────────────────────────────────────────────────────  │
│  법칙을 property-based test로 검증 가능                        │
│  "내가 만든 타입이 진짜 모노이드인가?" 테스트로 확인            │
│                                                                 │
│  목적 4: 병렬화                                                 │
│  ─────────────────────────────────────────────────────────────  │
│  결합법칙 덕분에 작업을 쪼개서 병렬 처리 가능                  │
│  parallelStream().reduce()가 동작하는 이유!                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.1 세미그룹과 모노이드

#### 비유: 레고 블록

레고 블록은 두 개를 붙이면 더 큰 블록이 되죠. 세 개를 붙일 때 (A+B)+C나 A+(B+C)나 결과가 같고요. 이게 **세미그룹**입니다. 여기에 "아무것도 없는 상태"(빈 바닥판)를 더하면 **모노이드**가 됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                 세미그룹 & 모노이드 = 레고                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  세미그룹: 결합할 수 있는 것들                                  │
│  ────────────────────────────────                               │
│   Block + Block = BiggerBlock  (두 블록 → 더 큰 블록)          │
│                                                                 │
│   법칙: (a + b) + c = a + (b + c)  [결합법칙]                  │
│   "어디서부터 결합하든 결과는 같음"                             │
│                                                                 │
│  모노이드: 세미그룹 + 빈 원소                                   │
│  ────────────────────────────────                               │
│   Block + Empty = Block  (블록 + 빈바닥 = 그 블록)             │
│   Empty + Block = Block  (빈바닥 + 블록 = 그 블록)             │
│                                                                 │
│   법칙: a + empty = a = empty + a  [항등원]                    │
│                                                                 │
│  실제 예시:                                                     │
│   • String:  결합 = +,       빈값 = ""                         │
│   • Integer: 결합 = +,       빈값 = 0                          │
│   • Integer: 결합 = *,       빈값 = 1                          │
│   • List:    결합 = concat,  빈값 = []                         │
│   • Boolean: 결합 = &&,      빈값 = true                       │
│   • Boolean: 결합 = ||,      빈값 = false                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```java
// src/main/java/com/example/fp/MonoidDemo.java

// 모노이드 인터페이스
public interface Monoid<T> {
    T empty();              // 항등원
    T combine(T a, T b);    // 결합 연산

    // 모노이드의 힘: 아무 컬렉션이나 reduce 가능!
    default T reduceAll(List<T> items) {
        return items.stream().reduce(empty(), this::combine);
    }
}

// 구체적인 모노이드들
public class Monoids {

    public static final Monoid<Integer> SUM = new Monoid<>() {
        public Integer empty() { return 0; }
        public Integer combine(Integer a, Integer b) { return a + b; }
    };

    public static final Monoid<Integer> PRODUCT = new Monoid<>() {
        public Integer empty() { return 1; }
        public Integer combine(Integer a, Integer b) { return a * b; }
    };

    public static final Monoid<String> STRING = new Monoid<>() {
        public String empty() { return ""; }
        public String combine(String a, String b) { return a + b; }
    };
}

// 사용
public static void main(String[] args) {
    var numbers = List.of(1, 2, 3, 4, 5);

    System.out.println(Monoids.SUM.reduceAll(numbers));      // 15
    System.out.println(Monoids.PRODUCT.reduceAll(numbers));  // 120
}
```

#### 왜 모노이드가 중요한가: 병렬화

```
┌─────────────────────────────────────────────────────────────────┐
│                  모노이드 = 병렬화 가능                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  결합법칙이 성립하니까 작업을 쪼갤 수 있음!                     │
│                                                                 │
│  순차: ((((1 + 2) + 3) + 4) + 5) = 15                          │
│                                                                 │
│  병렬:                                                          │
│       스레드 1: (1 + 2) = 3                                    │
│       스레드 2: (3 + 4) = 7                                    │
│       스레드 3: 5                                               │
│       합치기:   (3 + 7) + 5 = 15  결과 같음!                   │
│                                                                 │
│  이래서 parallelStream().reduce()가 동작하는 거임!             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5장 퀴즈

**퀴즈 5.1 (초급)**: 다음 중 모노이드가 아닌 것을 고르세요.
- A: (Integer, +, 0)
- B: (Integer, *, 1)
- C: (Integer, -, 0)
- D: (String, +, "")

**퀴즈 5.2 (중급)**: `parallelStream().reduce()`가 안전하게 동작하려면 reduce 연산이 어떤 법칙을 만족해야 하나요?

> 정답은 [Part 3: 퀴즈 정답](fp-guide-korean-part3.md#퀴즈-정답) 섹션에서 확인하세요.

---

## 6. 대수적 데이터 타입 (ADT)

ADT(Algebraic Data Type)는 **"Make Illegal States Unrepresentable"** 원칙의 핵심 도구입니다. 타입 시스템을 활용해서 잘못된 상태를 아예 표현할 수 없게 만드는 거죠.

```
┌─────────────────────────────────────────────────────────────────┐
│              왜 ADT가 필요한가? - 실제 문제                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Before: nullable 필드 지옥                                     │
│  ─────────────────────────────────────────────────────────────  │
│  class Order {                                                  │
│      String status;           // "UNPAID", "PAID", "SHIPPED"... │
│      LocalDateTime paidAt;    // 결제 안 했으면 null?           │
│      String trackingNumber;   // 배송 전이면 null?              │
│      LocalDateTime deliveredAt; // 배송 안 됐으면 null?         │
│  }                                                              │
│                                                                 │
│  문제점:                                                        │
│  • status="UNPAID"인데 paidAt이 있으면? → 모순!                │
│  • status="SHIPPED"인데 trackingNumber가 null이면? → 버그!     │
│  • 18개 조합 중 5개만 유효, 나머지는 "불가능한 상태"            │
│                                                                 │
│  After: ADT로 상태별 필요한 데이터만                            │
│  ─────────────────────────────────────────────────────────────  │
│  sealed interface OrderStatus permits Unpaid, Paid, Shipped...  │
│      record Unpaid(LocalDateTime createdAt)                     │
│      record Paid(LocalDateTime paidAt, String txId)             │
│      record Shipped(String trackingNumber, LocalDateTime shippedAt)│
│                                                                 │
│  각 상태가 자기에게 필요한 데이터만 가짐!                       │
│  불가능한 조합 자체가 타입 시스템에서 표현 불가!                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.1 곱 타입 (Product Type)

#### 비유: 신청서 양식

신청서 생각해보세요. 이름 **그리고** 나이 **그리고** 이메일을 다 채워야 하죠. 이게 곱 타입입니다. "AND"로 연결된 구조.

```
┌─────────────────────────────────────────────────────────────────┐
│                     곱 타입 = AND / 그리고                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   곱 타입: "A 그리고 B 그리고 C"                                │
│                                                                 │
│   Person = 이름 AND 나이 AND 이메일                             │
│                                                                 │
│   왜 "곱"이냐?                                                  │
│   가능한 값의 수 = |이름| x |나이| x |이메일|                   │
│   (각 필드 타입의 가능한 값을 곱함)                             │
│                                                                 │
│   자바: record Person(String name, int age, String email) {}   │
│   코틀린: data class Person(val name: String, val age: Int, val email: String)│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 합 타입 (Sum Type)

#### 비유: 객관식 문제

객관식 문제는 A **또는** B **또는** C **또는** D 중 하나만 선택하죠. 이게 합 타입입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     합 타입 = OR / 또는                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   합 타입: "A 또는 B 또는 C" (하나만!)                          │
│                                                                 │
│   결제수단 = 카드 OR 계좌이체 OR 현금                           │
│                                                                 │
│   왜 "합"이냐?                                                  │
│   가능한 값의 수 = |카드| + |계좌이체| + |현금|                 │
│   (각 선택지의 가능한 값을 합함)                                │
│                                                                 │
│   자바: sealed interface + permits                              │
│   코틀린: sealed class/interface                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```java
// src/main/java/com/example/fp/adt/PaymentMethod.java

// 자바에서 합 타입 (sealed + permits)
public sealed interface PaymentMethod
        permits CreditCard, BankTransfer, Cash {

    // 패턴 매칭으로 빠짐없이 처리!
    static String describe(PaymentMethod method) {
        return switch (method) {
            case CreditCard c -> "카드 끝번호 " + c.last4();
            case BankTransfer b -> b.bankName() + " 계좌이체";
            case Cash c -> "현금 " + c.amount() + "원";
            // default 필요 없음! 컴파일러가 모든 케이스 체크함
        };
    }
}

// 각 선택지 (곱 타입으로 정의)
public record CreditCard(String last4, String expiry) implements PaymentMethod {}
public record BankTransfer(String bankName, String accountNumber) implements PaymentMethod {}
public record Cash(BigDecimal amount) implements PaymentMethod {}
```

```kotlin
// src/main/kotlin/com/example/fp/adt/PaymentMethod.kt

// 코틀린 합 타입
sealed interface PaymentMethod {
    data class CreditCard(val last4: String, val expiry: String) : PaymentMethod
    data class BankTransfer(val bankName: String, val accountNumber: String) : PaymentMethod
    data class Cash(val amount: BigDecimal) : PaymentMethod
}

// when 표현식으로 빠짐없이 처리
fun describe(method: PaymentMethod): String = when (method) {
    is PaymentMethod.CreditCard -> "카드 끝번호 ${method.last4}"
    is PaymentMethod.BankTransfer -> "${method.bankName} 계좌이체"
    is PaymentMethod.Cash -> "현금 ${method.amount}원"
    // 컴파일러가 빠진 케이스 잡아줌!
}
```

#### 실전 예제: 숙박 예약 상태 모델링

숙박 예약의 상태 전이를 ADT로 모델링하면 불가능한 상태를 타입 시스템이 막아줍니다.

```java
// 예약 상태 합 타입 - 각 상태가 필요한 데이터만 가짐
public sealed interface BookingStatus
        permits Pending, Confirmed, CheckedIn, Completed, Cancelled {

    // 예약 대기: 결제 대기 중
    record Pending(
        LocalDateTime createdAt,
        LocalDateTime expiresAt     // 결제 만료 시간
    ) implements BookingStatus {}

    // 예약 확정: 결제 완료
    record Confirmed(
        String confirmationNumber,  // 확정 번호
        Money deposit,              // 선결제 금액
        LocalDateTime confirmedAt
    ) implements BookingStatus {}

    // 체크인 완료
    record CheckedIn(
        String roomNumber,          // 실제 배정된 방
        List<String> keyCards,      // 발급된 키카드
        LocalDateTime checkedInAt
    ) implements BookingStatus {}

    // 이용 완료
    record Completed(
        Money finalBill,            // 최종 청구액
        Optional<Review> review,    // 리뷰 (선택)
        LocalDateTime checkedOutAt
    ) implements BookingStatus {}

    // 취소됨
    record Cancelled(
        CancelReason reason,
        Optional<Money> refund,     // 환불 금액
        LocalDateTime cancelledAt
    ) implements BookingStatus {}
}

// 상태별 허용 동작을 컴파일 타임에 체크
public class BookingService {

    // Pending → Confirmed만 가능
    public BookingStatus confirm(BookingStatus.Pending pending, PaymentInfo payment) {
        var confirmation = generateConfirmationNumber();
        return new BookingStatus.Confirmed(
            confirmation,
            payment.amount(),
            LocalDateTime.now()
        );
    }

    // Confirmed → CheckedIn만 가능
    public BookingStatus checkIn(BookingStatus.Confirmed confirmed, String roomNumber) {
        var keyCards = generateKeyCards(2);
        return new BookingStatus.CheckedIn(
            roomNumber,
            keyCards,
            LocalDateTime.now()
        );
    }

    // Pending에서 바로 CheckIn? 컴파일 에러!
    // public BookingStatus checkIn(BookingStatus.Pending pending, ...) { }
    // → 타입 시스템이 불가능한 전이를 막음!
}
```

```
┌─────────────────────────────────────────────────────────────────┐
│                 예약 상태 전이 다이어그램                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐   결제    ┌───────────┐   체크인   ┌──────────┐  │
│  │ Pending │─────────→│ Confirmed │──────────→│ CheckedIn │  │
│  └────┬────┘          └─────┬─────┘           └─────┬─────┘   │
│       │                     │                       │          │
│       │     취소            │ 취소                  │ 체크아웃 │
│       │                     │                       │          │
│       ↓                     ↓                       ↓          │
│  ┌─────────────────────────────┐             ┌───────────┐    │
│  │         Cancelled           │             │ Completed │    │
│  │  (환불 여부는 상태에 따라)   │             │ (리뷰 가능)│    │
│  └─────────────────────────────┘             └───────────┘    │
│                                                                 │
│  각 화살표가 타입-안전한 메서드로 구현됨!                       │
│  잘못된 전이는 컴파일 에러로 방지                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 실전 예제: API 응답 모델링

```kotlin
// src/main/kotlin/com/example/fp/adt/ApiResponse.kt

// 가능한 모든 상태를 타입으로 명시
sealed interface ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>
    data class Error(val code: Int, val message: String) : ApiResponse<Nothing>
    data object Loading : ApiResponse<Nothing>
}

// ViewModel에서 사용
class UserViewModel {
    private val _state = MutableStateFlow<ApiResponse<User>>(ApiResponse.Loading)

    suspend fun loadUser(id: Long) {
        _state.value = ApiResponse.Loading
        _state.value = try {
            ApiResponse.Success(userRepository.getUser(id))
        } catch (e: Exception) {
            ApiResponse.Error(500, e.message ?: "알 수 없는 오류")
        }
    }
}

// UI에서 모든 상태 처리
@Composable
fun UserScreen(response: ApiResponse<User>) {
    when (response) {
        is ApiResponse.Loading -> CircularProgressIndicator()
        is ApiResponse.Success -> UserProfile(response.data)
        is ApiResponse.Error -> ErrorMessage(response.message)
    }
}
```

### 6장 퀴즈

**퀴즈 6.1 (초급)**: 곱 타입과 합 타입의 차이점을 설명하고, Java에서 각각을 어떻게 구현하는지 예를 들어 설명하세요.

**퀴즈 6.2 (중급)**: 다음 코드의 문제점을 ADT 관점에서 설명하고 개선하세요.

```java
class Order {
    String status;         // "PENDING", "PAID", "SHIPPED"
    LocalDateTime paidAt;  // null if not paid
    String trackingNumber; // null if not shipped
}
```

**퀴즈 6.3 (고급)**: "Make Illegal States Unrepresentable" 원칙이 왜 중요한지 설명하세요.

> 정답은 [Part 3: 퀴즈 정답](fp-guide-korean-part3.md#퀴즈-정답) 섹션에서 확인하세요.

---

## 다음 단계

이제 컨테이너와 ADT를 마스터했습니다! 다음 Part에서는 실전 활용을 배웁니다.

**[Part 3: 평가/실전](fp-guide-korean-part3.md)**로 계속
- 지연 평가 (Lazy Evaluation)
- 실전 예제: 주문 처리 파이프라인
- 모든 챕터의 퀴즈 정답

---

### 전체 가이드 네비게이션

- [Part 1: 기초](fp-guide-korean-part1.md) - 순수함수, 불변성, 고차함수, 합성, 커링
- **Part 2: 컨테이너/ADT** (현재 문서) - 펑터, 모나드, 어플리커티브, 대수적 구조, ADT
- [Part 3: 평가/실전](fp-guide-korean-part3.md) - 지연 평가, 실전 예제, 퀴즈 정답
