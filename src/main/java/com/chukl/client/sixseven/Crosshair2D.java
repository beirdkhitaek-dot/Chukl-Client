package com.chukl.client.sixseven;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * Minimal 2D vector drawing on top of DrawContext, filling in for 67Client's NanoVG-backed NVGRenderer
 * (dev.sixseven.render.nanovg.NVGRenderer isn't available to Chukl). Lines are either axis-aligned (fast
 * path via DrawContext.fill) or stepped as small filled squares for the rare diagonal case (the chevron
 * crosshair style); circles are scanline-filled. Good enough for small on-screen marks like a crosshair.
 */
public final class Crosshair2D {
    private final DrawContext ctx;

    public Crosshair2D(DrawContext ctx) { this.ctx = ctx; }

    public void line(float x1, float y1, float x2, float y2, float thickness, int argb) {
        if (y1 == y2) { hLine(Math.min(x1, x2), Math.max(x1, x2), y1, thickness, argb); return; }
        if (x1 == x2) { vLine(x1, Math.min(y1, y2), Math.max(y1, y2), thickness, argb); return; }
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        float half = thickness / 2f;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float x = x1 + dx * t;
            float y = y1 + dy * t;
            ctx.fill((int) (x - half), (int) (y - half), (int) Math.ceil(x + half), (int) Math.ceil(y + half), argb);
        }
    }

    public void hLine(float x1, float x2, float y, float thickness, int argb) {
        float half = thickness / 2f;
        ctx.fill((int) Math.floor(x1), (int) Math.floor(y - half), (int) Math.ceil(x2), (int) Math.ceil(y + half), argb);
    }

    public void vLine(float x, float y1, float y2, float thickness, int argb) {
        float half = thickness / 2f;
        ctx.fill((int) Math.floor(x - half), (int) Math.floor(y1), (int) Math.ceil(x + half), (int) Math.ceil(y2), argb);
    }

    public void circle(float cx, float cy, float r, int argb) {
        int ri = Math.round(r);
        for (int dy = -ri; dy <= ri; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill((int) (cx - dx), (int) (cy + dy), (int) (cx + dx + 1), (int) (cy + dy + 1), argb);
        }
    }

    public void circleOutline(float cx, float cy, float r, float thickness, int argb) {
        float inner = Math.max(0, r - thickness);
        int ri = Math.round(r);
        for (int dy = -ri; dy <= ri; dy++) {
            double outerDx = Math.sqrt(Math.max(0, r * r - (double) dy * dy));
            int ox = (int) Math.round(outerDx);
            if (Math.abs(dy) > inner) {
                ctx.fill((int) (cx - ox), (int) (cy + dy), (int) (cx + ox + 1), (int) (cy + dy + 1), argb);
            } else {
                int ix = (int) Math.round(Math.sqrt(Math.max(0, inner * inner - (double) dy * dy)));
                ctx.fill((int) (cx - ox), (int) (cy + dy), (int) (cx - ix), (int) (cy + dy + 1), argb);
                ctx.fill((int) (cx + ix), (int) (cy + dy), (int) (cx + ox + 1), (int) (cy + dy + 1), argb);
            }
        }
    }

    public void circleGlow(float cx, float cy, float r, float spread, int argb) {
        int steps = 3;
        int baseAlpha = (argb >>> 24) & 0xFF;
        for (int i = 0; i < steps; i++) {
            float rr = r + spread * (i + 1) / steps;
            int a = (int) (baseAlpha * (1f - (float) i / steps) * 0.5f);
            circleOutline(cx, cy, rr, 1.5f, (a << 24) | (argb & 0xFFFFFF));
        }
    }

    public void chevron(float cx, float cy, float size, float thickness, int argb, boolean pointDown) {
        if (pointDown) {
            line(cx - size, cy - size, cx, cy, thickness, argb);
            line(cx, cy, cx + size, cy - size, thickness, argb);
        } else {
            line(cx - size, cy + size, cx, cy, thickness, argb);
            line(cx, cy, cx + size, cy + size, thickness, argb);
        }
    }

    public float textWidth(String s, float size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.textRenderer.getWidth(s) * (size / 9f);
    }

    public void text(String s, float x, float y, float size, int argb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float scale = size / 9f;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        ctx.drawText(mc.textRenderer, s, 0, 0, argb, false);
        m.popMatrix();
    }

    public void textGlow(String s, float x, float y, float size, int argb) {
        text(s, x, y, size, argb);
    }
}
