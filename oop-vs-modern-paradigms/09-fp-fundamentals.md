# 09. FP Fundamentals (함수형 프로그래밍 기초)

> 함수형 프로그래밍의 핵심 빌딩 블록: 일급 함수, 고차 함수, 클로저, 커링, 합성을 이해하고 Java 25에서 활용한다.

---

## 1. 일급 함수 (First-Class Functions)

### 핵심 개념
- **관련 키워드**: First-Class Citizen, Functional Interface, Lambda, Method Reference
- **통찰**: 함수를 정수나 문자열처럼 변수에 저장하고, 인자로 전달하고, 반환할 수 있으면 일급 함수이다.
- **설명**:
  일급 함수(First-Class Function)란 함수를 프로그래밍 언어에서 "값"으로 다룰 수 있다는 의미이다. 정수를 변수에 저장하고 메서드에 넘기고 반환할 수 있듯이, 함수도 동일한 조작이 가능하다.

  Java에서는 Functional Interface(추상 메서드가 하나인 인터페이스)를 통해 일급 함수를 지원한다. `Function<T,R>`, `Predicate<T>`, `Consumer<T>`, `Supplier<T>` 등이 대표적이다. 람다 표현식은 이러한 Functional Interface의 인스턴스를 간결하게 생성하는 문법이다.

  일급 함수의 핵심은 "나중에 실행될 코드 조각"을 값처럼 전달한다는 점이다. 람다는 정의 시점에 실행되지 않고, 호출 시점에 비로소 실행된다. 이 특성이 지연 평가, 콜백, 전략 패턴의 함수형 대체를 가능하게 한다.

  메서드 레퍼런스(`String::length`)는 단순 위임에서 람다보다 간결하고, 변환이나 조합이 필요할 때는 람다(`x -> x.length() + 1`)가 적합하다.

**[그림 09.1]** 일급 함수 (First-Class Functions)
```
+-------------------------------------------------------------------+
|                  일급 함수 = 값처럼 다루는 함수                       |
+-------------------------------------------------------------------+
|                                                                     |
|   정수처럼:                     함수도 마찬가지:                      |
|   int x = 5;                   Function<A,B> f = a -> transform(a);|
|   process(x);                  process(f);                          |
|   return x;                    return f;                            |
|   List<Integer>                List<Function<A,B>>                  |
|                                                                     |
|   Lambda = "나중에 실행될 코드 조각을 값으로 포장"                    |
|   Method Reference = 기존 메서드를 Functional Interface로 참조       |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **익명 클래스와 동일한 것**: 람다는 익명 클래스의 단축 표기가 아니다. 컴파일러가 invokedynamic을 사용해 더 효율적으로 처리한다.
- **함수 포인터**: C의 함수 포인터와 달리 타입 안전하며, 클로저를 통해 환경을 캡처할 수 있다.
- **모든 인터페이스에 사용 가능**: 반드시 추상 메서드가 하나인 Functional Interface여야 한다.

### Before: Traditional OOP

**[코드 09.1]** Traditional OOP: Strategy Pattern을 위해 인터페이스 + 구현 클래스 필요
```java
 1| // package: com.ecommerce.shared
 2| // [X] Strategy Pattern을 위해 인터페이스 + 구현 클래스 필요
 3| interface Formatter {
 4|   String format(String input);
 5| }
 6| 
 7| class UpperCaseFormatter implements Formatter {
 8|   @Override
 9|   public String format(String input) {
10|     return input.toUpperCase();
11|   }
12| }
13| 
14| class TrimFormatter implements Formatter {
15|   @Override
16|   public String format(String input) {
17|     return input.trim();
18|   }
19| }
20| 
21| public class TextProcessor {
22|   private final List<Formatter> formatters;
23| 
24|   public TextProcessor(List<Formatter> formatters) {
25|     this.formatters = formatters;
26|   }
27| 
28|   public String process(String text) {
29|     String result = text;
30|     for (Formatter f : formatters) {
31|       result = f.format(result);
32|     }
33|     return result;
34|   }
35| }
```
- **의도 및 코드 설명**: 텍스트를 여러 단계로 포맷팅하기 위해 Strategy 패턴을 사용한다. 각 포맷 전략을 인터페이스와 구현 클래스로 분리한다.
- **뭐가 문제인가**:
  - 단순한 변환 로직에도 인터페이스 + 구현 클래스를 만들어야 함
  - 클래스 파일이 기하급수적으로 늘어남
  - 변환 로직의 조합과 재배치가 불편함
  - 코드량 대비 실제 로직이 차지하는 비율이 극히 낮음

### After: Modern Approach

**[코드 09.2]** Modern: 함수를 값으로 다뤄서 전략을 인라인으로 정의
```java
 1| // package: com.ecommerce.shared
 2| // [O] 함수를 값으로 다뤄서 전략을 인라인으로 정의
 3| import java.util.function.UnaryOperator;
 4| import java.util.List;
 5| 
 6| public class TextProcessor {
 7|   private final List<UnaryOperator<String>> formatters;
 8| 
 9|   public TextProcessor(List<UnaryOperator<String>> formatters) {
10|     this.formatters = formatters;
11|   }
12| 
13|   public String process(String text) {
14|     String result = text;
15|     for (var f : formatters) {
16|       result = f.apply(result);
17|     }
18|     return result;
19|   }
20| 
21|   public static void main(String[] args) {
22|     // 함수를 변수에 저장
23|     UnaryOperator<String> trim = String::trim;
24|     UnaryOperator<String> upper = String::toUpperCase;
25|     UnaryOperator<String> exclaim = s -> s + "!";
26| 
27|     // 함수를 컬렉션에 담기
28|     var processor = new TextProcessor(List.of(trim, upper, exclaim));
29| 
30|     // 함수를 인자로 전달
31|     System.out.println(processor.process("  hello world  "));
32|     // 출력: "HELLO WORLD!"
33|   }
34| }
```
- **의도 및 코드 설명**: `UnaryOperator<String>`을 활용해 변환 함수를 일급 값으로 다룬다. 메서드 레퍼런스와 람다로 전략을 인라인 정의한다.
- **무엇이 좋아지나**:
  - 별도 인터페이스/클래스 없이 함수만으로 전략 구현
  - 새로운 변환은 람다 한 줄 추가로 해결
  - 조합과 순서 변경이 리스트 조작으로 단순화
  - 코드량이 대폭 줄고, 로직이 명확히 드러남

### 이해를 위한 부가 상세
Java의 주요 Functional Interface 정리:

**[표 09.1]** 일급 함수 (First-Class Functions)
| Interface | 시그니처 | 용도 |
|-----------|----------|------|
| `Function<T,R>` | `T -> R` | 변환 |
| `Predicate<T>` | `T -> boolean` | 필터 |
| `Consumer<T>` | `T -> void` | 소비 |
| `Supplier<T>` | `() -> T` | 생성 |
| `UnaryOperator<T>` | `T -> T` | 동일 타입 변환 |
| `BinaryOperator<T>` | `(T,T) -> T` | 두 값 결합 |

### 틀리기/놓치기 쉬운 부분
- `Function<Integer, Integer>`는 auto-boxing이 발생한다. 원시 타입이 필요하면 `IntUnaryOperator` 사용.
- `@FunctionalInterface` 어노테이션은 선택적이지만, 컴파일러 검증을 위해 붙이는 것이 권장된다.
- 메서드 레퍼런스 4가지 형태를 구분할 것: 정적(`Integer::parseInt`), 임의 인스턴스(`String::length`), 특정 인스턴스(`list::contains`), 생성자(`User::new`).

### 꼭 기억할 것
일급 함수는 OOP의 Strategy, Command, Template Method 패턴을 클래스 없이 구현할 수 있게 해준다. "함수도 값이다"라는 사고방식이 함수형 프로그래밍의 출발점이다.

---

## 2. 고차 함수 (Higher-Order Functions)

### 핵심 개념
- **관련 키워드**: Higher-Order Function, map, filter, reduce, Behavior Abstraction
- **통찰**: 함수를 인자로 받거나 함수를 반환하는 함수를 고차 함수라 하며, "무엇을 할지"와 "어떻게 반복할지"를 분리한다.
- **설명**:
  고차 함수는 함수를 인자로 받거나(map, filter, reduce), 함수를 반환하는(팩토리, 데코레이터) 함수이다. OOP에서 총주방장이 레시피를 받아 새 요리법을 만들어내듯, 고차 함수는 행동(behavior) 자체를 추상화한다.

  map은 "각 요소를 변환하라", filter는 "조건에 맞는 것만 골라라", reduce는 "모두 합쳐서 하나로 만들라"를 선언적으로 표현한다. 이 세 연산만으로 대부분의 컬렉션 처리가 가능하다.

  고차 함수의 핵심 가치는 보일러플레이트 제거이다. for 루프 10번 복사 대신, filter/map 한 번에 로직만 교체하면 된다. 반복 구조의 버그가 있어도 한 곳만 수정하면 전체에 반영된다.

  또한 고차 함수는 의도를 명확히 표현한다. `filter`를 보면 "골라내기"임을 즉시 알 수 있고, `map`을 보면 "변환"임을 알 수 있다.

**[그림 09.2]** 고차 함수 (Higher-Order Functions)
```
+-------------------------------------------------------------------+
|                  고차 함수의 세 가지 핵심 연산                        |
+-------------------------------------------------------------------+
|                                                                     |
|   map:    [A, B, C] --f--> [f(A), f(B), f(C)]   요소별 변환        |
|   filter: [A, B, C] --p--> [A, C]               조건부 선택        |
|   reduce: [A, B, C] --op-> X                    전체를 하나로      |
|                                                                     |
|   조합 예시:                                                        |
|   orders.stream()                                                   |
|       .filter(Order::isPaid)        <-- 결제된 것만                  |
|       .map(Order::amount)           <-- 금액 추출                   |
|       .reduce(ZERO, BigDecimal::add) <-- 합산                       |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **단순 유틸 메서드**: `Math.max(a, b)`는 고차 함수가 아니다. 함수를 받거나 반환하지 않기 때문이다.
- **콜백과 동일**: 콜백은 고차 함수의 한 활용이지만, 고차 함수는 더 넓은 개념이다(데코레이터, 합성 등 포함).

### Before: Traditional OOP

**[코드 09.3]** Traditional OOP: 매번 for 루프를 복붙하며 로직 변경
```java
 1| // package: com.ecommerce.order
 2| // [X] 매번 for 루프를 복붙하며 로직 변경
 3| public class OrderService {
 4|   public BigDecimal getTotalPaidAmount(List<Order> orders) {
 5|     BigDecimal total = BigDecimal.ZERO;
 6|     for (Order order : orders) {
 7|       if (order.isPaid()) {
 8|         total = total.add(order.amount());
 9|       }
10|     }
11|     return total;
12|   }
13| 
14|   public List<String> getPaidProductNames(List<Order> orders) {
15|     List<String> names = new ArrayList<>();
16|     for (Order order : orders) {
17|       if (order.isPaid()) {
18|         names.add(order.productName());
19|       }
20|     }
21|     return names;
22|   }
23| 
24|   public int countUnpaidOrders(List<Order> orders) {
25|     int count = 0;
26|     for (Order order : orders) {
27|       if (!order.isPaid()) {
28|         count++;
29|       }
30|     }
31|     return count;
32|   }
33| }
```
- **의도 및 코드 설명**: 결제된 주문의 총액, 결제된 상품명 목록, 미결제 주문 수를 각각 구한다. 매번 for 루프와 조건문을 반복한다.
- **뭐가 문제인가**:
  - for-if 구조가 반복적으로 등장 (DRY 위반)
  - 반복 로직에 버그가 있으면 모든 메서드를 수정해야 함
  - 코드에서 "의도"가 아닌 "메커니즘"이 먼저 보임
  - 새로운 집계 기능 추가 시 동일한 보일러플레이트 재생산

### After: Modern Approach

**[코드 09.4]** Modern: 고차 함수로 "무엇을"과 "어떻게"를 분리
```java
 1| // package: com.ecommerce.order
 2| // [O] 고차 함수로 "무엇을"과 "어떻게"를 분리
 3| import java.util.function.*;
 4| import java.util.stream.*;
 5| import java.math.BigDecimal;
 6| 
 7| public class OrderService {
 8|   record Order(Long id, String productName, BigDecimal amount, boolean paid) {
 9|     public boolean isPaid() { return paid; }
10|   }
11| 
12|   public BigDecimal getTotalPaidAmount(List<Order> orders) {
13|     return orders.stream()
14|       .filter(Order::isPaid)
15|       .map(Order::amount)
16|       .reduce(BigDecimal.ZERO, BigDecimal::add);
17|   }
18| 
19|   public List<String> getPaidProductNames(List<Order> orders) {
20|     return orders.stream()
21|       .filter(Order::isPaid)
22|       .map(Order::productName)
23|       .toList();
24|   }
25| 
26|   public long countUnpaidOrders(List<Order> orders) {
27|     return orders.stream()
28|       .filter(Predicate.not(Order::isPaid))
29|       .count();
30|   }
31| 
32|   // 직접 고차 함수 정의: 함수를 반환하는 함수
33|   public static <T> Predicate<T> not(Predicate<T> p) {
34|     return t -> !p.test(t);
35|   }
36| }
```
- **의도 및 코드 설명**: Stream API의 고차 함수(filter, map, reduce)를 활용해 동일한 로직을 선언적으로 표현한다. 각 연산이 의도를 직접 드러낸다.
- **무엇이 좋아지나**:
  - 반복 구조가 Stream 내부로 추상화되어 한 번만 검증하면 됨
  - filter = "골라내기", map = "변환", reduce = "합치기"로 의도가 명확
  - 새 기능은 파이프라인 조립만으로 구현 가능
  - 병렬 처리가 `.parallelStream()`으로 즉시 전환 가능

### 이해를 위한 부가 상세
고차 함수를 직접 정의하면 도메인 특화 추상화를 만들 수 있다:

**[코드 09.5]** 함수를 인자로 받아 로깅을 추가하는 데코레이터 고차 함수
```java
 1| // package: com.ecommerce.order
 2| // 함수를 인자로 받아 로깅을 추가하는 데코레이터 고차 함수
 3| public static <T, R> Function<T, R> withLogging(
 4|     String label, Function<T, R> fn) {
 5|   return input -> {
 6|     System.out.println("[" + label + "] input: " + input);
 7|     R result = fn.apply(input);
 8|     System.out.println("[" + label + "] output: " + result);
 9|     return result;
10|   };
11| }
```

### 틀리기/놓치기 쉬운 부분
- `reduce`의 identity(초기값)는 결합 연산의 항등원이어야 한다. 합산이면 0, 곱이면 1.
- `stream()`은 한 번만 소비 가능하다. 여러 번 사용하려면 매번 새 stream을 생성해야 한다.
- `parallelStream()`에서 reduce가 올바르게 동작하려면 결합법칙(associativity)이 필수이다.

### 꼭 기억할 것
map-filter-reduce는 함수형 프로그래밍의 "삼신기"이다. 이 세 고차 함수만 잘 조합해도 대부분의 컬렉션 처리가 선언적이고 버그 없이 가능하다.

---

## 3. 클로저 (Closures)

### 핵심 개념
- **관련 키워드**: Closure, Lexical Scope, Variable Capture, Effectively Final
- **통찰**: 클로저는 자신이 생성된 환경(lexical scope)의 변수를 캡처하여 기억하는 함수이다.
- **설명**:
  클로저는 함수가 자기 본문 밖에 정의된 변수를 참조할 때 형성된다. 함수가 생성된 시점의 환경을 "배낭"처럼 메고 다니며, 나중에 실행될 때도 그 환경에 접근할 수 있다.

  Java에서 람다가 외부 변수를 캡처하려면 해당 변수가 effectively final이어야 한다. 이는 불변성 원칙과 직결된다. 캡처된 값이 변할 수 있다면 동일한 람다가 다른 시점에 다른 결과를 반환할 수 있어 참조 투명성이 깨진다.

  클로저의 실용적 활용은 세 가지이다. 첫째, 설정 캡슐화: 팩토리 없이 "설정이 고정된 함수"를 생성한다. 둘째, 상태 은닉: 클래스 없이 private 변수 효과를 낸다. 셋째, 부분 적용의 기반: 인자 일부를 미리 고정한 특수화된 함수를 만든다.

  Effectively final 제약은 FP 원칙(불변성, 참조 투명성)을 컴파일 타임에 강제하는 Java의 안전장치이다.

**[그림 09.3]** 클로저 (Closures)
```
+-------------------------------------------------------------------+
|                  클로저 = 환경을 캡처한 함수                          |
+-------------------------------------------------------------------+
|                                                                     |
|   Function<BigDecimal, BigDecimal>                                  |
|   createDiscounter(BigDecimal rate) {          <-- rate 정의        |
|       return price -> price * (1 - rate);      <-- rate 캡처        |
|   }                                                                 |
|                                                                     |
|   var tenOff = createDiscounter(0.10);  // rate=0.10 기억            |
|   var twentyOff = createDiscounter(0.20); // rate=0.20 기억          |
|                                                                     |
|   tenOff.apply(1000)   --> 900    (0.10을 기억)                     |
|   twentyOff.apply(1000) --> 800   (0.20을 기억)                     |
|                                                                     |
|   각 클로저는 자기만의 "배낭"(캡처된 rate)을 갖고 있음               |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **전역 변수 접근**: 전역 변수를 읽는 것은 클로저가 아니다. 클로저는 함수 생성 시점의 지역 환경을 캡처하는 것이다.
- **this 참조**: 인스턴스 메서드에서 this를 사용하는 것은 클로저 개념과 다르다. 클로저는 자유 변수(free variable)의 바인딩이다.

### Before: Traditional OOP

**[코드 09.6]** Traditional OOP: 설정값마다 별도 클래스를 만들어야 함
```java
 1| // package: com.ecommerce.coupon
 2| // [X] 설정값마다 별도 클래스를 만들어야 함
 3| interface DiscountCalculator {
 4|   BigDecimal calculate(BigDecimal price);
 5| }
 6| 
 7| class TenPercentDiscount implements DiscountCalculator {
 8|   @Override
 9|   public BigDecimal calculate(BigDecimal price) {
10|     return price.multiply(new BigDecimal("0.90"));
11|   }
12| }
13| 
14| class TwentyPercentDiscount implements DiscountCalculator {
15|   @Override
16|   public BigDecimal calculate(BigDecimal price) {
17|     return price.multiply(new BigDecimal("0.80"));
18|   }
19| }
20| 
21| // 할인율 10가지면 클래스 10개 필요!
```
- **의도 및 코드 설명**: 할인율별 계산기를 만들기 위해 각각 클래스로 구현한다.
- **뭐가 문제인가**:
  - 할인율이 달라질 때마다 새 클래스가 필요
  - 클래스 폭발 문제 (할인율 N개 = 클래스 N개)
  - 로직은 동일한데 설정값만 다름 -- 중복의 극치

### After: Modern Approach

**[코드 09.7]** Modern: 클로저로 설정값을 캡처한 함수 생성
```java
 1| // package: com.ecommerce.coupon
 2| // [O] 클로저로 설정값을 캡처한 함수 생성
 3| import java.math.BigDecimal;
 4| import java.util.function.Function;
 5| 
 6| public class DiscountFactory {
 7| 
 8|   // 클로저: discountRate를 캡처하여 "설정된 함수"를 반환
 9|   public static Function<BigDecimal, BigDecimal> createDiscounter(BigDecimal discountRate) {
10|     // discountRate는 effectively final -- 클로저에 캡처됨
11|     BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
12|     return price -> price.multiply(multiplier);
13|   }
14| 
15|   // 이벤트 핸들러 팩토리: userId를 캡처
16|   public static Runnable createGreeter(String userId, String message) {
17|     // userId와 message가 캡처됨
18|     return () -> System.out.println("To " + userId + ": " + message);
19|   }
20| 
21|   public static void main(String[] args) {
22|     // 할인율 N개도 클래스 생성 없이 해결
23|     var tenOff = createDiscounter(new BigDecimal("0.10"));
24|     var twentyOff = createDiscounter(new BigDecimal("0.20"));
25|     var vipDiscount = createDiscounter(new BigDecimal("0.30"));
26| 
27|     System.out.println(tenOff.apply(new BigDecimal("10000")));    // 9000
28|     System.out.println(twentyOff.apply(new BigDecimal("10000"))); // 8000
29|     System.out.println(vipDiscount.apply(new BigDecimal("10000"))); // 7000
30|   }
31| }
```
- **의도 및 코드 설명**: `createDiscounter`가 discountRate를 클로저로 캡처하여 할인 계산 함수를 반환한다. 설정값을 바꾸고 싶으면 인자만 변경하면 된다.
- **무엇이 좋아지나**:
  - 클래스 폭발 해소: 할인율 N개도 함수 N개로 해결
  - 설정 캡슐화: 외부에서 캡처된 값에 접근 불가
  - 런타임에 동적으로 특수화된 함수 생성 가능
  - 테스트 용이: 팩토리 함수와 반환된 함수를 독립적으로 검증

### 이해를 위한 부가 상세
Java의 effectively final 제약과 해결책:

**[코드 09.8]** 컴파일 에러: i는 변경되므로 effectively final이 아님
```java
 1| // package: com.ecommerce.shared
 2| // 컴파일 에러: i는 변경되므로 effectively final이 아님
 3| for (int i = 0; i < 10; i++) {
 4|   executor.submit(() -> System.out.println(i)); // 에러!
 5| }
 6| 
 7| // 해결: 루프 변수를 로컬 final로 복사
 8| for (int i = 0; i < 10; i++) {
 9|   final int captured = i;
10|   executor.submit(() -> System.out.println(captured)); // OK
11| }
```

### 틀리기/놓치기 쉬운 부분
- `final int[] count = {0}` 같은 우회법은 배열 참조가 final이지 내용은 가변이다. FP 정신에 어긋나며 스레드 안전하지 않다.
- 클로저가 가변 객체를 캡처하면 순수성이 깨진다. 불변 값만 캡처하도록 주의한다.
- 람다 내부에서 예외를 던질 때 checked exception은 Functional Interface 시그니처에 선언되어 있지 않아 처리가 까다롭다.

### 꼭 기억할 것
클로저는 "설정을 기억하는 함수"를 만드는 핵심 메커니즘이다. Java에서 effectively final 제약은 불변성 원칙의 컴파일 타임 강제이며, 이를 통해 참조 투명성과 스레드 안전성을 보장한다.

---

## 4. 커링과 부분 적용 (Currying and Partial Application)

### 핵심 개념
- **관련 키워드**: Currying, Partial Application, Function Specialization, Single-Argument Function
- **통찰**: 다인자 함수를 단일 인자 함수의 체인으로 변환(커링)하면, 인자를 하나씩 적용하여 특수화된 함수를 만들 수 있다(부분 적용).
- **설명**:
  커링(Currying)은 `f(a, b, c)`를 `f(a)(b)(c)` 형태로 변환하는 것이다. 즉, 인자 N개를 받는 함수를 인자 1개를 받고 나머지 인자를 받는 함수를 반환하는 체인으로 만든다. 부분 적용(Partial Application)은 다인자 함수에 인자 일부를 미리 고정하여 더 적은 인자를 받는 함수를 만드는 것이다.

  커링의 핵심 가치는 합성 적합성이다. `Function.andThen()`은 단일 인자 함수끼리만 연결 가능하므로, 다인자 함수를 커링하면 파이프라인에 참여시킬 수 있다.

  또한 커링은 재사용 가능한 특수화를 가능하게 한다. 범용 함수에서 설정값만 다른 여러 버전을 파생할 수 있다. 예를 들어 `log(level)(message)`에서 `infoLog = log("INFO")`로 특수화된 로거를 만든다.

  Java에서 커링된 함수의 타입은 `Function<A, Function<B, Function<C, D>>>`로 중첩되어 다소 장황하지만, 실제 사용은 직관적이다.

**[그림 09.4]** 커링과 부분 적용 (Currying and Partial Application)
```
+-------------------------------------------------------------------+
|                  커링 = 한 칸씩 채우기                                |
+-------------------------------------------------------------------+
|                                                                     |
|   일반:    f(size, milk, syrup) --> coffee                          |
|                                                                     |
|   커링:    f(size) --> g(milk) --> h(syrup) --> coffee               |
|                                                                     |
|   부분 적용:                                                        |
|   largeOrder = f("Large")       // size 고정                        |
|   largeSoy   = largeOrder("Soy") // milk도 고정                     |
|   result     = largeSoy("Vanilla") // 최종 결과                     |
|                                                                     |
|   각 단계에서 이전 인자는 클로저로 캡처됨!                           |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **오버로딩**: 메서드 오버로딩은 이름은 같지만 별개의 메서드이다. 커링은 하나의 함수에서 인자를 순차적으로 받는 것이다.
- **빌더 패턴**: 빌더는 가변 상태를 축적하지만, 커링은 불변 함수를 순차적으로 반환한다.
- **기본값 매개변수**: 기본값은 컴파일 타임에 고정되지만, 부분 적용은 런타임에 동적으로 특수화한다.

### Before: Traditional OOP

**[코드 09.9]** Traditional OOP: 설정 조합마다 메서드를 오버로딩하거나 빌더를 만듦
```java
 1| // package: com.ecommerce.shared
 2| // [X] 설정 조합마다 메서드를 오버로딩하거나 빌더를 만듦
 3| public class Logger {
 4|   public void log(String level, String module, String message) {
 5|     System.out.printf("[%s][%s] %s%n", level, module, message);
 6|   }
 7| 
 8|   // 편의 메서드를 계속 추가...
 9|   public void info(String module, String message) { log("INFO", module, message); }
10|   public void error(String module, String message) { log("ERROR", module, message); }
11|   public void infoAuth(String message) { log("INFO", "AUTH", message); }
12|   public void errorAuth(String message) { log("ERROR", "AUTH", message); }
13|   public void infoPayment(String message) { log("INFO", "PAYMENT", message); }
14|   // ... 조합 폭발!
15| }
```
- **의도 및 코드 설명**: 로깅 시 레벨, 모듈, 메시지를 조합하기 위해 편의 메서드를 다수 생성한다.
- **뭐가 문제인가**:
  - 레벨 M개, 모듈 N개면 편의 메서드 M*N개 필요
  - 새 모듈이나 레벨 추가 시 편의 메서드를 모두 추가해야 함
  - 조합의 유연성이 제한됨

### After: Modern Approach

**[코드 09.10]** Modern: 커링으로 범용 함수에서 특수화된 함수를 동적으로 파생
```java
 1| // package: com.ecommerce.shared
 2| // [O] 커링으로 범용 함수에서 특수화된 함수를 동적으로 파생
 3| import java.util.function.Function;
 4| 
 5| public class CurriedLogger {
 6| 
 7|   // 커링된 로거: level -> module -> message -> void
 8|   public static Function<String, Function<String, java.util.function.Consumer<String>>>
 9|       log() {
10|     return level -> module -> message ->
11|       System.out.printf("[%s][%s] %s%n", level, module, message);
12|   }
13| 
14|   public static void main(String[] args) {
15|     var curriedLog = log();
16| 
17|     // 부분 적용: 레벨 고정
18|     var infoLog = curriedLog.apply("INFO");
19|     var errorLog = curriedLog.apply("ERROR");
20| 
21|     // 부분 적용: 레벨 + 모듈 고정
22|     var infoAuth = infoLog.apply("AUTH");
23|     var errorPayment = errorLog.apply("PAYMENT");
24| 
25|     // 최종 사용: 메시지만 전달
26|     infoAuth.accept("사용자 로그인 성공");       // [INFO][AUTH] 사용자 로그인 성공
27|     errorPayment.accept("결제 시간 초과");       // [ERROR][PAYMENT] 결제 시간 초과
28| 
29|     // 새 모듈? 부분 적용 한 줄이면 됨
30|     var infoOrder = infoLog.apply("ORDER");
31|     infoOrder.accept("주문 생성됨");             // [INFO][ORDER] 주문 생성됨
32|   }
33| }
```
- **의도 및 코드 설명**: 커링된 `log()` 함수에서 레벨, 모듈을 순차적으로 부분 적용하여 특수화된 로거를 동적으로 생성한다.
- **무엇이 좋아지나**:
  - M*N 편의 메서드 대신 부분 적용 M+N줄로 해결
  - 새 조합은 부분 적용 한 줄 추가로 확장
  - 런타임에 동적으로 특수화 가능 (설정 파일에서 레벨/모듈을 읽어올 수 있음)
  - 특수화된 함수를 고차 함수에 바로 전달 가능

### 이해를 위한 부가 상세
커링된 함수 타입 해석법:

**[코드 09.11]** 커링과 부분 적용 (Currying and Partial Application)
```java
1| // package: com.ecommerce.shared
2| Function<String, Function<String, Function<String, BigDecimal>>>
3| //       1번 인자    2번 인자    3번 인자    최종 반환
4| 
5| // 화살표 표기: String -> String -> String -> BigDecimal
6| // 각 apply()마다 인자가 하나씩 소비되고, 나머지를 받는 함수가 반환됨
```

### 틀리기/놓치기 쉬운 부분
- Java의 중첩 Function 타입은 가독성이 떨어진다. 3인자 이상이면 커스텀 Functional Interface(`TriFunction` 등)를 정의하는 것이 나을 수 있다.
- 커링은 인자 순서가 중요하다. "가장 변하지 않는 인자"를 앞에 두어야 부분 적용의 효과가 극대화된다.
- 커링과 부분 적용은 다른 개념이다. 커링은 변환 기법이고, 부분 적용은 활용 패턴이다.

### 꼭 기억할 것
커링의 실전 가치는 "범용 함수에서 특수 함수를 파생"하는 것이다. 가장 고정적인 인자(설정, 의존성)를 앞에, 가장 변동적인 인자(데이터)를 뒤에 배치하면 부분 적용의 효과가 극대화된다.

---

## 5. 함수 합성 (Function Composition)

### 핵심 개념
- **관련 키워드**: Composition, andThen, compose, Pipeline, Unix Pipe
- **통찰**: 작은 단일 책임 함수들을 파이프라인으로 조립하여 복잡한 변환을 선언적으로 표현한다.
- **설명**:
  함수 합성은 Unix의 `cat | grep | sort | uniq`처럼, 작은 함수들을 파이프로 연결하여 복잡한 동작을 구성하는 것이다. `f.andThen(g)`는 "f를 적용한 후 g를 적용"을 의미하며, 새로운 합성 함수를 반환한다.

  합성의 핵심 가치는 단일 책임 원칙의 함수 버전이다. 각 함수는 하나의 변환만 담당하므로 이해, 테스트, 디버깅이 쉽다. 500줄짜리 메서드를 10개의 작은 순수 함수로 분해하면, 각각을 독립적으로 테스트하고 필요에 따라 재조합할 수 있다.

  `andThen`은 왼쪽에서 오른쪽으로(데이터 흐름 방향) 읽히므로 직관적이다. `compose`는 수학적 합성 순서(g . f = g(f(x)))로, 오른쪽에서 왼쪽으로 실행된다. 실무에서는 `andThen`이 가독성 면에서 권장된다.

  합성은 디버깅도 용이하다. 파이프라인 중간에 로깅 함수를 삽입(`f.andThen(log).andThen(g)`)하면 중간 결과를 확인할 수 있다.

**[그림 09.5]** 함수 합성 (Function Composition)
```
+-------------------------------------------------------------------+
|                  함수 합성 = 파이프라인                               |
+-------------------------------------------------------------------+
|                                                                     |
|   f.andThen(g).andThen(h):                                         |
|                                                                     |
|   input --> [  f  ] --> [  g  ] --> [  h  ] --> output              |
|              trim       lowercase    slugify                        |
|                                                                     |
|   andThen: 왼쪽 --> 오른쪽 (데이터 흐름 순서)                       |
|   compose: 오른쪽 --> 왼쪽 (수학적 합성 순서)                       |
|                                                                     |
|   핵심: 각 함수는 자기 입출력만 알면 됨.                             |
|         타입만 맞으면 어떤 함수든 조립 가능!                         |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **메서드 체이닝**: `builder.setA().setB().build()`는 가변 객체의 상태를 변경한다. 함수 합성은 순수 함수의 조립이다.
- **상속을 통한 확장**: OOP에서 상속으로 기능을 추가하는 것과 달리, 합성은 독립적 함수를 자유롭게 조합한다.

### Before: Traditional OOP

**[코드 09.12]** Traditional OOP: 하나의 거대한 메서드에 모든 변환 로직을 넣음
```java
 1| // package: com.ecommerce.shared
 2| // [X] 하나의 거대한 메서드에 모든 변환 로직을 넣음
 3| public class SlugGenerator {
 4|   public String generateSlug(String title) {
 5|     // 1단계: 공백 제거
 6|     String trimmed = title.trim();
 7|     // 2단계: 소문자 변환
 8|     String lowered = trimmed.toLowerCase();
 9|     // 3단계: 특수문자 제거
10|     String cleaned = lowered.replaceAll("[^a-z0-9\\s-]", "");
11|     // 4단계: 공백을 하이픈으로
12|     String slugified = cleaned.replaceAll("\\s+", "-");
13|     // 5단계: 연속 하이픈 제거
14|     String result = slugified.replaceAll("-+", "-");
15|     return result;
16|   }
17|   // 테스트: 전체 메서드를 한 번에 테스트해야 함
18|   // 재사용: 일부 단계만 재사용 불가
19|   // 확장: 새 단계 추가 시 메서드 내부를 수정해야 함
20| }
```
- **의도 및 코드 설명**: URL slug를 생성하기 위해 5단계 변환을 하나의 메서드에 모두 작성한다.
- **뭐가 문제인가**:
  - 개별 단계를 독립적으로 테스트할 수 없음
  - 일부 단계만 재사용하려면 코드를 복사해야 함
  - 새 단계 추가/순서 변경 시 메서드 내부를 수정해야 함 (OCP 위반)
  - 중간 결과 확인을 위해 디버거가 필수

### After: Modern Approach

**[코드 09.13]** Modern: 작은 순수 함수를 합성하여 파이프라인 구성
```java
 1| // package: com.example.pattern
 2| // [O] 작은 순수 함수를 합성하여 파이프라인 구성
 3| import java.util.function.Function;
 4| import java.util.function.UnaryOperator;
 5| 
 6| public class SlugGenerator {
 7| 
 8|   // 각 변환 단계를 독립적인 함수로 정의
 9|   static UnaryOperator<String> trim = String::trim;
10|   static UnaryOperator<String> lowercase = String::toLowerCase;
11|   static UnaryOperator<String> removeSpecialChars =
12|     s -> s.replaceAll("[^a-z0-9\\s-]", "");
13|   static UnaryOperator<String> spacesToHyphens =
14|     s -> s.replaceAll("\\s+", "-");
15|   static UnaryOperator<String> collapseHyphens =
16|     s -> s.replaceAll("-+", "-");
17| 
18|   // 합성: 파이프라인으로 조립
19|   static Function<String, String> generateSlug =
20|     ((Function<String, String>) trim)
21|       .andThen(lowercase)
22|       .andThen(removeSpecialChars)
23|       .andThen(spacesToHyphens)
24|       .andThen(collapseHyphens);
25| 
26|   // 디버깅: 중간에 로그 삽입 가능
27|   static <T> Function<T, T> peek(String label) {
28|     return value -> {
29|       System.out.println("[" + label + "] " + value);
30|       return value;
31|     };
32|   }
33| 
34|   static Function<String, String> debugSlug =
35|     ((Function<String, String>) trim)
36|       .andThen(peek("after trim"))
37|       .andThen(lowercase)
38|       .andThen(peek("after lowercase"))
39|       .andThen(removeSpecialChars)
40|       .andThen(spacesToHyphens)
41|       .andThen(collapseHyphens);
42| 
43|   public static void main(String[] args) {
44|     System.out.println(generateSlug.apply("  Hello World! Example  "));
45|     // 출력: "hello-world-example"
46|   }
47| }
```
- **의도 및 코드 설명**: 각 변환 단계를 독립 함수로 정의하고, `andThen`으로 파이프라인을 조립한다. `peek` 함수로 디버깅도 용이하다.
- **무엇이 좋아지나**:
  - 각 함수를 독립적으로 단위 테스트 가능
  - 다른 곳에서 일부 함수만 재사용 가능 (예: trim + lowercase만)
  - 새 단계 추가는 `andThen` 한 줄 삽입으로 해결 (OCP 준수)
  - 디버깅 시 중간 결과를 비침습적으로 확인 가능
  - 코드가 선언적: "무엇을 하는지"가 즉시 읽힘

### 이해를 위한 부가 상세
`andThen` vs `compose` 동작 원리:

**[코드 09.14]** andThen: 입력 -> f -> g -> 출력 (왼쪽에서 오른쪽)
```java
 1| // package: com.ecommerce.shared
 2| // andThen: 입력 -> f -> g -> 출력 (왼쪽에서 오른쪽)
 3| Function<Integer, Integer> addThenMultiply =
 4|   ((Function<Integer, Integer>) x -> x + 1).andThen(x -> x * 2);
 5| addThenMultiply.apply(5); // (5+1)*2 = 12
 6| 
 7| // compose: 입력 -> g -> f -> 출력 (오른쪽에서 왼쪽)
 8| Function<Integer, Integer> multiplyThenAdd =
 9|   ((Function<Integer, Integer>) x -> x + 1).compose(x -> x * 2);
10| multiplyThenAdd.apply(5); // (5*2)+1 = 11
```

### 틀리기/놓치기 쉬운 부분
- `UnaryOperator<String>`에서 바로 `andThen`을 사용하면 반환 타입이 `Function<String, String>`으로 변한다. 첫 함수를 `(Function<String, String>)`으로 캐스팅해야 체이닝이 원활하다.
- 합성할 함수들의 타입이 일치해야 한다. `Function<A, B>`와 `Function<B, C>`를 연결해야 `Function<A, C>`가 된다.
- 합성된 함수는 새 Function 인스턴스이다. 원본 함수들은 변경되지 않는다 (불변성).

### 꼭 기억할 것
함수 합성의 핵심 원칙: "작게 만들고, 조립한다." 각 함수가 하나의 책임만 갖고 타입만 맞으면 자유롭게 조합할 수 있다. 실무에서는 `andThen`을 사용하면 데이터 흐름 순서로 읽히므로 가독성이 좋다.
