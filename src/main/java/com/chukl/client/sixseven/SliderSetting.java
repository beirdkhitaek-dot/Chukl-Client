package com.chukl.client.sixseven;

import com.chukl.client.settings.NumberSetting;

import java.util.function.DoubleFunction;

public class SliderSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;
    private final String suffix;
    private DoubleFunction<String> labelFn;

    public SliderSetting(String name, String description, double defaultValue, double min, double max, double step) {
        this(name, description, defaultValue, min, max, step, "");
    }

    public SliderSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix == null ? "" : suffix;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public String getSuffix() { return suffix; }
    public float getFloat() { return get().floatValue(); }
    public int getInt() { return (int) Math.round(get()); }
    public double getNormalized() { double v = get(); return max == min ? 0 : (v - min) / (max - min); }

    public void setNormalized(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        set(min + (max - min) * t);
    }

    public SliderSetting withLabel(DoubleFunction<String> labelFn) {
        this.labelFn = labelFn;
        return this;
    }

    public String formatValue() {
        double v = get();
        if (labelFn != null) return labelFn.apply(v);
        String s;
        if (step >= 1.0 && v == Math.floor(v)) {
            s = Long.toString((long) v);
        } else {
            s = String.valueOf(Math.round(v * 100.0) / 100.0);
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        }
        return s + suffix;
    }

    @Override
    public void set(Double newValue) {
        double snapped = step > 0 ? Math.round(newValue / step) * step : newValue;
        snapped = Math.round(snapped * 1_000_000.0) / 1_000_000.0;
        super.set(Math.max(min, Math.min(max, snapped)));
    }

    @Override protected void pull() { if (backing != null) value = (Double) backing.get(); }
    @Override protected void pushToBacking(Double v) { if (backing != null) ((NumberSetting) backing).set(v); }

    @Override
    public com.chukl.client.settings.Setting<?> build() {
        NumberSetting n = new NumberSetting(getName(), getDescription(), value, min, max, step <= 0 ? 1 : step);
        n.visibleWhen(this::isVisible);
        backing = n;
        return n;
    }
}
