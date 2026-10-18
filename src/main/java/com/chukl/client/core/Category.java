package com.chukl.client.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A module group shown as its own panel in the Click GUI. Not an enum, so addons can register their own
 * category instead of filing their modules under an existing one -- the same extensibility Meteor Client
 * gives addons. Existing code that says Category.COMBAT, Category.values(), Category.valueOf(id) or
 * cat.name() keeps working unchanged; register(...) is the only new entry point.
 */
public final class Category {
    private static final Map<String, Category> BY_ID = new LinkedHashMap<>();

    public static final Category COMBAT = register("COMBAT", "Combat");
    public static final Category MISC = register("MISC", "Misc");
    public static final Category DONUT = register("DONUT", "Donut");
    public static final Category RENDER = register("RENDER", "Render");
    public static final Category CLIENT = register("CLIENT", "Client");

    public final String displayName;
    private final String id;

    private Category(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Registers a new category (or returns the existing one if the id is already taken). id is the stable
     * identifier saved to config (Click GUI panel layout/order) -- addons should pass something unlikely to
     * collide with another addon, such as the addon's own name. Safe to call more than once with the same
     * id; it will not create a duplicate panel.
     */
    public static synchronized Category register(String id, String displayName) {
        String key = id.trim().toUpperCase(Locale.ROOT);
        Category existing = BY_ID.get(key);
        if (existing != null) return existing;
        Category c = new Category(key, displayName);
        BY_ID.put(key, c);
        return c;
    }

    public static Category[] values() {
        return BY_ID.values().toArray(new Category[0]);
    }

    public static Category valueOf(String id) {
        Category c = BY_ID.get(id.toUpperCase(Locale.ROOT));
        if (c == null) throw new IllegalArgumentException("No Category: " + id);
        return c;
    }

    /** Stable identifier (the enum-constant-style id passed to register), not the display name. */
    public String name() { return id; }

    @Override
    public String toString() { return displayName; }
}
