package com.ecommerce.domain.order;

public record ShippingAddress(
    String recipient,
    String phoneNumber,
    String zipCode,
    String address,
    String detailAddress
) {
    public ShippingAddress {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address required");
        }
    }

    public static ShippingAddress simple(String address) {
        return new ShippingAddress("수령인", null, null, address, null);
    }

    public String fullAddress() {
        StringBuilder sb = new StringBuilder(address);
        if (detailAddress != null && !detailAddress.isBlank()) {
            sb.append(" ").append(detailAddress);
        }
        return sb.toString();
    }
}
