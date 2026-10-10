package com.chukl.client.compat;

/** Small stand-in for the Meteor color classes, so ported modules read the same. */
public class Color {
    public int r, g, b, a;

    public Color(int r, int g, int b) { this(r, g, b, 255); }

    public Color(int r, int g, int b, int a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
    }

    public Color(Color c) { this(c.r, c.g, c.b, c.a); }

    public Color set(int r, int g, int b, int a) { this.r = r; this.g = g; this.b = b; this.a = a; return this; }

    public int argb() {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static Color ofArgb(int argb) {
        return new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
