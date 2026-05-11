package org.example.coffee.common;

public enum ProductSite {
    USER,
    ADMIN;

    public static ProductSite from(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return ProductSite.valueOf(value.trim().toUpperCase());
    }
}
