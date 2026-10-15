package com.chukl.client.gui;

/**
 * A single eased value that smoothly chases a target, independent of frame rate. Used for the GUI's
 * fade-in panels and hover highlights (Argon-style "smooth" ClickGUI feel), driven by real elapsed
 * time rather than tick delta so it looks the same at any FPS.
 */
public final class Anim {
    private double value;
    private boolean inited;

    public Anim() { }

    public Anim(double initial) {
        this.value = initial;
        this.inited = true;
    }

    public double get() { return value; }

    public void snap(double v) { value = v; inited = true; }

    /**
     * Eases {@link #get()} toward {@code target}. {@code speed} is roughly "how much of the remaining
     * distance to close per second" (0.15-0.35 feels snappy without being instant).
     */
    public double step(double target, double dtSeconds, double speed) {
        if (!inited) { value = target; inited = true; return value; }
        double t = 1.0 - Math.exp(-speed * 60.0 * Math.max(0.0, dtSeconds));
        value += (target - value) * Math.min(1.0, Math.max(0.0, t));
        if (Math.abs(value - target) < 0.002) value = target;
        return value;
    }
}
