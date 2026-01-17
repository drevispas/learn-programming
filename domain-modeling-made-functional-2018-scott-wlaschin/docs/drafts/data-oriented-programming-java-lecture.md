# Data-Oriented Programming in Java: The Deep Dive
**(Java 데이터 지향 프로그래밍 심화 분석 및 실전 가이드)**

> **원전:** *Data-Oriented Programming in Java* by Chris Kiehl  
> **성격:** 단순 요약이 아닌, 책의 철학과 세부 기법을 풀어서 설명한 **심화 강의 교재**  
> **학습 목표:** Java의 새로운 기능을 넘어, **복잡성을 통제하는 수학적 사고방식**과 **엔터프라이즈급 아키텍처링** 능력을 배양한다.

---

## 📑 목차 (Table of Contents)

### Part 1: 사고의 전환 (Foundations)
*   [Chapter 1. 객체 지향의 환상과 데이터의 실체](#chapter-1-객체-지향의-환상과-데이터의-실체)
    *   복잡성의 근원: 섞여버린 데이터와 로직
    *   DOP의 4대 원칙
*   [Chapter 2. 데이터란 무엇인가? (Identity vs Value)](#chapter-2-데이터란-무엇인가-identity-vs-value)
    *   참조(Reference)의 배신
    *   불변성이 주는 자유: 스냅샷 프로그래밍

### Part 2: 데이터 모델링의 수학 (Algebraic Data Types)
*   [Chapter 3. 타입 시스템의 기수(Cardinality) 이론](#chapter-3-타입-시스템의-기수cardinality-이론)
    *   곱 타입(Product Type)과 상태 폭발
    *   합 타입(Sum Type)과 상태 축소
    *   **Deep Dive:** `Optional`은 왜 합 타입인가?
*   [Chapter 4. 불가능한 상태를 표현 불가능하게 만들기](#chapter-4-불가능한-상태를-표현-불가능하게-만들기)
    *   '유효성 검증'이 필요 없는 설계
    *   Sealed Interface의 망라성(Exhaustiveness) 활용

### Part 3: 데이터의 흐름과 제어 (Behavior & Control Flow)
*   [Chapter 5. 전체 함수(Total Functions)와 실패 처리](#chapter-5-전체-함수total-functions와-실패-처리)
    *   예외(Exception)는 거짓말이다
    *   `Result` 타입을 통한 흐름 제어
*   [Chapter 6. 파이프라인과 결정론적 시스템](#chapter-6-파이프라인과-결정론적-시스템)
    *   샌드위치 아키텍처 (Impure-Pure-Impure)
    *   비즈니스 로직에서 I/O 격리하기

### Part 4: 고급 기법과 대수적 추론 (Advanced Theory)
*   [Chapter 7. 대수적 속성을 활용한 설계](#chapter-7-대수적-속성을-활용한-설계)
    *   결합법칙(Associativity)과 병렬 처리
    *   멱등성(Idempotence)과 재시도 안전성
*   [Chapter 8. 인터프리터 패턴: 로직을 데이터로](#chapter-8-인터프리터-패턴-로직을-데이터로)
    *   Rule Engine 만들기

### Part 5: 레거시 탈출 (Refactoring & Architecture)
*   [Chapter 9. 현실 세계의 DOP: JPA/Spring과의 공존](#chapter-9-현실-세계의-dop-jpaspring과의-공존)
    *   엔티티(Entity)는 도메인 모델이 아니다
    *   점진적 리팩토링 전략

---

## Part 1: 사고의 전환 (Foundations)

### Chapter 1. 객체 지향의 환상과 데이터의 실체

#### 1.1 왜 우리는 고통스러운가?
우리는 수십 년간 **"데이터와 그 데이터를 조작하는 메서드를 한 클래스에 묶어야 한다(캡슐화)"**고 배웠습니다. 이 이론은 작은 프로그램에서는 완벽하게 작동합니다. 하지만 수백만 라인의 엔터프라이즈 시스템에서는 다음과 같은 문제를 야기합니다.

1.  **정보의 감옥:** 데이터가 객체 안에 갇혀 있습니다. A 객체의 데이터를 B 객체에서 쓰려면 getter를 만들고, 또 다른 DTO로 변환해야 합니다. 시스템의 50%는 데이터를 꺼내고 옮기는 코드입니다.
2.  **맥락의 혼재:** `User` 클래스는 'DB 저장용'이면서, '로그인 로직용'이면서, 'JSON 직렬화용'이 됩니다. 모든 맥락이 섞여 거대한 `God Class`가 탄생합니다.

#### 1.2 DOP의 4대 원칙
Chris Kiehl은 이를 해결하기 위해 4가지 원칙을 제시합니다.

1.  **Code와 Data를 분리하라:** 레코드(Record)에는 로직을 넣지 않고, 클래스(Class)에는 데이터를 넣지 않습니다.
2.  **데이터를 일반적인(Generic) 형태로 표현하라:** 커스텀 클래스보다는 맵, 리스트 같은 기본 자료구조의 조합을 선호하되, 타입 안정성을 위해 레코드를 사용합니다.
3.  **데이터는 불변(Immutable)이다:** 생성된 데이터는 절대 변하지 않습니다. 수정이 필요하면 새로운 복사본을 만듭니다.
4.  **데이터 스키마와 데이터 표현을 분리하라:** 데이터의 유효성 검증은 데이터가 생성될 때가 아니라, 데이터가 **사용되는 경계**에서 수행합니다.

---

### Chapter 2. 데이터란 무엇인가? (Identity vs Value)

#### 2.1 정체성(Identity)과 값(Value)의 차이
가장 중요한 개념적 구분입니다.

*   **Value (값):** 내용물이 같으면 같은 것입니다.
    *   숫자 `1000`은 어디에 있든 `1000`입니다.
    *   `좌표(10, 20)`은 불변의 사실입니다.
*   **Identity (정체성):** 내용물이 같아도 다를 수 있고, 내용물이 변해도 같을 수 있습니다.
    *   `회원(ID: 1)`은 이름을 개명해도 여전히 같은 회원입니다.
    *   메모리 주소(`==`)로 구분됩니다.

OOP는 모든 것을 **Identity(객체)**로 취급하려다 실패했습니다. DOP는 시스템의 90%를 **Value(값)**으로 다루고, 꼭 필요한 10%만 Identity로 관리합니다.

#### 2.2 Record: Java가 선물한 Value Type
Java 14+의 `record`는 단순한 보일러플레이트 제거기가 아닙니다. 이것은 **"이 데이터는 Value입니다"**라고 선언하는 것입니다.

```java
// 이것은 '객체'가 아닙니다. 불변의 '사실'입니다.
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        // 생성자에서 불변식(Invariant)을 강제합니다.
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("돈은 음수가 될 수 없습니다.");
        }
    }
}
```

> **Deep Dive: 얕은 불변성(Shallow Immutability)의 함정**
> Record는 필드 자체를 `final`로 만들지만, 필드가 가리키는 객체의 내부까지 얼리지는 못합니다.
> ```java
> record Team(List<Member> members) {} // 위험! List는 변경 가능함.
> ```
> **해결책:** 생성자에서 방어적 복사(`List.copyOf`)를 수행하여 **깊은 불변성**을 확보해야 합니다.

---

## Part 2: 데이터 모델링의 수학 (Algebraic Data Types)

이 챕터는 이 책의 하이라이트입니다. 복잡성을 '감'이 아니라 '수학'으로 제어하는 방법을 배웁니다.

### Chapter 3. 타입 시스템의 기수(Cardinality) 이론

시스템의 복잡도는 **"가능한 상태의 총 개수(Cardinality)"**와 비례합니다.

#### 3.1 곱 타입 (Product Type): 상태의 폭발
클래스에 필드를 추가하는 것은 경우의 수를 **곱하는(Product)** 행위입니다.

```java
class Order {
    boolean isShipped; // 2가지 경우 (T/F)
    boolean isPaid;    // 2가지 경우 (T/F)
    boolean isCanceled;// 2가지 경우 (T/F)
}
// 총 상태 수 = 2 * 2 * 2 = 8가지
```
필드가 늘어날수록 복잡도는 기하급수적으로 폭발합니다. 여기서 "배송되었지만(T) 결제 안 됨(F)" 같은 **논리적으로 불가능한 상태**가 발생합니다. 버그의 온상입니다.

#### 3.2 합 타입 (Sum Type): 상태의 축소
합 타입은 **"이것 아니면 저것(OR)"** 관계입니다. Java에서는 `sealed interface`로 구현합니다.

```java
sealed interface OrderStatus permits Paid, Shipped, Canceled {}
// 총 상태 수 = 1(Paid) + 1(Shipped) + 1(Canceled) = 3가지
```
**8가지 상태를 3가지로 줄였습니다.** 복잡도가 획기적으로 감소했습니다. DOP의 핵심은 **곱 타입(Record)을 줄이고 합 타입(Sealed Interface)을 늘리는 것**입니다.

#### 3.3 Deep Dive: `Optional`은 왜 합 타입인가?
`Optional<T>`는 값이 있거나(`Present`) 없거나(`Empty`) 둘 중 하나입니다.
즉, `Cardinality(Optional<T>) = Cardinality(T) + 1` 입니다.
null을 허용하는 참조 타입은 사실상 `T + 1`의 상태를 가지지만, 개발자가 `+1`(null)의 처리를 자주 잊어버린다는 것이 문제입니다.

---

### Chapter 4. 불가능한 상태를 표현 불가능하게 만들기 (Make Illegal States Unrepresentable)

#### 4.1 '유효성 검증'이 필요 없는 설계
대부분의 개발자는 데이터를 받고 나서 `isValid()`를 호출합니다. DOP 개발자는 **유효하지 않은 데이터는 아예 생성조차 못하게** 타입을 설계합니다.

**Bad Design:**
```java
class User {
    String email;
    boolean isEmailVerified;
    // 이메일이 없는데 verified가 true일 수 있음 (불가능한 상태)
}
```

**DOP Design:**
```java
sealed interface UserEmail {
    record Unverified(String email) implements UserEmail {}
    record Verified(String email, LocalDateTime verifiedAt) implements UserEmail {}
}
// '검증된 이메일' 상태에는 반드시 '검증 시각'이 존재해야 함을 타입으로 강제.
// 이메일 없이 검증됨 상태를 만들 수 없음.
```

#### 4.2 Switch Expression과 망라성(Exhaustiveness)
Sealed interface를 사용하면 컴파일러가 우리의 비서가 됩니다.

```java
String getStatusMessage(UserEmail email) {
    return switch (email) {
        case Unverified u -> "인증해주세요: " + u.email();
        case Verified v   -> "인증 완료 (" + v.verifiedAt() + ")";
        // 컴파일러: "모든 경우를 처리했습니다. default가 필요 없습니다."
    };
}
```
나중에 `Banned` 상태가 추가되면, 컴파일러가 "이 곳을 처리하지 않았다"고 에러를 띄워줍니다. **유지보수의 안전망**이 획기적으로 튼튼해집니다.

---

## Part 3: 데이터의 흐름과 제어 (Behavior & Control Flow)

### Chapter 5. 전체 함수(Total Functions)와 실패 처리

#### 5.1 부분 함수(Partial Function)의 위험
입력값 중 일부에 대해서만 결과를 반환하고, 나머지는 예외를 던지거나 null을 리턴하는 함수를 '부분 함수'라 합니다. 이는 거짓말을 하는 함수입니다.

```java
// 시그니처는 User를 반환한다고 약속하지만, 실제로는 에러를 던질 수 있음.
User findUser(int id) { ... throw new NotFoundException(); }
```

#### 5.2 전체 함수(Total Function) 만들기
모든 입력에 대해 반드시 결과를 반환해야 합니다. 실패 또한 결과(Data)입니다.

```java
sealed interface Result<S, F> {
    record Success<S, F>(S value) implements Result<S, F> {}
    record Failure<S, F>(F error) implements Result<S, F> {}
}

// 이제 시그니처만 보고도 실패 가능성을 알 수 있음.
Result<User, String> findUser(int id) {
    if (db.exists(id)) return new Result.Success<>(...);
    return new Result.Failure<>("User not found");
}
```
이것이 **"Failure as Data"** 패턴입니다. 예외 처리를 강제하여 런타임 에러를 획기적으로 줄입니다.

---

### Chapter 6. 파이프라인과 결정론적 시스템

#### 6.1 결정론적(Deterministic) 함수 = 순수 함수
*   **정의:** 입력이 같으면 언제나 출력이 같다. 외부 상태를 변경하지 않는다.
*   **장점:**
    1.  **테스트 혁명:** Mock 객체가 전혀 필요 없음.
    2.  **캐싱:** 결과값을 저장해두고 재사용 가능 (Memoization).
    3.  **병렬화:** 스레드 안전(Thread-safe)함이 보장됨.

#### 6.2 샌드위치 아키텍처 (Functional Core, Imperative Shell)
순수 함수만으로 프로그램을 짤 수는 없습니다. DB도 가야 하고 API도 호출해야 합니다. 그래서 **샌드위치**를 만듭니다.

1.  **Top Bun (Impure):** 데이터를 수집합니다. (DB 조회, HTTP 요청)
2.  **Meat (Pure):** 수집된 데이터를 인자로 받아 비즈니스 로직을 수행하고, **결과 데이터**를 리턴합니다. (핵심 로직)
3.  **Bottom Bun (Impure):** 결과 데이터를 이용해 세상을 변경합니다. (DB 저장, 메일 발송)

> **핵심:** 비즈니스 로직(Meat) 안에서는 절대 DB를 호출하지 마십시오. 필요한 데이터는 모두 파라미터로 받아야 합니다.

---

## Part 4: 고급 기법과 대수적 추론 (Advanced Theory)

### Chapter 7. 대수적 속성을 활용한 설계

데이터 모델링에 수학적 속성을 적용하면 분산 시스템에서 강력한 위력을 발휘합니다.

#### 7.1 결합법칙 (Associativity)
`(A + B) + C == A + (B + C)`
순서에 상관없이 묶어서 계산할 수 있다는 속성입니다. 이 속성을 만족하는 데이터 구조(예: 로그 병합, 리스트 합치기)는 **병렬 처리(MapReduce)**가 가능합니다. 데이터를 여러 서버에 나눠서 처리하고 합쳐도 결과가 같음이 보장됩니다.

#### 7.2 멱등성 (Idempotence)
`f(f(x)) == f(x)`
같은 연산을 여러 번 수행해도 결과가 달라지지 않는 성질입니다.
*   **DOP 적용:** "결제 요청"을 "결제 상태로 변경"이라는 데이터 변환으로 모델링하면, 네트워크 오류로 요청을 두 번 보내도 안전합니다.

---

### Chapter 8. 인터프리터 패턴: 로직을 데이터로

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하는 대신 규칙 자체를 데이터로 만듭니다.

#### 8.1 Rule Engine 설계
```java
// 로직을 데이터 구조(Tree)로 표현
sealed interface Rule {
    record MinTotal(double amount) implements Rule {}
    record UserVip() implements Rule {}
    record And(Rule left, Rule right) implements Rule {} // 재귀적 구조
}

// 해석기(Interpreter)
boolean evaluate(Rule rule, Context ctx) {
    return switch (rule) {
        case MinTotal(a) -> ctx.total >= a;
        case UserVip() -> ctx.user.isVip();
        case And(l, r) -> evaluate(l, ctx) && evaluate(r, ctx);
    };
}
```
이 방식의 장점은 **비즈니스 로직을 런타임에 동적으로 생성하거나 수정**할 수 있다는 점입니다. (예: DB에 저장된 규칙을 불러와서 실행)

---

## Part 5: 레거시 탈출 (Refactoring & Architecture)

### Chapter 9. 현실 세계의 DOP: JPA/Spring과의 공존

우리는 Spring과 Hibernate를 쓰는 프로젝트에서 일합니다. 여기서 어떻게 DOP를 할까요?

#### 9.1 엔티티(Entity) 분리 전략
JPA Entity는 **Identity(정체성)**이자 **가변 객체**입니다. DOP의 **Value(값)**와는 상극입니다.

*   **타협하지 마십시오:** Entity에 비즈니스 로직을 넣지 마십시오.
*   **변환:** Controller/Service 진입 시점에 Entity를 **DOP Record(도메인 모델)**로 즉시 변환하십시오.
*   **로직 수행:** 모든 비즈니스 로직은 Record를 입출력으로 사용하십시오.
*   **재변환:** 저장이 필요할 때만 Record의 값을 Entity로 업데이트하십시오.

#### 9.2 점진적 리팩토링
1.  가장 복잡하고 버그가 많은 비즈니스 로직 하나를 선정합니다.
2.  해당 로직에 필요한 데이터를 담을 `Record`를 정의합니다.
3.  로직을 `static` 순수 함수로 추출하여 `Record`를 입력받고 `Result`를 반환하게 만듭니다.
4.  기존 서비스 코드(Shell)에서 데이터를 모아 순수 함수를 호출하도록 변경합니다.
5.  단위 테스트를 작성합니다(Mocking 없이!).

---

## 부록: 실전 연습문제 및 해설

### Q1. 부분적 생성(Partial Construction) 문제 해결
**문제:** 빌더 패턴을 쓰면 필수 필드를 빼먹고 객체를 생성할 위험이 있습니다. DOP에서는 이를 어떻게 해결하나요?
**해설:** 모든 필수 데이터가 모이기 전에는 객체 생성을 허용하지 않습니다. 만약 데이터가 단계적으로 모인다면, 각 단계별로 별도의 Record를 정의해야 합니다 (`SignUpRequest` -> `VerifiedUser` -> `ActiveUser`).

### Q2. 성능에 대한 우려
**문제:** 불변 객체를 계속 복사하면(Copy-on-Write) 메모리 낭비가 심하지 않나요?
**해설:**
1.  Java의 GC(Garbage Collector)는 짧게 생존하고 죽는 객체(Short-lived object)를 처리하는 데 최적화되어 있습니다. Record 복사는 생각보다 매우 저렴합니다.
2.  구조적 공유(Structural Sharing)를 통해, 변경되지 않은 필드는 기존 객체의 참조를 그대로 재사용하므로 메모리 낭비가 최소화됩니다.
3.  성능보다 **안전성**과 **유지보수성**이 현대 소프트웨어 개발의 병목입니다.

---
**[강의 마무리]**
DOP는 새로운 언어를 배우는 것이 아닙니다. Java가 가진 강력한 타입 시스템을 최대한 활용하여, **우리의 머리로는 감당할 수 없는 복잡성을 컴파일러에게 외주 주는 기술**입니다. 이제 여러분은 데이터의 본질을 꿰뚫어 보고, 불가능한 상태가 존재하지 않는 견고한 시스템을 설계할 준비가 되었습니다.