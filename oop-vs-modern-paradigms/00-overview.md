# OOP vs Modern Paradigms: DDD+FP & DOP 비교 학습서

## 개요

이 문서는 두 권의 책에서 제시하는 현대적 프로그래밍 패러다임을 **Traditional OOP와의 Before/After 비교**를 통해 학습합니다.

**[표 00.1]** 개요
| 출처 | 원서 | 저자 | 핵심 접근 |
|------|------|------|-----------|
| DMMF | Domain Modeling Made Functional | Scott Wlaschin | DDD + FP (타입 주도 설계) |
| DOP | Data-Oriented Programming in Java | Chris Kiehl | DOP (데이터/코드 분리) |

모든 코드 예제는 **Java 25**를 기준으로 합니다.

---

## 패러다임 비교표

**[표 00.2]** 패러다임 비교표
| 관점 | Traditional OOP | DDD + FP (DMMF) | DOP |
|------|----------------|-----------------|-----|
| 기본 단위 | 객체 (데이터+행위) | 타입 (Value Object + 순수 함수) | 데이터(Record) + 함수(static) |
| 상태 관리 | 가변 (Mutable) | 불변 (Immutable) | 불변 (Immutable) |
| 캡슐화 | 데이터 은닉 (private) | 도메인 타입 래핑 | 데이터 노출 (투명성) |
| 다형성 | 상속 / 인터페이스 | Sealed + Pattern Matching | Sealed + Pattern Matching |
| 에러 처리 | 예외 (throw/catch) | Result + ROP | Result + Total Functions |
| 아키텍처 | 계층형 (Controller-Service-DAO) | Onion Architecture | Sandwich Architecture |
| 검증 | 방어적 코딩 (매번 검증) | Compact Constructor (생성 시 검증) | 경계에서 검증, 내부는 신뢰 |
| 워크플로우 | 절차적 메서드 호출 | 타입으로 단계 강제 | 파이프라인 합성 |
| 테스트 | Mock 객체 다수 필요 | 순수 함수 단위 테스트 | 순수 함수 단위 테스트 |

---

## 목차

### Part I: 기초와 사고 전환

- [01. DDD Fundamentals](01-ddd-fundamentals.md)
  Ubiquitous Language, Bounded Context, Aggregate, 도메인 이벤트

- [02. Foundations & Paradigm Shift](02-foundations-paradigm-shift.md)
  OOP의 한계, DDD 기초, DOP 4원칙, 패러다임 전환의 동기

- [03. Value Objects / Wrapped Types](03-value-objects-wrapped-types.md)
  Primitive Obsession 해결, Record 기반 Value Object, 유효성 검증

- [04. Algebraic Data Types](04-algebraic-data-types.md)
  곱 타입과 합 타입, Sealed Interface, 기수(Cardinality) 이론

- [05. Impossible States & Type Safety](05-impossible-states-type-safety.md)
  불가능한 상태 제거, 망라적 패턴 매칭, 컴파일 타임 안전성

### Part II: 에러 처리와 데이터 흐름

- [06. Error Handling & ROP](06-error-handling-rop.md)
  Railway-Oriented Programming, Result 타입, Total Functions

- [07. Pipeline & Workflow Composition](07-pipeline-workflow-composition.md)
  워크플로우 파이프라인, 함수 합성, 결정론적 시스템

- [08. Architecture & Domain Separation](08-architecture-domain-separation.md)
  Onion vs Sandwich Architecture, Functional Core / Imperative Shell

### Part III: 함수형 프로그래밍 심화

- [09. FP Fundamentals](09-fp-fundamentals.md)
  일급 함수, 고차 함수, 클로저, 커링

- [10. FP Containers](10-fp-containers.md)
  Functor, Monad, Applicative, 타입 클래스

- [11. FP Practical Patterns](11-fp-practical.md)
  실전 FP 패턴, 렌즈, 트랜스듀서, 메모이제이션

### Part IV: 고급 기법

- [12. Algebraic Properties](12-algebraic-properties.md)
  결합법칙, 멱등성, 항등원, 병렬 처리 안전성

- [13. Interpreter Pattern](13-interpreter-pattern.md)
  Rule as Data, 비즈니스 규칙의 데이터화, 동적 평가

- [14. Comprehensive Integration](14-comprehensive-integration.md)
  전체 패턴 통합, 실전 프로젝트, JPA/Spring 공존

- [15. Event Sourcing](15-event-sourcing.md)
  이벤트 저장소, Aggregate 재구성, Decider 패턴, CQRS

### 부록

- [Appendix A: Terms Glossary](appendix-a-terms-glossary.md)
  용어 사전 (한/영, 알파벳순)

- [Appendix B: Pattern Quick Reference](appendix-b-pattern-quick-reference.md)
  패턴 요약표 + 미니 코드

- [Appendix C: Java 25 Cheat Sheet](appendix-c-java25-cheat-sheet.md)
  Java 25 DOP/FP 관련 기능 정리

---

## DMMF vs DOP: 7가지 상충점 요약

두 책은 OOP 탈피라는 같은 방향을 지향하지만, 구체적 구현에서 차이가 있습니다.

**[표 00.3]** DMMF vs DOP: 7가지 상충점 요약
| # | 주제 | DMMF 입장 | DOP 입장 | 해당 토픽 |
|---|------|-----------|----------|-----------|
| 1 | Record 비즈니스 메서드 | 허용 (`Money.add()`) | 비허용 (Calculations 분리) | 03 |
| 2 | 캡슐화 vs 투명성 | 도메인별 타입 래핑 | 표준 Record + 표준 컬렉션 | 02, 03 |
| 3 | 스키마 위치 | 타입 안에 (compact constructor) | 스키마/데이터 분리 | 03, 05 |
| 4 | 검증 전략 | 모든 검증을 constructor에 | 불변식만 constructor, 나머지는 Validator | 03, 06 |
| 5 | 아키텍처 | Onion Architecture | Sandwich Architecture | 08 |
| 6 | 타입 철학 | 도메인 전용 VO 다수 생성 | 범용 Record + 표준 컬렉션 | 02, 03, 04 |
| 7 | OOP 태도 | "OOP를 FP로 강화" | "OOP 캡슐화를 근본 대체" | 02 |

---

## 학습 로드맵

**[그림 00.1]** 학습 로드맵
```
[01 DDD] --> [02 Foundations] --> [03 Value Objects] --> [04 ADT] --> [05 Impossible States]
                                                                            |
                                                                            v
[09 FP Basics] --> [10 FP Containers] --> [06 Error/ROP] --> [07 Pipeline]
                                                                            |
                                                                            v
[11 FP Practical] --> [12 Algebra] --> [13 Interpreter] --> [08 Architecture]
                                                                            |
                                                                            v
                                         [15 Event Sourcing] <-- [14 Integration]
```
