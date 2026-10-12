package com.chukl.client.sixseven;

import java.util.function.BooleanSupplier;

/**
 * Stand-in for 67Client's dev.sixseven.settings.Setting&lt;T&gt;. Keeps the same get/set/reset/visibleWhen
 * API so ported modules need no changes beyond their import. When addSetting() registers it, it builds a
 * real Chukl setting (see each subclass's build()) so it shows in the Click GUI and saves with the config.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    protected final T defaultValue;
    private BooleanSupplier visibility = () -> true;

    protected com.chukl.client.settings.Setting<?> backing;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public T get() {
        pull();
        return value;
    }

    public void set(T v) {
        this.value = v;
        pushToBacking(v);
    }

    public void reset() { set(defaultValue); }

    public void visibleWhen(BooleanSupplier condition) {
        this.visibility = condition == null ? () -> true : condition;
    }

    public boolean isVisible() {
        try { return visibility.getAsBoolean(); } catch (Exception e) { return true; }
    }

    /** Refreshes {@link #value} from the backing Chukl setting, if one is bound. */
    protected void pull() { }

    /** Pushes a new value into the backing Chukl setting, if one is bound. */
    protected void pushToBacking(T v) { }

    /** Builds (once) the real Chukl setting this is backed by; null keeps the value in-memory only. */
    public com.chukl.client.settings.Setting<?> build() { return null; }
}
