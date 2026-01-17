package com.travel.domain.payment;

/**
 * 결제 수단 - Sum Type
 *
 * <h2>목적 (Purpose)</h2>
 * 다양한 결제 수단을 타입으로 명시하고 각 수단별 필요한 정보 캡슐화
 *
 * <h2>핵심 개념 (Key Concept): Ch 3 Sum Type</h2>
 * <pre>
 * [Key Point] 각 결제 수단마다 다른 정보 필요:
 *
 * CreditCard: 카드번호, 유효기간, CVV
 * BankTransfer: 은행코드, 계좌번호
 * VirtualAccount: 가상계좌번호
 * KakaoPay/NaverPay: 토큰
 *
 * [Before] 모든 필드를 하나의 클래스에:
 *   class PaymentMethod {
 *       String type;
 *       String cardNumber;  // 카드 결제 시에만 사용
 *       String bankCode;    // 계좌이체 시에만 사용
 *       String token;       // 간편결제 시에만 사용
 *   }
 *
 * [After] 각 결제 수단별 전용 타입:
 *   → 불필요한 필드가 null인 상태 방지
 * </pre>
 */
public sealed interface PaymentMethod permits
        PaymentMethod.CreditCard,
        PaymentMethod.DebitCard,
        PaymentMethod.BankTransfer,
        PaymentMethod.VirtualAccount,
        PaymentMethod.KakaoPay,
        PaymentMethod.NaverPay,
        PaymentMethod.TossPay,
        PaymentMethod.Point {

    /**
     * 결제 수단 표시 이름
     */
    String displayName();

    // ============================================
    // 카드 결제
    // ============================================

    /**
     * 신용카드 결제
     *
     * @param cardNumber   카드 번호 (마스킹됨: 1234-****-****-5678)
     * @param cardCompany  카드사
     * @param installments 할부 개월 (0이면 일시불)
     */
    record CreditCard(
            String cardNumber,
            CardCompany cardCompany,
            int installments
    ) implements PaymentMethod {

        public enum CardCompany {
            SHINHAN("신한"), SAMSUNG("삼성"), KB("국민"), HYUNDAI("현대"),
            LOTTE("롯데"), HANA("하나"), BC("BC"), NH("농협");

            private final String displayName;
            CardCompany(String displayName) { this.displayName = displayName; }
            public String displayName() { return displayName; }
        }

        public CreditCard {
            if (cardNumber == null || cardNumber.isBlank()) {
                throw new IllegalArgumentException("카드 번호는 필수입니다");
            }
            if (cardCompany == null) {
                throw new IllegalArgumentException("카드사는 필수입니다");
            }
            if (installments < 0) {
                throw new IllegalArgumentException("할부 개월은 0 이상이어야 합니다");
            }
        }

        @Override
        public String displayName() {
            String installmentText = installments == 0 ? "일시불" : installments + "개월";
            return cardCompany.displayName() + " " + installmentText;
        }

        /**
         * 일시불 여부
         */
        public boolean isOneTime() {
            return installments == 0;
        }
    }

    /**
     * 체크카드 결제
     */
    record DebitCard(
            String cardNumber,
            CreditCard.CardCompany cardCompany
    ) implements PaymentMethod {

        public DebitCard {
            if (cardNumber == null || cardNumber.isBlank()) {
                throw new IllegalArgumentException("카드 번호는 필수입니다");
            }
            if (cardCompany == null) {
                throw new IllegalArgumentException("카드사는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return cardCompany.displayName() + " 체크";
        }
    }

    // ============================================
    // 계좌 기반 결제
    // ============================================

    /**
     * 실시간 계좌이체
     *
     * @param bankCode      은행 코드
     * @param accountNumber 계좌번호 (마스킹됨)
     */
    record BankTransfer(
            BankCode bankCode,
            String accountNumber
    ) implements PaymentMethod {

        public enum BankCode {
            KB("국민", "004"), SHINHAN("신한", "088"), WOORI("우리", "020"),
            HANA("하나", "081"), NH("농협", "011"), IBK("기업", "003"),
            SC("SC제일", "023"), CITI("씨티", "027"), KAKAO("카카오뱅크", "090"),
            TOSS("토스뱅크", "092");

            private final String name;
            private final String code;
            BankCode(String name, String code) { this.name = name; this.code = code; }
            public String displayName() { return name; }
            public String code() { return code; }
        }

        public BankTransfer {
            if (bankCode == null) {
                throw new IllegalArgumentException("은행은 필수입니다");
            }
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("계좌번호는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return bankCode.displayName() + " 계좌이체";
        }
    }

    /**
     * 가상계좌 결제
     *
     * @param bankCode             은행 코드
     * @param virtualAccountNumber 가상계좌번호
     * @param depositorName        입금자명
     */
    record VirtualAccount(
            BankTransfer.BankCode bankCode,
            String virtualAccountNumber,
            String depositorName
    ) implements PaymentMethod {

        public VirtualAccount {
            if (bankCode == null) {
                throw new IllegalArgumentException("은행은 필수입니다");
            }
            if (virtualAccountNumber == null || virtualAccountNumber.isBlank()) {
                throw new IllegalArgumentException("가상계좌번호는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return bankCode.displayName() + " 가상계좌";
        }
    }

    // ============================================
    // 간편결제
    // ============================================

    /**
     * 카카오페이
     *
     * @param tid 카카오페이 트랜잭션 ID
     */
    record KakaoPay(String tid) implements PaymentMethod {
        public KakaoPay {
            if (tid == null || tid.isBlank()) {
                throw new IllegalArgumentException("카카오페이 TID는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return "카카오페이";
        }
    }

    /**
     * 네이버페이
     *
     * @param paymentId 네이버페이 결제 ID
     */
    record NaverPay(String paymentId) implements PaymentMethod {
        public NaverPay {
            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalArgumentException("네이버페이 결제 ID는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return "네이버페이";
        }
    }

    /**
     * 토스페이
     *
     * @param paymentKey 토스 결제 키
     */
    record TossPay(String paymentKey) implements PaymentMethod {
        public TossPay {
            if (paymentKey == null || paymentKey.isBlank()) {
                throw new IllegalArgumentException("토스 결제 키는 필수입니다");
            }
        }

        @Override
        public String displayName() {
            return "토스페이";
        }
    }

    // ============================================
    // 포인트 결제
    // ============================================

    /**
     * 포인트 결제
     *
     * @param pointType  포인트 유형
     * @param usedPoints 사용 포인트
     */
    record Point(
            PointType pointType,
            int usedPoints
    ) implements PaymentMethod {

        public enum PointType {
            TRAVEL_POINT("트래블 포인트"),
            MEMBERSHIP_POINT("멤버십 포인트"),
            EVENT_POINT("이벤트 포인트");

            private final String displayName;
            PointType(String displayName) { this.displayName = displayName; }
            public String displayName() { return displayName; }
        }

        public Point {
            if (pointType == null) {
                throw new IllegalArgumentException("포인트 유형은 필수입니다");
            }
            if (usedPoints <= 0) {
                throw new IllegalArgumentException("사용 포인트는 0보다 커야 합니다");
            }
        }

        @Override
        public String displayName() {
            return pointType.displayName();
        }
    }
}
