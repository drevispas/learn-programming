package com.ecommerce.domain.product;

/**
 * 창고 위치 Value Object (Chapter 2)
 *
 * <h2>핵심 개념: Compound Value Object</h2>
 * 창고 코드, 섹션, 선반을 하나의 위치 개념으로 묶는다.
 * 예: "A-1-3" = 창고A, 섹션1, 선반3
 */
public record WarehouseLocation(String code, String section, String shelf) {
    public WarehouseLocation {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Warehouse code required");
        }
    }

    public String fullLocation() {
        StringBuilder sb = new StringBuilder(code);
        if (section != null && !section.isBlank()) {
            sb.append("-").append(section);
        }
        if (shelf != null && !shelf.isBlank()) {
            sb.append("-").append(shelf);
        }
        return sb.toString();
    }

    public static WarehouseLocation of(String code) {
        return new WarehouseLocation(code, null, null);
    }
}
