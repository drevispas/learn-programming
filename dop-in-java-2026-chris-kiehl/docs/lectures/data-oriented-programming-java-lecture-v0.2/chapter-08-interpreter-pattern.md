# Chapter 8: 인터프리터 패턴 - Rule as Data ⭐ (강화)

## 학습 목표
1. 인터프리터 패턴의 개념과 필요성을 이해한다
2. 비즈니스 로직을 데이터로 표현하는 방법을 알 수 있다
3. 재귀적 데이터 구조를 설계할 수 있다
4. Rule Engine을 단계별로 구현할 수 있다
5. 흔한 실수를 피하고 동적 규칙 로딩을 구현할 수 있다

---

## 8.1 왜 로직을 데이터로 만드는가?

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하면 여러 문제가 발생합니다:
- 규칙 추가/변경마다 코드 수정 필요
- 규칙 조합이 복잡해지면 이해하기 어려움
- 런타임에 규칙을 변경할 수 없음

**해결책**: 규칙 자체를 **데이터(ADT)**로 표현하고, 별도의 **해석기(Interpreter)**가 실행합니다.

---

## 8.2 Rule Engine 단계별 구현 ⭐

### Step 1: 기본 Equals 조건

```java
public record Customer(String country, String type, int totalSpend) {}

public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
}

public class RuleEngine {
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var attr, var value) -> switch (attr) {
                case "country" -> customer.country().equals(value);
                case "type" -> customer.type().equals(value);
                default -> false;
            };
        };
    }
}
```

### Step 2: And/Or/Not 논리 연산 추가

```java
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}
}
```

### Step 3: GTE 조건 추가 - 흔한 실수 사례 ⭐

#### [Trap] "데이터"와 "값"의 시점 차이

**잘못된 설계** (흔한 실수):
```java
// ❌ 값(int)을 직접 받음 - 규칙 생성 시점에 결과 확정!
record GTE(int left, int right) implements Rule {}
```

**올바른 설계**:
```java
// ✅ 속성 이름(String)을 저장 - 평가 시점에 데이터 참조
record GTE(String attribute, int threshold) implements Rule {}

case Rule.GTE(var attr, var threshold) -> switch (attr) {
    case "totalSpend" -> customer.totalSpend() >= threshold;
    default -> false;
};
```

---

## 8.3 복잡한 할인 규칙 예제

"한국(KR)에 살면서, (구매액 100만원 이상이거나 VIP)"

```java
Rule krDiscountRule = new Rule.And(
    new Rule.Equals("country", "KR"),
    new Rule.Or(
        new Rule.Equals("type", "VIP"),
        new Rule.GTE("totalSpend", 1_000_000)
    )
);

Customer krCustomer = new Customer("KR", "GOLD", 1_500_000);
boolean discountable = RuleEngine.evaluate(krDiscountRule, krCustomer); // true
```

---

## 8.4 동적 규칙 로딩 (DB/JSON)

```json
{
  "type": "and",
  "left": { "type": "equals", "attribute": "country", "value": "KR" },
  "right": { "type": "gte", "attribute": "totalSpend", "threshold": 500000 }
}
```

마케팅 팀이 관리자 화면에서 규칙을 수정하면, **코드 배포 없이** 즉시 적용됩니다.

---

## 8.5 Before/After: if-else vs Rule Engine

### Before: 하드코딩
```java
if (customer.country().equals("KR") && customer.type().equals("VIP")) {
    return true;
}
// 새 규칙마다 코드 수정 필요...
```

### After: 데이터 주도
```java
List<Rule> rules = loadFromDatabase();
return rules.stream().anyMatch(r -> RuleEngine.evaluate(r, customer));
// 새 규칙은 데이터 추가만!
```

---

## 퀴즈 Chapter 8

### Q8.1 [함정 문제] GTE 규칙 설계 실수 ⭐
다음 GTE 규칙 설계의 문제점은?
```java
record GTE(int left, int right) implements Rule {}
Rule rule = new Rule.GTE(customer.totalSpend(), 1_000_000);
```

A. 문제없음
B. 규칙 생성 시점에 이미 결과가 결정되어, 다른 고객에게 재사용 불가
C. 컴파일 에러
D. 성능 문제

---

### Q8.2 [코드 분석] 규칙 표현
다음 비즈니스 규칙을 표현한 Rule은?
"VIP 등급이 아니면서 주문금액이 3만원 이상인 경우"

A. `new And(new Not(new UserGrade(VIP)), new MinTotal(30000))`
B. `new Or(new Not(new UserGrade(VIP)), new MinTotal(30000))`
C. `new Not(new And(new UserGrade(VIP), new MinTotal(30000)))`
D. `new And(new UserGrade(VIP), new MinTotal(30000))`

---

### Q8.3 [코드 분석] 재귀 구조
다음 Rule의 평가 순서는?

```java
Rule rule = new Or(
    new And(new UserGrade(VIP), new MinTotal(50000)),
    new ProductCategory(FASHION)
);
```

A. VIP → MinTotal → And → FASHION → Or
B. Or → And → VIP → MinTotal → FASHION
C. FASHION → MinTotal → VIP → And → Or
D. Or → FASHION → And → VIP → MinTotal

---

### Q8.4 [설계 문제] 새 규칙 추가
"첫 구매 고객" 조건을 추가하려면?

A. if-else 문 추가
B. Rule sealed interface에 새 record 추가 + 해석기에 case 추가
C. 새로운 클래스 상속
D. 기존 Rule 수정

---

### Q8.5 [코드 작성] 규칙 설계
다음 프로모션 규칙을 Rule 타입으로 표현하세요.
"신규 회원(가입 30일 이내)이거나, 주문금액이 10만원 이상이면서 전자제품 카테고리 상품을 포함한 경우"

---

정답은 Appendix C에서 확인할 수 있습니다.
