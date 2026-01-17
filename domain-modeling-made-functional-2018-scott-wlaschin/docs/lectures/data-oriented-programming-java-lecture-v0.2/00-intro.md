# Java로 정복하는 데이터 지향 프로그래밍 v0.2
## 참조: Data-Oriented Programming in Java

**버전**: 0.2 (Enhanced Edition)

**대상**: 백엔드 자바 개발자

**목표**: 복잡성을 수학적으로 제어하고, 컴파일러에게 검증을 위임하는 견고한 시스템 구축

**도구**: Java 25 (Record, Sealed Interface, Pattern Matching, Record Patterns)

**원전**: *Data-Oriented Programming in Java* by Chris Kiehl

---

## Java 17 → Java 25 DOP 관련 변경 사항

| 버전 | 핵심 기능 | JEP | 상태 | DOP 영향 |
|------|----------|-----|------|---------|
| **Java 21** | Record Patterns | 440 | Final | ⭐ 핵심 - 패턴에서 레코드 분해 |
| **Java 21** | Pattern Matching for switch | 441 | Final | ⭐ 핵심 - switch 표현식 완성 |
| **Java 21** | Sequenced Collections | 431 | Final | 유용 - `getFirst()`, `getLast()` |
| **Java 22** | Unnamed Variables & Patterns (`_`) | 456 | Final | 중요 - 사용하지 않는 변수 표시 |
| **Java 25** | Primitive Types in Patterns | 507 | Preview | 보통 - 기본 타입 패턴 매칭 |
| **Java 25** | Scoped Values | 506 | Final | 보통 - 불변 컨텍스트 전달 |

> ⚠️ **중요**: JEP 468 (Derived Record Creation / `with` expression)은 Java 25에 **포함되지 않았습니다**.
> 따라서 Record의 값을 변경한 새 객체를 만들려면 수동 `withXxx()` 메서드가 여전히 필요합니다.

---

## 전체 목차 (Map)

### Part I: 사고의 전환 (Foundations)
- **Chapter 1: 객체 지향의 환상과 데이터의 실체**
  - OOP 캡슐화의 문제점(God Class)과 DOP 4대 원칙(코드/데이터 분리, 일반적 표현, 불변성, 스키마/표현 분리) 소개.
- **Chapter 2: 데이터란 무엇인가? (Identity vs Value)**
  - 정체성(Identity)과 값(Value)의 차이, Java Record를 활용한 Value Type 모델링, 얕은 불변성과 깊은 불변성.

### Part II: 데이터 모델링의 수학 (Algebraic Data Types)
- **Chapter 3: 타입 시스템의 기수(Cardinality) 이론**
  - 시스템 복잡도와 기수의 관계, 곱 타입(Product Type)의 상태 폭발 문제와 합 타입(Sum Type/Sealed Interface)을 통한 해결.
- **Chapter 4: 불가능한 상태를 표현 불가능하게 만들기**
  - 런타임 검증 대신 컴파일 타임에 불가능한 상태를 제거하는 설계, 망라성(Exhaustiveness) 체크 활용.

### Part III: 데이터의 흐름과 제어 (Behavior & Control Flow)
- **Chapter 5: 전체 함수(Total Functions)와 실패 처리**
  - 부분 함수(예외 발생)의 위험성과 전체 함수(Result 반환)의 안정성, "Failure as Data" 패턴.
- **Chapter 6: 파이프라인과 결정론적 시스템**
  - 결정론적 순수 함수의 장점, I/O와 로직을 분리하는 샌드위치 아키텍처(Impure-Pure-Impure).

### Part IV: 고급 기법과 대수적 추론 (Advanced Theory)
- **Chapter 7: 대수적 속성을 활용한 설계**
  - 결합법칙, 멱등성, 항등원 등 수학적 속성을 활용한 병렬 처리 및 재시도 안전성 확보.
- **Chapter 8: 인터프리터 패턴: Rule as Data**
  - 비즈니스 규칙을 코드가 아닌 데이터로 표현하여 유연성과 동적 변경 가능성 확보.

### Part V: 레거시 탈출 (Refactoring & Architecture)
- **Chapter 9: 현실 세계의 DOP: JPA/Spring과의 공존**
  - JPA Entity(Identity/Mutable)와 Domain Record(Value/Immutable)의 공존 및 변환 전략, 점진적 리팩토링 가이드.

### Appendix
- **Appendix A**: 흔한 실수와 안티패턴 모음
- **Appendix B**: DOP Java 치트시트
- **Appendix C**: 전체 퀴즈 정답 및 해설
- **Appendix D**: Final Boss Quiz
