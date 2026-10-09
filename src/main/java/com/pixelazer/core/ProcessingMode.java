package com.pixelazer.core;

public enum ProcessingMode {
    AUTO("Автоматически"),
    PHOTO("Фото / пейзаж"),
    ILLUSTRATION("Иллюстрация / чёткие контуры"),
    LINE_ART("Лайн-арт / комикс");

    private final String displayName;

    ProcessingMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
