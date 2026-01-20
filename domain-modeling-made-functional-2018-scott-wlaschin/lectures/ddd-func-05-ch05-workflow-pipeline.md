# Chapter 5: 워크플로우를 함수 파이프라인으로

> Part II: 심화 - 워크플로우와 에러 처리

---

#### 🎯 WHY: 비즈니스 프로세스 = 함수

#### 💡 비유: 공장 조립 라인
> **비즈니스 프로세스는 공장 조립 라인입니다.**
> - **철판** → 프레스 → **차체**
> - **차체** → 도색 → **도색된 차체**
> - **도색된 차체** → 조립 → **완성차**

**코드 5.1**: 주문 프로세스 파이프라인
```
주문 프로세스:
  PlaceOrderCommand → [Validate] → ValidatedOrder
                           ↓
                      [Price] → PricedOrder
                           ↓
                      [Pay] → PaidOrder
                           ↓
                   OrderPlaced
```

---

### 5.1 중간 타입으로 데이터 품질 표현

**코드 5.2**: 워크플로우의 중간 타입들
```java
// 1. 명령 (Command) - 사용자의 의도 (검증 전)
public record UnvalidatedOrderLine(String productId, int quantity) {}

public record PlaceOrderCommand(
    String customerId,
    List<UnvalidatedOrderLine> lines,
    String shippingAddress,
    String couponCode
) {}

// 2. 검증 후 - 유효한 상태
public record ValidatedOrderLine(ProductId productId, Quantity quantity, Money unitPrice) {}

public record ValidatedOrder(
    CustomerId customerId,
    List<ValidatedOrderLine> lines,
    ShippingAddress shippingAddress,
    CouponCode couponCode
) {}

// 3. 가격 계산 후
public record PricedOrder(CustomerId customerId, Money totalAmount) {}

// 4. 이벤트 - 확정된 과거
public record OrderPlaced(OrderId orderId, Money totalAmount, LocalDateTime occurredAt) {}
```

> ⚠️ **흔한 실수**: Record 알맹이 꺼내기/포장 미흡
>
> ```java
> // ❌ raw.trim() - raw는 RawText, trim()은 String 메서드
> return raw.trim();
>
> // ✅ new SanitizedText(raw.s().trim())
> return new SanitizedText(raw.s().trim());
> ```

#### 📚 Production Readiness & Expert Opinions

**Production에서 사용해도 되나요?**
✅ 예. 워크플로우를 타입으로 표현하는 것은 다음에서 검증되었습니다:
- Stripe의 결제 파이프라인
- Uber의 주문 처리 시스템
- 대부분의 이벤트 소싱 시스템

**Expert Opinions:**
- **Scott Wlaschin** (원저자): "각 단계의 출력을 다른 타입으로 표현하면 워크플로우가 자기 문서화된다."
- **Eric Evans**: "도메인 이벤트는 비즈니스적으로 의미 있는 상태 변화를 포착한다."

---

### 5.2 함수 합성

#### 💡 비유: 레고 블록
> 함수도 출력 타입 = 다음 입력 타입이면 조립됩니다.
> ```
> f: A → B
> g: B → C
> 합성: A → B → C
> ```

**그림 5.1**: 함수 합성 파이프라인 - 타입이 일치하면 연결 가능

![Function Composition](https://fsharpforfunandprofit.com/posts/recipe-part2/Recipe_Railway_Cargo3.png)

*출처: [Railway Oriented Programming - F# for Fun and Profit](https://fsharpforfunandprofit.com/posts/recipe-part2/) - Scott Wlaschin*

**코드 5.3**: 함수 합성으로 워크플로우 구현
```java
public class PlaceOrderWorkflow {
    public Result<OrderPlaced, OrderError> execute(PlaceOrderCommand command) {
        return validateOrder.apply(command)
            .map(priceOrder::apply)
            .flatMap(processPayment::apply)
            .map(this::createOrderPlaced);
    }
}
```

> 💡 전체 워크플로우 구현은 `examples/functional-domain-modeling/` 프로젝트의
> `PlaceOrderUseCase.java`에서 확인할 수 있습니다.

---

### 퀴즈 Chapter 5

#### Q5.1 [개념 확인] `PlaceOrderCommand`와 `ValidatedOrder`를 분리하는 이유는?

**A.** 메모리 절약<br/>
**B.** 검증 전후의 데이터가 다른 보장을 가지므로 *(정답)*<br/>
**C.** Java 문법 제약<br/>
**D.** 디버깅 용이

---

정답은 Appendix D에서 확인할 수 있습니다.
