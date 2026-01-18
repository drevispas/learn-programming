package com.ecommerce.shared.types;

/**
 * Money 연산 실패를 표현하는 에러 타입 (Chapter 6)
 *
 * <h2>목적 (Purpose)</h2>
 * Money 뺄셈/나눗셈 등에서 발생할 수 있는 비즈니스 실패를 타입으로 표현한다.
 * 음수 결과를 조용히 0으로 클램핑하는 대신, 명시적으로 실패를 반환한다.
 *
 * <h2>설계 원칙</h2>
 * <ul>
 *   <li><b>Compact Constructor (예외)</b>: 불변식 위반 (null, 음수) → 프로그래밍 오류</li>
 *   <li><b>Result 반환</b>: 비즈니스 로직 실패 → 예상 가능한 비즈니스 실패</li>
 * </ul>
 *
 * <h2>사용 예시</h2>
 * <pre>{@code
 * Money balance = Money.krw(10000);
 * Money withdrawal = Money.krw(15000);
 *
 * Result<Money, MoneyError> result = balance.subtractSafe(withdrawal);
 * return result.fold(
 *     newBalance -> "출금 성공: " + newBalance,
 *     error -> switch (error) {
 *         case InsufficientAmount e -> "잔액 부족: " + e.from() + " < " + e.subtract();
 *         case CurrencyMismatch e -> "통화 불일치: " + e.expected() + " != " + e.actual();
 *     }
 * );
 * }</pre>
 *
 * @see Money#subtractSafe(Money) Result를 반환하는 안전한 뺄셈
 */
public sealed interface MoneyError permits MoneyError.InsufficientAmount, MoneyError.CurrencyMismatch {

    /**
     * 잔액 부족 에러
     *
     * 뺄셈 결과가 음수가 되는 경우 발생.
     * 예: 10,000원에서 15,000원을 빼려고 할 때
     *
     * @param from 원래 금액
     * @param subtract 빼려고 한 금액
     */
    record InsufficientAmount(Money from, Money subtract) implements MoneyError {
        public InsufficientAmount {
            if (from == null) throw new IllegalArgumentException("from must not be null");
            if (subtract == null) throw new IllegalArgumentException("subtract must not be null");
        }

        @Override
        public String toString() {
            return "잔액 부족: " + from.amount() + from.currency().displayName() +
                   "에서 " + subtract.amount() + subtract.currency().displayName() + "을(를) 뺄 수 없습니다";
        }
    }

    /**
     * 통화 불일치 에러
     *
     * 다른 통화 간 연산을 시도할 때 발생.
     * 예: KRW 금액에서 USD 금액을 빼려고 할 때
     *
     * @param expected 예상한 통화 (연산 대상)
     * @param actual 실제 통화 (연산에 사용된)
     */
    record CurrencyMismatch(Currency expected, Currency actual) implements MoneyError {
        public CurrencyMismatch {
            if (expected == null) throw new IllegalArgumentException("expected must not be null");
            if (actual == null) throw new IllegalArgumentException("actual must not be null");
        }

        @Override
        public String toString() {
            return "통화 불일치: " + expected.displayName() + " != " + actual.displayName();
        }
    }
}
