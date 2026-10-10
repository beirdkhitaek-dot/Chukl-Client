package com.chukl.client.settings;

import com.chukl.client.core.Config;
import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;
    protected final T def;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, String description, T def) {
        this.name = name;
        this.description = description;
        this.def = def;
        this.value = def;
    }

    public T get() { return value; }

    public void set(T v) {
        T s = sanitize(v);
        if (!s.equals(value)) {
            value = s;
            Config.markDirty();
        }
    }

    public void reset() { set(def); }
    protected T sanitize(T v) { return v; }

    public void visibleWhen(BooleanSupplier s) { this.visible = s; }

    public boolean visible() { return visible.getAsBoolean(); }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement e);
}
