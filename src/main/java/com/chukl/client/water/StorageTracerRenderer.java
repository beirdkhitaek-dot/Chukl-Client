package com.chukl.client.water;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;

/** Stand-in for Water's tracer renderer: draws plain lines (camera-relative coordinates, like Water). */
public final class StorageTracerRenderer {
    private RenderUtils.PersistentBatch batch;

    public void begin(MatrixStack matrices) {
        if (batch == null) batch = RenderUtils.createTracerBatch();
        batch.begin(matrices);
    }

    public void addLine(Vec3d start, Vec3d end, Color color, float width) { batch.addLine(start, end, color, width); }

    public void flush() { if (batch != null) batch.flush(); }

    public void close() {
        if (batch != null) batch.close();
        batch = null;
    }
}
