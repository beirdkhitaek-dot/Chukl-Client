package com.chukl.client.sixseven;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Stand-in for 67Client's NotificationManager (no toast system here; sends a chat line instead). */
public final class Notify {
    private Notify() { }

    public static void info(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("§d[67] §f" + message), false);
    }
}
