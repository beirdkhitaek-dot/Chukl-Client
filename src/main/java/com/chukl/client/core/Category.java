package com.chukl.client.core;

public enum Category {
    COMBAT("Combat"), MISC("Misc"), DONUT("Donut"), RENDER("Render"), CLIENT("Client");

    public final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}
