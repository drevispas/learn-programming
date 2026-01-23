# 11. Algebraic Properties (대수적 속성)

> **Sources**: DOP Ch.7 (Algebraic Properties)

---

## 1. Associativity (결합법칙) - 병렬 처리 안전성

### 핵심 개념
- **관련 키워드**: Associativity, Parallel Processing, MapReduce, parallelStream, Monoid
- **통찰**: (A + B) + C = A + (B + C)가 성립하면, 어떻게 분할하여 병렬 처리해도 결과가 동일하다.
- **설명**: 결합법칙은 연산의 그룹핑(괄호)을 바꿔도 결과가 동일한 속성이다. 이 속성이 보장되면 대량 데이터를 여러 파티션으로 나누어 병렬 처리한 후 합산해도 순차 처리와 동일한 결과를 얻는다. MapReduce, parallelStream, 분산 계산의 수학적 기반이 바로 결합법칙이다.

  Java의 `parallelStream().reduce()`는 내부적으로 데이터를 분할(fork)하여 각 청크를 독립적으로 reduce한 후 합치는(join) 방식으로 동작한다. 이때 결합법칙이 성립하지 않으면 분할 방식에 따라 결과가 달라지므로 병렬 처리가 불가능하다.

  실전에서 결합법칙을 만족하는 대표적인 연산은 덧셈, 문자열 연결, 리스트 병합, 최대/최소값 등이다. 반면 뺄셈, 나눗셈, 평균 계산 등은 결합법칙을 만족하지 않는다.

```
SEQUENTIAL vs PARALLEL PROCESSING (Associativity)
==================================================

Sequential: O(n)                 Parallel: O(n/p)
                                 (p = processor count)

  [1, 2, 3, 4, 5, 6, 7, 8]      [1, 2, 3, 4, 5, 6, 7, 8]
  |                               |           |
  v                            [1,2,3,4]   [5,6,7,8]
  1+2+3+4+5+6+7+8 = 36           |           |
                                  v           v
                                 10          26
                                  |           |
                                  +-----+-----+
                                        |
                                        v
                                       36 (same result!)

  Associativity guarantees:
  (1+2+3+4) + (5+6+7+8) = 1+2+3+4+5+6+7+8
```

### 개념이 아닌 것
- **"결합법칙 = 교환법칙"**: 결합법칙은 괄호 위치, 교환법칙은 피연산자 순서에 관한 것. 문자열 연결은 결합법칙은 만족하지만 교환법칙은 만족하지 않음.
- **"모든 연산이 병렬화 가능"**: 뺄셈, 나눗셈, 평균 계산 등 결합법칙을 만족하지 않는 연산은 병렬 처리 시 결과가 달라질 수 있음.

### Before: Traditional OOP
```java
// [X] 순차 처리만 가능한 가변 누적기
public class OrderTotalCalculator {
    private BigDecimal total = BigDecimal.ZERO; // 가변 상태

    public void addOrderTotal(Order order) {
        this.total = this.total.add(order.getTotal()); // 상태 변경
    }

    public BigDecimal getTotal() {
        return this.total;
    }
}

// 사용: 반드시 순차 처리
OrderTotalCalculator calc = new OrderTotalCalculator();
for (Order order : millionOrders) {
    calc.addOrderTotal(order); // 순서대로만 가능
}
```
- **의도 및 코드 설명**: 가변 누적 변수를 사용하여 주문 합계를 계산하는 전통적 방식
- **뭐가 문제인가**:
  - 가변 상태로 인해 멀티스레드 환경에서 동기화 필요
  - parallelStream 적용 불가 (경쟁 조건 발생)
  - 100만 건 처리 시 단일 스레드로만 가능 (O(n))
  - 분산 환경에서 분할 처리 불가

### After: Modern Approach
```java
// [O] 결합법칙을 만족하는 불변 Money 타입
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("금액 필수");
        if (currency == null) throw new IllegalArgumentException("통화 필수");
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // 결합법칙 만족: (a.add(b)).add(c) == a.add(b.add(c))
    public Money add(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException("통화 불일치");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// 결합법칙 덕분에 parallelStream 안전하게 사용
List<Money> orderTotals = getOrderTotals(); // 100만 건

Money total = orderTotals.parallelStream()
    .reduce(Money.zero(Currency.KRW), Money::add); // O(n/p)
```
- **의도 및 코드 설명**: 불변 record의 add 연산이 결합법칙을 만족하므로, parallelStream의 분할-합산이 안전
- **무엇이 좋아지나**:
  - 병렬 처리로 성능 향상 (O(n/p), p = 프로세서 수)
  - 불변 타입이므로 동기화 불필요
  - 분산 환경 확장 가능 (MapReduce 패턴)
  - 수학적으로 정확성 보장

### 이해를 위한 부가 상세
결합법칙을 만족하는 연산과 항등원이 함께 존재하면 이를 **Monoid**라 한다. Monoid는 함수형 프로그래밍에서 병렬 처리, 폴딩, 스트림 처리의 핵심 추상화이다. Java의 `Stream.reduce(identity, accumulator, combiner)`는 Monoid 구조를 그대로 반영한 API이다.

### 틀리기/놓치기 쉬운 부분
- `parallelStream().reduce()`에서 combiner 함수도 결합법칙을 만족해야 한다
- BigDecimal의 뺄셈이나 나눗셈으로 reduce하면 parallelStream에서 결과가 달라질 수 있다
- 장바구니 병합 시 같은 상품의 수량 합산도 결합법칙을 만족하도록 설계해야 한다

### 꼭 기억할 것
- 결합법칙 = 괄호 위치 무관 = 분할 처리 안전
- parallelStream, MapReduce를 쓰려면 연산이 결합법칙을 만족하는지 먼저 확인
- 불변 타입 + 결합법칙 = 안전한 병렬 처리

---

## 2. Idempotency (멱등성) - 재시도 안전성

### 핵심 개념
- **관련 키워드**: Idempotency, Retry Safety, At-Least-Once Delivery, Distributed Systems, Idempotency Key
- **통찰**: f(f(x)) = f(x)가 성립하면, 같은 연산을 여러 번 실행해도 결과가 변하지 않아 재시도가 안전하다.
- **설명**: 멱등성은 동일한 연산을 여러 번 적용해도 한 번 적용한 것과 동일한 결과를 보장하는 속성이다. 분산 시스템에서는 네트워크 장애, 타임아웃 등으로 인해 요청이 중복 전송될 수 있다. 멱등한 연산이면 재시도가 안전하므로, "최소 한 번 전송(at-least-once delivery)" 전략을 안심하고 사용할 수 있다.

  HTTP 메서드 중 GET, PUT, DELETE는 명세상 멱등하도록 설계되어야 하고, POST는 멱등하지 않다. 실무에서는 결제, 포인트 차감, 재고 감소 등 부수효과가 있는 쓰기 연산에 멱등성 키(idempotency key)를 도입하여 중복 실행을 방지한다.

  멱등성은 "토글 스위치(ON/OFF)"가 아닌 "ON 버튼"과 같다. ON 버튼은 이미 켜져 있으면 아무 변화가 없고, 꺼져 있으면 켠다. 두 번 눌러도 결과는 같다.

```
IDEMPOTENT vs NON-IDEMPOTENT
==============================

Non-Idempotent (Toggle):     Idempotent (Set ON):
  Press 1: OFF -> ON            Press 1: OFF -> ON
  Press 2: ON -> OFF  [!]      Press 2: ON -> ON  [safe]
  Press 3: OFF -> ON            Press 3: ON -> ON  [safe]

Payment Example:
  Non-Idempotent:               Idempotent:
  +----------+                  +----------+
  | withdraw |                  | check ID |
  | 10000    |  x2 = -20000!   |  exists? |
  +----------+                  +----+-----+
                                     |
                                  N  |  Y
                                  v  |  v
                              [process] [skip]
                              [save ID]  (safe!)
```

### 개념이 아닌 것
- **"읽기 전용이면 멱등"**: 읽기(GET)는 자연히 멱등하지만, 쓰기 연산도 설계에 따라 멱등하게 만들 수 있음
- **"멱등 = 순수 함수"**: 순수 함수는 부수효과가 없지만, 멱등한 연산은 부수효과가 있을 수 있음 (첫 실행에서만)

### Before: Traditional OOP
```java
// [X] 멱등하지 않은 결제 - 중복 호출 시 이중 출금
public class PaymentService {
    public void processPayment(PaymentRequest request) {
        account.withdraw(request.getAmount());  // 호출할 때마다 출금!
        merchant.deposit(request.getAmount());
        log.info("결제 완료: {}", request.getAmount());
    }
}

// 네트워크 오류로 재시도 시:
// 1차 호출: 10,000원 출금 (성공, 하지만 응답 타임아웃)
// 2차 호출: 10,000원 또 출금! (이중 결제!)
```
- **의도 및 코드 설명**: 결제 요청을 받을 때마다 무조건 출금하는 단순한 구현
- **뭐가 문제인가**:
  - 네트워크 재시도 시 이중 결제 발생
  - 분산 시스템에서 at-least-once delivery 적용 불가
  - 클라이언트 버그로 중복 요청 시 방어 불가
  - 결제 취소/환불 로직이 복잡해짐

### After: Modern Approach
```java
// [O] 멱등한 결제 - 재시도 안전
public record PaymentId(String value) {}

public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Result<Payment, PaymentError> processPayment(
            PaymentId id, PaymentRequest request) {
        // 이미 처리된 결제인지 확인
        if (paymentRepository.exists(id)) {
            return Result.success(paymentRepository.findById(id).get());
        }

        // 첫 실행에서만 실제 처리
        Result<TransactionId, PaymentError> txResult =
            account.withdraw(request.amount());

        return txResult.map(txId -> {
            Payment payment = new Payment(id, request, txId, LocalDateTime.now());
            paymentRepository.save(payment);
            return payment;
        });
    }
}

// 네트워크 오류로 재시도 시:
// 1차 호출: 10,000원 출금 + 결과 저장 (성공)
// 2차 호출: 이미 존재하므로 저장된 결과 반환 (안전!)
```
- **의도 및 코드 설명**: PaymentId로 중복 여부를 확인하여, 이미 처리된 요청은 기존 결과를 반환
- **무엇이 좋아지나**:
  - 네트워크 재시도 시에도 이중 결제 방지
  - at-least-once delivery 전략 안전하게 적용 가능
  - 클라이언트 중복 요청에도 동일 결과 보장
  - 분산 환경에서 안정적인 결제 처리

### 이해를 위한 부가 상세
멱등성 키 저장소는 일반적으로 TTL(Time-To-Live)을 설정하여 일정 시간 후 자동 삭제한다. Redis의 SETNX(SET if Not eXists)를 활용하면 원자적으로 중복 검사와 키 저장을 동시에 수행할 수 있다.

실무 우선순위:
- 필수: 결제, 포인트 차감, 재고 감소 (중복 실행 시 금전적 손실)
- 권장: 주문 상태 변경, 알림 발송 (중복 실행 시 사용자 혼란)
- 선택: 로그 기록, 조회 API (중복 실행해도 실질적 피해 없음)

### 틀리기/놓치기 쉬운 부분
- 멱등성 키 확인과 비즈니스 처리 사이에 경쟁 조건이 발생할 수 있다 (분산 락 필요)
- "상태를 SET"하는 연산은 자연히 멱등하지만, "상태를 INCREMENT"하는 연산은 멱등하지 않다
- HTTP PUT은 명세상 멱등하지만, 구현에서 auto-increment 카운터를 사용하면 멱등성이 깨진다

### 꼭 기억할 것
- 멱등성 = 여러 번 실행해도 한 번 실행한 것과 동일한 결과
- 부수효과 있는 CUD 연산은 멱등성 키로 보호
- 읽기 연산(R)은 이미 멱등

---

## 3. Identity Element (항등원) - 초기값과 빈 컬렉션 처리

### 핵심 개념
- **관련 키워드**: Identity Element, Monoid, reduce, fold, Empty Collection, Zero Value
- **통찰**: A + e = A를 만족하는 항등원(e)이 있으면, 빈 컬렉션의 reduce에서도 예외 없이 안전한 초기값을 제공한다.
- **설명**: 항등원은 어떤 연산에서 다른 값과 결합해도 그 값을 변화시키지 않는 특별한 원소이다. 덧셈의 0, 곱셈의 1, 문자열 연결의 빈 문자열, 리스트 병합의 빈 리스트가 대표적이다.

  항등원이 있으면 `Stream.reduce(identity, accumulator)` 형태로 빈 컬렉션에도 안전하게 reduce를 적용할 수 있다. 항등원이 없으면 `reduce()`가 `Optional`을 반환하므로 빈 컬렉션 처리를 별도로 해야 한다.

  도메인에서 "아무 할인 없음", "무료 배송", "빈 장바구니" 등은 모두 해당 연산의 항등원으로 모델링할 수 있다.

```
IDENTITY ELEMENT IN reduce()
==============================

Without Identity:                With Identity:
  [].reduce(Money::add)            [].reduce(ZERO, Money::add)
       |                                |
       v                                v
  Optional.empty()                  Money(0, KRW)
  -> orElseThrow()???               -> safe! no exception
  -> NoSuchElementException!

Domain Identity Elements:
  +----------+---------+-------------------+
  | Operation| Identity| Example           |
  +----------+---------+-------------------+
  | add      | 0       | Money.zero(KRW)   |
  | multiply | 1       | Rate.one()        |
  | concat   | ""      | Name.empty()      |
  | merge    | []      | Cart.empty()      |
  | discount | NoDsc   | NoDiscount()      |
  +----------+---------+-------------------+
```

### 개념이 아닌 것
- **"항등원 = null"**: null은 NPE를 유발하지만, 항등원은 정상적인 도메인 값
- **"항등원 = 기본값"**: 기본값은 임의의 초기값이지만, 항등원은 연산과의 관계에서 수학적으로 정의됨

### Before: Traditional OOP
```java
// [X] 빈 컬렉션에서 예외 발생 가능한 누적 계산
public class OrderSummary {
    public BigDecimal calculateTotal(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO; // 매번 null/empty 체크 필수
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            total = total.add(order.getTotal());
        }
        return total;
    }

    public BigDecimal calculateAverage(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO; // 또 null/empty 체크
        }
        return calculateTotal(orders)
            .divide(BigDecimal.valueOf(orders.size()));
    }
}
```
- **의도 및 코드 설명**: 매번 null/empty 방어 코드를 작성하여 빈 컬렉션을 처리
- **뭐가 문제인가**:
  - 모든 메서드에서 null/empty 체크 반복
  - 체크를 빠뜨리면 NoSuchElementException 또는 NPE
  - "빈 주문 목록"의 의미가 코드에 드러나지 않음
  - 도메인 의미 없이 기술적 방어만 존재

### After: Modern Approach
```java
// [O] 항등원을 활용한 안전한 reduce
public record Money(BigDecimal amount, Currency currency) {
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// 할인 타입에도 항등원 적용
public sealed interface Discount {
    record NoDiscount() implements Discount {}    // 항등원!
    record Percentage(int rate) implements Discount {}
    record FixedAmount(Money amount) implements Discount {}
}

// 항등원 덕분에 빈 컬렉션에도 안전
public static Money totalPaid(Order order) {
    return order.payments().stream()
        .map(Payment::amount)
        .reduce(Money.zero(order.currency()), Money::add);
    // 결제 내역이 없어도 0원 반환 (예외 없음)
}

// 여러 할인을 순차 적용 (NoDiscount = 항등원)
public static Money applyAll(Money original, List<Discount> discounts) {
    return discounts.stream()
        .reduce(original,
            (money, discount) -> DiscountCalculator.apply(money, discount),
            (m1, m2) -> m1);
}
```
- **의도 및 코드 설명**: Money.zero()와 NoDiscount()를 항등원으로 정의하여, 빈 컬렉션에서도 예외 없이 자연스러운 결과 반환
- **무엇이 좋아지나**:
  - null/empty 체크 코드 제거
  - 빈 컬렉션에서도 의미 있는 기본값 (0원, 할인 없음)
  - NoSuchElementException 원천 차단
  - 도메인 의미가 타입에 반영 (NoDiscount는 "할인 없음"이라는 비즈니스 개념)

### 이해를 위한 부가 상세
결합법칙을 만족하는 이항 연산 + 항등원 = **Monoid**. 이 Monoid 구조는 Java Stream API의 `reduce(identity, accumulator, combiner)` 시그니처에 그대로 반영되어 있다.

### 틀리기/놓치기 쉬운 부분
- `Stream.reduce(BinaryOperator)`는 항등원이 없어 `Optional`을 반환한다. `Stream.reduce(T identity, BinaryOperator)`는 항등원이 있어 `T`를 직접 반환한다.
- 할인율 연산의 항등원은 0%이지, 100%가 아니다 (NoDiscount는 "할인율 0%"에 해당)
- Optional.empty()를 항등원으로 생각할 수 있지만, 이는 Optional 모나드의 zero이지 도메인 항등원과는 다르다

### 꼭 기억할 것
- 항등원이 있으면 빈 컬렉션 reduce가 예외 없이 안전
- 도메인에서 "없음" 상태를 ADT의 case로 표현하면 항등원 역할
- 결합법칙 + 항등원 = Monoid = Stream.reduce의 수학적 기반

---

## 4. Commutativity (교환법칙) - 순서 무관 연산

### 핵심 개념
- **관련 키워드**: Commutativity, Order-Independence, CRDT, Eventual Consistency, Set Operations
- **통찰**: A + B = B + A가 성립하면, 연산 순서에 관계없이 동일한 결과를 보장하여 분산 시스템의 순서 문제를 해결한다.
- **설명**: 교환법칙은 피연산자의 순서를 바꿔도 결과가 동일한 속성이다. 결합법칙이 "분할 방식의 자유"를 보장한다면, 교환법칙은 "처리 순서의 자유"를 보장한다.

  분산 시스템에서 메시지 도착 순서를 보장할 수 없을 때, 교환법칙을 만족하는 연산이면 순서에 관계없이 동일한 최종 상태에 도달한다. CRDT(Conflict-free Replicated Data Type)가 바로 이 원리를 활용한다.

  주의: 교환법칙과 결합법칙은 독립적인 속성이다. 문자열 연결은 결합법칙은 만족하지만 교환법칙은 만족하지 않는다 ("AB" != "BA"). 반면 max(a, b)는 둘 다 만족한다.

```
COMMUTATIVITY IN DISTRIBUTED SYSTEMS
======================================

Messages arrive in different order:

  Node A              Node B
  +-----+            +-----+
  | +5  |            | +3  |
  | +3  |            | +5  |
  +-----+            +-----+
     |                  |
     v                  v
  5 + 3 = 8          3 + 5 = 8  (same! commutative)

  Non-commutative example:
  "A"+"B" = "AB"    "B"+"A" = "BA"  (different! NOT commutative)

  Commutative operations: +, *, max, min, union, intersection
  Non-commutative: -, /, concat, matrix multiply
```

### 개념이 아닌 것
- **"교환법칙 = 결합법칙"**: 서로 독립적인 속성. 문자열 연결은 결합법칙만 만족, 뺄셈은 둘 다 불만족
- **"교환법칙이 필수"**: 순서가 보장되는 단일 노드 환경에서는 결합법칙만으로 충분

### Before: Traditional OOP
```java
// [X] 순서 의존적인 할인 적용 - 순서에 따라 결과 다름
public class DiscountService {
    private BigDecimal price;

    public DiscountService(BigDecimal price) {
        this.price = price;
    }

    public void applyPercentageDiscount(int rate) {
        this.price = this.price.multiply(
            BigDecimal.valueOf(100 - rate).divide(BigDecimal.valueOf(100)));
    }

    public void applyFixedDiscount(BigDecimal amount) {
        this.price = this.price.subtract(amount);
    }
}

// 순서에 따라 결과가 다름!
// 10000원, 10% 할인 후 1000원 할인: 10000 * 0.9 - 1000 = 8000
// 10000원, 1000원 할인 후 10% 할인: (10000 - 1000) * 0.9 = 8100
```
- **의도 및 코드 설명**: 가변 상태에 할인을 순차적으로 적용하여, 호출 순서에 따라 결과가 달라짐
- **뭐가 문제인가**:
  - 할인 적용 순서에 따라 최종 가격이 달라짐
  - 비즈니스 규칙("어떤 순서든 동일한 결과")을 보장할 수 없음
  - 분산 환경에서 이벤트 순서가 다르면 결과 불일치

### After: Modern Approach
```java
// [O] 교환법칙을 만족하도록 설계: 할인을 독립적으로 계산 후 합산
public sealed interface Discount {
    record NoDiscount() implements Discount {}
    record Percentage(int rate) implements Discount {}
    record FixedAmount(Money amount) implements Discount {}
}

public class DiscountCalculator {
    // 각 할인의 할인액을 독립적으로 계산
    public static Money discountAmount(Money original, Discount discount) {
        return switch (discount) {
            case NoDiscount() -> Money.zero(original.currency());
            case Percentage(int rate) ->
                new Money(original.amount()
                    .multiply(BigDecimal.valueOf(rate))
                    .divide(BigDecimal.valueOf(100)), original.currency());
            case FixedAmount(Money amt) -> amt;
        };
    }

    // 할인액을 합산하여 적용 (합산은 교환법칙 만족)
    public static Money applyAll(Money original, List<Discount> discounts) {
        Money totalDiscount = discounts.stream()
            .map(d -> discountAmount(original, d))
            .reduce(Money.zero(original.currency()), Money::add); // 합산은 교환법칙!
        return original.subtract(totalDiscount);
    }
}

// 순서와 무관하게 동일한 결과
// 10000원, [10%할인, 1000원할인] = 10000 - (1000 + 1000) = 8000
// 10000원, [1000원할인, 10%할인] = 10000 - (1000 + 1000) = 8000
```
- **의도 및 코드 설명**: 각 할인의 할인액을 원래 가격 기준으로 독립 계산한 후 합산. 합산(덧셈)은 교환법칙을 만족하므로 순서 무관
- **무엇이 좋아지나**:
  - 할인 적용 순서에 무관한 결과
  - 분산 환경에서 이벤트 순서 불일치에도 동일한 최종 가격
  - 비즈니스 규칙의 예측 가능성 확보
  - 테스트가 순서 조합을 고려할 필요 없음

### 틀리기/놓치기 쉬운 부분
- 할인율을 순차 적용하면 교환법칙이 깨진다 (10% 후 20% vs 20% 후 10%는 결과 다름)
- max, min은 교환법칙과 결합법칙을 모두 만족하여 CRDT에 많이 활용됨
- 결합법칙 + 교환법칙 + 멱등성을 모두 만족하면 수렴(convergence)이 보장됨

### 꼭 기억할 것
- 교환법칙 = 순서 무관 = 분산 시스템에서 순서 보장 불필요
- 결합법칙과 교환법칙은 독립적인 속성
- 할인 시스템에서 순서 무관 결과를 원하면, 할인액을 독립 계산 후 합산하는 전략 사용

---

## 5. Practical Applications (실전 적용) - reduce, fold, retry

### 핵심 개념
- **관련 키워드**: reduce, fold, retry, Stream API, Resilience, Monoid, Combiner
- **통찰**: 대수적 속성은 이론이 아니라 reduce의 안전성, retry의 안전성, 빈 값 처리를 직접 보장하는 실전 도구이다.
- **설명**: 앞서 배운 네 가지 대수적 속성을 종합하면 다음과 같은 실전 패턴이 나온다:
  - **결합법칙**: `parallelStream().reduce()` 안전성
  - **항등원**: `reduce(identity, accumulator)` 빈 컬렉션 안전성
  - **멱등성**: `retryOnFailure(operation)` 재시도 안전성
  - **교환법칙**: 분산 환경 순서 무관 처리

  이 속성들은 개별로도 유용하지만, 결합하면 더 강력해진다. 결합법칙 + 항등원 = Monoid로 안전한 병렬 reduce를, 멱등성 + 재시도 = 안전한 분산 처리를 보장한다.

```
ALGEBRAIC PROPERTIES IN PRACTICE
==================================

+----------------+------------------+---------------------+
| Property       | Java API         | Guarantee           |
+----------------+------------------+---------------------+
| Associativity  | parallelStream() | safe parallel split |
|                | .reduce()        |                     |
+----------------+------------------+---------------------+
| Identity       | reduce(id, acc)  | safe empty reduce   |
+----------------+------------------+---------------------+
| Idempotency    | retryOnFailure() | safe retry          |
+----------------+------------------+---------------------+
| Commutativity  | unordered events | order independence  |
+----------------+------------------+---------------------+

Combined: Monoid (Assoc + Id) -> parallelStream().reduce(id, op)
```

### 개념이 아닌 것
- **"대수적 속성은 학술적 개념"**: 아님. 매일 사용하는 Stream.reduce, 재시도 로직, CRDT의 수학적 기반
- **"모든 연산에 적용 가능"**: 뺄셈, 나눗셈, 평균 등은 이러한 속성을 만족하지 않으므로 별도 설계 필요

### Before: Traditional OOP
```java
// [X] 대수적 속성을 고려하지 않은 설계
public class CartService {
    private Cart cart = new Cart();

    // 결합법칙 미고려: 병렬 처리 불가
    public void addItem(CartItem item) {
        cart.getItems().add(item); // 가변 조작
    }

    // 멱등성 미고려: 중복 호출 시 아이템 중복
    public void mergeCart(Cart other) {
        for (CartItem item : other.getItems()) {
            cart.getItems().add(item); // 중복 체크 없음
        }
    }

    // 항등원 미고려: 빈 장바구니에서 예외 가능
    public BigDecimal getTotal() {
        return cart.getItems().stream()
            .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQty())))
            .reduce(BigDecimal::add)
            .orElseThrow(); // 빈 카트에서 예외!
    }
}
```
- **의도 및 코드 설명**: 가변 상태, 중복 체크 없음, 빈 컬렉션 미처리로 여러 대수적 속성을 위반
- **뭐가 문제인가**:
  - 병렬 처리 불가 (가변 상태)
  - 중복 호출 시 데이터 오염 (멱등성 없음)
  - 빈 장바구니에서 예외 (항등원 없음)
  - 장바구니 병합 순서에 따라 결과 다를 수 있음

### After: Modern Approach
```java
// [O] 대수적 속성을 종합 적용한 Cart 설계
public record CartItem(ProductId productId, Quantity quantity, Money price) {
    public CartItem addQuantity(Quantity additional) {
        return new CartItem(productId, quantity.add(additional), price);
    }
}

public record Cart(List<CartItem> items) {
    public static Cart empty() { return new Cart(List.of()); } // 항등원

    // 결합법칙 만족: (a.merge(b)).merge(c) == a.merge(b.merge(c))
    // 교환법칙 만족: a.merge(b) == b.merge(a) (같은 상품은 수량 합산)
    public Cart merge(Cart other) {
        Map<ProductId, CartItem> merged = new HashMap<>();
        for (CartItem item : this.items) {
            merged.merge(item.productId(), item,
                (existing, newItem) -> existing.addQuantity(newItem.quantity()));
        }
        for (CartItem item : other.items) {
            merged.merge(item.productId(), item,
                (existing, newItem) -> existing.addQuantity(newItem.quantity()));
        }
        return new Cart(List.copyOf(merged.values()));
    }

    // 항등원 덕분에 안전한 total 계산
    public Money total() {
        return items.stream()
            .map(item -> item.price().multiply(item.quantity().value()))
            .reduce(Money.zero(Currency.KRW), Money::add); // 빈 카트도 안전
    }
}

// 멱등한 장바구니 동기화
public Result<Cart, CartError> syncCart(UserId userId, Cart clientCart) {
    Cart serverCart = cartRepository.findByUserId(userId)
        .orElse(Cart.empty());   // 항등원 활용
    Cart merged = serverCart.merge(clientCart); // 결합+교환법칙
    cartRepository.save(userId, merged);
    return Result.success(merged);
}
// 중복 호출해도 merge는 멱등하게 동작 (같은 아이템 수량만 합산)
```
- **의도 및 코드 설명**: Cart 병합이 결합법칙, 교환법칙을 만족하고, empty()가 항등원 역할을 하며, 동기화가 멱등하게 설계됨
- **무엇이 좋아지나**:
  - 병렬 처리 가능 (결합법칙)
  - 빈 장바구니에도 안전 (항등원)
  - 네트워크 재시도 안전 (멱등성)
  - 순서 무관 병합 (교환법칙)
  - 분산 환경에서의 장바구니 동기화 정합성 보장

### 이해를 위한 부가 상세
Stream.reduce의 세 인자 시그니처 `reduce(identity, accumulator, combiner)`는 Monoid 구조를 직접 반영한다:
- `identity`: 항등원
- `accumulator`: 이항 연산 (결합법칙 필요)
- `combiner`: 병렬 처리 시 부분 결과 합산 (결합법칙 + 교환법칙 필요)

### 틀리기/놓치기 쉬운 부분
- `reduce`의 combiner는 parallelStream에서만 호출됨. 순차 스트림에서는 무시되므로, 순차 전용이라면 combiner의 정확성을 테스트하지 못할 수 있음
- 장바구니 merge에서 "같은 상품 수량 합산"이 교환법칙을 만족하는 이유는, 덧셈이 교환법칙을 만족하기 때문
- retry 로직에서 멱등성이 아닌 연산을 재시도하면 데이터가 꼬인다. retry 전에 반드시 연산의 멱등성을 확인할 것

### 꼭 기억할 것
- 결합법칙 + 항등원 = Monoid = Stream.reduce의 수학적 기반
- 멱등성 = 안전한 재시도
- 교환법칙 = 분산 환경 순서 무관
- 이 네 속성을 점검표로 활용하면 안전한 분산 시스템 설계 가능
