# 10. FP Practical Patterns (실전 함수형 패턴)

> 불변 객체 중첩 업데이트(Lens), 순수 함수 캐싱(Memoization), 지연 평가(Lazy Evaluation), 효율적 스트림 합성(Transducer), 참조 투명성의 실전 활용을 다룬다.

---

## 1. Lens 패턴 (불변 객체의 중첩 업데이트)

### 핵심 개념
- **관련 키워드**: Lens, Optics, Immutable Update, Nested Record, Wither Pattern
- **통찰**: 불변 객체의 깊이 중첩된 필드를 수정할 때, Lens는 getter와 setter를 합성 가능한 단위로 추상화하여 보일러플레이트를 제거한다.
- **설명**:
  불변 객체(record)에서 중첩된 필드를 "수정"하려면 경로상의 모든 객체를 새로 생성해야 한다. 예를 들어 `order.customer.address.city`를 바꾸려면 새 Address, 새 Customer, 새 Order를 모두 만들어야 한다. 깊이가 깊을수록 코드가 폭발적으로 복잡해진다.

  Lens는 이 문제를 해결하는 함수형 패턴이다. 하나의 Lens는 "어떤 구조에서 특정 부분을 읽는 방법(getter)"과 "그 부분을 수정한 새 구조를 만드는 방법(setter)"을 캡슐화한다. Lens끼리 합성(compose)하면 깊은 중첩도 한 줄로 업데이트할 수 있다.

  Java에서는 record의 wither 패턴(`withXxx()` 메서드)을 조합하여 Lens를 구현할 수 있다. 핵심은 getter와 wither를 함수로 추상화하고, andThen으로 합성하는 것이다.

  Lens의 본질: "불변 세계에서의 setter" = getter + 새 값으로 구조를 재구성하는 함수.

```
+-------------------------------------------------------------------+
|                  Lens = 불변 구조의 초점                              |
+-------------------------------------------------------------------+
|                                                                     |
|   Order                                                             |
|     +-- Customer                                                    |
|           +-- Address                                               |
|                 +-- city: "Seoul"  <-- 여기를 "Busan"으로 바꾸고 싶음|
|                                                                     |
|   Without Lens:                                                     |
|   new Order(order.id(),                                             |
|     new Customer(customer.id(),                                     |
|       new Address(address.street(), "Busan")),  // 3단계 재생성!    |
|     order.items())                                                  |
|                                                                     |
|   With Lens:                                                        |
|   orderCityLens.set(order, "Busan")  // 한 줄!                     |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **가변 setter**: Lens는 원본을 변경하지 않는다. 새 객체를 반환한다.
- **리플렉션 기반 접근**: Lens는 컴파일 타임에 타입 안전하며, 리플렉션과 무관하다.
- **Builder 패턴**: Builder는 가변 상태를 축적하지만, Lens는 순수 함수적 업데이트이다.

### Before: Traditional OOP
```java
// [X] 중첩된 불변 객체 수정 시 모든 계층을 수동으로 재생성
public record Address(String street, String city, String zipCode) {}
public record Customer(Long id, String name, Address address) {}
public record Order(Long id, Customer customer, List<Item> items) {}

public class OrderService {
    public Order updateCustomerCity(Order order, String newCity) {
        // 3단계 중첩 재생성 -- 보일러플레이트 지옥
        Address oldAddress = order.customer().address();
        Address newAddress = new Address(oldAddress.street(), newCity, oldAddress.zipCode());
        Customer newCustomer = new Customer(
            order.customer().id(), order.customer().name(), newAddress);
        return new Order(order.id(), newCustomer, order.items());
    }

    // 필드 하나 더 깊어지면?
    public Order updateCustomerStreet(Order order, String newStreet) {
        Address oldAddress = order.customer().address();
        Address newAddress = new Address(newStreet, oldAddress.city(), oldAddress.zipCode());
        Customer newCustomer = new Customer(
            order.customer().id(), order.customer().name(), newAddress);
        return new Order(order.id(), newCustomer, order.items());
    }
    // 패턴은 같은데 코드를 계속 복붙...
}
```
- **의도 및 코드 설명**: 불변 record의 깊이 중첩된 필드를 수정하기 위해 경로상의 모든 객체를 수동으로 재생성한다.
- **뭐가 문제인가**:
  - 중첩 깊이 N이면 N개 객체를 수동 재생성해야 함
  - 필드가 하나 추가되면 관련 모든 업데이트 메서드를 수정해야 함
  - 동일한 패턴의 반복 (DRY 위반)
  - 실수로 잘못된 필드를 복사하는 버그 가능성

### After: Modern Approach
```java
// [O] Lens 패턴으로 중첩 업데이트를 합성 가능한 단위로 추상화
import java.util.function.Function;
import java.util.function.BiFunction;

public class LensPattern {

    // Lens 정의: getter + wither(setter 역할)
    record Lens<S, A>(
        Function<S, A> get,
        BiFunction<S, A, S> set
    ) {
        // Lens 합성: 외부 Lens와 내부 Lens를 결합
        <B> Lens<S, B> andThen(Lens<A, B> inner) {
            return new Lens<>(
                s -> inner.get().apply(this.get().apply(s)),
                (s, b) -> this.set().apply(s,
                    inner.set().apply(this.get().apply(s), b))
            );
        }

        // modify: 현재 값에 함수를 적용하여 업데이트
        S modify(S source, Function<A, A> f) {
            return set.apply(source, f.apply(get.apply(source)));
        }
    }

    // 각 계층별 Lens 정의
    record Address(String street, String city, String zipCode) {}
    record Customer(Long id, String name, Address address) {}
    record Order(Long id, Customer customer, List<String> items) {}

    // Order -> Customer Lens
    static final Lens<Order, Customer> orderCustomer = new Lens<>(
        Order::customer,
        (order, cust) -> new Order(order.id(), cust, order.items())
    );

    // Customer -> Address Lens
    static final Lens<Customer, Address> customerAddress = new Lens<>(
        Customer::address,
        (cust, addr) -> new Customer(cust.id(), cust.name(), addr)
    );

    // Address -> city Lens
    static final Lens<Address, String> addressCity = new Lens<>(
        Address::city,
        (addr, city) -> new Address(addr.street(), city, addr.zipCode())
    );

    // Lens 합성: Order -> Customer -> Address -> city
    static final Lens<Order, String> orderCity =
        orderCustomer.andThen(customerAddress).andThen(addressCity);

    public static void main(String[] args) {
        var order = new Order(1L,
            new Customer(100L, "Kim",
                new Address("Main St", "Seoul", "12345")),
            List.of("item1"));

        // 한 줄로 깊은 중첩 업데이트!
        Order updated = orderCity.set().apply(order, "Busan");
        System.out.println(updated.customer().address().city()); // "Busan"

        // modify: 현재 값을 기반으로 업데이트
        Order modified = orderCity.modify(order, String::toUpperCase);
        System.out.println(modified.customer().address().city()); // "SEOUL"
    }
}
```
- **의도 및 코드 설명**: Lens를 정의하여 각 계층의 getter/setter를 캡슐화한다. andThen으로 합성하면 깊은 중첩도 한 줄로 업데이트한다.
- **무엇이 좋아지나**:
  - 깊은 중첩도 합성된 Lens 한 줄로 업데이트
  - 개별 Lens는 재사용 가능 (다른 업데이트에도 활용)
  - modify로 현재 값 기반 업데이트도 깔끔
  - 타입 안전: 합성 시 타입이 맞지 않으면 컴파일 에러
  - 새 필드는 Lens 하나만 추가하면 됨

### 이해를 위한 부가 상세
Wither 패턴은 Lens의 간소화 버전으로, record에 직접 적용할 수 있다:
```java
public record Address(String street, String city, String zipCode) {
    public Address withCity(String newCity) {
        return new Address(street, newCity, zipCode);
    }
    public Address withStreet(String newStreet) {
        return new Address(newStreet, city, zipCode);
    }
}
```

### 틀리기/놓치기 쉬운 부분
- Lens 합성 시 타입 매칭을 주의한다. `Lens<A, B>`와 `Lens<B, C>`를 합성해야 `Lens<A, C>`가 된다.
- Java에는 Lens 라이브러리가 표준이 아니다. 간단한 경우 wither 메서드로 충분하고, 복잡한 경우 직접 Lens를 정의하거나 서드파티를 활용한다.
- Lens는 불변 객체에서만 의미가 있다. 가변 객체는 그냥 setter를 쓰면 된다.

### 꼭 기억할 것
Lens의 핵심은 "불변 세계에서 합성 가능한 업데이트"이다. 중첩 깊이와 무관하게 개별 Lens를 합성하여 어떤 깊이의 필드든 한 줄로 업데이트할 수 있다. 간단한 경우 wither 패턴, 복잡한 경우 Lens 합성을 사용한다.

---

## 2. Memoization (순수 함수 캐싱)

### 핵심 개념
- **관련 키워드**: Memoization, Pure Function, Cache, Referential Transparency, computeIfAbsent
- **통찰**: 순수 함수는 같은 입력에 항상 같은 출력을 보장하므로, 결과를 안전하게 캐싱하여 중복 계산을 제거할 수 있다.
- **설명**:
  순수 함수의 결정적 특성(동일 입력 = 동일 출력)은 메모이제이션의 이론적 기반이다. 한 번 계산한 결과를 저장해두면, 같은 입력이 다시 들어올 때 저장된 결과를 즉시 반환할 수 있다. 이는 참조 투명성이 보장하는 안전한 최적화이다.

  메모이제이션의 핵심 조건: 대상 함수가 반드시 순수해야 한다. 부수효과가 있는 함수를 캐싱하면 부수효과가 누락되어 버그가 발생한다.

  Java에서는 `ConcurrentHashMap.computeIfAbsent()`를 활용하여 스레드 안전한 메모이제이션을 구현할 수 있다. 함수를 인자로 받아 메모이제이션된 버전을 반환하는 고차 함수로 일반화하면, 어떤 순수 함수에도 적용 가능하다.

  참조 투명성 관점에서 메모이제이션된 함수는 내부에 캐시(가변 상태)를 갖지만, 외부에서 관찰 가능한 동작은 순수하다. 이를 "관찰 가능한 순수성(observational purity)"이라 한다.

```
+-------------------------------------------------------------------+
|                  Memoization = 순수 함수 + 캐시                      |
+-------------------------------------------------------------------+
|                                                                     |
|   순수 함수: f(5) = 25   (항상!)                                    |
|                                                                     |
|   첫 호출:  f(5) --> [계산: 5*5] --> 25  --> 캐시에 저장             |
|   두번째:   f(5) --> [캐시 히트!] --> 25  --> 재계산 없음!           |
|                                                                     |
|   전제 조건:                                                        |
|   - 함수가 순수해야 함 (동일 입력 = 동일 출력)                      |
|   - 부수효과 있는 함수를 캐싱하면 버그 발생!                        |
|                                                                     |
|   결과:                                                             |
|   - 시간 복잡도: O(계산시간) --> O(1) (캐시 히트 시)                |
|   - 공간 복잡도: 캐시 크기만큼 추가 메모리 사용                     |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **일반 캐싱과 동일**: 일반 캐싱은 TTL, 무효화 전략이 필요하지만, 순수 함수 메모이제이션은 영구 유효하다(입력이 변하지 않는 한).
- **불순 함수에도 적용 가능**: 아니다. 부수효과가 있는 함수를 캐싱하면 부수효과가 한 번만 실행된다.
- **항상 사용해야 하는 최적화**: 캐시 메모리 비용 vs 재계산 비용을 고려해야 한다. 계산이 가볍거나 입력 공간이 무한하면 비효율적.

### Before: Traditional OOP
```java
// [X] 비싼 계산을 매번 반복 수행
public class PricingService {
    // 복잡한 가격 계산 (매번 재계산!)
    public BigDecimal calculateDiscountedPrice(String productId, String couponCode) {
        // DB 조회 + 복잡한 규칙 적용 + 외부 API 호출...
        Product product = productRepository.findById(productId); // 매번 DB!
        Coupon coupon = couponService.validate(couponCode);      // 매번 API!
        return applyComplexRules(product, coupon);               // 매번 계산!
    }
}

// 순수하지 않아서 캐싱이 위험한 구조
// DB 데이터가 변하면 캐시가 stale 해짐
```
- **의도 및 코드 설명**: 상품 가격을 계산할 때마다 DB와 외부 API를 호출하고 복잡한 규칙을 적용한다.
- **뭐가 문제인가**:
  - 동일한 입력에 대해 매번 비싼 계산을 반복
  - 순수하지 않은 함수(DB, API 의존)라 단순 캐싱이 위험
  - 캐시 무효화 전략이 복잡해짐
  - 테스트 시 외부 의존성을 모킹해야 함

### After: Modern Approach
```java
// [O] 순수 함수 분리 + 메모이제이션으로 안전한 캐싱
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class PricingService {

    // 메모이제이션 고차 함수: 어떤 순수 함수든 캐싱 가능
    public static <T, R> Function<T, R> memoize(Function<T, R> fn) {
        ConcurrentHashMap<T, R> cache = new ConcurrentHashMap<>();
        return input -> cache.computeIfAbsent(input, fn);
    }

    // 순수 함수: 가격 규칙 적용 (외부 의존성 없음!)
    public static BigDecimal applyDiscountRules(DiscountInput input) {
        BigDecimal base = input.basePrice();
        BigDecimal rate = input.discountRate();
        int quantity = input.quantity();

        // 복잡한 규칙이지만 순수! (입력만으로 결과 결정)
        BigDecimal discounted = base.multiply(BigDecimal.ONE.subtract(rate));
        if (quantity >= 10) {
            discounted = discounted.multiply(new BigDecimal("0.95")); // 대량 할인
        }
        return discounted.setScale(0, java.math.RoundingMode.HALF_UP);
    }

    // 메모이제이션 적용
    private static final Function<DiscountInput, BigDecimal> memoizedDiscount =
        memoize(PricingService::applyDiscountRules);

    // 불순한 부분은 바깥에서 처리 (Functional Core / Imperative Shell)
    public BigDecimal calculatePrice(String productId, String couponCode) {
        // Imperative Shell: 외부 데이터 조회 (불순)
        Product product = productRepository.findById(productId);
        Coupon coupon = couponService.validate(couponCode);

        // Functional Core: 순수 계산 (메모이제이션 가능!)
        var input = new DiscountInput(product.basePrice(), coupon.rate(), 1);
        return memoizedDiscount.apply(input);
    }

    record DiscountInput(BigDecimal basePrice, BigDecimal discountRate, int quantity) {}
}
```
- **의도 및 코드 설명**: 순수한 계산 로직을 분리하고 메모이제이션을 적용한다. 불순한 부분(DB, API)은 바깥 shell에서 처리한다.
- **무엇이 좋아지나**:
  - 순수 함수에 메모이제이션을 적용하여 안전하게 중복 계산 제거
  - 캐시 무효화가 불필요 (순수 함수이므로 결과가 변할 일 없음)
  - 입력이 같으면 O(1)로 결과 반환
  - memoize 고차 함수를 어떤 순수 함수에도 재사용 가능
  - 테스트: 순수 함수와 불순 shell을 독립적으로 테스트

### 이해를 위한 부가 상세
메모이제이션의 다인자 함수 처리:
```java
// 다인자 함수는 record로 묶어서 단일 인자로 변환
record FibKey(int n) {} // 또는 그냥 Integer 사용

// 피보나치 메모이제이션
Function<Integer, Long> fib = memoize(new Function<>() {
    Function<Integer, Long> self = this;
    @Override
    public Long apply(Integer n) {
        if (n <= 1) return (long) n;
        return memoize(self).apply(n - 1) + memoize(self).apply(n - 2);
    }
});
```

### 틀리기/놓치기 쉬운 부분
- `ConcurrentHashMap.computeIfAbsent`에서 재귀 호출을 하면 데드락이 발생할 수 있다. 재귀 메모이제이션은 별도 구현이 필요하다.
- 캐시 크기가 무한히 커질 수 있다. 입력 공간이 크면 LRU 캐시(`LinkedHashMap` 또는 Caffeine) 사용을 고려한다.
- 메모이제이션 대상 함수의 인자에 equals/hashCode가 올바르게 구현되어 있어야 한다. record는 자동으로 제공된다.

### 꼭 기억할 것
메모이제이션은 "순수 함수 + 참조 투명성"이 보장하는 안전한 최적화이다. 순수한 계산을 분리(Functional Core)하고 메모이제이션을 적용하면, 캐시 무효화 걱정 없이 성능을 개선할 수 있다.

---

## 3. Lazy Evaluation (지연 평가)

### 핵심 개념
- **관련 키워드**: Lazy Evaluation, Stream, Supplier, Short-Circuit, Infinite Sequence
- **통찰**: 지연 평가는 "진짜 필요할 때까지 계산을 미루는" 전략으로, 무한 구조 표현과 불필요한 계산 회피를 가능하게 한다.
- **설명**:
  즉시 평가(Eager)는 정의 즉시 모든 값을 계산한다. 지연 평가(Lazy)는 실제로 값이 요청될 때까지 계산을 미룬다. DVD(미리 다 구매)와 Netflix(볼 때만 스트리밍)의 차이와 같다.

  Java의 Stream은 대표적인 지연 평가 구조이다. `filter`, `map` 등의 중간 연산은 실행되지 않고 "레시피"만 정의한다. `toList()`, `findFirst()` 같은 터미널 연산이 호출되어야 비로소 계산이 시작된다.

  지연 평가의 핵심 특성은 요소별 개별 처리이다. 전체를 filter한 후 전체를 map하는 것이 아니라, 한 요소가 filter -> map을 모두 거친 후 다음 요소가 처리된다. 이 때문에 `findFirst()`가 첫 번째 매칭을 찾는 즉시 나머지를 무시할 수 있다(short-circuit).

  무한 Stream도 가능한 이유: `Stream.iterate(1, n -> n+1)`은 "1부터 시작해서 1씩 더하는 레시피"만 정의한다. `limit(10)`과 결합하면 필요한 10개만 계산하고 멈춘다.

```
+-------------------------------------------------------------------+
|                  지연 평가 = 필요할 때만 계산                         |
+-------------------------------------------------------------------+
|                                                                     |
|   Eager (즉시):                                                     |
|   [1,2,3,4,5].filter(even) --> [2,4] --> .map(*10) --> [20,40]     |
|   전부 filter 한 후, 전부 map                                       |
|                                                                     |
|   Lazy (지연):                                                      |
|   Stream: 1 -> F(홀) -> skip                                       |
|           2 -> F(짝) -> M(*10) -> 20  [첫 결과!]                   |
|           3 -> F(홀) -> skip                                       |
|           4 -> F(짝) -> M(*10) -> 40  [두번째!] --> limit 도달, 종료|
|   요소별 개별 처리, 필요한 만큼만!                                   |
|                                                                     |
|   장점:                                                             |
|   - 무한 스트림 표현 가능                                           |
|   - 불필요한 계산 회피 (short-circuit)                              |
|   - 메모리 효율 (중간 컬렉션 없음)                                  |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **단순 반복**: for 루프도 하나씩 처리하지만, 지연 평가는 "요청이 있을 때만" 처리한다는 pull 모델이다.
- **비동기 처리**: 지연 평가는 "시점의 지연"이지 "스레드 분리"가 아니다. 같은 스레드에서 실행된다.
- **Collection의 stream()만 지연**: `Supplier<T>`, `Stream.iterate()`, `Stream.generate()` 등 다양한 지연 구조가 있다.

### Before: Traditional OOP
```java
// [X] 모든 요소를 즉시 계산하여 메모리 낭비와 불필요한 작업 발생
public class LogAnalyzer {
    public String findFirstError(List<String> allLines) {
        // 모든 라인을 먼저 파싱 (10만 줄!)
        List<LogEntry> parsed = new ArrayList<>();
        for (String line : allLines) {
            parsed.add(parseLogEntry(line)); // 10만 개 전부 파싱!
        }

        // 모든 에러를 필터링
        List<LogEntry> errors = new ArrayList<>();
        for (LogEntry entry : parsed) {
            if (entry.level().equals("ERROR")) {
                errors.add(entry); // 전부 필터링!
            }
        }

        // 첫 번째만 필요한데...
        return errors.isEmpty() ? null : errors.getFirst().message();
    }
    // 첫 번째 에러가 10번째 줄에 있어도 10만 줄 전부 처리!
}
```
- **의도 및 코드 설명**: 로그에서 첫 번째 에러 메시지를 찾는다. 모든 줄을 파싱하고 모든 에러를 필터링한 후 첫 번째를 선택한다.
- **뭐가 문제인가**:
  - 첫 번째 에러만 필요한데 전체 10만 줄을 파싱
  - 중간 리스트(parsed, errors)에 불필요한 메모리 할당
  - 첫 에러가 초반에 있어도 끝까지 처리해야 함
  - 무한 데이터 소스(실시간 로그 스트림)에는 적용 불가

### After: Modern Approach
```java
// [O] Stream의 지연 평가로 필요한 만큼만 계산
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public class LogAnalyzer {

    public Optional<String> findFirstError(Path logFile) {
        try (Stream<String> lines = Files.lines(logFile)) { // 지연 읽기!
            return lines                          // 한 줄씩만 메모리에
                .map(this::parseLogEntry)         // 지연: 아직 안 파싱
                .filter(entry -> "ERROR".equals(entry.level()))  // 지연: 아직 안 필터링
                .map(LogEntry::message)           // 지연: 아직 안 매핑
                .findFirst();                     // 터미널: 여기서 실행 시작!
            // 첫 에러 찾는 순간 나머지 줄은 읽지도 않음!
        }
    }

    // 무한 스트림에서도 안전하게 동작
    public List<Integer> firstNPrimes(int n) {
        return Stream.iterate(2, x -> x + 1)     // 무한!
            .filter(this::isPrime)                // 지연: 필요할 때만 판별
            .limit(n)                             // n개 찾으면 종료
            .toList();                            // 여기서 실행 시작
    }

    // Supplier를 활용한 지연 초기화
    private final Supplier<ExpensiveResource> lazyResource =
        new Supplier<>() {
            private ExpensiveResource cached;
            @Override
            public synchronized ExpensiveResource get() {
                if (cached == null) {
                    cached = new ExpensiveResource(); // 처음 접근할 때만 생성
                }
                return cached;
            }
        };

    record LogEntry(String level, String message, String timestamp) {}
    record ExpensiveResource() {}

    private LogEntry parseLogEntry(String line) {
        // 파싱 로직
        return new LogEntry("INFO", line, "");
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        return java.util.stream.IntStream.rangeClosed(2, (int) Math.sqrt(n))
            .noneMatch(i -> n % i == 0);
    }
}
```
- **의도 및 코드 설명**: Stream의 지연 평가로 첫 에러를 찾는 즉시 중단한다. 무한 스트림에서 소수 N개를 안전하게 추출한다.
- **무엇이 좋아지나**:
  - 첫 에러가 10번째 줄에 있으면 10줄만 파싱 (10만 줄 절약)
  - 중간 컬렉션 없음: 메모리 효율 극대화
  - 무한 데이터 소스도 안전하게 처리 가능
  - short-circuit: findFirst, anyMatch 등으로 조기 종료
  - 10GB 파일도 한 줄씩만 메모리에 올려 처리 가능

### 이해를 위한 부가 상세
Stream의 실행 순서 (요소별 개별 처리):
```java
Stream.of(1, 2, 3, 4, 5)
    .filter(n -> { System.out.print("F"+n+" "); return n%2==0; })
    .map(n -> { System.out.print("M"+n+" "); return n*10; })
    .limit(1)
    .toList();
// 출력: F1 F2 M2
// 1은 filter 통과 못함, 2는 통과해서 map 실행, limit(1) 도달 -> 종료!
// 3, 4, 5는 아예 처리하지 않음
```

### 틀리기/놓치기 쉬운 부분
- Stream은 한 번만 소비 가능하다. 재사용하려면 매번 새 Stream을 생성해야 한다.
- `peek()`는 디버깅 용도이며, 부수효과 의존 로직을 넣으면 안 된다 (지연 평가 때문에 실행이 보장되지 않음).
- `sorted()`는 전체 요소를 소비해야 하므로 지연의 이점을 상당 부분 잃는다. 무한 스트림에 sorted를 적용하면 영원히 안 끝난다.
- `parallel()` + `findFirst()`는 "첫 번째 요소"가 아닌 "아무 요소"를 반환할 수 있다. 순서 보장이 필요하면 `findFirst()`가 아닌 설계를 재고한다.

### 꼭 기억할 것
지연 평가의 3대 이점: (1) 무한 구조 표현, (2) 불필요한 계산 회피(short-circuit), (3) 메모리 효율(중간 컬렉션 없음). 중간 연산은 "레시피"이고 터미널 연산이 실행을 트리거한다. 이 구분이 Stream 활용의 핵심이다.

---

## 4. Transducer 패턴 (효율적 스트림 합성)

### 핵심 개념
- **관련 키워드**: Transducer, Stream Pipeline, Intermediate Operations, Collector, Stateless Transformation
- **통찰**: 변환 로직을 데이터 소스와 분리하여 재사용 가능한 "변환 파이프라인"을 합성하면, 단일 순회로 여러 변환을 효율적으로 수행할 수 있다.
- **설명**:
  Transducer는 "변환기의 합성"이라는 개념이다. 일반적으로 map, filter를 각각 적용하면 중간 컬렉션이 생기거나 여러 번 순회해야 한다. Transducer는 이 변환들을 하나로 합성하여 단일 순회(single pass)로 처리한다.

  Java의 Stream은 이미 내부적으로 Transducer와 유사한 최적화를 수행한다. 중간 연산들이 하나의 파이프라인으로 합쳐지고, 터미널 연산 시 한 번만 순회한다. 하지만 Stream 파이프라인 자체를 재사용하거나 합성하려면 별도의 추상화가 필요하다.

  Java에서 Transducer 패턴은 `Function<Stream<T>, Stream<R>>` 형태의 변환 함수를 합성하는 것으로 구현할 수 있다. 이 변환 함수들을 andThen으로 연결하면 재사용 가능한 파이프라인이 된다.

  핵심 가치: (1) 변환 로직의 재사용, (2) 데이터 소스와 변환의 분리, (3) 합성을 통한 새 변환 파생.

```
+-------------------------------------------------------------------+
|                  Transducer = 합성 가능한 변환기                      |
+-------------------------------------------------------------------+
|                                                                     |
|   일반 접근:                                                        |
|   data -> [filter] -> tempList1 -> [map] -> tempList2 -> result    |
|            ^^^^^^ 중간 컬렉션 생성, 2회 순회                        |
|                                                                     |
|   Transducer:                                                       |
|   data -> [filter+map 합성] -> result                               |
|            ^^^^^^ 단일 순회, 중간 컬렉션 없음                       |
|                                                                     |
|   합성 예시:                                                        |
|   xform = filterEven.andThen(mapDouble).andThen(takeN)              |
|   xform.apply(stream1)  // 재사용!                                  |
|   xform.apply(stream2)  // 같은 변환을 다른 소스에!                  |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **Stream API 자체**: Stream은 Transducer와 유사하지만, 파이프라인 자체를 값으로 다루거나 합성하기 어렵다.
- **Collector**: Collector는 최종 수집 전략이고, Transducer는 중간 변환 합성이다.
- **병렬 처리**: Transducer는 합성에 관한 패턴이지, 병렬화와 직접적 관련은 없다.

### Before: Traditional OOP
```java
// [X] 변환 로직을 매번 인라인으로 작성하여 재사용 불가
public class ReportService {
    // 보고서 A: 활성 사용자의 이메일 목록
    public List<String> getActiveUserEmails(List<User> users) {
        return users.stream()
            .filter(User::isActive)
            .filter(u -> u.age() >= 18)
            .map(User::email)
            .map(String::toLowerCase)
            .toList();
    }

    // 보고서 B: 같은 필터링 + 다른 결과 형태
    public long countActiveUsers(List<User> users) {
        return users.stream()
            .filter(User::isActive)        // 동일한 필터!
            .filter(u -> u.age() >= 18)    // 동일한 필터!
            .count();
    }

    // 보고서 C: 같은 필터링 + 또 다른 결과
    public List<String> getActiveUserNames(List<User> users) {
        return users.stream()
            .filter(User::isActive)        // 또 동일한 필터!
            .filter(u -> u.age() >= 18)    // 또 동일한 필터!
            .map(User::name)
            .toList();
    }
    // 필터 조건이 바뀌면 3곳 모두 수정해야 함!
}
```
- **의도 및 코드 설명**: 동일한 필터링 로직을 여러 보고서에 반복 사용한다. 필터 조건 변경 시 모든 곳을 수정해야 한다.
- **뭐가 문제인가**:
  - 동일한 filter 체인이 여러 곳에 복붙됨 (DRY 위반)
  - 필터 조건 변경 시 모든 사용처를 찾아 수정해야 함
  - 변환 로직을 조합하거나 교체하기 어려움
  - 변환 파이프라인 자체를 테스트할 수 없음

### After: Modern Approach
```java
// [O] 변환 파이프라인을 값으로 추상화하여 재사용/합성
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.function.Predicate;

public class ReportService {
    record User(String name, String email, int age, boolean active) {
        public boolean isActive() { return active; }
    }

    // Transducer: Stream 변환을 값으로 추상화
    @FunctionalInterface
    interface StreamTransformer<T, R> extends Function<Stream<T>, Stream<R>> {
        default <V> StreamTransformer<T, V> andThen(StreamTransformer<R, V> after) {
            return stream -> after.apply(this.apply(stream));
        }
    }

    // 기본 변환기 팩토리
    static <T> StreamTransformer<T, T> filtering(Predicate<T> pred) {
        return stream -> stream.filter(pred);
    }

    static <T, R> StreamTransformer<T, R> mapping(Function<T, R> fn) {
        return stream -> stream.map(fn);
    }

    static <T> StreamTransformer<T, T> limiting(long n) {
        return stream -> stream.limit(n);
    }

    // 재사용 가능한 변환 파이프라인 합성
    static final StreamTransformer<User, User> activeAdults =
        filtering(User::isActive)
            .andThen(filtering(u -> u.age() >= 18));

    static final StreamTransformer<User, String> activeAdultEmails =
        activeAdults
            .andThen(mapping(User::email))
            .andThen(mapping(String::toLowerCase));

    static final StreamTransformer<User, String> activeAdultNames =
        activeAdults
            .andThen(mapping(User::name));

    // 모든 보고서에서 재사용
    public List<String> getActiveUserEmails(List<User> users) {
        return activeAdultEmails.apply(users.stream()).toList();
    }

    public long countActiveUsers(List<User> users) {
        return activeAdults.apply(users.stream()).count();
    }

    public List<String> getActiveUserNames(List<User> users) {
        return activeAdultNames.apply(users.stream()).toList();
    }

    // 새 변환이 필요하면? 기존 변환을 합성!
    static final StreamTransformer<User, String> topActiveAdultEmails =
        activeAdultEmails.andThen(limiting(10));
}
```
- **의도 및 코드 설명**: Stream 변환을 `StreamTransformer`로 추상화하고, andThen으로 합성한다. 공통 변환은 한 번 정의하고 여러 곳에서 재사용한다.
- **무엇이 좋아지나**:
  - 공통 필터/변환을 한 번 정의, 여러 보고서에서 재사용
  - 필터 조건 변경 시 한 곳만 수정하면 전체 반영
  - 변환 파이프라인을 값으로 다뤄 합성, 교체, 테스트 가능
  - 새 변환은 기존 변환을 조합하여 파생 (조합적 확장)

### 이해를 위한 부가 상세
Transducer의 Collector 통합:
```java
// 변환 파이프라인 + 다양한 수집 전략 결합
var transformer = activeAdults.andThen(mapping(User::email));

// 같은 변환, 다른 수집 전략
List<String> list = transformer.apply(users.stream()).toList();
Set<String> set = transformer.apply(users.stream()).collect(Collectors.toSet());
String joined = transformer.apply(users.stream()).collect(Collectors.joining(", "));
```

### 틀리기/놓치기 쉬운 부분
- Stream은 한 번만 소비 가능하므로, `StreamTransformer`에 Stream을 전달할 때 매번 새 Stream을 생성해야 한다.
- 상태 있는 중간 연산(`sorted`, `distinct`)은 합성 시 주의가 필요하다. 순서에 따라 결과가 달라질 수 있다.
- 변환 파이프라인이 너무 길어지면 디버깅이 어려워진다. 적절히 이름 붙은 중간 변환으로 분리한다.

### 꼭 기억할 것
Transducer 패턴의 핵심: "변환 로직을 데이터 소스에서 분리하고, 합성하여 재사용한다." Java에서는 `Function<Stream<T>, Stream<R>>`을 andThen으로 합성하여 구현한다. 공통 변환을 한 곳에 정의하고 합성으로 확장하면 DRY 원칙을 지키면서 유연한 파이프라인을 구축할 수 있다.

---

## 5. Referential Transparency in Practice (실전 참조 투명성)

### 핵심 개념
- **관련 키워드**: Referential Transparency, Substitution Model, Pure/Impure Boundary, Functional Core / Imperative Shell
- **통찰**: 참조 투명한 표현식은 언제 어디서 실행해도 같은 결과를 보장하므로, 안전한 리팩토링, 메모이제이션, 병렬화, 테스트 용이성을 모두 얻을 수 있다.
- **설명**:
  참조 투명성(Referential Transparency)이란 표현식을 그 결과값으로 대체해도 프로그램의 의미가 변하지 않는 성질이다. `add(2, 3)`이 항상 5를 반환하면, 코드 어디에서든 `add(2, 3)`을 `5`로 바꿔 써도 프로그램 동작이 달라지지 않는다.

  실전에서 참조 투명성은 "Functional Core / Imperative Shell" 아키텍처로 적용된다. 비즈니스 로직(Core)은 순수 함수로 작성하여 참조 투명하게 만들고, 부수효과(DB, API, I/O)는 가장자리(Shell)에서 처리한다.

  참조 투명성이 주는 실전적 이점 네 가지: (1) 로컬 추론: 함수만 보고 동작 예측 가능, 전역 상태 추적 불필요. (2) 안전한 리팩토링: 표현식을 값으로 치환하거나, 값을 표현식으로 인라인해도 안전. (3) 메모이제이션: 같은 입력의 결과를 캐시해도 안전. (4) 병렬화: 순서 무관하게 실행해도 결과 동일.

  관찰 가능한 순수성: 메모이제이션된 함수는 내부에 캐시(가변 상태)를 갖지만, 외부 관찰자에게는 순수하게 동작한다. 이를 "관찰 가능한 순수성"이라 하며, 실용적으로 참조 투명하다고 인정한다.

```
+-------------------------------------------------------------------+
|                  참조 투명성 = 대입 가능한 표현식                     |
+-------------------------------------------------------------------+
|                                                                     |
|   참조 투명:                                                        |
|   add(2, 3) --> 5    어디서든 add(2,3)을 5로 바꿔 써도 OK           |
|   result = add(2,3) + add(2,3)  =  5 + 5  =  10                   |
|                                                                     |
|   참조 불투명:                                                      |
|   random()  --> ???   매번 다른 값, 대체 불가!                      |
|   now()     --> ???   시점마다 다름, 대체 불가!                      |
|                                                                     |
|   Functional Core / Imperative Shell:                               |
|   +---------------------------------------------+                  |
|   |  Imperative Shell (부수효과: DB, API, I/O)   |                  |
|   |  +---------------------------------------+  |                  |
|   |  |  Functional Core (순수, 참조 투명)     |  |                  |
|   |  |  - 도메인 로직                         |  |                  |
|   |  |  - 검증, 계산, 변환                    |  |                  |
|   |  +---------------------------------------+  |                  |
|   +---------------------------------------------+                  |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **모든 코드를 순수하게 만드는 것**: 현실적으로 불가능하다. I/O는 반드시 필요하며, 핵심은 순수/불순 경계를 명확히 하는 것이다.
- **성능 최적화 기법**: 참조 투명성은 최적화를 "가능하게" 하지만, 그 자체가 최적화는 아니다.
- **테스트만을 위한 설계**: 테스트 용이성은 결과이지 목적이 아니다. 본질은 "예측 가능하고 합성 가능한 코드"이다.

### Before: Traditional OOP
```java
// [X] 비즈니스 로직과 부수효과가 혼합되어 참조 투명성 없음
public class OrderService {
    private final OrderRepository repository;
    private final PaymentGateway gateway;
    private final EmailService emailService;

    public void processOrder(OrderRequest request) {
        // 검증 + DB 조회 + 결제 + 이메일이 한 메서드에 뒤섞임
        if (request.items().isEmpty()) {
            throw new ValidationException("항목이 비어있습니다");
        }

        // 부수효과: DB에서 재고 확인
        for (var item : request.items()) {
            int stock = repository.getStock(item.productId()); // DB!
            if (stock < item.quantity()) {
                throw new InsufficientStockException(item.productId());
            }
        }

        // 비즈니스 로직: 가격 계산
        BigDecimal total = calculateTotal(request); // 이건 순수한데...

        // 부수효과: 결제
        gateway.charge(request.customerId(), total); // API!

        // 부수효과: 재고 감소
        for (var item : request.items()) {
            repository.decreaseStock(item.productId(), item.quantity()); // DB!
        }

        // 부수효과: 이메일 발송
        emailService.sendConfirmation(request.customerId(), total); // I/O!
    }
    // 테스트하려면 repository, gateway, emailService 전부 모킹해야 함
    // 비즈니스 로직만 단독 테스트 불가
}
```
- **의도 및 코드 설명**: 주문 처리를 하나의 메서드에서 수행한다. 검증, DB, 결제, 이메일이 모두 섞여있다.
- **뭐가 문제인가**:
  - 비즈니스 로직(검증, 계산)과 부수효과(DB, API, I/O)가 혼재
  - 메서드 호출 결과가 외부 상태에 의존 (참조 불투명)
  - 비즈니스 로직만 독립 테스트 불가능 (모킹 필수)
  - 리팩토링 시 부수효과 순서가 바뀌면 버그 발생
  - 메모이제이션, 병렬화 등 최적화 불가능

### After: Modern Approach
```java
// [O] Functional Core / Imperative Shell로 참조 투명성 확보
import java.util.function.Function;

public class OrderProcessing {

    // === Functional Core: 순수 함수, 참조 투명 ===

    // 검증 (순수: 입력만으로 결과 결정)
    static Result<ValidatedOrder, OrderError> validate(OrderRequest request) {
        if (request.items().isEmpty())
            return Result.failure(new OrderError.ValidationFailed("항목이 비어있습니다"));
        return Result.success(new ValidatedOrder(request));
    }

    // 재고 확인 (순수: 재고 정보를 "인자로" 받음)
    static Result<CheckedOrder, OrderError> checkStock(
            ValidatedOrder order, Map<String, Integer> stockLevels) {
        for (var item : order.items()) {
            int available = stockLevels.getOrDefault(item.productId(), 0);
            if (available < item.quantity())
                return Result.failure(new OrderError.InsufficientStock(item.productId()));
        }
        return Result.success(new CheckedOrder(order));
    }

    // 가격 계산 (순수: 동일 입력 = 동일 출력, 메모이제이션 가능!)
    static OrderTotal calculateTotal(CheckedOrder order) {
        BigDecimal subtotal = order.items().stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = subtotal.multiply(new BigDecimal("0.1"));
        return new OrderTotal(subtotal, tax, subtotal.add(tax));
    }

    // 영수증 생성 (순수)
    static Receipt createReceipt(CheckedOrder order, OrderTotal total, String txId) {
        return new Receipt(order.orderId(), total.grandTotal(), txId);
    }

    // === Imperative Shell: 부수효과 처리 ===

    public class OrderService {
        private final OrderRepository repository;
        private final PaymentGateway gateway;
        private final EmailService emailService;

        public Result<Receipt, OrderError> processOrder(OrderRequest request) {
            // 1. Functional Core: 순수 검증
            return validate(request)
                .flatMap(validated -> {
                    // 2. Shell: DB에서 재고 조회 (부수효과)
                    Map<String, Integer> stocks = repository.getStockLevels(
                        validated.productIds());

                    // 3. Core: 순수 재고 확인
                    return checkStock(validated, stocks);
                })
                .map(checked -> {
                    // 4. Core: 순수 가격 계산
                    OrderTotal total = calculateTotal(checked);
                    return new Pair<>(checked, total);
                })
                .flatMap(pair -> {
                    // 5. Shell: 결제 (부수효과)
                    var payResult = gateway.charge(
                        pair.first().customerId(), pair.second().grandTotal());
                    if (!payResult.isSuccess())
                        return Result.failure(new OrderError.PaymentFailed(payResult.reason()));

                    // 6. Shell: 재고 감소 (부수효과)
                    repository.decreaseStocks(pair.first().items());

                    // 7. Core: 순수 영수증 생성
                    Receipt receipt = createReceipt(
                        pair.first(), pair.second(), payResult.transactionId());

                    // 8. Shell: 이메일 발송 (부수효과)
                    emailService.sendConfirmation(pair.first().customerId(), receipt);

                    return Result.success(receipt);
                });
        }
    }

    // 순수 함수 테스트: 모킹 불필요!
    // @Test
    // void calculateTotal_정상_계산() {
    //     var order = new CheckedOrder(...);
    //     var total = calculateTotal(order);  // 순수! 입력만 주면 됨
    //     assertEquals(expected, total.grandTotal());
    // }

    // sealed interface + records (생략)
    sealed interface Result<S, F> permits Result.Success, Result.Failure {
        record Success<S, F>(S value) implements Result<S, F> {}
        record Failure<S, F>(F error) implements Result<S, F> {}

        default <R> Result<R, F> map(Function<S, R> fn) {
            return switch (this) {
                case Success<S, F> s -> new Success<>(fn.apply(s.value()));
                case Failure<S, F> f -> new Failure<>(f.error());
            };
        }
        default <R> Result<R, F> flatMap(Function<S, Result<R, F>> fn) {
            return switch (this) {
                case Success<S, F> s -> fn.apply(s.value());
                case Failure<S, F> f -> new Failure<>(f.error());
            };
        }
        static <S, F> Result<S, F> success(S value) { return new Success<>(value); }
        static <S, F> Result<S, F> failure(F error) { return new Failure<>(error); }
    }

    sealed interface OrderError permits OrderError.ValidationFailed,
            OrderError.InsufficientStock, OrderError.PaymentFailed {
        record ValidationFailed(String message) implements OrderError {}
        record InsufficientStock(String productId) implements OrderError {}
        record PaymentFailed(String reason) implements OrderError {}
    }

    record ValidatedOrder(OrderRequest request) {
        List<Item> items() { return request.items(); }
        List<String> productIds() { return items().stream().map(Item::productId).toList(); }
    }
    record CheckedOrder(ValidatedOrder validated) {
        String orderId() { return "ORD-" + System.nanoTime(); }
        String customerId() { return "CUST-1"; }
        List<Item> items() { return validated.items(); }
    }
    record OrderTotal(BigDecimal subtotal, BigDecimal tax, BigDecimal grandTotal) {}
    record Receipt(String orderId, BigDecimal total, String transactionId) {}
    record OrderRequest(List<Item> items) {}
    record Item(String productId, BigDecimal price, int quantity) {}
    record Pair<A, B>(A first, B second) {}
}
```
- **의도 및 코드 설명**: 비즈니스 로직(validate, checkStock, calculateTotal, createReceipt)을 순수 함수로 분리하고, 부수효과(DB, 결제, 이메일)는 Shell에서 처리한다.
- **무엇이 좋아지나**:
  - 비즈니스 로직이 순수하여 참조 투명: 입력만 주면 테스트 가능 (모킹 불필요)
  - 순수 함수끼리 자유롭게 합성, 리팩토링, 메모이제이션 가능
  - 부수효과가 Shell에 격리되어 영향 범위 명확
  - 동일 입력에 동일 결과 보장: 디버깅, 재현, 회귀 테스트 용이
  - calculateTotal은 메모이제이션 적용 가능 (순수하므로 안전)

### 이해를 위한 부가 상세
참조 투명성 판별 체크리스트:
```
Q1: 이 함수가 외부 상태를 읽거나 변경하는가?
    YES --> 참조 불투명
    NO  --> Q2

Q2: 같은 인자로 호출하면 항상 같은 결과인가?
    NO  --> 참조 불투명
    YES --> Q3

Q3: 부수효과(I/O, 로깅, 예외)가 있는가?
    YES --> 참조 불투명
    NO  --> 참조 투명!
```

### 틀리기/놓치기 쉬운 부분
- "순수해 보이지만 불순한" 함수 주의: `LocalDate.now()`, `UUID.randomUUID()`, `System.currentTimeMillis()` 등은 부수효과이다.
- 참조 투명한 함수 내부에서 예외를 던지면 참조 투명성이 깨진다. 예외 대신 Result를 사용한다.
- 불변 컬렉션을 사용해도 그 안에 가변 객체가 있으면 순수성이 깨진다. 깊은 불변성(deep immutability)을 확보해야 한다.
- 로깅도 부수효과이다. 순수 함수 내에서 로깅하면 엄밀히 참조 불투명하지만, 실용적으로는 관찰 가능한 순수성으로 인정하기도 한다.

### 꼭 기억할 것
참조 투명성의 실전 적용법은 "Functional Core / Imperative Shell"이다. 비즈니스 로직은 순수 함수로 작성하고(참조 투명), 부수효과는 가장자리에서 처리한다. 순수한 Core는 모킹 없이 테스트하고, 메모이제이션으로 최적화하며, 자유롭게 리팩토링할 수 있다.
