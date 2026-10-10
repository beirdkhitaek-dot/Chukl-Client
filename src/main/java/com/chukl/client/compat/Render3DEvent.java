package com.chukl.client.compat;

import com.chukl.client.core.Render3D;
import net.minecraft.util.math.BlockPos;

/** Shaped like Meteor's Render3DEvent so ported modules keep `event.renderer.box(...)`. */
public class Render3DEvent {
    public final Renderer renderer;
    public final Render3D r3d;

    public Render3DEvent(Render3D r3d) {
        this.r3d = r3d;
        this.renderer = new Renderer(r3d);
    }

    public static final class Renderer {
        private final Render3D r;

        Renderer(Render3D r) { this.r = r; }

        public void box(double x1, double y1, double z1, double x2, double y2, double z2, Color side, Color line, ShapeMode mode, int excludeDir) {
            r.box(x1, y1, z1, x2, y2, z2, side.argb(), line.argb(), mode.mode());
        }

        public void box(BlockPos pos, Color side, Color line, ShapeMode mode, int excludeDir) {
            r.box(pos, side.argb(), line.argb(), mode.mode());
        }

        public void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
            r.line(x1, y1, z1, x2, y2, z2, color.argb(), 2.0f);
        }
    }
}
