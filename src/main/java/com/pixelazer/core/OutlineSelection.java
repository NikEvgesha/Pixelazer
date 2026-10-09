package com.pixelazer.core;

public enum OutlineSelection {
    AUTO("Автоматически"),
    DARKEST("Самый тёмный цвет"),
    MANUAL("Выбрать вручную");

    private final String displayName;
    OutlineSelection(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
