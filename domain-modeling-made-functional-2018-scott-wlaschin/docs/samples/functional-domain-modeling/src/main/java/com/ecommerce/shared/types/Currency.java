package com.ecommerce.shared.types;

public enum Currency {
    KRW("원"),
    USD("달러"),
    EUR("유로");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
