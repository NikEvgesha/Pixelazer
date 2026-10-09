package com.pixelazer.core;

public enum OutlineThickness {
    THIN("Тонкий"),
    NORMAL("Обычный"),
    THICK("Усиленный");

    private final String displayName;
    OutlineThickness(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
