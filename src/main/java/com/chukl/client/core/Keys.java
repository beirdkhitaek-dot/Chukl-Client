package com.chukl.client.core;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Polls bound keys. Only fires while in-world with no screen open. */
public final class Keys {
    private static Map<Integer, Boolean> prev = new HashMap<>();

    private Keys() { }

    public static void tick(MinecraftClient mc) {
        if (mc.getWindow() == null) return;
        long h = mc.getWindow().getHandle();
        boolean active = mc.player != null && mc.currentScreen == null;
        Map<Integer, Boolean> now = new HashMap<>();
        for (Module m : ModuleManager.all()) {
            int k = m.getBind();
            if (k >= 0 && !now.containsKey(k)) now.put(k, GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS);
        }
        for (Module m : ModuleManager.all()) {
            int k = m.getBind();
            if (k < 0) continue;
            boolean down = now.get(k);
            boolean was = prev.getOrDefault(k, false);
            if (down && !was && active) m.toggle();
        }
        prev = now;
    }
}
