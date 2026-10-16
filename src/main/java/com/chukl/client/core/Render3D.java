package com.chukl.client.core;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * Drawing in the 3D world. Passed to Module.onRender3D. All coordinates are world coordinates.
 * Colors are ARGB ints. Things are drawn through walls.
 */
public final class Render3D {
    public enum Mode { SIDES, LINES, BOTH }

    private static final RenderPipeline FILL_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
        .withLocation("chuklclient/pipeline/fill")
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build();
    public static final RenderLayer FILL = RenderLayer.of("chuklclient:fill", RenderSetup.builder(FILL_PIPELINE).translucent().build());

    private static final RenderPipeline LINE_PIPELINE = RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.RENDERTYPE_LINES_SNIPPET})
        .withLocation("chuklclient/pipeline/lines")
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build();
    public static final RenderLayer LINES = RenderLayer.of("chuklclient:lines", RenderSetup.builder(LINE_PIPELINE).build());

    private final VertexConsumerProvider.Immediate buffers;
    private final MatrixStack matrices;
    public final Vec3d camera;

    public Render3D(VertexConsumerProvider.Immediate buffers, MatrixStack matrices, Vec3d camera) {
        this.buffers = buffers;
        this.matrices = matrices;
        this.camera = camera;
    }

    /** Raw access for renderers (e.g. {@link com.chukl.client.render.EspBoxRenderer}) that build their own vertex layers. */
    public VertexConsumerProvider.Immediate buffers() { return buffers; }
    public MatrixStack matrices() { return matrices; }

    /** Horizontal quad at height y. */
    public void quadY(double x1, double z1, double x2, double z2, double y, int color) {
        VertexConsumer b = buffers.getBuffer(FILL);
        MatrixStack.Entry p = matrices.peek();
        float ax = (float) (x1 - camera.x), az = (float) (z1 - camera.z);
        float bx = (float) (x2 - camera.x), bz = (float) (z2 - camera.z);
        float fy = (float) (y - camera.y);
        b.vertex(p, ax, fy, az).color(color);
        b.vertex(p, ax, fy, bz).color(color);
        b.vertex(p, bx, fy, bz).color(color);
        b.vertex(p, bx, fy, az).color(color);
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) {
        VertexConsumer b = buffers.getBuffer(LINES);
        MatrixStack.Entry p = matrices.peek();
        float ax = (float) (x1 - camera.x), ay = (float) (y1 - camera.y), az = (float) (z1 - camera.z);
        float bx = (float) (x2 - camera.x), by = (float) (y2 - camera.y), bz = (float) (z2 - camera.z);
        Vector3f n = new Vector3f(bx - ax, by - ay, bz - az);
        if (n.lengthSquared() < 1.0E-8f) return;
        n.normalize();
        b.vertex(p, ax, ay, az).color(color).normal(p, n).lineWidth(width);
        b.vertex(p, bx, by, bz).color(color).normal(p, n).lineWidth(width);
    }

    public void box(BlockPos pos, int fill, int line, Mode mode) {
        box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, fill, line, mode);
    }

    public void box(double x1, double y1, double z1, double x2, double y2, double z2, int fill, int line, Mode mode) {
        if (mode != Mode.LINES) {
            VertexConsumer b = buffers.getBuffer(FILL);
            MatrixStack.Entry p = matrices.peek();
            float ax = (float) (x1 - camera.x), ay = (float) (y1 - camera.y), az = (float) (z1 - camera.z);
            float bx = (float) (x2 - camera.x), by = (float) (y2 - camera.y), bz = (float) (z2 - camera.z);
            // bottom, top
            face(b, p, fill, ax, ay, az, bx, ay, az, bx, ay, bz, ax, ay, bz);
            face(b, p, fill, ax, by, az, bx, by, az, bx, by, bz, ax, by, bz);
            // north, south
            face(b, p, fill, ax, ay, az, bx, ay, az, bx, by, az, ax, by, az);
            face(b, p, fill, ax, ay, bz, bx, ay, bz, bx, by, bz, ax, by, bz);
            // west, east
            face(b, p, fill, ax, ay, az, ax, ay, bz, ax, by, bz, ax, by, az);
            face(b, p, fill, bx, ay, az, bx, ay, bz, bx, by, bz, bx, by, az);
        }
        if (mode != Mode.SIDES) {
            float w = 2.0f;
            line(x1, y1, z1, x2, y1, z1, line, w); line(x2, y1, z1, x2, y1, z2, line, w);
            line(x2, y1, z2, x1, y1, z2, line, w); line(x1, y1, z2, x1, y1, z1, line, w);
            line(x1, y2, z1, x2, y2, z1, line, w); line(x2, y2, z1, x2, y2, z2, line, w);
            line(x2, y2, z2, x1, y2, z2, line, w); line(x1, y2, z2, x1, y2, z1, line, w);
            line(x1, y1, z1, x1, y2, z1, line, w); line(x2, y1, z1, x2, y2, z1, line, w);
            line(x2, y1, z2, x2, y2, z2, line, w); line(x1, y1, z2, x1, y2, z2, line, w);
        }
    }

    private static void face(VertexConsumer b, MatrixStack.Entry p, int c,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4) {
        b.vertex(p, x1, y1, z1).color(c);
        b.vertex(p, x2, y2, z2).color(c);
        b.vertex(p, x3, y3, z3).color(c);
        b.vertex(p, x4, y4, z4).color(c);
    }

    /** Draws everything queued so far. Called by the world render hook after all modules ran. */
    public void flush() {
        buffers.draw(FILL);
        buffers.draw(LINES);
    }

    /** ARGB from 0-255 channels. */
    public static int argb(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
