package com.pixelazer.core;

public enum DetailLevel {
    LOW("Низкий"),
    BALANCED("Средний"),
    HIGH("Высокий");

    private final String displayName;
    DetailLevel(String displayName) { this.displayName = displayName; }
    public double lineThresholdFactor() {
        return switch (this) {
            case LOW -> 0.72;
            case BALANCED -> 0.45;
            case HIGH -> 0.22;
        };
    }
    @Override public String toString() { return displayName; }
}
