package com.chukl.client.water;

import com.chukl.client.core.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Water's RenderUtils on top of Chukl's Render3D. Water modules give coordinates relative to the camera;
 * the batches add the camera position back. Everything is drawn through walls when flush() is called.
 */
public final class RenderUtils {
    private static Render3D current;

    private RenderUtils() { }

    static void begin(Render3D r) { current = r; }
    static void end() { current = null; }

    private static int argb(Color c) { return Render3D.argb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()); }

    // ---------- batches ----------
    public static PersistentBatch createPersistentBatch() { return new PersistentBatch(); }
    public static PersistentBatch createStorageBoxBatch() { return new PersistentBatch(); }
    public static PersistentBatch createTracerBatch() { return new PersistentBatch(); }
    public static WorldBatch beginWorldBatch(MatrixStack matrices) { return new WorldBatch(); }
    public static void restoreWorldDepthState() { }
    public static boolean isWorldBoxVisible(double a, double b, double c, double d, double e, double f) { return true; }

    public static void renderFilledBox(MatrixStack m, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        WorldBatch b = beginWorldBatch(m);
        b.renderFilledBox(x1, y1, z1, x2, y2, z2, color);
        b.flush();
    }

    public static void renderOutlineBox(MatrixStack m, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        WorldBatch b = beginWorldBatch(m);
        b.renderOutlineBox(x1, y1, z1, x2, y2, z2, color);
        b.flush();
    }

    public static void renderLine(MatrixStack m, Color color, Vec3d start, Vec3d end) { renderLine(m, color, start, end, 1.0F); }

    public static void renderLine(MatrixStack m, Color color, Vec3d start, Vec3d end, float lw) {
        WorldBatch b = beginWorldBatch(m);
        b.renderLine(color, start, end, lw);
        b.flush();
    }

    // ---------- camera helpers ----------
    public static Camera getCamera() { return MinecraftClient.getInstance().gameRenderer.getCamera(); }

    public static Vec3d getCameraPos(Camera camera) {
        if (current != null) return current.camera;
        return camera.getCameraPos();
    }

    public static Vec3d getCameraForward(Camera camera) {
        return new Vec3d(0.0, 0.0, 1.0).rotateX(-((float) Math.toRadians(camera.getPitch())))
            .rotateY(-((float) Math.toRadians(camera.getYaw()))).normalize();
    }

    public static Vec3d getCameraRight(Camera camera) {
        return new Vec3d(1.0, 0.0, 0.0).rotateY(-((float) Math.toRadians(camera.getYaw()))).normalize();
    }

    public static Vec3d getCameraUp(Vec3d forward, Vec3d right) { return forward.crossProduct(right).normalize(); }

    public static Vec3d getSpreadTracerEnd(double tx, double ty, double tz, Vec3d fwd, Vec3d right, Vec3d up, double endDist, double behindSpread) {
        double r = tx * right.x + ty * right.y + tz * right.z;
        double u = tx * up.x + ty * up.y + tz * up.z;
        double f = tx * fwd.x + ty * fwd.y + tz * fwd.z;
        double sf = Math.max(Math.abs(f), 0.25);
        double pr = r / sf;
        double pu = u / sf;
        if (f <= 0.0) {
            double pl = Math.hypot(pr, pu);
            if (pl < behindSpread) {
                if (pl < 1.0E-4) {
                    pr = behindSpread;
                    pu = 0.0;
                } else {
                    double s = behindSpread / pl;
                    pr *= s;
                    pu *= s;
                }
            }
        }
        return fwd.add(right.multiply(pr)).add(up.multiply(pu)).normalize().multiply(endDist);
    }

    public static Vec3d getSpreadTracerEnd(Vec3d t, Vec3d fwd, Vec3d right, Vec3d up, double endDist, double behindSpread) {
        return getSpreadTracerEnd(t.x, t.y, t.z, fwd, right, up, endDist, behindSpread);
    }

    public static Vec3d getClampedTracerEnd(Vec3d camRelTarget, Vec3d worldTarget, Vec3d fwd, Vec3d right, Vec3d up, double projDist) {
        return camRelTarget;
    }

    // ---------- drawing ----------
    private enum Kind { FILL, OUTLINE, LINE }

    private record Op(Kind kind, double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) { }

    private static void draw(List<Op> ops) {
        Render3D r = current;
        if (r == null || ops.isEmpty()) return;
        Vec3d c = r.camera;
        for (Op op : ops) {
            double x1 = op.x1 + c.x, y1 = op.y1 + c.y, z1 = op.z1 + c.z;
            double x2 = op.x2 + c.x, y2 = op.y2 + c.y, z2 = op.z2 + c.z;
            switch (op.kind) {
                case FILL -> r.box(x1, y1, z1, x2, y2, z2, op.color, op.color, Render3D.Mode.SIDES);
                case OUTLINE -> r.box(x1, y1, z1, x2, y2, z2, op.color, op.color, Render3D.Mode.LINES);
                case LINE -> r.line(x1, y1, z1, x2, y2, z2, op.color, op.width);
            }
        }
    }

    public static final class WorldBatch {
        private final List<Op> ops = new ArrayList<>();
        private WorldBatch() { }

        public void renderFilledBox(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            ops.add(new Op(Kind.FILL, x1, y1, z1, x2, y2, z2, argb(color), 1f));
        }
        public void renderFilledBoxTriangles(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            renderFilledBox(x1, y1, z1, x2, y2, z2, color);
        }
        public void renderOutlineBox(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            ops.add(new Op(Kind.OUTLINE, x1, y1, z1, x2, y2, z2, argb(color), 1f));
        }
        public void renderLine(Color color, Vec3d start, Vec3d end, float lineWidth) {
            ops.add(new Op(Kind.LINE, start.x, start.y, start.z, end.x, end.y, end.z, argb(color), lineWidth));
        }
        /** Water's glass panes become thin filled slabs. */
        public void renderGlassPane(double x1, double y, double z1, double x2, double z2, Color tint, float intensity, double time) {
            if (tint.getAlpha() <= 0 || intensity <= 0.0F || x2 <= x1 || z2 <= z1) return;
            ops.add(new Op(Kind.FILL, x1, y, z1, x2, y + 0.02, z2, argb(tint), 1f));
        }
        public void flush() { draw(ops); ops.clear(); }
        public void flushWithDepth() { flush(); }
    }

    public static final class PersistentBatch implements AutoCloseable {
        private final List<Op> ops = new ArrayList<>();
        private PersistentBatch() { }

        public void begin(MatrixStack m) { ops.clear(); }
        public void addFilledBox(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            ops.add(new Op(Kind.FILL, x1, y1, z1, x2, y2, z2, argb(color), 1f));
        }
        public void addOutlineBox(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            ops.add(new Op(Kind.OUTLINE, x1, y1, z1, x2, y2, z2, argb(color), 1f));
        }
        public void addLine(Vec3d start, Vec3d end, Color color, float width) {
            ops.add(new Op(Kind.LINE, start.x, start.y, start.z, end.x, end.y, end.z, argb(color), width));
        }
        public void flush() { draw(ops); ops.clear(); }
        public void flushWithDepth() { flush(); }
        public void flushFill() { flush(); }
        @Override public void close() { ops.clear(); }
    }
}
