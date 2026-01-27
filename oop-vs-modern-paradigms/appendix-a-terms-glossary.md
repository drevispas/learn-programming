# Appendix A: Terms Glossary (용어 사전)

> 두 원서(DMMF: Domain Modeling Made Functional, DOP: Data-Oriented Programming in Java)에서 다루는 핵심 용어를 알파벳순으로 정리한 사전이다. 각 용어의 한국어 번역, 간략한 정의, 관련 토픽을 포함한다.

---

**[표 A.1]** Appendix A: Terms Glossary (용어 사전)
| English Term | Korean Translation | Brief Definition | Topic |
|---|---|---|---|
| Abstract Data Type | 추상 데이터 타입 | 데이터와 그 연산을 함께 정의하는 타입 (ADT와 구분 필요) | 03 |
| Accumulator | 누적기 | reduce/fold에서 중간 결과를 저장하는 값 | 11 |
| Aggregate Reconstitution | 애그리거트 재구성 | 이벤트 스트림을 fold하여 현재 상태를 계산하는 과정 | 14 |
| ADT (Algebraic Data Type) | 대수적 데이터 타입 | Sum Type + Product Type의 조합으로 도메인 모델을 표현하는 타입 체계 | 03 |
| Aggregate | 애그리거트 | 트랜잭션 일관성의 경계를 형성하는 도메인 객체 클러스터 | 13 |
| Anti-Corruption Layer (ACL) | 반부패 계층 | 외부 시스템의 모델이 도메인을 오염시키지 않도록 변환하는 계층 | 13 |
| Applicative | 어플리커티브 | 독립적인 컨테이너들을 결합하는 타입 클래스. Validation에 사용 | 08 |
| Associativity | 결합법칙 | (A+B)+C = A+(B+C). 연산의 그룹핑을 변경해도 결과 동일 | 11 |
| Bounded Context | 바운디드 컨텍스트 | 동일 용어가 다른 의미를 가질 수 있는 도메인의 경계 | 01, 13 |
| Builder Pattern | 빌더 패턴 | 복잡한 객체를 단계적으로 생성하는 생성 패턴 | 02 |
| Cardinality | 카디널리티 | 타입이 표현할 수 있는 가능한 값의 수 | 03 |
| Combiner | 결합기 | parallelStream의 reduce에서 부분 결과를 합치는 함수 | 11 |
| Command | 커맨드 | 시스템에 대한 상태 변경 요청을 나타내는 불변 객체 (명령형, 미래 시제) | 14 |
| Commutativity | 교환법칙 | A+B = B+A. 피연산자 순서를 바꿔도 결과 동일 | 11 |
| CQRS | 명령 쿼리 책임 분리 | Command(쓰기)와 Query(읽기) 모델을 분리하는 아키텍처 패턴 | 14 |
| Compact Constructor | 컴팩트 생성자 | Record에서 검증 로직을 넣는 축약 생성자 형태 | 02 |
| CRDT | 충돌 없는 복제 데이터 타입 | 분산 시스템에서 순서 무관하게 수렴하는 데이터 구조 | 11 |
| Curry (Currying) | 커링 | 다인자 함수를 단인자 함수의 연쇄로 변환하는 기법 | 06 |
| Decider | 디사이더 | (State, Command) -> Result<Events, Error> 형태의 순수 함수로 비즈니스 규칙 검증 | 14 |
| Defensive Copy | 방어적 복사 | 컬렉션 필드의 불변성을 보장하기 위해 복사본을 저장하는 패턴 | 02 |
| Dependency Inversion (DIP) | 의존성 역전 | 상위 모듈이 하위 모듈에 의존하지 않고 추상화에 의존하는 원칙 | 13 |
| DOP (Data-Oriented Programming) | 데이터 지향 프로그래밍 | 데이터와 로직을 분리하고, 불변 데이터를 다루는 프로그래밍 패러다임 | 01 |
| DMMF (Domain Modeling Made Functional) | 도메인 모델링을 함수형으로 | Scott Wlaschin의 저서. DDD + FP 접근법 | 01 |
| Domain Event | 도메인 이벤트 | 도메인에서 발생한 중요한 사건을 나타내는 불변 메시지 (과거 시제) | 07, 14 |
| DSL (Domain-Specific Language) | 도메인 전용 언어 | 특정 도메인 문제를 해결하기 위한 전용 미니 언어 | 12 |
| Entity (DDD) | 엔티티 | 고유 식별자로 구분되는 도메인 객체 (JPA Entity와 구분) | 01 |
| Entity (JPA) | JPA 엔티티 | @Entity 어노테이션으로 표시된 영속성 객체. 가변, Identity 기반 | 13 |
| Escape Analysis | 탈출 분석 | JIT 컴파일러가 객체의 스코프를 분석하여 스택 할당을 결정하는 기법 | Appendix C |
| Event Sourcing | 이벤트 소싱 | 상태를 직접 저장하는 대신 상태 변화 이벤트를 저장하는 패턴 | 14 |
| Event Store | 이벤트 저장소 | 이벤트를 append-only로 저장하는 영구 저장소 | 14 |
| Exhaustiveness | 망라성 | sealed interface의 switch에서 모든 케이스를 다뤄야 하는 속성 | 03 |
| Expression Problem | 표현 문제 | 기존 코드 수정 없이 새 타입과 새 연산을 동시에 추가하기 어려운 문제 | 12 |
| Factory Method | 팩토리 메서드 | 객체 생성을 캡슐화하는 정적 메서드 패턴 | 02 |
| Failure | 실패 | Result 타입에서 연산 실패를 나타내는 case | 06, 08 |
| flatMap | 플랫맵 | 중첩된 컨테이너를 평탄화하면서 변환하는 연산 (Monad의 핵심) | 06, 08 |
| Fluent API | 플루언트 API | 메서드 체이닝으로 읽기 좋은 코드를 만드는 API 설계 스타일 | 12 |
| Fold | 폴드 | 컬렉션을 하나의 값으로 축약하는 연산 (reduce와 유사) | 11 |
| Free Monad | 자유 모나드 | 프로그램을 데이터로 표현하고 해석을 분리하는 고급 FP 패턴 | 12 |
| Functional Core | 함수형 핵심 | 순수 함수로 구성된 비즈니스 로직 계층 | 06, 13 |
| Functor | 펑터 | map 연산을 지원하는 컨테이너 타입 클래스 | Appendix C |
| God Class | 신 클래스 | 너무 많은 책임을 가진 거대한 클래스 (안티패턴) | 01 |
| Guard Clause | 가드 절 | 메서드 시작 부분에서 전제 조건을 검증하는 패턴 | 04 |
| Hexagonal Architecture | 헥사고날 아키텍처 | 포트와 어댑터로 도메인을 외부에서 격리하는 아키텍처 | 13 |
| Idempotency | 멱등성 | f(f(x))=f(x). 동일 연산을 여러 번 적용해도 결과 동일 | 11 |
| Idempotency Key | 멱등성 키 | 중복 요청을 탐지하기 위한 고유 식별자 | 11 |
| Identity Element | 항등원 | A+e=A. 연산 결과가 원래 값 그대로인 특별한 원소 | 11 |
| Illegal State | 불가능한 상태 | 도메인 규칙상 존재할 수 없는 상태 조합 | 04 |
| Immutability | 불변성 | 객체 생성 후 상태를 변경할 수 없는 속성 | 01, 02 |
| Imperative Shell | 명령형 껍질 | I/O와 부수효과를 담당하는 외곽 계층 | 06, 13 |
| Interpreter Pattern | 인터프리터 패턴 | 규칙을 데이터로 표현하고 별도의 해석기가 실행하는 패턴 | 12 |
| Invariant | 불변 조건 | 항상 참이어야 하는 도메인 규칙 (compact constructor로 강제) | 02 |
| JEP 468 | JEP 468 | Derived Record Creation. with 구문 제안 (Java 25 미포함) | Appendix C |
| Lazy Loading | 지연 로딩 | JPA에서 연관 객체를 실제 접근 시점에 로딩하는 전략 | 13 |
| Local Reasoning | 지역적 추론 | 함수 내부만 보고 동작을 이해할 수 있는 속성 (순수 함수의 장점) | 06 |
| Make Illegal States Unrepresentable | 불가능한 상태를 표현 불가능하게 | 타입 시스템으로 도메인에서 불가능한 상태를 아예 만들 수 없게 하는 원칙 | 04 |
| map | 맵 | 컨테이너 안의 값을 변환하는 연산 (Functor의 핵심) | 06 |
| Mapper | 매퍼 | Entity와 Domain Record 간 변환을 담당하는 클래스 | 13 |
| MapReduce | 맵리듀스 | 대용량 데이터를 분할(map)하고 합산(reduce)하는 분산 처리 패턴 | 11 |
| Monad | 모나드 | flatMap을 가진 Functor. 순차적 의존 연산의 체이닝 지원 | 06, 08 |
| Monoid | 모노이드 | 결합법칙을 만족하는 이항 연산 + 항등원 | 11 |
| Non-sealed | 비봉인 | sealed 계층에서 하위 타입 제한을 해제하는 키워드 | 03 |
| Null Object Pattern | 널 객체 패턴 | null 대신 "아무것도 안 하는" 객체를 사용하는 패턴 | 03 |
| Optional | 옵셔널 | 값의 존재/부재를 타입으로 표현하는 컨테이너 | 05 |
| Partial Function | 부분 함수 | 일부 입력에 대해 정의되지 않는(예외를 던지는) 함수 | 05 |
| Pattern Matching | 패턴 매칭 | switch expression에서 타입과 구조를 동시에 검사하고 분해하는 기법 | 03 |
| Permits | 허용 | sealed interface에서 구현을 허용하는 타입을 명시하는 키워드 | 03 |
| Phantom Type | 팬텀 타입 | 런타임 데이터 없이 컴파일 타임 상태만 추적하는 타입 매개변수 | 04 |
| Pipeline | 파이프라인 | 함수들을 순차적으로 연결하여 데이터를 변환하는 패턴 | 06, 08 |
| Primitive Obsession | 원시 타입 집착 | String, int 등 원시 타입으로 도메인 값을 표현하는 안티패턴 | 02 |
| Product Type | 곱 타입 | 여러 필드를 AND로 결합한 타입 (record) | 03 |
| Projection | 프로젝션 | 이벤트 스트림을 읽기에 최적화된 별도 모델로 변환하는 과정 | 14 |
| Property-Based Testing | 속성 기반 테스트 | 무작위 입력으로 속성(법칙)을 검증하는 테스트 기법 | 13 |
| Pure Function | 순수 함수 | 동일 입력에 항상 동일 출력, 부수효과 없는 함수 | 06, 13 |
| Railway-Oriented Programming (ROP) | 철도 지향 프로그래밍 | Result/flatMap으로 성공/실패 경로를 분리하는 에러 처리 패턴 | 06, 08 |
| Record | 레코드 | Java 16+의 불변 데이터 클래스. 자동으로 equals, hashCode, toString 제공 | 01, 02 |
| Record Pattern | 레코드 패턴 | switch에서 Record의 필드를 분해하여 변수에 바인딩하는 패턴 | 03 |
| Reduce | 리듀스 | 컬렉션의 원소를 하나의 값으로 축약하는 연산 | 11 |
| Referential Transparency | 참조 투명성 | 표현식을 그 결과값으로 치환해도 프로그램 동작이 변하지 않는 속성 | 06 |
| Result | 결과 타입 | Success/Failure로 연산 결과를 타입으로 표현하는 sealed interface | 05, 06 |
| Rule Engine | 규칙 엔진 | 규칙 데이터를 입력받아 컨텍스트에 대해 평가하는 시스템 | 12 |
| Sealed Interface | 봉인된 인터페이스 | 구현 타입을 제한하여 망라적 패턴 매칭을 가능하게 하는 인터페이스 | 03 |
| Separation of Concerns | 관심사 분리 | 서로 다른 관심사를 독립적인 모듈로 분리하는 설계 원칙 | 12, 13 |
| Sequenced Collection | 순서 컬렉션 | Java 21+의 getFirst, getLast, reversed 등을 지원하는 컬렉션 | Appendix C |
| Short-circuit Evaluation | 단축 평가 | &&, ||에서 첫 피연산자로 결과가 결정되면 나머지를 평가하지 않는 전략 | 12 |
| Side Effect | 부수효과 | 함수 외부 상태를 변경하는 동작 (DB 저장, 출력 등) | 06 |
| Smart Constructor | 스마트 생성자 | 검증을 포함하여 유효한 객체만 생성하는 생성자 패턴 | 02 |
| Snapshot | 스냅샷 | 특정 시점의 상태를 저장하여 재구성 성능을 최적화하는 기법 | 14 |
| State Machine | 상태 기계 | 유한한 상태와 상태 간 전이를 정의하는 모델 | 04, 13 |
| Strangler Fig Pattern | 교살자 무화과 패턴 | 레거시 시스템을 점진적으로 새 시스템으로 교체하는 전략 | 13 |
| Structural Sharing | 구조적 공유 | 불변 객체 생성 시 변경되지 않은 부분을 참조로 재사용하는 기법 | Appendix C |
| Success | 성공 | Result 타입에서 연산 성공을 나타내는 case | 06, 08 |
| Sum Type | 합 타입 | 여러 가능한 형태 중 하나를 OR로 선택하는 타입 (sealed interface) | 03 |
| Switch Expression | 스위치 표현식 | 값을 반환하는 switch 문법 (Java 14+) | 03 |
| Total Function | 전체 함수 | 모든 입력에 대해 유효한 결과를 반환하는 함수 (예외 없음) | 05 |
| Ubiquitous Language | 유비쿼터스 언어 | 개발팀과 도메인 전문가가 공유하는 공통 언어 | 01 |
| Unnamed Pattern | 이름 없는 패턴 | Java 22+에서 사용하지 않는 변수를 _로 표기하는 패턴 | 03 |
| Validation | 검증 타입 | 모든 에러를 수집하는 Applicative 기반 검증 패턴 | 07, 08 |
| Value Object | 값 객체 | 식별자 없이 값으로만 동등성을 판단하는 도메인 객체 | 02 |
| Virtual Thread | 가상 스레드 | Java 21+의 경량 스레드. OS 스레드 대비 매우 적은 메모리 사용 | Appendix C |
| Wither | 위더 | Record에서 일부 필드만 변경한 새 인스턴스를 반환하는 메서드 패턴 | 02 |
| Workflow | 워크플로우 | 여러 도메인 연산을 순서대로 조합한 비즈니스 프로세스 | 06, 13 |
| ZGC | Z 가비지 컬렉터 | Java 21+의 저지연 GC. 힙 크기 무관하게 1ms 미만 정지 | Appendix C |

---

> 총 95개 용어. 각 Topic 번호는 해당 용어가 처음 또는 주로 다뤄지는 문서를 의미한다.
