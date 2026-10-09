package com.pixelazer.core;

public enum GridAlignmentMode {
    AUTO("Автоматически"),
    CENTERED("Без смещения"),
    MANUAL("Ручное смещение");

    private final String displayName;
    GridAlignmentMode(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
