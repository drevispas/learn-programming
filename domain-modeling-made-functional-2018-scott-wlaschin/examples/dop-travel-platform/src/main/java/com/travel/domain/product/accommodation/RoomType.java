package com.travel.domain.product.accommodation;

/**
 * 객실 유형 - sealed interface Sum Type
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 객실 유형을 sealed interface로 표현:
 * - 각 유형별 특성 캡슐화
 * - 패턴 매칭으로 유형별 처리
 *
 * [Why sealed] enum 대신 sealed:
 * - 각 유형이 다른 속성을 가질 수 있음
 * - 확장성 (새 유형 추가 용이)
 * </pre>
 */
public sealed interface RoomType permits
        RoomType.Standard,
        RoomType.Deluxe,
        RoomType.Suite,
        RoomType.Family,
        RoomType.Penthouse {

    /**
     * 표시 이름
     */
    String displayName();

    /**
     * 기본 인원
     */
    int baseOccupancy();

    /**
     * 가격 배수 (Standard 기준)
     */
    double priceMultiplier();

    /**
     * 스탠다드 객실
     */
    record Standard() implements RoomType {
        @Override
        public String displayName() {
            return "스탠다드";
        }

        @Override
        public int baseOccupancy() {
            return 2;
        }

        @Override
        public double priceMultiplier() {
            return 1.0;
        }
    }

    /**
     * 디럭스 객실
     */
    record Deluxe() implements RoomType {
        @Override
        public String displayName() {
            return "디럭스";
        }

        @Override
        public int baseOccupancy() {
            return 2;
        }

        @Override
        public double priceMultiplier() {
            return 1.3;
        }
    }

    /**
     * 스위트 객실
     */
    record Suite() implements RoomType {
        @Override
        public String displayName() {
            return "스위트";
        }

        @Override
        public int baseOccupancy() {
            return 2;
        }

        @Override
        public double priceMultiplier() {
            return 2.0;
        }
    }

    /**
     * 패밀리 객실
     *
     * @param maxChildren 최대 어린이 수
     */
    record Family(int maxChildren) implements RoomType {
        public Family {
            if (maxChildren < 0) throw new IllegalArgumentException("최대 어린이 수는 0 이상이어야 합니다");
        }

        @Override
        public String displayName() {
            return "패밀리";
        }

        @Override
        public int baseOccupancy() {
            return 4;
        }

        @Override
        public double priceMultiplier() {
            return 1.5;
        }
    }

    /**
     * 펜트하우스
     *
     * @param floorNumber 층수
     */
    record Penthouse(int floorNumber) implements RoomType {
        public Penthouse {
            if (floorNumber <= 0) throw new IllegalArgumentException("층수는 1 이상이어야 합니다");
        }

        @Override
        public String displayName() {
            return "펜트하우스 " + floorNumber + "층";
        }

        @Override
        public int baseOccupancy() {
            return 4;
        }

        @Override
        public double priceMultiplier() {
            return 5.0;
        }
    }
}
