package com.chukl.client.core;

import com.chukl.client.modules.client.ClickGuiModule;
import com.chukl.client.modules.client.FontsModule;
import com.chukl.client.modules.client.hud.*;
import com.chukl.client.modules.combat.*;
import com.chukl.client.modules.donut.*;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() { }

    /** Add new modules here: register(new MyModule()); */
    public static void init() {
        register(new ClickGuiModule());
        register(new FontsModule());
        register(new WatermarkModule());
        register(new CoordinatesModule());
        register(new ModuleListModule());
        register(new ClockModule());
        register(new LogoModule());

        // Donut
        register(new WaterSusChunk());
        register(new ChuklSusChunkFinder());
        register(new KrispyChunkFinder());
        register(new NeonChunkFinder());
        register(new OGChunkFinder());
        register(new SixSevenSusChunkFinder());
        register(new AntiVanishModule());

        // Combat
        register(new AnchorMacro());
        register(new AutoCrystal());
        register(new AutoCrystalAC());
        register(new AutoDoubleHand());
        register(new AutoHitCrystal());
        register(new AutoTotem());
        register(new AutoInvTotem());
        register(new DoubleAnchor());
        register(new Hitbox());
        register(new HoverTotem());
        register(new ShieldBreaker());
        register(new SingleAnchor());
        register(new SpearSwap());
        register(new Triggerbot());
        register(new HitAura());
        register(new LegitAura());
    }

    public static void register(Module m) { MODULES.add(m); }
    public static List<Module> all() { return MODULES; }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> cls) {
        for (Module m : MODULES) if (m.getClass() == cls) return (T) m;
        return null;
    }

    public static Module byName(String n) {
        String k = n.replace(" ", "");
        for (Module m : MODULES) if (m.name.replace(" ", "").equalsIgnoreCase(k)) return m;
        return null;
    }

    public static void keyPress(int key) {
        for (Module m : MODULES) {
            if (!m.isEnabled()) continue;
            try { if (m.onKeyPress(key)) return; } catch (Throwable t) { com.chukl.client.ChuklClient.LOG.error("onKeyPress {}", m.name, t); }
        }
    }

    public static void tick() {
        for (Module m : MODULES) if (m.isEnabled()) m.tickSafe();
    }

    public static void render3D(net.minecraft.client.render.VertexConsumerProvider.Immediate b, net.minecraft.client.util.math.MatrixStack m, net.minecraft.util.math.Vec3d cam) {
        for (Module mod : MODULES) if (mod.isEnabled()) mod.render3DSafe(b, m, cam);
    }

    public static void packetReceive(net.minecraft.network.packet.Packet<?> p) {
        for (Module mod : MODULES) if (mod.isEnabled()) mod.packetReceiveSafe(p);
    }

    /** True if any module cancelled the packet. */
    public static boolean packetSend(net.minecraft.network.packet.Packet<?> p) {
        boolean cancel = false;
        for (Module mod : MODULES) if (mod.isEnabled() && mod.packetSendSafe(p)) cancel = true;
        return cancel;
    }

    public static void render3D(Render3D r) {
        for (Module m : MODULES) if (m.isEnabled()) m.render3DSafe(r);
        r.flush();
    }

    public static void packet(net.minecraft.network.packet.Packet<?> p) {
        com.chukl.client.suschunk.ServerLightCache.get().onPacket(p);
        for (Module m : MODULES) if (m.isEnabled()) m.packetSafe(p);
    }

    public static void render2D(DrawContext ctx, float delta) {
        for (Module m : MODULES) if (m.isEnabled()) m.renderSafe(ctx, delta);
    }
}
