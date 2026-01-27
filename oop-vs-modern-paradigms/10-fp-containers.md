# 10. FP Containers (함수형 컨테이너)

> 값을 감싸는 컨테이너(Functor, Monad, Applicative)의 원리를 이해하고, Optional과 Result를 모나드적으로 활용하여 null 체크 지옥과 에러 처리 보일러플레이트를 제거한다.

---

## 1. Functor 패턴 (map으로 값 변환)

### 핵심 개념
- **관련 키워드**: Functor, map, thenApply, Identity Law, Composition Law
- **통찰**: Functor는 "상자를 열지 않고 내용물을 변환할 수 있는 컨테이너"이며, map 연산을 통해 컨텍스트를 유지한 채 값만 바꾼다.
- **설명**:
  Functor는 타입 생성자(`Optional<T>`, `Stream<T>`, `CompletableFuture<T>`)와 그에 대한 map 연산의 조합이다. 선물 상자 안의 내용물을 상자를 열지 않고 변환하는 것에 비유할 수 있다. 상자의 구조(Optional인지, List인지, Future인지)는 보존하면서 내부 값만 함수를 적용해 바꾼다.

  Functor의 핵심 가치는 통일된 인터페이스이다. Optional, Stream, CompletableFuture 모두 "내용물 변환"이라는 동일한 개념을 map으로 표현한다. 컨테이너 종류와 무관하게 동일한 사고방식을 적용할 수 있다.

  Functor가 되려면 두 가지 법칙을 만족해야 한다. 항등 법칙(`map(x -> x)`은 원본과 같아야 함)과 합성 법칙(`map(f).map(g)`는 `map(f.andThen(g))`와 같아야 함). 이 법칙들이 보장되면 리팩토링이 안전해지고, `map(f).map(g)`를 하나의 map으로 합쳐 성능을 최적화할 수 있다.

  Java에서 Functor에 해당하는 대표적인 타입들: Optional의 `.map()`, Stream의 `.map()`, CompletableFuture의 `.thenApply()`.

**[그림 10.1]** Functor 패턴 (map으로 값 변환)
```
+-------------------------------------------------------------------+
|                  Functor = map 가능한 컨테이너                       |
+-------------------------------------------------------------------+
|                                                                     |
|   +-------+     map(f)      +-------+                              |
|   | Box   |  ------------>  | Box   |                              |
|   |  [A]  |    내용만 변환   |  [B]  |                              |
|   +-------+                 +-------+                              |
|                                                                     |
|   법칙 1 (Identity):  box.map(x -> x) == box                       |
|   법칙 2 (Composition): box.map(f).map(g) == box.map(f.andThen(g)) |
|                                                                     |
|   Java의 Functor들:                                                 |
|   - Optional<T>.map(f)                                              |
|   - Stream<T>.map(f)                                                |
|   - CompletableFuture<T>.thenApply(f)                               |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **map() 메서드 자체가 Functor**: 아니다. Functor = 타입 생성자 + map 연산 + 법칙 만족의 조합이다.
- **Functor는 특별한 클래스**: Java에는 Functor 인터페이스가 없다. 개념적으로 Functor 패턴을 따르는 타입들이 존재할 뿐이다.
- **flatMap과 같은 것**: map은 중첩을 방지하지 않는다. `map`의 결과가 컨테이너이면 `Container<Container<T>>`가 된다.

### Before: Traditional OOP

**[코드 10.1]** Traditional OOP: null 체크를 수동으로 반복하며 변환 수행
```java
 1| // package: com.ecommerce.auth
 2| // [X] null 체크를 수동으로 반복하며 변환 수행
 3| public class UserService {
 4|   public String getUserDisplayName(Long userId) {
 5|     User user = userRepository.findById(userId);
 6|     if (user == null) {
 7|       return "Unknown";
 8|     }
 9|     String name = user.getName();
10|     if (name == null) {
11|       return "Unknown";
12|     }
13|     String trimmed = name.trim();
14|     if (trimmed.isEmpty()) {
15|       return "Unknown";
16|     }
17|     return trimmed.toUpperCase();
18|   }
19| }
```
- **의도 및 코드 설명**: 사용자 ID로 조회한 후 이름을 변환(trim, uppercase)한다. 각 단계에서 null 체크를 수행한다.
- **뭐가 문제인가**:
  - null 체크가 변환 로직을 가림 (본질적 로직 3줄, 방어 코드 6줄)
  - 체크를 하나라도 빠뜨리면 NullPointerException 발생
  - 새 변환 단계 추가 시 또 다시 null 체크 패턴 반복
  - "무엇을 하는지"보다 "방어 코드"가 먼저 눈에 들어옴

### After: Modern Approach

**[코드 10.2]** Modern: Functor(Optional)의 map으로 컨텍스트 유지하며 변환
```java
 1| // package: com.ecommerce.shared
 2| // [O] Functor(Optional)의 map으로 컨텍스트 유지하며 변환
 3| import java.util.Optional;
 4| 
 5| public class UserService {
 6|   public String getUserDisplayName(Long userId) {
 7|     return Optional.ofNullable(userRepository.findById(userId))
 8|       .map(User::getName)         // Optional<String>: null이면 빈 Optional 유지
 9|       .map(String::trim)          // Optional<String>: 빈 문자열은 그대로 통과
10|       .filter(name -> !name.isEmpty())  // 빈 문자열이면 Optional.empty()
11|       .map(String::toUpperCase)   // Optional<String>: 대문자 변환
12|       .orElse("Unknown");         // 최종: 값이 없으면 기본값
13|   }
14| }
```
- **의도 및 코드 설명**: Optional을 Functor로 활용하여 map 체이닝으로 변환한다. null 처리는 Optional 내부에서 자동으로 이루어진다.
- **무엇이 좋아지나**:
  - 변환 로직이 선형적으로 읽힘 (위에서 아래로 데이터 흐름)
  - null 체크가 자동화되어 빠뜨릴 가능성 없음
  - 새 변환 단계 추가는 `.map()` 한 줄 삽입으로 해결
  - Functor 합성 법칙 덕분에 여러 map을 하나로 합쳐 최적화 가능

### 이해를 위한 부가 상세
Functor 법칙을 코드로 검증:

**[코드 10.3]** Functor 패턴 (map으로 값 변환)
```java
 1| // package: com.ecommerce.shared
 2| Optional<Integer> box = Optional.of(5);
 3| Function<Integer, Integer> f = x -> x + 1;
 4| Function<Integer, Integer> g = x -> x * 2;
 5| 
 6| // Identity Law
 7| assert box.map(x -> x).equals(box);
 8| 
 9| // Composition Law
10| assert box.map(f).map(g).equals(box.map(f.andThen(g)));
```

### 틀리기/놓치기 쉬운 부분
- `Optional.of(null)`은 NullPointerException을 던진다. nullable 값에는 반드시 `Optional.ofNullable()`을 사용한다.
- `map`의 결과가 또 Optional이면 `Optional<Optional<T>>`가 된다. 이때는 flatMap을 사용해야 한다(다음 섹션).
- CompletableFuture의 Functor 연산은 `thenApply()`이다. `thenCompose()`는 Monad 연산(flatMap)에 해당한다.

### 꼭 기억할 것
Functor의 map은 "컨텍스트(Optional, Stream, Future)를 보존하면서 내용물만 변환"한다. 값이 없는 경우(empty, null)는 map이 자동으로 건너뛴다. 이것이 수동 null 체크를 대체하는 원리이다.

---

## 2. Monad 패턴 (flatMap으로 컨텍스트 연결)

### 핵심 개념
- **관련 키워드**: Monad, flatMap, bind, thenCompose, Railway-Oriented Programming
- **통찰**: Monad는 "값을 컨테이너로 감싸는 함수"들을 중첩 없이 연결하는 패턴이며, flatMap이 자동으로 중첩을 풀어준다.
- **설명**:
  map의 한계는 변환 함수 자체가 컨테이너를 반환할 때 드러난다. `Optional<User>`에 `findAddress`를 map하면 `Optional<Optional<Address>>`가 되어 중첩이 발생한다. Monad의 flatMap은 이 중첩을 자동으로 평탄화(flatten)한다.

  Monad가 되려면 세 가지가 필요하다. (1) 타입 생성자(`Optional<T>`), (2) 값을 감싸는 방법(`Optional.of()`), (3) 연산 연결 방법(`flatMap`). 그리고 세 가지 법칙(Left Identity, Right Identity, Associativity)을 만족해야 한다.

  flatMap의 핵심 가치는 순차 의존성의 선형 표현이다. "A 성공하면 B, B 성공하면 C"라는 의존적 파이프라인을 중첩된 if문 없이 일직선으로 표현한다. 중간에 실패(empty, error)하면 이후 단계가 자동으로 건너뛰어진다.

  이 패턴을 Railway-Oriented Programming이라 부르기도 한다. 성공 트랙과 실패 트랙 두 개의 선로가 있고, flatMap이 분기점(switch) 역할을 한다.

**[그림 10.2]** Monad 패턴 (flatMap으로 컨텍스트 연결)
```
+-------------------------------------------------------------------+
|                  Monad = 중첩 없는 연결                              |
+-------------------------------------------------------------------+
|                                                                     |
|   map의 문제:                                                       |
|   Optional<A>.map(a -> findB(a))  -->  Optional<Optional<B>>       |
|                                         ^^^^ 중첩!                  |
|                                                                     |
|   flatMap의 해결:                                                   |
|   Optional<A>.flatMap(a -> findB(a))  -->  Optional<B>             |
|                                            ^^^^ 평탄!               |
|                                                                     |
|   Railway 비유:                                                     |
|   =====[step1]=======>[step2]=======>[step3]=====> Success          |
|          |                                                          |
|          v (실패 시)                                                |
|   -------+--------------------------------------------> Failure     |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **단순 flatten**: flatMap = map + flatten이지만, flatten만으로는 순차 의존성을 표현할 수 없다.
- **예외 처리의 대체**: Monad는 예외를 "대체"하는 것이 아니라, 에러를 "값"으로 다루어 타입 시스템에 통합하는 것이다.
- **모든 컨텍스트에 적용 가능**: Validation(Applicative)처럼 flatMap이 의미를 잃는 경우도 있다(에러 수집 불가).

### Before: Traditional OOP

**[코드 10.4]** Traditional OOP: 중첩 null 체크로 순차 조회 수행 (null 지옥)
```java
 1| // package: com.ecommerce.shared
 2| // [X] 중첩 null 체크로 순차 조회 수행 (null 지옥)
 3| public class OrderService {
 4|   public String getDeliveryCity(Long userId) {
 5|     User user = userRepository.findById(userId);
 6|     if (user == null) {
 7|       return null;
 8|     }
 9|     Long addressId = user.getDefaultAddressId();
10|     if (addressId == null) {
11|       return null;
12|     }
13|     Address address = addressRepository.findById(addressId);
14|     if (address == null) {
15|       return null;
16|     }
17|     return address.getCity();
18|   }
19| }
```
- **의도 및 코드 설명**: 사용자 -> 기본 주소 ID -> 주소 -> 도시를 순차적으로 조회한다. 각 단계가 null을 반환할 수 있어 매번 체크한다.
- **뭐가 문제인가**:
  - 핵심 로직 4줄에 null 체크 6줄이 덧붙음
  - 들여쓰기가 깊어지며 가독성 악화 (pyramid of doom)
  - null 체크 하나 빠뜨리면 NPE 발생
  - 조회 단계가 추가될수록 중첩이 기하급수적으로 증가

### After: Modern Approach

**[코드 10.5]** Modern: Monad(Optional)의 flatMap으로 순차 의존 조회를 선형 표현
```java
 1| // package: com.ecommerce.shared
 2| // [O] Monad(Optional)의 flatMap으로 순차 의존 조회를 선형 표현
 3| import java.util.Optional;
 4| 
 5| public class OrderService {
 6|   record User(Long id, String name, Optional<Long> defaultAddressId) {}
 7|   record Address(Long id, String street, String city) {}
 8| 
 9|   private Optional<User> findUser(Long id) {
10|     return Optional.ofNullable(userRepository.findById(id));
11|   }
12| 
13|   private Optional<Address> findAddress(Long id) {
14|     return Optional.ofNullable(addressRepository.findById(id));
15|   }
16| 
17|   public Optional<String> getDeliveryCity(Long userId) {
18|     return findUser(userId)                     // Optional<User>
19|       .flatMap(User::defaultAddressId)        // Optional<Long> (중첩 방지!)
20|       .flatMap(this::findAddress)             // Optional<Address> (중첩 방지!)
21|       .map(Address::city);                    // Optional<String>
22|   }
23|   // 어느 단계에서든 empty이면 이후 자동 건너뜀!
24| }
```
- **의도 및 코드 설명**: 각 조회가 Optional을 반환하고, flatMap으로 중첩 없이 연결한다. 마지막 단순 변환은 map으로 처리한다.
- **무엇이 좋아지나**:
  - null 체크 완전 제거, 핵심 로직만 남음
  - 선형적으로 읽힘: "사용자 찾고, 주소 ID 얻고, 주소 찾고, 도시 추출"
  - 단계 추가는 flatMap 한 줄 삽입으로 해결
  - 실패 전파가 자동: 중간 empty이면 이후 전부 건너뜀

### 이해를 위한 부가 상세
Java의 주요 Monad와 그 flatMap:

**[표 10.1]** Monad 패턴 (flatMap으로 컨텍스트 연결)
| Monad | of | flatMap | 컨텍스트 |
|-------|------|---------|----------|
| `Optional<T>` | `Optional.of()` | `.flatMap()` | 값 존재/부재 |
| `Stream<T>` | `Stream.of()` | `.flatMap()` | 0~N개 값 |
| `CompletableFuture<T>` | `.completedFuture()` | `.thenCompose()` | 비동기 계산 |

### flatMap이 필요한 이유: 반환 타입의 의미론
함수의 반환 타입이 컨테이너인 것은 설계 선택이 아니라, 해당 연산의 **본질적 의미(semantics)** 를 표현하는 것이다:

**[표 10.A]** 컨테이너 반환 타입의 의미론
| 반환 타입 | 의미 | 예시 |
|-----------|------|------|
| `Optional<T>` | 없을 수도 있다 | `findById(id)` - 해당 ID가 없을 수 있음 |
| `List<T>` / `Stream<T>` | 여러 개일 수 있다 | `findByCategory(cat)` - 0~N개 결과 |
| `Either<E, T>` / `Try<T>` | 실패할 수 있다 | `parseJson(str)` - 파싱 실패 가능 |
| `CompletableFuture<T>` | 나중에 올 수 있다 | `fetchFromApi(url)` - 비동기 결과 |

따라서 `flatMap`은 "선택"이 아니라 "필연"이다:
- `findUserById(id)`가 `Optional<User>`를 반환하는 것은 "사용자가 없을 수도 있음"이라는 도메인 진실
- `findOrdersByUser(user)`가 `List<Order>`를 반환하는 것은 "주문이 여러 개일 수 있음"이라는 도메인 진실
- 이 함수들을 `map`으로 연결하면 `Optional<Optional<T>>`나 `List<List<T>>`가 되어 의미가 왜곡됨
- `flatMap`은 이 중첩을 평탄화하여 본래의 의미론을 유지함

**핵심**: 컨테이너 반환 타입을 보면 "왜 이 함수가 컨테이너를 반환하는가?"를 먼저 질문하라. 그 답이 곧 `flatMap`이 필요한 이유이다.

### 틀리기/놓치기 쉬운 부분
- map과 flatMap의 선택 기준: 변환 함수의 반환 타입이 `T`이면 map, `Optional<T>`(컨테이너)이면 flatMap.
- `Optional.flatMap()`에 전달하는 함수는 반드시 `Optional`을 반환해야 한다. 일반 값을 반환하면 컴파일 에러.
- CompletableFuture에서는 `thenApply`가 map, `thenCompose`가 flatMap이다. 이름이 다르므로 혼동하지 말 것.

### 꼭 기억할 것
flatMap은 "이전 단계의 결과에 의존하는 다음 단계"를 중첩 없이 연결한다. map은 단순 변환, flatMap은 컨텍스트를 생성하는 변환에 사용한다. 이 구분만 명확히 하면 Monad는 어렵지 않다.

---

## 3. Applicative 패턴 (여러 컨텍스트 결합)

### 핵심 개념
- **관련 키워드**: Applicative, Validation, Error Accumulation, combine, Independent Computations
- **통찰**: Monad가 순차적 의존 관계를 표현한다면, Applicative는 독립적인 여러 계산의 결과를 결합하며 에러를 수집한다.
- **설명**:
  Monad(flatMap)는 ATM 줄처럼 순차적이다. 앞 사람이 끝나야 다음 사람이 할 수 있다. 반면 Applicative는 여러 은행 창구처럼 각각 독립적으로 업무를 보고, 마지막에 결과를 합친다.

  Applicative의 킬러 유스케이스는 폼 검증이다. 이름, 이메일, 나이 검증은 서로 독립적이다. Monad(Result)를 사용하면 첫 번째 에러에서 멈추지만(fail-fast), Applicative(Validation)를 사용하면 모든 에러를 한 번에 수집(error accumulation)할 수 있다.

  Validation이 Monad가 될 수 없는 이유는 본질적이다. flatMap은 "이전 값으로 다음 계산을 결정"해야 하는데, Invalid(에러)일 때 값이 없으므로 다음 검증을 호출할 수 없다. 이 충돌 때문에 에러 수집과 모나드적 체이닝은 양립 불가하다.

  선택 기준: 검증 간 의존성이 있으면 Result(Monad), 독립적이면 Validation(Applicative).

**[그림 10.3]** Applicative 패턴 (여러 컨텍스트 결합)
```
+-------------------------------------------------------------------+
|                  Monad vs Applicative                                |
+-------------------------------------------------------------------+
|                                                                     |
|   Monad (순차적 - 의존 관계):                                       |
|   [step1] ---> [step2] ---> [step3]    앞 결과가 뒤 입력            |
|   실패 시 즉시 중단 (fail-fast)                                     |
|                                                                     |
|   Applicative (독립적 - 병렬 가능):                                  |
|   [check1] --+                                                      |
|   [check2] --+--> [combine]    각자 독립, 마지막에 합침             |
|   [check3] --+                                                      |
|   모든 에러 수집 (error accumulation)                                |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **Monad의 하위 개념**: Applicative는 Functor와 Monad 사이의 독립적 추상화이다. Monad보다 약하지만 에러 수집이라는 고유 능력이 있다.
- **단순 리스트 결합**: 여러 리스트를 합치는 것이 아니라, 여러 "결과/에러 컨텍스트"를 결합하는 것이다.
- **try-catch 여러 개**: try-catch는 예외를 "하나만" 잡는다. Validation은 모든 검증을 실행하고 에러를 모은다.

### Before: Traditional OOP

**[코드 10.6]** Traditional OOP: Monad(Result) 체이닝: 첫 에러에서 중단, 나머지 에러 안 보임
```java
 1| // package: com.ecommerce.shared
 2| // [X] Monad(Result) 체이닝: 첫 에러에서 중단, 나머지 에러 안 보임
 3| public class UserRegistration {
 4|   public Result<User, String> register(String name, String email, int age) {
 5|     return validateName(name)
 6|       .flatMap(validName -> validateEmail(email)
 7|         .flatMap(validEmail -> validateAge(age)
 8|           .map(validAge -> new User(validName, validEmail, validAge))));
 9|     // 이름이 잘못되면 이메일, 나이 에러는 사용자에게 안 보임!
10|     // 사용자는 에러를 하나씩 고치며 여러 번 제출해야 함
11|   }
12| }
```
- **의도 및 코드 설명**: 회원가입 시 이름, 이메일, 나이를 순차적으로 검증한다. flatMap으로 연결하면 첫 에러에서 중단된다.
- **뭐가 문제인가**:
  - 사용자가 3개 필드 모두 잘못 입력해도 에러 하나만 보임
  - 에러를 하나씩 고치며 여러 번 제출해야 하는 UX 악화
  - 독립적인 검증인데 불필요하게 순차적으로 처리
  - 실제로 앞 검증 결과가 뒤 검증에 필요하지 않음

### After: Modern Approach

**[코드 10.7]** Modern: Applicative(Validation)로 독립 검증 수행, 에러 전체 수집
```java
 1| // package: com.ecommerce.auth
 2| // [O] Applicative(Validation)로 독립 검증 수행, 에러 전체 수집
 3| import java.util.List;
 4| import java.util.function.Function;
 5| 
 6| public class UserRegistration {
 7| 
 8|   // Validation 타입 정의
 9|   sealed interface Validation<E, A> permits Valid, Invalid {
10|     record Valid<E, A>(A value) implements Validation<E, A> {}
11|     record Invalid<E, A>(List<E> errors) implements Validation<E, A> {}
12|   }
13| 
14|   // 각 필드 검증 (서로 독립!)
15|   Validation<String, String> validateName(String name) {
16|     if (name == null || name.isBlank())
17|       return new Validation.Invalid<>(List.of("이름은 필수입니다"));
18|     if (name.length() < 2)
19|       return new Validation.Invalid<>(List.of("이름은 2자 이상이어야 합니다"));
20|     return new Validation.Valid<>(name.trim());
21|   }
22| 
23|   Validation<String, String> validateEmail(String email) {
24|     if (email == null || !email.contains("@"))
25|       return new Validation.Invalid<>(List.of("유효한 이메일을 입력하세요"));
26|     return new Validation.Valid<>(email.toLowerCase());
27|   }
28| 
29|   Validation<String, Integer> validateAge(int age) {
30|     if (age < 0 || age > 150)
31|       return new Validation.Invalid<>(List.of("나이는 0~150 사이여야 합니다"));
32|     return new Validation.Valid<>(age);
33|   }
34| 
35|   // Applicative 결합: 모든 에러 수집!
36|   public Validation<String, User> register(String name, String email, int age) {
37|     var nameV = validateName(name);
38|     var emailV = validateEmail(email);
39|     var ageV = validateAge(age);
40| 
41|     return combine3(nameV, emailV, ageV, User::new);
42|     // 세 검증이 모두 성공하면 User 생성
43|     // 하나라도 실패하면 모든 에러를 합쳐서 반환!
44|   }
45| 
46|   // combine3: 세 Validation을 결합
47|   static <E, A, B, C, R> Validation<E, R> combine3(
48|       Validation<E, A> va, Validation<E, B> vb, Validation<E, C> vc,
49|       TriFunction<A, B, C, R> combiner) {
50|     List<E> errors = new java.util.ArrayList<>();
51|     if (va instanceof Validation.Invalid<E, A> i) errors.addAll(i.errors());
52|     if (vb instanceof Validation.Invalid<E, B> i) errors.addAll(i.errors());
53|     if (vc instanceof Validation.Invalid<E, C> i) errors.addAll(i.errors());
54| 
55|     if (!errors.isEmpty()) return new Validation.Invalid<>(errors);
56| 
57|     return new Validation.Valid<>(combiner.apply(
58|       ((Validation.Valid<E, A>) va).value(),
59|       ((Validation.Valid<E, B>) vb).value(),
60|       ((Validation.Valid<E, C>) vc).value()
61|     ));
62|   }
63| 
64|   @FunctionalInterface
65|   interface TriFunction<A, B, C, R> {
66|     R apply(A a, B b, C c);
67|   }
68| 
69|   record User(String name, String email, int age) {}
70| }
71| 
72| // 사용 예
73| // register("", "bad", -5)
74| // --> Invalid(["이름은 필수입니다", "유효한 이메일을 입력하세요", "나이는 0~150 사이여야 합니다"])
75| // 세 개의 에러가 한 번에!
```
- **의도 및 코드 설명**: 각 필드를 독립적으로 검증하고, combine3으로 모든 에러를 수집한다. 전부 성공하면 User를 생성한다.
- **무엇이 좋아지나**:
  - 사용자에게 모든 에러를 한 번에 보여줄 수 있음 (UX 대폭 개선)
  - 각 검증이 독립적이어서 병렬 실행도 가능
  - 새 필드 추가 시 검증 함수와 combine에 추가만 하면 됨
  - fail-fast와 error-accumulation을 용도에 맞게 선택 가능

### 이해를 위한 부가 상세
Result(Monad) vs Validation(Applicative) 선택 가이드:

**[그림 10.4]** Applicative 패턴 (여러 컨텍스트 결합)
```
단계 간 의존성 있는가?
  |
  +-- YES --> Result (Monad)
  |           예: 재고확인 -> 결제 -> 배송 (순차 의존)
  |
  +-- NO --> 모든 에러를 수집해야 하는가?
              |
              +-- YES --> Validation (Applicative)
              |           예: 회원가입 폼 (이름, 이메일, 나이 독립)
              |
              +-- NO --> Result도 OK (더 간단)
```

### 틀리기/놓치기 쉬운 부분
- Validation에 flatMap을 억지로 구현하면 에러 수집 능력을 잃고 Result와 동일해진다.
- combine의 인자 수에 따라 combine2, combine3, combine4 등 오버로드가 필요하다. Java의 제네릭 한계.
- Applicative는 검증들이 진정으로 독립적일 때만 사용한다. "비밀번호 확인" 같이 다른 필드에 의존하면 Monad가 필요하다.

### 꼭 기억할 것
Applicative의 핵심 가치는 "독립 연산의 에러 수집"이다. 폼 검증에서 사용자에게 모든 에러를 한 번에 보여주려면 Applicative(Validation)를, 순차적 의존 파이프라인에는 Monad(Result)를 사용한다.

---

## 4. Optional as Monad (Optional의 모나드적 활용)

### 핵심 개념
- **관련 키워드**: Optional, Null Safety, map, flatMap, orElse, orElseThrow
- **통찰**: Optional을 단순 null 체크 도구가 아닌, 값의 존재/부재라는 컨텍스트를 가진 Monad로 활용하면 null 관련 버그를 구조적으로 방지할 수 있다.
- **설명**:
  대부분의 Java 프로젝트에서 Optional은 `.isPresent()`와 `.get()`으로만 사용된다. 이는 null 체크의 단순 포장에 불과하다. Optional을 Monad로 활용하면, map과 flatMap 체이닝으로 "값이 있을 때만 수행할 변환"을 선언적으로 표현할 수 있다.

  Optional의 Monad적 활용 패턴: (1) 단순 변환은 map, (2) Optional을 반환하는 함수 연결은 flatMap, (3) 기본값은 orElse/orElseGet, (4) 실패 시 예외는 orElseThrow.

  Optional을 메서드 파라미터로 사용하지 않는 것이 컨벤션이다. 메서드의 반환 타입으로만 사용하고, "값이 없을 수 있음"을 타입으로 명시한다. 필드에도 사용하지 않는 것이 권장된다(직렬화 이슈).

  Optional 체이닝에서 map과 flatMap을 적절히 조합하면, 중첩된 if-null-check를 완전히 제거하고 선형적인 파이프라인으로 대체할 수 있다.

**[그림 10.5]** Optional as Monad (Optional의 모나드적 활용)
```
+-------------------------------------------------------------------+
|                  Optional Monad 패턴                                 |
+-------------------------------------------------------------------+
|                                                                     |
|   Optional.of(value)                                                |
|       .map(f)        // 값 변환 (T -> R)                            |
|       .flatMap(g)    // Optional 반환 함수 연결 (T -> Optional<R>)  |
|       .filter(p)     // 조건부 비움 (T -> boolean)                  |
|       .orElse(def)   // 기본값                                      |
|                                                                     |
|   핵심: empty()이면 map/flatMap/filter 모두 자동 건너뜀             |
|                                                                     |
|   [X] if (opt.isPresent()) { opt.get()... }  <-- 안티패턴!          |
|   [O] opt.map(...).flatMap(...).orElse(...)  <-- 모나드 활용        |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **null의 완전한 대체**: Optional은 모든 null을 대체하지 않는다. 성능 민감한 로컬 변수에는 null이 적합할 수 있다.
- **isPresent + get 패턴**: 이것은 Optional의 안티패턴이다. null 체크를 Optional로 감싼 것에 불과하다.
- **메서드 파라미터 타입**: Optional을 인자로 받는 것은 호출자에게 불필요한 래핑을 강제한다.

### Before: Traditional OOP

**[코드 10.8]** Traditional OOP: Optional을 단순 null 체크 래퍼로만 사용 (안티패턴)
```java
 1| // package: com.ecommerce.shared
 2| // [X] Optional을 단순 null 체크 래퍼로만 사용 (안티패턴)
 3| public class BookingService {
 4|   public String getBookingConfirmation(Long bookingId) {
 5|     Optional<Booking> optBooking = bookingRepository.findById(bookingId);
 6|     if (optBooking.isPresent()) {
 7|       Booking booking = optBooking.get();
 8|       Optional<Payment> optPayment = paymentRepository.findByBookingId(booking.id());
 9|       if (optPayment.isPresent()) {
10|         Payment payment = optPayment.get();
11|         if (payment.isConfirmed()) {
12|           return "예약 확정: " + booking.guestName() + ", 결제액: " + payment.amount();
13|         }
14|       }
15|     }
16|     return "예약 정보를 찾을 수 없습니다";
17|   }
18| }
```
- **의도 및 코드 설명**: 예약 -> 결제 -> 확정 여부를 조회한다. Optional을 사용했지만 isPresent/get 패턴으로 null 체크와 본질적으로 동일하다.
- **뭐가 문제인가**:
  - Optional 사용의 이점이 전혀 없음 (null 체크와 동일 구조)
  - 중첩 if문으로 가독성 악화
  - `.get()` 호출은 NoSuchElementException 위험 (isPresent 빠뜨리면)
  - 새 조회 단계 추가 시 중첩이 더 깊어짐

### After: Modern Approach

**[코드 10.9]** Modern: Optional을 Monad로 활용하여 선형 파이프라인 구성
```java
 1| // package: com.ecommerce.shared
 2| // [O] Optional을 Monad로 활용하여 선형 파이프라인 구성
 3| public class BookingService {
 4|   record Booking(Long id, String guestName, Long paymentId) {}
 5|   record Payment(Long id, java.math.BigDecimal amount, boolean confirmed) {
 6|     public boolean isConfirmed() { return confirmed; }
 7|   }
 8| 
 9|   public String getBookingConfirmation(Long bookingId) {
10|     return bookingRepository.findById(bookingId)            // Optional<Booking>
11|       .flatMap(booking ->
12|         paymentRepository.findByBookingId(booking.id()) // Optional<Payment>
13|           .filter(Payment::isConfirmed)               // 확정된 것만
14|           .map(payment -> "예약 확정: " + booking.guestName()
15|             + ", 결제액: " + payment.amount())
16|       )
17|       .orElse("예약 정보를 찾을 수 없습니다");
18|   }
19| 
20|   // 더 복잡한 체이닝 예시
21|   public Optional<String> getGuestEmail(Long bookingId) {
22|     return bookingRepository.findById(bookingId)
23|       .map(Booking::guestName)
24|       .flatMap(name -> guestRepository.findByName(name))
25|       .map(Guest::email)
26|       .filter(email -> email.contains("@"));
27|   }
28| }
```
- **의도 및 코드 설명**: flatMap, map, filter를 체이닝하여 "예약 찾기 -> 결제 찾기 -> 확정 여부 -> 메시지 생성"을 선형으로 표현한다.
- **무엇이 좋아지나**:
  - isPresent/get 제거: NoSuchElementException 위험 원천 차단
  - 선형적으로 읽히는 파이프라인
  - 각 단계에서 empty이면 자동으로 최종 orElse로 이동
  - filter로 조건 충족하지 않는 경우도 우아하게 처리

### 이해를 위한 부가 상세
Optional 활용 패턴 정리:

**[코드 10.10]** 패턴 1: 단순 변환
```java
 1| // package: com.ecommerce.shared
 2| // 패턴 1: 단순 변환
 3| optional.map(String::toUpperCase);
 4| 
 5| // 패턴 2: Optional 반환 함수 연결
 6| optional.flatMap(this::findAddress);
 7| 
 8| // 패턴 3: 조건부 비움
 9| optional.filter(age -> age >= 18);
10| 
11| // 패턴 4: 대안 Optional
12| optional.or(() -> findAlternative());  // Java 9+
13| 
14| // 패턴 5: 부재 시 예외
15| optional.orElseThrow(() -> new NotFoundException("not found"));
16| 
17| // 패턴 6: 부재 시 기본값 (지연 계산)
18| optional.orElseGet(() -> computeDefault());
```

### 틀리기/놓치기 쉬운 부분
- `orElse`는 Optional에 값이 있어도 인자를 평가한다. 비용이 큰 연산이면 `orElseGet()`을 사용한다.
- Optional을 직렬화할 수 없다(Serializable이 아님). 엔티티 필드에는 사용하지 않는다.
- `Optional.of(null)`은 NPE를 던진다. nullable 소스에는 반드시 `Optional.ofNullable()`을 사용한다.
- Stream에서 Optional의 값을 추출할 때: `stream.flatMap(Optional::stream)` (Java 9+).

### 꼭 기억할 것
Optional을 "있거나 없거나"라는 컨텍스트를 가진 Monad로 사용한다. isPresent/get은 안티패턴이며, map/flatMap/filter/orElse 체이닝이 올바른 사용법이다. 메서드 반환 타입에만 사용하고, 파라미터와 필드에는 사용하지 않는다.

---

## 5. Result/Either as Monad (에러 처리 모나드)

### 핵심 개념
- **관련 키워드**: Result, Either, Railway-Oriented Programming, Fail-Fast, Error as Value
- **통찰**: 에러를 예외가 아닌 "값"으로 다루면, 타입 시스템이 에러 처리를 강제하고 flatMap으로 성공 경로를 선형적으로 표현할 수 있다.
- **설명**:
  전통적 예외 처리의 문제점은 세 가지이다. (1) 함수 시그니처에 드러나지 않음(어떤 예외가 발생할지 모름), (2) 제어 흐름이 비선형적(throw로 점프), (3) checked exception은 함수형 인터페이스와 궁합이 나쁨.

  Result(또는 Either) 패턴은 성공과 실패를 하나의 sealed interface로 표현한다. `Result<Success, Failure>`에서 Success는 결과값, Failure는 도메인 에러를 담는다. flatMap은 성공일 때만 다음 단계를 실행하고, 실패이면 자동으로 전파한다.

  이 패턴을 Railway-Oriented Programming이라 부른다. 성공 트랙과 실패 트랙 두 선로가 있고, flatMap이 분기점 역할을 한다. 중간에 실패하면 이후 단계를 건너뛰고 실패 트랙으로 이동한다.

  Result의 에러 타입을 sealed interface로 정의하면, 패턴 매칭으로 모든 에러 케이스를 빠짐없이 처리할 수 있다.

**[그림 10.6]** Result/Either as Monad (에러 처리 모나드)
```
+-------------------------------------------------------------------+
|                  Result = 성공 OR 실패                                |
+-------------------------------------------------------------------+
|                                                                     |
|   sealed interface Result<S, F>                                     |
|       record Success<S>(S value)                                    |
|       record Failure<F>(F error)                                    |
|                                                                     |
|   Railway-Oriented Programming:                                     |
|   ====[validate]======[process]======[save]===> Success<Receipt>    |
|          |                |                                         |
|          v                v                                         |
|   -------+----------------+---------------------> Failure<Error>    |
|                                                                     |
|   flatMap: Success이면 다음 단계 실행                                |
|            Failure이면 자동 전파 (나머지 건너뜀)                     |
|                                                                     |
+-------------------------------------------------------------------+
```

### 개념이 아닌 것
- **예외의 완전 대체**: 인프라 에러(OutOfMemory, StackOverflow)는 여전히 예외가 적합하다. Result는 비즈니스/도메인 에러에 사용한다.
- **Optional과 동일**: Optional은 "값 있음/없음"만 표현하고, Result는 "성공값/에러 정보"를 모두 담는다.
- **try-catch의 함수형 래퍼**: Result는 try-catch를 감싸는 것이 아니라, 에러를 타입으로 표현하여 컴파일러가 처리를 강제하는 것이다.

### Before: Traditional OOP

**[코드 10.11]** Traditional OOP: 예외로 에러를 처리: 시그니처에 드러나지 않고 제어 흐름이 비선형적
```java
 1| // package: com.ecommerce.payment
 2| // [X] 예외로 에러를 처리: 시그니처에 드러나지 않고 제어 흐름이 비선형적
 3| public class PaymentService {
 4|   public Receipt processPayment(OrderRequest request)
 5|       throws ValidationException, InventoryException, PaymentException {
 6|     // 검증
 7|     if (request.items().isEmpty()) {
 8|       throw new ValidationException("주문 항목이 비어있습니다");
 9|     }
10|     // 재고 확인
11|     for (var item : request.items()) {
12|       if (!inventoryService.isAvailable(item)) {
13|         throw new InventoryException("재고 부족: " + item.name());
14|       }
15|     }
16|     // 결제
17|     PaymentResult result = gateway.charge(request.totalAmount());
18|     if (!result.isSuccess()) {
19|       throw new PaymentException("결제 실패: " + result.reason());
20|     }
21|     return new Receipt(result.transactionId(), request);
22|   }
23| }
24| 
25| // 호출 측: 어떤 예외가 올지 모르고, catch 순서에 의존
26| try {
27|   Receipt receipt = paymentService.processPayment(request);
28| } catch (ValidationException e) {
29|   // ...
30| } catch (InventoryException e) {
31|   // ...
32| } catch (PaymentException e) {
33|   // ...
34| }
```
- **의도 및 코드 설명**: 주문 검증 -> 재고 확인 -> 결제를 순차 수행한다. 각 단계의 실패를 예외로 표현한다.
- **뭐가 문제인가**:
  - 함수 시그니처만으로 어떤 에러가 발생할지 불명확 (throws 선언 있어도 비포괄적)
  - throw로 제어 흐름이 점프하여 추적이 어려움
  - catch 블록 빠뜨리면 런타임에 unhandled exception
  - 람다/Stream 내에서 checked exception 사용이 불편

### After: Modern Approach

**[코드 10.12]** Modern: Result Monad로 에러를 값으로 다루고 flatMap으로 파이프라인 구성
```java
 1| // package: com.ecommerce.shared
 2| // [O] Result Monad로 에러를 값으로 다루고 flatMap으로 파이프라인 구성
 3| public class PaymentService {
 4| 
 5|   // 도메인 에러를 sealed interface로 정의
 6|   sealed interface OrderError permits ValidationError, InventoryError, PaymentError {
 7|     record ValidationError(List<String> messages) implements OrderError {}
 8|     record InventoryError(String itemName) implements OrderError {}
 9|     record PaymentError(String reason) implements OrderError {}
10|   }
11| 
12|   // Result 타입 정의
13|   sealed interface Result<S, F> permits Result.Success, Result.Failure {
14|     record Success<S, F>(S value) implements Result<S, F> {}
15|     record Failure<S, F>(F error) implements Result<S, F> {}
16| 
17|     default <R> Result<R, F> map(Function<S, R> f) {
18|       return switch (this) {
19|         case Success<S, F> s -> new Success<>(f.apply(s.value()));
20|         case Failure<S, F> fail -> new Failure<>(fail.error());
21|       };
22|     }
23| 
24|     default <R> Result<R, F> flatMap(Function<S, Result<R, F>> f) {
25|       return switch (this) {
26|         case Success<S, F> s -> f.apply(s.value());
27|         case Failure<S, F> fail -> new Failure<>(fail.error());
28|       };
29|     }
30|   }
31| 
32|   // 각 단계를 Result를 반환하는 순수 함수로 정의
33|   Result<ValidatedOrder, OrderError> validate(OrderRequest request) {
34|     if (request.items().isEmpty())
35|       return new Result.Failure<>(
36|         new OrderError.ValidationError(List.of("주문 항목이 비어있습니다")));
37|     return new Result.Success<>(new ValidatedOrder(request));
38|   }
39| 
40|   Result<ReservedOrder, OrderError> checkInventory(ValidatedOrder order) {
41|     for (var item : order.items()) {
42|       if (!inventoryService.isAvailable(item))
43|         return new Result.Failure<>(new OrderError.InventoryError(item.name()));
44|     }
45|     return new Result.Success<>(new ReservedOrder(order));
46|   }
47| 
48|   Result<Receipt, OrderError> charge(ReservedOrder order) {
49|     var payResult = gateway.charge(order.totalAmount());
50|     if (!payResult.isSuccess())
51|       return new Result.Failure<>(new OrderError.PaymentError(payResult.reason()));
52|     return new Result.Success<>(new Receipt(payResult.transactionId(), order));
53|   }
54| 
55|   // Railway-Oriented Programming: flatMap 체이닝
56|   public Result<Receipt, OrderError> processPayment(OrderRequest request) {
57|     return validate(request)                    // Result<ValidatedOrder, OrderError>
58|       .flatMap(this::checkInventory)          // Result<ReservedOrder, OrderError>
59|       .flatMap(this::charge);                 // Result<Receipt, OrderError>
60|     // 어느 단계에서든 Failure이면 이후 자동 건너뜀!
61|   }
62| 
63|   // 호출 측: 패턴 매칭으로 모든 에러 빠짐없이 처리
64|   public String handleResult(Result<Receipt, OrderError> result) {
65|     return switch (result) {
66|       case Result.Success<Receipt, OrderError> s ->
67|         "결제 성공: " + s.value().transactionId();
68|       case Result.Failure<Receipt, OrderError> f -> switch (f.error()) {
69|         case OrderError.ValidationError e -> "검증 실패: " + e.messages();
70|         case OrderError.InventoryError e -> "재고 부족: " + e.itemName();
71|         case OrderError.PaymentError e -> "결제 실패: " + e.reason();
72|       };
73|     };
74|   }
75| }
```
- **의도 및 코드 설명**: 각 단계가 Result를 반환하고, flatMap으로 체이닝한다. 에러는 sealed interface로 타입화하여 패턴 매칭으로 빠짐없이 처리한다.
- **무엇이 좋아지나**:
  - 함수 시그니처에 에러 타입이 명시됨 (어떤 에러가 가능한지 컴파일 타임에 확인)
  - 에러 처리 누락이 컴파일 에러로 방지됨 (sealed + switch exhaustiveness)
  - 제어 흐름이 선형적: throw/catch 없이 데이터가 흘러감
  - 각 단계를 독립적으로 테스트 가능 (순수 함수)
  - 람다/Stream과 자연스럽게 결합 가능

### 이해를 위한 부가 상세
에러 처리 전략 선택:

**[표 10.2]** Result/Either as Monad (에러 처리 모나드)
| 상황 | 방식 |
|------|------|
| 비즈니스/도메인 에러 | Result (값으로 표현) |
| 인프라/프로그래밍 에러 | 예외 (throw) |
| 독립적 검증, 에러 수집 | Validation (Applicative) |
| 순차적 의존 파이프라인 | Result (Monad) |

### 틀리기/놓치기 쉬운 부분
- Java 표준 라이브러리에 Result 타입이 없다. 직접 정의하거나 Vavr의 `Either`를 사용한다.
- Result의 에러 타입을 String으로 하면 타입 안전성을 잃는다. sealed interface로 도메인 에러를 정의하는 것이 권장된다.
- Result 체인 내에서 부수효과(DB 저장, 이메일 발송)는 파이프라인 끝에서 처리한다 (Functional Core / Imperative Shell).
- Result와 Optional을 변환할 때: `result.toOptional()`, `Optional.map(Result::success)` 등 유틸 메서드를 제공하면 편리하다.

### 꼭 기억할 것
Result/Either는 "에러를 값으로 다루는" 패러다임 전환이다. 함수 시그니처에 에러가 드러나고, 컴파일러가 처리를 강제하며, flatMap으로 성공 경로를 선형적으로 표현한다. 비즈니스 로직의 에러는 Result, 인프라 에러는 예외로 구분하여 사용한다.
