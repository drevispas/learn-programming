# 12. Interpreter Pattern (인터프리터 패턴)

> **Sources**: DOP Ch.8 (Interpreter Pattern - Rule as Data)

---

## 1. Rule as Data (비즈니스 규칙의 데이터화)

### 핵심 개념
- **관련 키워드**: Rule as Data, ADT, Sealed Interface, Declarative Programming, Business Rules
- **통찰**: 비즈니스 규칙을 if-else 코드가 아닌 데이터(ADT)로 표현하면, 코드 배포 없이 규칙을 추가/변경할 수 있다.
- **설명**: 비즈니스 규칙이 if-else 코드에 하드코딩되면, 규칙 변경마다 코드 수정과 배포가 필요하다. 또한 규칙이 코드 여러 곳에 흩어져 있으면 규칙 전체를 파악하기 어렵고, 비개발자(마케팅, 운영 팀)가 직접 규칙을 관리할 수 없다.

  인터프리터 패턴은 규칙을 sealed interface(ADT)로 표현하여 **규칙 자체를 데이터**로 만든다. 규칙의 정의(what)와 실행(how)이 분리되므로, 규칙은 DB/JSON에 저장하고 해석기(Interpreter)가 이를 실행한다. 이 패턴은 GoF 인터프리터 패턴의 함수형 재해석이다.

  핵심 원리: "규칙은 데이터다. 데이터는 직렬화 가능하다. 직렬화 가능하면 외부에서 관리할 수 있다."

```
RULE AS DATA vs HARDCODED LOGIC
=================================

Hardcoded (if-else):           Rule as Data (ADT):
+------------------+           +------------------+
| if (country==KR) |           | Rule definition  |
|   if (type==VIP) |           | (sealed iface)   |
|     return true  |           +--------+---------+
|   ...            |                    |
+------------------+                    v
                               +------------------+
  Code deploy needed           | Rule data        |
  for every change!            | (DB / JSON)      |
                               +--------+---------+
                                        |
                                        v
                               +------------------+
                               | Interpreter      |
                               | (evaluate)       |
                               +------------------+
                                 No deploy needed!
```

### 개념이 아닌 것
- **"규칙 엔진 = Drools 같은 거대 프레임워크"**: sealed interface + switch만으로도 충분한 규칙 엔진 구현 가능
- **"모든 로직을 데이터화"**: 핵심 알고리즘은 코드로, 자주 변경되는 비즈니스 규칙만 데이터화 대상

### Before: Traditional OOP
```java
// [X] 하드코딩된 비즈니스 규칙 - 변경마다 배포 필요
public class DiscountService {
    public boolean isEligibleForDiscount(Customer customer) {
        // 규칙 1: 한국 VIP
        if ("KR".equals(customer.getCountry())
            && "VIP".equals(customer.getType())) {
            return true;
        }
        // 규칙 2: 구매액 100만원 이상
        if (customer.getTotalSpend() >= 1_000_000) {
            return true;
        }
        // 규칙 3: 신규 추가! -> 코드 수정 + 배포 필요
        if ("US".equals(customer.getCountry())
            && customer.getTotalSpend() >= 500) {
            return true;
        }
        return false;
    }
}
```
- **의도 및 코드 설명**: 할인 자격 조건을 if-else로 직접 코딩. 새 규칙 추가 시 코드 수정 필수
- **뭐가 문제인가**:
  - 규칙 추가/변경마다 코드 수정 + 빌드 + 배포 필요
  - 규칙이 코드 여러 곳에 분산될 수 있음
  - 비개발자(마케팅 팀)가 규칙을 이해하거나 수정 불가
  - 규칙 전체 목록을 한눈에 파악하기 어려움
  - 규칙 조합 로직이 복잡해지면 이해/유지보수 곤란

### After: Modern Approach
```java
// [O] 규칙을 ADT(sealed interface)로 표현 - 데이터화
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record GTE(String attribute, int threshold) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}
}

// 해석기: 규칙을 평가하는 순수 함수
public class RuleEngine {
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var attr, var val) -> getStringAttr(customer, attr).equals(val);
            case Rule.GTE(var attr, var threshold) -> getIntAttr(customer, attr) >= threshold;
            case Rule.And(var left, var right) -> evaluate(left, customer) && evaluate(right, customer);
            case Rule.Or(var left, var right) -> evaluate(left, customer) || evaluate(right, customer);
            case Rule.Not(var inner) -> !evaluate(inner, customer);
        };
    }
}

// 사용: 규칙을 데이터로 조합
Rule vipInKorea = new Rule.And(
    new Rule.Equals("country", "KR"),
    new Rule.Equals("type", "VIP")
);

// 규칙 추가는 데이터 추가만! 코드 배포 불필요
List<Rule> rules = ruleRepository.loadAll();
boolean eligible = rules.stream().anyMatch(r -> RuleEngine.evaluate(r, customer));
```
- **의도 및 코드 설명**: 규칙을 sealed interface의 record로 정의하고, evaluate 함수가 패턴 매칭으로 해석. 규칙 추가는 DB에 데이터를 넣는 것으로 완료
- **무엇이 좋아지나**:
  - 규칙 변경 시 코드 배포 불필요
  - 비개발자가 관리자 UI로 규칙 편집 가능
  - 모든 규칙이 한 곳(DB/JSON)에서 중앙 관리
  - 규칙 조합이 타입 안전하게 표현됨
  - 규칙의 직렬화/역직렬화 가능

### 이해를 위한 부가 상세
이 패턴은 GoF의 Interpreter Pattern을 함수형으로 재해석한 것이다. 전통적인 Interpreter Pattern은 클래스 상속으로 구현하지만, DOP에서는 sealed interface + pattern matching으로 구현하여 코드가 훨씬 간결하고 안전하다.

### 틀리기/놓치기 쉬운 부분
- Rule sealed interface에 새 record를 추가하면, evaluate의 switch에서 컴파일 에러 발생 (망라성 검증)
- 규칙 데이터를 외부에서 로딩할 때 악의적 입력에 대한 검증 필요 (무한 재귀 방지 등)
- Rule의 attribute는 문자열이므로 오타 시 런타임에서야 발견됨. enum이나 상수로 정의하면 더 안전

### 꼭 기억할 것
- 자주 변경되는 비즈니스 규칙 -> 데이터화 대상
- sealed interface = 규칙의 문법, evaluate = 규칙의 해석기
- 규칙 정의(what)와 규칙 실행(how)을 분리

---

## 2. DSL (Domain-Specific Language) with Records

### 핵심 개념
- **관련 키워드**: DSL, Internal DSL, Fluent API, Builder Pattern, Compositional, Record
- **통찰**: Record 조합으로 도메인 전용 미니 언어(DSL)를 만들면, 비즈니스 규칙을 도메인 용어로 읽을 수 있는 선언적 코드가 된다.
- **설명**: Rule record들의 중첩 조합은 그 자체로 도메인 전용 언어(DSL)를 형성한다. `new And(new Equals(...), new Or(...))` 같은 코드는 마치 "A이면서 (B이거나 C)"라는 자연어에 가까운 표현이 된다.

  이 DSL은 컴파일 타임에 타입 검증이 이루어지므로, 문법적으로 잘못된 규칙은 작성 자체가 불가능하다. 또한 정적 팩토리 메서드나 빌더를 추가하면 더욱 읽기 좋은 fluent API를 구축할 수 있다.

  핵심: Record의 중첩 조합 = 구문 트리(AST) = DSL의 프로그램

```
RULE DSL AS SYNTAX TREE (AST)
================================

Business Rule:
  "Korean customer AND (VIP OR totalSpend >= 1000000)"

DSL Code:
  and(equals("country","KR"), or(equals("type","VIP"), gte("totalSpend",1000000)))

Syntax Tree:
             And
            /   \
     Equals      Or
     /   \      /   \
  "country" "KR"  Equals   GTE
                  /   \     /  \
              "type" "VIP" "totalSpend" 1000000
```

### 개념이 아닌 것
- **"DSL = 새 프로그래밍 언어"**: 여기서의 DSL은 Java 내부에서 Record 조합으로 만드는 Internal DSL
- **"DSL은 항상 복잡"**: sealed interface + record 몇 개로 간단하게 시작 가능

### Before: Traditional OOP
```java
// [X] Specification 패턴 - 클래스 폭발, 읽기 어려움
public interface Specification<T> {
    boolean isSatisfied(T target);
}

public class CountrySpec implements Specification<Customer> {
    private final String country;
    public CountrySpec(String country) { this.country = country; }
    public boolean isSatisfied(Customer c) { return c.getCountry().equals(country); }
}

public class AndSpec<T> implements Specification<T> {
    private final Specification<T> left, right;
    public AndSpec(Specification<T> left, Specification<T> right) {
        this.left = left; this.right = right;
    }
    public boolean isSatisfied(T target) {
        return left.isSatisfied(target) && right.isSatisfied(target);
    }
}
// ... OrSpec, NotSpec, TypeSpec, TotalSpendSpec 등 클래스가 계속 증가
```
- **의도 및 코드 설명**: 전통적인 Specification 패턴으로 규칙을 조합. 각 조건마다 별도 클래스 필요
- **뭐가 문제인가**:
  - 클래스 개수 폭발 (조건마다 1클래스)
  - 보일러플레이트 코드 과다 (생성자, 필드, 메서드)
  - 규칙 조합 시 코드가 장황하고 읽기 어려움
  - sealed 되지 않아 망라성 검증 불가

### After: Modern Approach
```java
// [O] Record DSL - 간결하고 타입 안전한 규칙 표현
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record GTE(String attribute, int threshold) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}

    // Fluent API (정적 팩토리 메서드)
    static Rule equals(String attr, String value) { return new Equals(attr, value); }
    static Rule gte(String attr, int threshold) { return new GTE(attr, threshold); }
    static Rule and(Rule left, Rule right) { return new And(left, right); }
    static Rule or(Rule left, Rule right) { return new Or(left, right); }
    static Rule not(Rule rule) { return new Not(rule); }
}

// 도메인 전용 DSL로 읽기 좋은 규칙 정의
Rule krVipOrHighSpender = Rule.and(
    Rule.equals("country", "KR"),
    Rule.or(
        Rule.equals("type", "VIP"),
        Rule.gte("totalSpend", 1_000_000)
    )
);

// 의미: "한국 고객 중 VIP이거나 100만원 이상 구매"
```
- **의도 및 코드 설명**: sealed interface에 정적 팩토리 메서드를 추가하여 fluent API를 구성. 규칙이 도메인 용어로 읽힘
- **무엇이 좋아지나**:
  - 5개 record로 무한한 규칙 조합 표현 가능
  - 규칙이 비즈니스 언어에 가깝게 읽힘
  - 컴파일 타임 타입 검증 (잘못된 조합 불가)
  - sealed 특성으로 망라성 보장
  - 보일러플레이트 최소화

### 틀리기/놓치기 쉬운 부분
- Record의 중첩이 깊어지면 가독성이 떨어질 수 있다. 중간 변수로 분리하거나, 도메인별 복합 규칙을 별도로 정의하라
- 정적 팩토리 메서드의 이름을 도메인 용어에 맞추면 더 좋다 (예: `customerFrom("KR")`, `spentAtLeast(1000000)`)

### 꼭 기억할 것
- Record 조합 = AST = 내부 DSL
- 정적 팩토리 메서드로 fluent API 구축
- 5개 Rule record로 AND/OR/NOT/비교 조합 가능

---

## 3. Rule Engine Pattern

### 핵심 개념
- **관련 키워드**: Rule Engine, Evaluate, Pattern Matching, Recursive, Exhaustive Switch, Pure Function
- **통찰**: Rule Engine은 규칙 데이터를 입력받아 컨텍스트(Customer 등)에 대해 평가하는 순수 함수이다.
- **설명**: Rule Engine의 핵심은 `evaluate(Rule, Context) -> boolean` 형태의 순수 함수이다. 이 함수는 패턴 매칭(switch expression)으로 각 Rule 타입에 맞는 평가 로직을 수행하며, And/Or/Not 같은 복합 규칙은 재귀적으로 평가된다.

  Rule Engine이 순수 함수인 것이 핵심이다. 외부 상태에 의존하지 않으므로 테스트가 매우 쉽고, 규칙 데이터와 컨텍스트 데이터만 주면 결과가 결정된다.

  sealed interface의 망라성 덕분에, 새로운 Rule record를 추가하면 evaluate에서 컴파일 에러가 발생하여 처리 누락을 방지한다.

```
RULE ENGINE EVALUATION FLOW
==============================

Input:                         Output:
+----------+  +--------+      +-------+
| Rule     |  | Context|  --> | true/ |
| (data)   |  | (data) |      | false |
+----------+  +--------+      +-------+

Recursive Evaluation:
  evaluate(And(Equals("country","KR"), GTE("spend",1M)), customer)
       |
       +-> evaluate(Equals("country","KR"), customer) = true
       |
       +-> evaluate(GTE("spend",1M), customer) = true
       |
       +-> true && true = true
```

### 개념이 아닌 것
- **"Rule Engine = 복잡한 프레임워크"**: switch + 재귀 호출만으로 구현 가능
- **"Rule Engine은 성능이 나쁘다"**: 재귀 깊이가 보통 5-10 수준이므로 성능 문제 없음

### Before: Traditional OOP
```java
// [X] 전략 패턴 기반 - 실행과 정의가 혼재
public interface DiscountRule {
    boolean matches(Customer customer);
    Money calculateDiscount(Money total);
}

public class VipDiscountRule implements DiscountRule {
    public boolean matches(Customer c) { return "VIP".equals(c.getType()); }
    public Money calculateDiscount(Money total) { return total.multiply(0.1); }
}

public class DiscountEngine {
    private final List<DiscountRule> rules;
    public Money applyBestDiscount(Customer c, Money total) {
        return rules.stream()
            .filter(r -> r.matches(c))
            .map(r -> r.calculateDiscount(total))
            .max(Comparator.naturalOrder())
            .orElse(Money.ZERO);
    }
}
// 새 규칙 = 새 클래스 + 배포
```
- **의도 및 코드 설명**: 전략 패턴으로 규칙을 인터페이스 구현체로 정의. 규칙 추가 시 새 클래스 필요
- **뭐가 문제인가**:
  - 규칙 추가마다 새 클래스 작성 + 배포
  - 규칙의 "조건"과 "행동"이 하나의 클래스에 결합
  - 규칙 조합(AND/OR)을 위한 별도 메커니즘 필요
  - 규칙을 외부에서 동적으로 로딩 불가

### After: Modern Approach
```java
// [O] 순수 함수 기반 Rule Engine - 재귀적 평가
public record Customer(String country, String type, int totalSpend) {}

public class RuleEngine {
    // 순수 함수: 규칙 + 컨텍스트 -> 결과
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var attr, var value) -> switch (attr) {
                case "country" -> customer.country().equals(value);
                case "type" -> customer.type().equals(value);
                default -> false;
            };
            case Rule.GTE(var attr, var threshold) -> switch (attr) {
                case "totalSpend" -> customer.totalSpend() >= threshold;
                default -> false;
            };
            // 재귀적 평가
            case Rule.And(var left, var right) ->
                evaluate(left, customer) && evaluate(right, customer);
            case Rule.Or(var left, var right) ->
                evaluate(left, customer) || evaluate(right, customer);
            case Rule.Not(var inner) ->
                !evaluate(inner, customer);
        };
    }
}

// 테스트: 순수 함수이므로 Mock 불필요
@Test
void koreanVipGetsDiscount() {
    Rule rule = Rule.and(Rule.equals("country", "KR"), Rule.equals("type", "VIP"));
    Customer customer = new Customer("KR", "VIP", 500_000);
    assertTrue(RuleEngine.evaluate(rule, customer));
}
```
- **의도 및 코드 설명**: evaluate는 Rule의 각 case를 패턴 매칭으로 처리하며, 복합 규칙은 재귀 호출. 순수 함수이므로 입력만으로 결과 결정
- **무엇이 좋아지나**:
  - 규칙 추가는 sealed interface에 record 추가 + switch에 case 추가 (단순)
  - 규칙 조합이 재귀적으로 자유자재
  - 순수 함수이므로 Mock 없는 단위 테스트
  - sealed interface 망라성으로 처리 누락 방지
  - 규칙 정의와 실행 완전 분리

### 틀리기/놓치기 쉬운 부분
- And의 short-circuit 평가: 왼쪽이 false면 오른쪽은 평가하지 않음 (Java &&의 동작)
- default -> false 패턴은 알 수 없는 attribute에 대한 방어이지만, 가능하면 enum으로 attribute를 정의하여 컴파일 타임 검증 권장
- Rule 트리가 무한 재귀(순환 참조)를 가지면 StackOverflow 발생. 외부 로딩 시 깊이 제한 필요

### 꼭 기억할 것
- evaluate = 순수 함수 (Rule + Context -> boolean)
- 복합 규칙은 재귀로 자연스럽게 처리
- sealed interface + switch = 망라적 평가 보장

---

## 4. Dynamic Rule Evaluation (동적 규칙 평가)

### 핵심 개념
- **관련 키워드**: Dynamic Loading, JSON Deserialization, DB Storage, Hot Reload, Admin UI
- **통찰**: 규칙이 데이터이므로 JSON/DB에 저장하고 런타임에 동적으로 로딩하여, 코드 배포 없이 규칙을 변경할 수 있다.
- **설명**: Rule as Data의 핵심 가치는 규칙을 외부 저장소(DB, JSON, YAML)에 보관하고 런타임에 동적으로 로딩할 수 있다는 점이다. 이를 통해:
  - 마케팅 팀이 관리자 UI에서 할인 규칙을 직접 편집
  - 운영 팀이 사기 탐지 규칙을 실시간 조정
  - A/B 테스트로 다른 규칙 세트를 동시에 적용

  JSON으로 표현된 규칙은 Jackson 등의 라이브러리로 역직렬화하여 Rule ADT로 변환한다. 이 과정에서 타입 검증을 거치므로, 잘못된 형식의 규칙은 로딩 시점에 거부된다.

```
DYNAMIC RULE LOADING ARCHITECTURE
====================================

  Admin UI            DB / JSON
  +--------+         +----------+
  | Rule   |  save   | {"type": |
  | Editor | ------> |  "and",  |
  +--------+         |  ...}    |
                     +----+-----+
                          |
                     load |
                          v
                     +----------+
                     | Rule     |
                     | Loader   |
                     | (deser)  |
                     +----+-----+
                          |
                          v
                     +----------+       +---------+
                     | Rule     |       |Customer |
                     | Engine   | <---- | data    |
                     | evaluate |       +---------+
                     +----+-----+
                          |
                          v
                     true / false
```

### 개념이 아닌 것
- **"동적 로딩 = eval() 같은 코드 실행"**: ADT로 변환된 데이터를 해석하는 것이지, 임의의 코드를 실행하는 것이 아님 (보안 안전)
- **"모든 규칙을 DB에"**: 시스템 핵심 규칙(결제 검증 등)은 코드에 유지하고, 마케팅/프로모션 규칙만 외부화 권장

### Before: Traditional OOP
```java
// [X] 하드코딩 + 배포 의존 - 변경마다 개발팀 투입
public class PromotionService {
    // 프로모션 규칙 변경 = 코드 수정 + PR + 코드리뷰 + 빌드 + 배포
    public boolean isPromotionTarget(Customer customer) {
        // 2024 여름 프로모션
        if (customer.getJoinDate().isAfter(LocalDate.of(2024, 6, 1))
            && customer.getTotalSpend() >= 50000) {
            return true;
        }
        // 2024 겨울 프로모션 추가! -> 또 배포...
        return false;
    }
}
```
- **의도 및 코드 설명**: 프로모션 규칙이 코드에 하드코딩되어 변경 시 전체 개발 프로세스 필요
- **뭐가 문제인가**:
  - 규칙 변경 리드타임: 요청 -> 개발 -> 리뷰 -> 배포 (수일)
  - 마케팅 팀이 직접 변경 불가
  - 긴급 프로모션 대응 불가
  - 과거 프로모션 코드가 데드코드로 남음

### After: Modern Approach
```java
// [O] 규칙을 JSON/DB에서 동적 로딩
// 1. JSON 형태의 규칙 저장
// {
//   "type": "and",
//   "left": {"type": "equals", "attribute": "country", "value": "KR"},
//   "right": {"type": "gte", "attribute": "totalSpend", "threshold": 500000}
// }

// 2. JSON -> Rule ADT 변환
public class RuleLoader {
    public static Rule fromJson(JsonNode node) {
        return switch (node.get("type").asText()) {
            case "equals" -> new Rule.Equals(
                node.get("attribute").asText(),
                node.get("value").asText()
            );
            case "gte" -> new Rule.GTE(
                node.get("attribute").asText(),
                node.get("threshold").asInt()
            );
            case "and" -> new Rule.And(
                fromJson(node.get("left")),
                fromJson(node.get("right"))
            );
            case "or" -> new Rule.Or(
                fromJson(node.get("left")),
                fromJson(node.get("right"))
            );
            case "not" -> new Rule.Not(
                fromJson(node.get("rule"))
            );
            default -> throw new IllegalArgumentException(
                "Unknown rule type: " + node.get("type"));
        };
    }
}

// 3. 동적 규칙 평가
public class PromotionService {
    private final RuleRepository ruleRepository;

    public boolean isPromotionTarget(Customer customer) {
        List<Rule> rules = ruleRepository.findActiveRules("promotion");
        return rules.stream()
            .anyMatch(rule -> RuleEngine.evaluate(rule, customer));
    }
}
// 마케팅 팀이 Admin UI에서 규칙 수정 -> 즉시 적용!
```
- **의도 및 코드 설명**: JSON을 Rule ADT로 역직렬화하는 RuleLoader와, DB에서 규칙을 로딩하여 평가하는 서비스. 규칙 변경은 DB 업데이트만으로 완료
- **무엇이 좋아지나**:
  - 규칙 변경 리드타임: 관리자 UI에서 즉시 (수분)
  - 마케팅/운영 팀이 직접 규칙 관리
  - 코드 배포 없이 규칙 추가/수정/삭제
  - 규칙 이력 관리 (DB 히스토리)
  - A/B 테스트용 규칙 세트 관리 가능

### 이해를 위한 부가 상세
실무에서는 규칙 캐싱(인메모리 캐시 + TTL)을 적용하여 매 요청마다 DB를 조회하지 않도록 한다. 규칙이 변경되면 캐시를 무효화하거나, 이벤트 기반으로 캐시를 갱신한다.

### 틀리기/놓치기 쉬운 부분
- JSON에서 Rule로 역직렬화 시 "type" 필드의 오타나 미지원 타입에 대한 예외 처리 필요
- 재귀적 fromJson 호출에서 순환 참조 방지를 위한 깊이 제한 구현
- 규칙 DB에 버전 관리를 추가하면 롤백과 A/B 테스트가 용이

### 꼭 기억할 것
- 규칙 데이터화 = JSON/DB 저장 가능 = 동적 로딩 가능
- RuleLoader: JSON -> Rule ADT 변환기
- 시스템 핵심 규칙은 코드에, 변동성 높은 비즈니스 규칙만 외부화

---

## 5. Separation of Rule Definition from Execution (규칙 정의와 실행의 분리)

### 핵심 개념
- **관련 키워드**: Separation of Concerns, CQRS, Data vs Behavior, Interpreter, Reusability
- **통찰**: 규칙의 "정의(what)"와 "실행(how)"을 분리하면, 동일한 규칙을 다양한 방식으로 해석하거나 다양한 컨텍스트에 적용할 수 있다.
- **설명**: Rule as Data 패턴의 핵심 설계 원칙은 규칙의 정의와 실행을 완전히 분리하는 것이다. Rule(sealed interface)은 "무엇을 검사할지"만 기술하고, Interpreter(evaluate 함수)가 "어떻게 검사할지"를 결정한다.

  이 분리 덕분에:
  - 동일한 Rule을 Customer, Order, Product 등 다양한 컨텍스트에 적용 가능
  - Rule을 직접 평가하는 대신, 설명 문자열로 변환하거나, SQL WHERE절로 변환하는 등 다양한 Interpreter 구현 가능
  - Rule의 테스트와 Interpreter의 테스트를 독립적으로 수행 가능

```
MULTIPLE INTERPRETERS FOR SAME RULE
======================================

            +----------+
            |   Rule   |  (definition: WHAT)
            |   Data   |
            +----+-----+
                 |
        +--------+--------+
        |        |        |
        v        v        v
  +---------+ +-------+ +--------+
  |Evaluate | |Explain| |to SQL  |  (execution: HOW)
  |Engine   | |Engine | |Engine  |
  +---------+ +-------+ +--------+
       |          |          |
       v          v          v
   true/false  "Korean   "WHERE country
               AND VIP"   = 'KR' AND
                           type = 'VIP'"
```

### 개념이 아닌 것
- **"분리 = 성능 오버헤드"**: 간접 참조 한 단계 추가일 뿐, 실질적 성능 차이 무시 가능
- **"항상 여러 Interpreter가 필요"**: 하나만 있어도 규칙 재사용성과 테스트 용이성이 향상됨

### Before: Traditional OOP
```java
// [X] 규칙 정의와 실행이 결합된 전략 패턴
public class KoreanVipRule {
    // 규칙 정의 + 실행이 한 클래스에 결합
    public boolean check(Customer customer) {
        return "KR".equals(customer.getCountry())
            && "VIP".equals(customer.getType());
    }

    // 규칙을 설명하려면 또 다른 메서드 필요
    public String describe() {
        return "한국 VIP 고객";
    }

    // SQL로 변환하려면 또 다른 메서드 필요
    public String toSql() {
        return "country = 'KR' AND type = 'VIP'";
    }
}
// 규칙마다 check, describe, toSql을 모두 구현해야 함
// 새 해석 방식 추가 시 모든 규칙 클래스 수정 필요!
```
- **의도 및 코드 설명**: 규칙 클래스가 평가, 설명, SQL 변환 등 모든 책임을 가짐
- **뭐가 문제인가**:
  - 새 해석 방식(예: toElasticsearch) 추가 시 모든 규칙 클래스 수정
  - 규칙 데이터를 외부에서 로딩해도 check/describe/toSql이 코드에 묶여있음
  - 단일 책임 원칙(SRP) 위반: 규칙 클래스가 너무 많은 역할
  - 규칙 재사용 불가 (다른 도메인에 적용하려면 새 클래스 필요)

### After: Modern Approach
```java
// [O] 규칙 정의와 실행 완전 분리
// 1. 규칙 정의 (WHAT) - 순수 데이터
public sealed interface Rule {
    record Equals(String attribute, String value) implements Rule {}
    record GTE(String attribute, int threshold) implements Rule {}
    record And(Rule left, Rule right) implements Rule {}
    record Or(Rule left, Rule right) implements Rule {}
    record Not(Rule rule) implements Rule {}
}

// 2. 해석기 1: 평가 (boolean 결과)
public class RuleEvaluator {
    public static boolean evaluate(Rule rule, Customer customer) {
        return switch (rule) {
            case Rule.Equals(var a, var v) -> getAttr(customer, a).equals(v);
            case Rule.GTE(var a, var t) -> getIntAttr(customer, a) >= t;
            case Rule.And(var l, var r) -> evaluate(l, customer) && evaluate(r, customer);
            case Rule.Or(var l, var r) -> evaluate(l, customer) || evaluate(r, customer);
            case Rule.Not(var inner) -> !evaluate(inner, customer);
        };
    }
}

// 3. 해석기 2: 사람이 읽을 수 있는 설명
public class RuleExplainer {
    public static String explain(Rule rule) {
        return switch (rule) {
            case Rule.Equals(var a, var v) -> a + " = '" + v + "'";
            case Rule.GTE(var a, var t) -> a + " >= " + t;
            case Rule.And(var l, var r) -> "(" + explain(l) + " AND " + explain(r) + ")";
            case Rule.Or(var l, var r) -> "(" + explain(l) + " OR " + explain(r) + ")";
            case Rule.Not(var inner) -> "NOT(" + explain(inner) + ")";
        };
    }
}

// 4. 해석기 3: SQL WHERE절 생성
public class RuleToSql {
    public static String toWhere(Rule rule) {
        return switch (rule) {
            case Rule.Equals(var a, var v) -> a + " = '" + v + "'";
            case Rule.GTE(var a, var t) -> a + " >= " + t;
            case Rule.And(var l, var r) -> toWhere(l) + " AND " + toWhere(r);
            case Rule.Or(var l, var r) -> "(" + toWhere(l) + " OR " + toWhere(r) + ")";
            case Rule.Not(var inner) -> "NOT(" + toWhere(inner) + ")";
        };
    }
}

// 하나의 Rule을 다양한 방식으로 해석
Rule rule = Rule.and(Rule.equals("country", "KR"), Rule.gte("totalSpend", 1_000_000));

boolean result = RuleEvaluator.evaluate(rule, customer);    // true/false
String desc = RuleExplainer.explain(rule);                  // "(country = 'KR' AND totalSpend >= 1000000)"
String sql = RuleToSql.toWhere(rule);                       // "country = 'KR' AND totalSpend >= 1000000"
```
- **의도 및 코드 설명**: Rule은 순수 데이터, 각 해석기는 독립적인 순수 함수. 새 해석 방식은 새 클래스 추가만으로 가능
- **무엇이 좋아지나**:
  - 새 해석기 추가 시 기존 코드 수정 없음 (Open-Closed Principle)
  - Rule 데이터를 다양한 컨텍스트에서 재사용
  - 각 해석기를 독립적으로 테스트 가능
  - 관심사 분리가 명확 (정의 vs 실행)
  - SQL 변환으로 DB 레벨 필터링까지 가능

### 이해를 위한 부가 상세
이 패턴은 함수형 프로그래밍의 **Free Monad** 또는 **Tagless Final** 패턴의 단순화된 버전이다. 규칙을 ADT로 표현하고 해석기를 분리하는 것은 "프로그램(규칙)을 데이터로, 실행을 별도로"라는 함수형 핵심 원리의 구현이다.

Expression Problem의 관점에서, sealed interface + external interpreter 조합은 "새 해석기 추가가 쉽고, 새 Rule 추가 시 모든 해석기에 case 추가 필요" 트레이드오프를 가진다. 하지만 sealed interface의 망라성 덕분에 누락을 컴파일러가 잡아준다.

### 틀리기/놓치기 쉬운 부분
- 새 Rule record 추가 시 모든 Interpreter의 switch에서 컴파일 에러 발생 (장점이자 주의점)
- RuleToSql에서 SQL Injection 방지를 위해 PreparedStatement 파라미터화 필요 (위 예시는 교육용)
- 너무 많은 Interpreter를 만들면 Rule 변경 시 수정 범위가 넓어지므로, 실제 필요한 것만 구현

### 꼭 기억할 것
- Rule = What (순수 데이터), Interpreter = How (순수 함수)
- 동일 Rule에 여러 Interpreter 적용 가능 (evaluate, explain, toSql 등)
- 새 Interpreter는 기존 코드 수정 없이 추가 (OCP 준수)
- sealed interface 망라성이 새 Rule 추가 시 Interpreter 업데이트를 강제
