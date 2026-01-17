package com.ecommerce.domain.product;

/**
 * 창고 위치 Value Object
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
