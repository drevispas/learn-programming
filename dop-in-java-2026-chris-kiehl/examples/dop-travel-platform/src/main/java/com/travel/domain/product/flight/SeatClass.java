package com.travel.domain.product.flight;

/**
 * 좌석 등급 - sealed interface Sum Type
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 좌석 등급을 sealed interface로 표현:
 * - 각 등급별 특성 캡슐화
 * - 수하물 허용량, 마일리지 적립률 등 포함
 * </pre>
 */
public sealed interface SeatClass permits
        SeatClass.Economy,
        SeatClass.PremiumEconomy,
        SeatClass.Business,
        SeatClass.First {

    String displayName();
    int baggageAllowanceKg();
    double mileageRate();

    /**
     * 이코노미
     */
    record Economy() implements SeatClass {
        @Override
        public String displayName() {
            return "이코노미";
        }

        @Override
        public int baggageAllowanceKg() {
            return 23;
        }

        @Override
        public double mileageRate() {
            return 0.5;
        }
    }

    /**
     * 프리미엄 이코노미
     */
    record PremiumEconomy() implements SeatClass {
        @Override
        public String displayName() {
            return "프리미엄 이코노미";
        }

        @Override
        public int baggageAllowanceKg() {
            return 28;
        }

        @Override
        public double mileageRate() {
            return 0.75;
        }
    }

    /**
     * 비즈니스
     */
    record Business() implements SeatClass {
        @Override
        public String displayName() {
            return "비즈니스";
        }

        @Override
        public int baggageAllowanceKg() {
            return 32;
        }

        @Override
        public double mileageRate() {
            return 1.25;
        }
    }

    /**
     * 퍼스트
     */
    record First() implements SeatClass {
        @Override
        public String displayName() {
            return "퍼스트";
        }

        @Override
        public int baggageAllowanceKg() {
            return 40;
        }

        @Override
        public double mileageRate() {
            return 1.5;
        }
    }
}
