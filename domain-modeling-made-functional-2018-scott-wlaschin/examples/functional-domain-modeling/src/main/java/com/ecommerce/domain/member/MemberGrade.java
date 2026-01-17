package com.ecommerce.domain.member;

/**
 * 회원 등급 - Sum Type으로 표현 (Chapter 4)
 *
 * <h2>핵심 개념: 행동을 포함한 Sum Type</h2>
 * 단순 enum과 달리, 각 등급이 다른 행동(할인율, 무료배송)을 가진다.
 * sealed interface + permits로 확장 가능하면서도 완전 열거 보장.
 *
 * <h2>얻어갈 것 (Takeaway)</h2>
 * <pre>{@code
 * // Exhaustive pattern matching 강제
 * int discount = switch (grade) {
 *     case Bronze b -> 0;
 *     case Silver s -> 5;
 *     case Gold g -> 8;
 *     case Vip v -> 10;
 *     // 새 등급 추가 시 컴파일 에러
 * };
 * }</pre>
 */
public sealed interface MemberGrade
    permits MemberGrade.Bronze, MemberGrade.Silver, MemberGrade.Gold, MemberGrade.Vip {

    /**
     * 등급별 할인율 (%)
     */
    int discountRate();

    /**
     * 무료배송 여부
     */
    boolean hasFreeShipping();

    /**
     * 등급 이름
     */
    String gradeName();

    record Bronze() implements MemberGrade {
        @Override public int discountRate() { return 0; }
        @Override public boolean hasFreeShipping() { return false; }
        @Override public String gradeName() { return "브론즈"; }
    }

    record Silver() implements MemberGrade {
        @Override public int discountRate() { return 5; }
        @Override public boolean hasFreeShipping() { return false; }
        @Override public String gradeName() { return "실버"; }
    }

    record Gold() implements MemberGrade {
        @Override public int discountRate() { return 8; }
        @Override public boolean hasFreeShipping() { return false; }
        @Override public String gradeName() { return "골드"; }
    }

    record Vip() implements MemberGrade {
        @Override public int discountRate() { return 10; }
        @Override public boolean hasFreeShipping() { return true; }
        @Override public String gradeName() { return "VIP"; }
    }
}
