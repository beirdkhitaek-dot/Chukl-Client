package com.chukl.client.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Scanline-based rounded-rect and circle fills, drawn with nothing but repeated {@link DrawContext#fill}
 * calls (one solid-color row at a time) -- no shaders, no vertex buffers, so it works anywhere a flat
 * {@code ctx.fill(...)} does. Same technique Argon's ClickGui uses for its rounded panels/buttons.
 */
public final class GuiShapes {
    private GuiShapes() { }

    /** Rounded rect with a separate radius per corner (top-left, top-right, bottom-left, bottom-right). */
    public static void roundRect(DrawContext ctx, int x, int y, int x2, int y2, int rTL, int rTR, int rBL, int rBR, int argb) {
        int left = Math.min(x, x2), right = Math.max(x, x2);
        int top = Math.min(y, y2), bottom = Math.max(y, y2);
        int w = right - left, h = bottom - top;
        if (w <= 0 || h <= 0) return;
        int radTL = clampRadius(rTL, w, h), radTR = clampRadius(rTR, w, h);
        int radBL = clampRadius(rBL, w, h), radBR = clampRadius(rBR, w, h);
        for (int row = 0; row < h; row++) {
            int li = inset(row, h, radTL, radBL);
            int ri = inset(row, h, radTR, radBR);
            int ly = top + row;
            int lx = left + li, rx = right - ri;
            if (rx > lx) ctx.fill(lx, ly, rx, ly + 1, argb);
        }
    }

    /** Same radius on every corner. */
    public static void roundRect(DrawContext ctx, int x, int y, int x2, int y2, int radius, int argb) {
        roundRect(ctx, x, y, x2, y2, radius, radius, radius, radius, argb);
    }

    public static void circle(DrawContext ctx, double cx, double cy, double radius, int argb) {
        int top = (int) Math.floor(cy - radius);
        int bottom = (int) Math.ceil(cy + radius);
        double r2 = radius * radius;
        for (int py = top; py < bottom; py++) {
            double dy = (py + 0.5) - cy;
            double inside = r2 - dy * dy;
            if (inside <= 0) continue;
            double dx = Math.sqrt(inside);
            int left = (int) Math.floor(cx - dx);
            int right = (int) Math.ceil(cx + dx);
            if (right > left) ctx.fill(left, py, right, py + 1, argb);
        }
    }

    private static int clampRadius(int r, int w, int h) {
        return Math.max(0, Math.min(r, Math.min(w, h) / 2));
    }

    private static int inset(int row, int h, int top, int bottom) {
        int i = 0;
        if (top > 0 && row < top) i = Math.max(i, cornerInset(top, row));
        if (bottom > 0 && row >= h - bottom) i = Math.max(i, cornerInset(bottom, h - 1 - row));
        return i;
    }

    private static int cornerInset(int radius, int offsetFromEdge) {
        double d = radius - (offsetFromEdge + 0.5);
        double inside = (double) radius * radius - d * d;
        if (inside <= 0) return radius;
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(inside)));
    }
}
