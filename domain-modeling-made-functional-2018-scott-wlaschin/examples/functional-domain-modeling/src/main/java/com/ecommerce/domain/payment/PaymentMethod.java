package com.ecommerce.domain.payment;

/**
 * 결제 수단 - 신용카드, 계좌이체, 포인트, 간편결제
 * sealed interface로 타입을 제한하여 exhaustive pattern matching 강제
 */
public sealed interface PaymentMethod
    permits PaymentMethod.CreditCard, PaymentMethod.BankTransfer,
            PaymentMethod.Points, PaymentMethod.SimplePay {

    /**
     * 신용카드 결제
     */
    record CreditCard(
        String cardNumber,
        String expiryDate,
        String cvc,
        int installmentMonths
    ) implements PaymentMethod {
        public CreditCard {
            if (cardNumber == null || cardNumber.isBlank()) {
                throw new IllegalArgumentException("Card number required");
            }
            if (expiryDate == null || expiryDate.isBlank()) {
                throw new IllegalArgumentException("Expiry date required");
            }
            if (cvc == null || cvc.length() != 3) {
                throw new IllegalArgumentException("CVC must be 3 digits");
            }
            if (installmentMonths < 0) {
                throw new IllegalArgumentException("Installment months must be >= 0");
            }
        }

        public CreditCard(String cardNumber, String expiryDate, String cvc) {
            this(cardNumber, expiryDate, cvc, 0);
        }

        public boolean isInstallment() {
            return installmentMonths > 0;
        }

        public String maskedCardNumber() {
            if (cardNumber.length() < 8) return "****";
            return cardNumber.substring(0, 4) + "-****-****-" +
                   cardNumber.substring(cardNumber.length() - 4);
        }
    }

    /**
     * 계좌이체 결제
     */
    record BankTransfer(
        String bankCode,
        String accountNumber,
        String holderName
    ) implements PaymentMethod {
        public BankTransfer {
            if (bankCode == null || bankCode.isBlank()) {
                throw new IllegalArgumentException("Bank code required");
            }
            if (accountNumber == null || accountNumber.isBlank()) {
                throw new IllegalArgumentException("Account number required");
            }
        }

        public String maskedAccountNumber() {
            if (accountNumber.length() < 6) return "****";
            return accountNumber.substring(0, 3) + "****" +
                   accountNumber.substring(accountNumber.length() - 3);
        }
    }

    /**
     * 포인트 결제
     */
    record Points(int amount) implements PaymentMethod {
        public Points {
            if (amount <= 0) {
                throw new IllegalArgumentException("Points must be positive");
            }
        }
    }

    /**
     * 간편결제 (카카오페이, 네이버페이, 토스 등)
     */
    record SimplePay(
        SimplePayProvider provider,
        String transactionToken
    ) implements PaymentMethod {
        public SimplePay {
            if (provider == null) {
                throw new IllegalArgumentException("Provider required");
            }
            if (transactionToken == null || transactionToken.isBlank()) {
                throw new IllegalArgumentException("Transaction token required");
            }
        }
    }
}

/**
 * 간편결제 공급자
 */
enum SimplePayProvider {
    KAKAO("카카오페이"),
    NAVER("네이버페이"),
    TOSS("토스");

    private final String displayName;

    SimplePayProvider(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
