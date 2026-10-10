package com.chukl.client.water;

import net.minecraft.util.math.Vec3d;

/** Stand-in for Water's Freecam: tracers simply start at the camera. */
public final class Freecam {
    private Freecam() { }
    public static Vec3d resolveTracerOrigin(Vec3d camPos, float tickDelta) { return camPos; }
}
