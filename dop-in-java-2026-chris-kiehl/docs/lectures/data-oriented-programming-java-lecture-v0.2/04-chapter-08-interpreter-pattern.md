# Chapter 8: 인터프리터 패턴 - Rule as Data ⭐ (강화)

## 학습 목표
1. 인터프리터 패턴의 개념과 필요성을 이해한다
2. 비즈니스 로직을 데이터로 표현하는 방법을 알 수 있다
3. 재귀적 데이터 구조를 설계할 수 있다
4. Rule Engine을 단계별로 구현할 수 있다
5. 흔한 실수를 피하고 동적 규칙 로딩을 구현할 수 있다

---

## 8.1 왜 로직을 데이터로 만드는가?

> **🎯 왜 배우는가?**
>
> 마케팅 팀이 "이 조건일 때 할인해주세요"라고 할 때마다 코드를 수정하고 배포하시나요?
> 인터프리터 패턴을 익히면 **비즈니스 규칙을 데이터로 표현**하여
> 코드 배포 없이 규칙을 추가/변경할 수 있습니다.

비즈니스 규칙이 복잡해질 때, `if-else`를 떡칠하면 여러 문제가 발생합니다:
- 규칙 추가/변경마다 코드 수정 필요
- 규칙 조합이 복잡해지면 이해하기 어려움
- 런타임에 규칙을 변경할 수 없음

**해결책**: 규칙 자체를 **데이터(ADT)**로 표현하고, 별도의 **해석기(Interpreter)**가 실행합니다.

---

## 8.2 Rule Engine 단계별 구현 ⭐

> **🎯 왜 배우는가?**
>
> Rule Engine을 어디서부터 시작해야 할지 막막하셨나요?
> 단계별 구현을 따라가면 **점진적으로 기능을 확장하는 방법**을 익히고,
> 실제 프로젝트에 적용할 수 있는 기반을 갖출 수 있습니다.

### Step 1: 기본 Equals 조건

**Code 8.1**: 기본 Equals 조건
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

**Code 8.2**: 논리 연산 추가
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

**Code 8.3**: 잘못된 GTE 설계 (흔한 실수)
```java
// ❌ 값(int)을 직접 받음 - 규칙 생성 시점에 결과 확정!
record GTE(int left, int right) implements Rule {}
```

**Code 8.4**: 올바른 GTE 설계
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

> **🎯 왜 배우는가?**
>
> "A이면서 B이거나 C인 경우"같은 복잡한 조건을 어떻게 표현하셨나요?
> 논리 연산 조합 예제를 통해 **복잡한 비즈니스 규칙을 선언적으로 표현**하는 방법을 익힐 수 있습니다.

"한국(KR)에 살면서, (구매액 100만원 이상이거나 VIP)"

### Rule Engine 트리 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│              Rule Engine 트리 구조 시각화                            │
│                                                                     │
│  규칙: "한국에 살면서 (100만원 이상이거나 VIP)"                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                          ┌───────────┐                              │
│                          │    And    │ ← 루트 노드                  │
│                          └─────┬─────┘                              │
│                     ┌──────────┴──────────┐                         │
│                     ↓                     ↓                         │
│              ┌───────────┐          ┌───────────┐                   │
│              │  Equals   │          │    Or     │                   │
│              │ country   │          └─────┬─────┘                   │
│              │   "KR"    │       ┌────────┴────────┐                │
│              └───────────┘       ↓                 ↓                │
│                           ┌───────────┐     ┌───────────┐           │
│                           │  Equals   │     │    GTE    │           │
│                           │   type    │     │totalSpend │           │
│                           │  "VIP"    │     │ 1,000,000 │           │
│                           └───────────┘     └───────────┘           │
│                                                                     │
│  평가 과정:                                                          │
│  1. Equals("country", "KR") → true                                  │
│  2. Or 평가:                                                         │
│     - Equals("type", "VIP") → false                                 │
│     - GTE("totalSpend", 1000000) → true (1,500,000 >= 1,000,000)   │
│     - Or 결과: true                                                 │
│  3. And(true, true) → true ✅ 할인 대상!                            │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**Code 8.5**: 복잡한 할인 규칙 예제
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

> **🎯 왜 배우는가?**
>
> 규칙을 변경할 때마다 개발팀이 배포해야 하는 상황이 부담스러우셨나요?
> 동적 규칙 로딩을 통해 **운영팀이 직접 규칙을 관리**할 수 있게 되어
> 개발팀의 부담을 줄이고 비즈니스 민첩성을 높일 수 있습니다.

**Code 8.6**: JSON으로 표현된 규칙
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

> **🎯 왜 배우는가?**
>
> if-else 방식과 Rule Engine 방식의 차이를 명확히 보고 싶으신가요?
> Before/After 비교를 통해 **Rule Engine 도입의 효과를 직관적으로 이해**하고,
> 도입 여부를 판단하는 기준을 세울 수 있습니다.

### ❌ 안티패턴: 하드코딩된 규칙

**왜 문제인가?**
- **배포 필수**: 규칙 변경마다 코드 수정 및 배포 필요
- **규칙 분산**: 비즈니스 규칙이 코드 여러 곳에 흩어져 있음
- **비개발자 접근 불가**: 마케팅 팀이 직접 규칙을 수정할 수 없음

**Code 8.7**: Before - 하드코딩
```java
if (customer.country().equals("KR") && customer.type().equals("VIP")) {
    return true;
}
// 새 규칙마다 코드 수정 필요...
```

### ✅ 권장패턴: Rule as Data

**왜 좋은가?**
- **무배포 변경**: 규칙을 데이터로 관리하여 코드 배포 없이 변경
- **규칙 중앙화**: 모든 비즈니스 규칙이 한 곳에서 관리됨
- **비개발자 편집**: 관리자 UI로 운영팀이 직접 규칙 수정 가능

**Code 8.8**: After - 데이터 주도
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

A. 문제없음<br/>
B. 규칙 생성 시점에 이미 결과가 결정되어, 다른 고객에게 재사용 불가<br/>
C. 컴파일 에러<br/>
D. 성능 문제

---

### Q8.2 [코드 분석] 규칙 표현
다음 비즈니스 규칙을 표현한 Rule은?
"VIP 등급이 아니면서 주문금액이 3만원 이상인 경우"

A. `new And(new Not(new UserGrade(VIP)), new MinTotal(30000))`<br/>
B. `new Or(new Not(new UserGrade(VIP)), new MinTotal(30000))`<br/>
C. `new Not(new And(new UserGrade(VIP), new MinTotal(30000)))`<br/>
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

A. VIP → MinTotal → And → FASHION → Or<br/>
B. Or → And → VIP → MinTotal → FASHION<br/>
C. FASHION → MinTotal → VIP → And → Or<br/>
D. Or → FASHION → And → VIP → MinTotal

---

### Q8.4 [설계 문제] 새 규칙 추가
"첫 구매 고객" 조건을 추가하려면?

A. if-else 문 추가<br/>
B. Rule sealed interface에 새 record 추가 + 해석기에 case 추가<br/>
C. 새로운 클래스 상속<br/>
D. 기존 Rule 수정

---

### Q8.5 [코드 작성] 규칙 설계
다음 프로모션 규칙을 Rule 타입으로 표현하세요.
"신규 회원(가입 30일 이내)이거나, 주문금액이 10만원 이상이면서 전자제품 카테고리 상품을 포함한 경우"

---

정답은 Appendix C에서 확인할 수 있습니다.
