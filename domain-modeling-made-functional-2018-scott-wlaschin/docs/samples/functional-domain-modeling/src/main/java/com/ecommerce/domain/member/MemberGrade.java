package com.ecommerce.domain.member;

/**
 * 회원 등급 - 등급별 할인율과 무료배송 여부 정의
 * sealed interface로 타입을 제한하여 exhaustive pattern matching 강제
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
