package com.freya02.emojis;

public enum Fritzpatrick {

    LIGHT("\uD83C\uDFFB"),
    MEDIUM_LIGHT("\uD83C\uDFFC"),
    MEDIUM("\uD83C\uDFFD"),
    MEDIUM_DARK("\uD83C\uDFFE"),
    DARK("\uD83C\uDFFF");

    private final String unicode;

    Fritzpatrick(String unicode) {
        this.unicode = unicode;
    }

    public String getUnicode() {
        return unicode;
    }
}
