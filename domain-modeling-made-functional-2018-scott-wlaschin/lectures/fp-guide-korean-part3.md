# 자바/코틀린 개발자를 위한 함수형 프로그래밍 가이드 - Part 3: 평가/실전

> **"OOP는 움직이는 부품을 캡슐화해서 코드를 이해하기 쉽게 만들고, FP는 움직이는 부품 자체를 줄여서 코드를 이해하기 쉽게 만든다."** — Michael Feathers

---

## 이 파트의 목차

7. [평가 전략](#7-평가-전략)
   - 지연 평가
8. [실전에서 조립하기](#8-실전에서-조립하기)
   - 주문 처리 파이프라인
   - 정리: 머릿속 지도
   - 다음 단계
- [퀴즈 정답](#퀴즈-정답) (1장~7장 모든 정답)

### 전체 가이드 네비게이션

- [Part 1: 기초](fp-guide-korean-part1.md) - 순수함수, 불변성, 고차함수, 합성, 커링
- [Part 2: 컨테이너/ADT](fp-guide-korean-part2.md) - 펑터, 모나드, 어플리커티브, 대수적 구조, ADT
- **Part 3: 평가/실전** (현재 문서) - 지연 평가, 실전 예제, 퀴즈 정답

---

## 7. 평가 전략

### 7.1 지연 평가 (Lazy Evaluation)

```
┌─────────────────────────────────────────────────────────────────┐
│                   "지연" 용어 설명                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  지연(遲延, Lazy): "진짜 필요할 때까지 계산을 미룸"             │
│                                                                 │
│  즉시 평가 (Eager/Strict):                                      │
│  • 정의 즉시 실행                                               │
│  • List.of(1,2,3).map(n -> n*2)  → 바로 [2,4,6] 생성          │
│                                                                 │
│  지연 평가 (Lazy):                                              │
│  • 사용 시점에 실행                                             │
│  • Stream.of(1,2,3).map(n -> n*2)  → 아직 아무것도 안 함!     │
│  • .toList() 호출해야 비로소 계산                               │
│                                                                 │
│  별명들: 느긋한 평가, 게으른 평가, 필요시 평가                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 비유: 넷플릭스 vs DVD

예전에 DVD 빌리던 시절 기억나세요? 미리 다 빌려와야 했죠. 안 보는 것도 빌려오느라 돈 낭비. 반면 넷플릭스는 보는 것만 스트리밍합니다. 이게 지연 평가입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│              지연 vs 즉시 = 넷플릭스 vs DVD                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  즉시 평가 (DVD):                                               │
│  모든 DVD 미리 구매 → 공간 차지, 돈 낭비                        │
│  2개만 볼 건데 100개 샀으면?                                    │
│                                                                 │
│  지연 평가 (넷플릭스):                                          │
│  볼 때만 스트리밍 → 실제 보는 것만 로딩                         │
│  무한 카탈로그도 가능!                                          │
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ 즉시: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, ...] // 다 계산! │    │
│  │                                                         │    │
│  │ 지연: 1 → 2 → 3 → ... (필요할 때 계산)                │    │
│  │      ↑                                                  │    │
│  │      요청하기 전엔 이것만 존재                          │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```java
// src/main/java/com/example/fp/LazyDemo.java

public class LazyDemo {

    // 자바 Stream은 지연 평가!
    public void demonstrateLaziness() {
        // 아직 아무 일도 안 일어남! 그냥 "레시피"만 정의
        var stream = Stream.iterate(1, n -> n + 1)  // 무한!
                .filter(n -> {
                    System.out.println("필터링: " + n);
                    return n % 2 == 0;
                })
                .map(n -> {
                    System.out.println("매핑: " + n);
                    return n * 10;
                });

        System.out.println("스트림 생성됨, 아직 아무것도 실행 안 됨!");

        // 터미널 연산이 실행을 트리거
        var result = stream.limit(3).toList();
        // 출력 보면 filter/map이 번갈아 나옴 - 한 번에 하나씩 처리!
    }
}
```

#### 7.1.1 상세 실행 흐름

위 코드의 실행 순서를 자세히 따라가봅시다.

```java
var stream = Stream.iterate(1, n -> n + 1)
    .filter(n -> { System.out.println("F:" + n); return n % 2 == 0; })
    .map(n -> { System.out.println("M:" + n); return n * 10; });

var result = stream.limit(3).toList();
```

```
┌─────────────────────────────────────────────────────────────────┐
│                 실제 실행 순서 (limit(3))                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  F:1 → (홀수, 통과 X)                                          │
│  F:2 → (짝수, 통과 O) → M:2 → 20 (첫 번째 결과!)              │
│  F:3 → (홀수, 통과 X)                                          │
│  F:4 → (짝수, 통과 O) → M:4 → 40 (두 번째 결과!)              │
│  F:5 → (홀수, 통과 X)                                          │
│  F:6 → (짝수, 통과 O) → M:6 → 60 (세 번째 결과!) → 종료!     │
│                                                                 │
│  핵심 통찰:                                                     │
│  ─────────────────────────────────────────────────────────────  │
│  • 요소별 개별 처리: 한 요소가 filter→map 다 거친 후 다음 요소 │
│  • 쇼트서킷: limit(3) 도달 즉시 종료, 7부터는 아예 안 봄      │
│  • 무한 스트림도 OK: 필요한 3개만 계산하고 멈춤                 │
│                                                                 │
│  즉시 평가였다면?                                               │
│  모든 요소 filter 완료 → 모든 요소 map 완료 → limit           │
│  → 무한 스트림이면 영원히 안 끝남!                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

```kotlin
// src/main/kotlin/com/example/fp/LazyDemo.kt

// 코틀린은 lazy 델리게이트 내장
class ExpensiveService {
    // 처음 접근할 때만 계산
    val expensiveData: List<Data> by lazy {
        println("비싼 데이터 계산 중...")
        fetchFromDatabase()  // 딱 한 번만, 필요할 때만 실행
    }
}

// Sequence = 코틀린의 지연 컬렉션
fun lazySequenceDemo() {
    val result = generateSequence(1) { it + 1 }  // 무한!
        .filter { println("필터: $it"); it % 2 == 0 }
        .map { println("맵: $it"); it * 10 }
        .take(3)  // 아직 지연
        .toList() // 터미널: 여기서 평가 시작
}
```

#### 왜 지연 평가가 필요한가?

```
┌─────────────────────────────────────────────────────────────────┐
│                  지연 평가의 목적과 이점                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 무한 구조 표현 (Infinite Structures)                        │
│     Stream.iterate() - 필요한 만큼만 계산                       │
│     무한 시퀀스도 메모리 터지지 않음                            │
│                                                                 │
│  2. 불필요한 계산 회피 (Short-Circuit)                          │
│     findFirst() - 첫 번째 찾으면 즉시 중단                      │
│     anyMatch() - 조건 만족하면 나머지 무시                      │
│                                                                 │
│  3. 메모리 효율 (Memory Efficiency)                             │
│     10GB 파일 처리 - 한 줄씩만 메모리에                         │
│     중간 컬렉션 없이 파이프라인 처리                            │
│                                                                 │
│  4. 조합 용이 (Easy Composition)                                │
│     중간 연산을 자유롭게 추가/제거                              │
│     실제 실행은 터미널 연산에서만                               │
│                                                                 │
│  ═══════════════════════════════════════════════════════════    │
│                                                                 │
│  실제 문제 해결:                                                │
│  ─────────────────────────────────────────────────────────────  │
│  • 페이지네이션: 첫 10개만 필요한데 전체 정렬?                  │
│    → 지연 평가로 필요한 10개만 처리                            │
│                                                                 │
│  • 검색: 조건 맞는 첫 항목 찾으면 나머지는 불필요               │
│    → findFirst()로 즉시 중단                                   │
│                                                                 │
│  • 로그 분석: 100GB 로그에서 에러만 추출                        │
│    → 한 줄씩 읽으며 필터링, 메모리 문제 없음                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7장 퀴즈

**퀴즈 7.1 (초급)**: Java Stream과 Kotlin Sequence의 공통점은 무엇인가요?

**퀴즈 7.2 (중급)**: 다음 코드의 실행 순서를 예측하세요.

```java
Stream.of(1, 2, 3, 4, 5)
    .filter(n -> { System.out.print("F" + n + " "); return n % 2 == 0; })
    .map(n -> { System.out.print("M" + n + " "); return n * 10; })
    .limit(1)
    .toList();
```

**퀴즈 7.3 (고급)**: 무한 Stream이 가능한 이유를 지연 평가 관점에서 설명하세요.

> 정답은 아래 [퀴즈 정답](#퀴즈-정답) 섹션에서 확인하세요.

---

## 8. 실전에서 조립하기

### 실전 예제: 주문 처리 파이프라인

지금까지 배운 것들을 다 합쳐봅시다.

```java
// src/main/java/com/example/fp/OrderProcessor.java

public class OrderProcessor {

    // ADT: 가능한 모든 주문 결과 상태
    sealed interface OrderResult permits
            OrderSuccess, ValidationError, PaymentError, InventoryError {}

    record OrderSuccess(String orderId, Receipt receipt) implements OrderResult {}
    record ValidationError(List<String> errors) implements OrderResult {}
    record PaymentError(String reason) implements OrderResult {}
    record InventoryError(List<String> unavailableItems) implements OrderResult {}

    // 각 단계별 순수 함수
    private Function<OrderRequest, Either<ValidationError, ValidatedOrder>>
            validate = this::validateOrder;

    private Function<ValidatedOrder, Either<InventoryError, ReservedOrder>>
            reserveInventory = this::reserveInventory;

    private Function<ReservedOrder, Either<PaymentError, PaidOrder>>
            processPayment = this::processPayment;

    private Function<PaidOrder, OrderSuccess>
            createReceipt = this::createReceipt;

    // 모나드 합성: 철도 지향 프로그래밍
    public OrderResult process(OrderRequest request) {
        return validate.apply(request)                          // Either<E, ValidatedOrder>
                .flatMap(reserveInventory)                      // Either<E, ReservedOrder>
                .flatMap(processPayment)                        // Either<E, PaidOrder>
                .map(createReceipt)                             // Either<E, OrderSuccess>
                .fold(
                    error -> error,        // 왼쪽: 에러 반환
                    success -> success     // 오른쪽: 성공 반환
                );
    }

    // 각 함수는 순수하고, 테스트 가능하고, 조합 가능
    private Either<ValidationError, ValidatedOrder> validateOrder(OrderRequest req) {
        var errors = new ArrayList<String>();
        if (req.items().isEmpty()) errors.add("주문 항목이 비어있습니다");
        if (req.customerId() == null) errors.add("고객 ID가 필요합니다");

        return errors.isEmpty()
                ? Either.right(new ValidatedOrder(req))
                : Either.left(new ValidationError(errors));
    }

    // ... 나머지 순수 함수들
}
```

---

## 정리: 머릿속 지도

```
┌─────────────────────────────────────────────────────────────────┐
│                    FP 개념 정리                                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  기초 체력                                                      │
│  ──────────                                                     │
│  • 순수 함수: f(x)는 항상 같은 y 반환                          │
│  • 불변성: 데이터 안 바꿈, 새 복사본 만듦                       │
│  • 참조 투명성: 표현식을 값으로 대체 가능                       │
│                                                                 │
│  함수를 값처럼                                                  │
│  ───────────────                                                │
│  • 일급 함수: 함수도 변수에 담고 전달 가능                      │
│  • 고차 함수: 함수를 받거나 반환하는 함수                       │
│  • 클로저: 자기 환경을 기억하는 함수                            │
│                                                                 │
│  함수 조립                                                      │
│  ───────────                                                    │
│  • 합성: f.andThen(g) = g(f(x))                                │
│  • 커링: f(a,b,c) → f(a)(b)(c)                                 │
│                                                                 │
│  컨테이너                                                       │
│  ──────────                                                     │
│  • 펑터: map() - 컨테이너 안 내용 변환                         │
│  • 모나드: flatMap() - 연산 연결, 중첩 방지                    │
│  • 어플리커티브: combine() - 독립 계산 결합, 에러 수집         │
│  • Result vs Validation: 순차 의존 vs 독립 검증                │
│                                                                 │
│  대수적 구조                                                    │
│  ───────                                                        │
│  • 모노이드: combine() + empty - reduce/fold 가능              │
│  • ADT: 곱 타입(AND) + 합 타입(OR)                             │
│                                                                 │
│  평가 전략                                                      │
│  ──────────                                                     │
│  • 지연 평가: 필요할 때만 계산                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

1. **Result/Validation 연습** - 철도 패턴과 에러 수집 체화하기
2. **Vavr 라이브러리** - 자바용 본격 FP 라이브러리 (Either, Validation, Try 등)
3. **Arrow-kt** - 코틀린용 종합 FP 라이브러리 (Validated, Either 등)
4. **"Functional Programming in Kotlin"** - 깊이 있는 학습용

---

> **"일급 함수는 함수형 프로그래밍으로 가는 입문 약물이다."** — 누군가

> **"모나드는 자기함자 범주의 모노이드일 뿐이야."** — 이 가이드 다 읽으면 이해될 농담

---

## 퀴즈 정답

### 1장 정답

**퀴즈 1.1 정답**: **A, D**
- A: `add(int, int)` - 순수 함수. 같은 입력에 항상 같은 출력, 부작용 없음.
- B: `getCount()` - 불순. 외부 상태(`this.count`)를 변경함.
- C: `doubled()` - 불순. 입력 리스트를 직접 변경함(mutate).
- D: `format(LocalDate)` - 순수 함수. 포맷터는 불변이고 결과가 결정적.
- E: `random()` - 불순. 호출할 때마다 다른 값 반환.

**퀴즈 1.2 정답**: **참조 투명함 (관찰 가능한 관점에서)**
- 같은 key로 `compute()`를 여러 번 호출해도 항상 같은 값 반환
- 내부 캐시는 변경되지만, 외부에서 관찰 가능한 동작은 순수함
- 이를 "관찰 가능한 순수성(observational purity)"이라 함
- 엄밀히 말하면 불순하지만, 실용적으로는 참조 투명으로 취급

**퀴즈 1.3 정답**:
- 불변성은 순수 함수의 **필요조건**이지만 충분조건은 아님
- 불변 데이터 없이도 순수 함수 작성 가능 (예: `int add(int a, int b)`)
- 하지만 객체를 다루는 순수 함수를 작성하려면 불변성이 필수
- 가변 객체를 인자로 받으면 그 객체의 상태에 따라 결과가 달라질 수 있음

### 2장 정답

**퀴즈 2.1 정답**:
```java
Predicate<T>:      T → boolean
Consumer<T>:       T → void
Supplier<T>:       () → T
BiFunction<T,U,R>: (T, U) → R
```

**퀴즈 2.2 정답**:
```
5
3
7
```
`String::length`가 각 이름의 길이를 반환하고, `System.out::println`이 출력함.

**퀴즈 2.3 정답**:
- **불변성 원칙**: 캡처된 값이 변하면 람다 실행 결과가 비결정적이 됨
- **참조 투명성 위반**: 같은 람다가 다른 시점에 다른 결과를 낼 수 있음
- **멀티스레드 안전성**: 변경 가능한 변수 캡처 시 동기화 문제 발생
- Java는 이 문제를 컴파일 타임에 방지하기 위해 effectively final 제약 도입

### 3장 정답

**퀴즈 3.1 정답**:
- `f.andThen(g)`: 입력 → f → g → 출력 (f 먼저, g 다음)
- `f.compose(g)`: 입력 → g → f → 출력 (g 먼저, f 다음)
- 수학에서 (g . f)(x) = g(f(x))이므로 `compose`가 수학적 순서
- 코드 가독성 측면에서는 `andThen`이 데이터 흐름 방향과 일치해서 더 직관적

**퀴즈 3.2 정답**: **12**
- `addOne.apply(5)` = 6
- `multiplyTwo.apply(6)` = 12
- `andThen`이므로 addOne → multiplyTwo 순서

**퀴즈 3.3 정답**:
```java
// 타입
Function<String, Function<String, Function<String, String>>> createEmail =
    user -> domain -> tld -> user + "@" + domain + "." + tld;

// 부분 적용
Function<String, Function<String, String>> atGmail = createEmail.apply("user");
Function<String, String> gmailCom = atGmail.apply("gmail");
String email = gmailCom.apply("com");  // "user@gmail.com"

// 또는 한 줄로
String email2 = createEmail.apply("user").apply("gmail").apply("com");
```

### 4장 정답

**퀴즈 4.1 정답**: **B**
- A: map()은 내용물만 변환, 평탄화 안 함
- B: flatMap()은 중첩 제거 (평탄화)
- C: 펑터는 map()을 제공, flatMap은 모나드
- D: 모나드는 of + flatMap 둘 다 필요

**퀴즈 4.2 정답**:
```java
// map 사용: Optional<Optional<Integer>>
// flatMap 사용: Optional<Integer>
```
- `map`은 함수 결과를 그대로 감싸서 중첩 발생
- `flatMap`은 함수 결과(Optional)를 평탄화

**퀴즈 4.3 정답**:
- **Result 사용 시**: 첫 에러에서 중단, 나머지 검증 실행 안 됨
- **Validation 사용 시**: 모든 검증 실행, 에러 전체 수집
- **Monad 불가 이유**: flatMap은 이전 값으로 다음 계산을 결정해야 함. Invalid일 때 값이 없어서 다음 검증을 호출할 수 없음. 억지로 구현하면 에러 수집 불가.

### 5장 정답

**퀴즈 5.1 정답**: **C**
- A: (0 + a = a), ((a + b) + c = a + (b + c)) - 모노이드
- B: (1 * a = a), ((a * b) * c = a * (b * c)) - 모노이드
- C: (a - 0 = a) 하지만 (0 - a != a), 또한 결합법칙 불만족 - 모노이드 아님
- D: ("" + s = s), 문자열 연결은 결합법칙 만족 - 모노이드

**퀴즈 5.2 정답**: **결합법칙(Associativity)**
- `(a op b) op c = a op (b op c)`
- 병렬로 나눠 계산한 결과를 어떤 순서로 합쳐도 같아야 함
- 결합법칙이 없으면 병렬 처리 결과가 순서에 따라 달라짐

### 6장 정답

**퀴즈 6.1 정답**:
- **곱 타입 (AND)**: 모든 필드가 "그리고"로 연결. `record Person(String name, int age)`
- **합 타입 (OR)**: 여러 선택지 중 "하나". `sealed interface Status permits Active, Inactive`
- 곱 타입은 필드 수만큼 정보 필요, 합 타입은 선택지 중 하나만

**퀴즈 6.2 정답**:
```java
// 문제점: nullable 필드로 불가능한 상태 표현 가능
// - status="PENDING"인데 paidAt이 있는 모순
// - status="SHIPPED"인데 trackingNumber가 null인 버그

// 개선: sealed interface로 상태별 필수 데이터 명시
sealed interface OrderStatus permits Pending, Paid, Shipped {
    record Pending(LocalDateTime createdAt) implements OrderStatus {}
    record Paid(LocalDateTime paidAt, String txId) implements OrderStatus {}
    record Shipped(String trackingNumber, LocalDateTime shippedAt) implements OrderStatus {}
}
```

**퀴즈 6.3 정답**:
- 컴파일 타임에 불가능한 상태를 방지 → 런타임 버그 원천 차단
- 패턴 매칭으로 모든 케이스 강제 처리 → 빠진 케이스 방지
- 코드 자체가 문서 역할 → 각 상태에 필요한 데이터가 명시적

### 7장 정답

**퀴즈 7.1 정답**:
- 둘 다 **지연 평가(Lazy Evaluation)** 사용
- 터미널 연산 전까지 실제 계산 수행 안 함
- 요소별 개별 처리 (배치 처리가 아님)
- 무한 시퀀스 표현 가능

**퀴즈 7.2 정답**: `F1 F2 M2 ` (공백 포함)
- F1: 1은 홀수, 통과 X
- F2: 2는 짝수, 통과 O → M2: 20 생성
- limit(1) 도달 → 종료
- 3, 4, 5는 아예 처리 안 됨 (쇼트서킷)

**퀴즈 7.3 정답**:
- 지연 평가는 "요청할 때만" 다음 요소 계산
- `Stream.iterate(1, n -> n + 1)`은 "레시피"만 정의, 실제 무한 리스트 생성 X
- `limit(n)` 같은 쇼트서킷 연산과 결합하면 필요한 만큼만 계산
- 즉시 평가였다면 무한 루프 발생

---

### 전체 가이드 네비게이션

- [Part 1: 기초](fp-guide-korean-part1.md) - 순수함수, 불변성, 고차함수, 합성, 커링
- [Part 2: 컨테이너/ADT](fp-guide-korean-part2.md) - 펑터, 모나드, 어플리커티브, 대수적 구조, ADT
- **Part 3: 평가/실전** (현재 문서) - 지연 평가, 실전 예제, 퀴즈 정답
