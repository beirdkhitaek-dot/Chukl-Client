package com.chukl.client.water;

import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ColorSetting;
import com.chukl.client.settings.NumberSetting;

import java.awt.Color;
import java.util.function.Supplier;

/**
 * Water-style setting (getValue/setValue/visibleWhen). When the module registers it with addSetting() it
 * becomes a real Chukl setting, so it shows in the Click GUI and is saved in the config.
 */
public class Setting<T> {
    private final String name;
    private final T defaultValue;
    private final T min;
    private final T max;
    private T value;
    private Supplier<Boolean> visibility = () -> true;

    private com.chukl.client.settings.Setting<?> backing;

    public Setting(String name, T value) { this(name, value, null, null); }

    public Setting(String name, T value, T min, T max) {
        this.name = name;
        this.value = value;
        this.defaultValue = value;
        this.min = min;
        this.max = max;
    }

    public String getName() { return name; }
    public T getDefaultValue() { return defaultValue; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public boolean matchesName(String n) { return name.equalsIgnoreCase(n); }

    @SuppressWarnings("unchecked")
    public T getValue() {
        if (backing == null) return value;
        Object v = backing.get();
        if (defaultValue instanceof Boolean) return (T) v;
        if (defaultValue instanceof Integer) return (T) (Integer) (int) Math.round((Double) v);
        if (defaultValue instanceof Float) return (T) (Float) ((Double) v).floatValue();
        if (defaultValue instanceof Double) return (T) v;
        if (defaultValue instanceof Color) {
            int a = (Integer) v;
            return (T) new Color((a >> 16) & 255, (a >> 8) & 255, a & 255, (a >>> 24) & 255);
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setValue(T v) {
        this.value = v;
        if (backing == null) return;
        if (v instanceof Boolean || v instanceof Number) {
            Object o = v instanceof Number n ? (Object) n.doubleValue() : v;
            ((com.chukl.client.settings.Setting) backing).set(o);
        } else if (v instanceof Color c) {
            ((com.chukl.client.settings.Setting) backing).set((c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
        }
    }

    public Setting<T> visibleWhen(Supplier<Boolean> v) {
        this.visibility = v == null ? () -> true : v;
        return this;
    }

    public boolean isVisible() {
        try { return visibility.get(); } catch (Exception e) { return true; }
    }

    /** Builds the Chukl setting that stores this value, or null when the type is only kept in memory. */
    com.chukl.client.settings.Setting<?> build() {
        if (defaultValue instanceof Boolean b) {
            backing = new BoolSetting(name, name, b);
        } else if (defaultValue instanceof Number n) {
            double d = n.doubleValue();
            boolean whole = defaultValue instanceof Integer;
            double lo = min instanceof Number x ? x.doubleValue() : Math.min(0, d);
            double hi = max instanceof Number x ? x.doubleValue() : Math.max(100, Math.abs(d) * 10);
            double step = whole ? 1 : Math.max(0.01, (hi - lo) / 100.0);
            backing = new NumberSetting(name, name, d, lo, hi, step);
        } else if (defaultValue instanceof Color c) {
            backing = new ColorSetting(name, name, (c.getAlpha() << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue());
        }
        if (backing != null) backing.visibleWhen(this::isVisible);
        return backing;
    }
}
