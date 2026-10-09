package com.pixelazer.core;

public enum DitheringMode {
    OFF("Выключен"),
    FLOYD_STEINBERG("Floyd–Steinberg"),
    ORDERED("Упорядоченный"),
    EDGE_LIMITED("Только вне контуров");

    private final String displayName;
    DitheringMode(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
