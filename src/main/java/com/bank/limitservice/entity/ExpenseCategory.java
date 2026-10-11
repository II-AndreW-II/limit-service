package com.bank.limitservice.entity;

public enum ExpenseCategory {
    PRODUCT,
    SERVICE;

    public static ExpenseCategory fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("expense_category must not be null");
        }
        return switch (value.toLowerCase()) {
            case "product" -> PRODUCT;
            case "service" -> SERVICE;
            default -> throw new IllegalArgumentException("Unknown expense_category: " + value);
        };
    }

    public String toApiValue() {
        return name().toLowerCase();
    }
}
