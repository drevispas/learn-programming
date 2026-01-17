package com.ecommerce.domain.order;

/**
 * 배송지 주소 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: Compound Value Object</h2>
 * 여러 필드(수령인, 전화번호, 우편번호, 주소)를 하나의 의미 있는 단위로 묶는다.
 *
 * <h2>불변식 (Invariant)</h2>
 * recipient와 address는 필수, 나머지는 선택.
 *
 * <h2>파생 값</h2>
 * fullAddress()는 저장하지 않고 필요 시 계산 (Single Source of Truth).
 */
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
