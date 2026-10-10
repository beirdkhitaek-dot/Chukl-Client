package com.chukl.client.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    public final double min, max, step;

    public NumberSetting(String name, String description, double def, double min, double max, double step) {
        super(name, description, def);
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = sanitize(def);
    }

    @Override
    protected Double sanitize(Double v) {
        double x = v;
        if (step > 0) x = Math.round((x - min) / step) * step + min;
        x = Math.max(min, Math.min(max, x));
        return Math.round(x * 1_000_000d) / 1_000_000d;
    }

    public int getInt() { return (int) Math.round(value); }
    public float getFloat() { return value.floatValue(); }
    public double fraction() { return max == min ? 0 : (value - min) / (max - min); }

    public String display() {
        return step >= 1 ? String.valueOf(getInt()) : String.format("%.2f", value);
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
}
