package com.smartparking.model;

public enum SpotType {
    SMALL("S"),
    MEDIUM("M"),
    LARGE("L");

    private final String code;

    SpotType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
