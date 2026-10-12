package com.chukl.client.antivanish;

import com.chukl.client.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Small chat builder used by the Anti Vanish port. */
public final class ModuleChat {
    private ModuleChat() { }

    public static Body body() { return new Body(); }

    public static void send(Module module, Body body) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        MutableText line = Text.literal("[" + module.name + "] ").formatted(Formatting.LIGHT_PURPLE);
        line.append(body.build());
        mc.player.sendMessage(line, false);
    }

    public static final class Body {
        private final MutableText out = Text.empty();

        private Body add(String s, Formatting f) {
            out.append(Text.literal(s).formatted(f));
            return this;
        }

        public Body text(String s) { return add(s, Formatting.WHITE); }
        public Body name(String s) { return add(s, Formatting.AQUA); }
        public Body good(String s) { return add(s, Formatting.GREEN); }
        public Body warn(String s) { return add(s, Formatting.YELLOW); }
        public Body danger(String s) { return add(s, Formatting.RED); }
        public Body value(String s) { return add(s, Formatting.GOLD); }
        public Body append(Text t) { out.append(t); return this; }
        public Text build() { return out.copy(); }
    }
}
