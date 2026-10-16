package com.chukl.client.core;

import com.chukl.client.ChuklClient;
import com.chukl.client.modules.client.ClickGuiModule;
import com.chukl.client.modules.client.FontsModule;
import com.chukl.client.modules.client.hud.*;
import com.chukl.client.modules.combat.*;
import com.chukl.client.modules.donut.*;
import com.chukl.client.modules.misc.*;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() { }

    /** Add new modules here: safeRegister(MyModule::new); */
    public static void init() {
        safeRegister(ClickGuiModule::new);
        safeRegister(FontsModule::new);
        safeRegister(WatermarkModule::new);
        safeRegister(CoordinatesModule::new);
        safeRegister(ModuleListModule::new);
        safeRegister(ClockModule::new);
        safeRegister(LogoModule::new);

        // Donut
        safeRegister(WaterSusChunk::new);
        safeRegister(ChuklSusChunkFinder::new);
        safeRegister(KrispyChunkFinder::new);
        safeRegister(NeonChunkFinder::new);
        safeRegister(OGChunkFinder::new);
        safeRegister(SixSevenSusChunkFinder::new);
        safeRegister(AntiVanishModule::new);
        safeRegister(StorageEspModule::new);
        safeRegister(BlockEspModule::new);
        safeRegister(SusChunkV2Module::new);
        safeRegister(ChunkFinderBetterModule::new);
        safeRegister(LuckyFinderModule::new);

        // Combat
        safeRegister(AnchorMacro::new);
        safeRegister(AutoCrystal::new);
        safeRegister(AutoCrystalAC::new);
        safeRegister(AutoDoubleHand::new);
        safeRegister(AutoHitCrystal::new);
        safeRegister(AutoTotem::new);
        safeRegister(AutoInvTotem::new);
        safeRegister(DoubleAnchor::new);
        safeRegister(Hitbox::new);
        safeRegister(HoverTotem::new);
        safeRegister(ShieldBreaker::new);
        safeRegister(SingleAnchor::new);
        safeRegister(SpearSwap::new);
        safeRegister(Triggerbot::new);
        safeRegister(HitAura::new);
        safeRegister(LegitAura::new);

        // Misc
        safeRegister(ArmorTrimHiderModule::new);
        safeRegister(AutoClickerModule::new);
        safeRegister(AutoTpaModule::new);
        safeRegister(AutoWalkModule::new);
        safeRegister(CoordSnapperModule::new);
        safeRegister(CustomCrosshairModule::new);
        safeRegister(CustomFovModule::new);
        safeRegister(CustomGlintModule::new);
        safeRegister(FakeStatsModule::new);
        safeRegister(FakePayModule::new);
        safeRegister(FakeRolesModule::new);
        safeRegister(FastUseModule::new);
        safeRegister(FreeLookModule::new);
        safeRegister(FreecamModule::new);
        safeRegister(GambleRiggerModule::new);
        safeRegister(NameProtectModule::new);
        safeRegister(NameTagsModule::new);
        safeRegister(SkinProtectModule::new);
        safeRegister(SpawnerProtectModule::new);
        safeRegister(StaffListModule::new);
        safeRegister(WeatherNotifierModule::new);
        safeRegister(ZoomModule::new);

        ChuklClient.LOG.info("ModuleManager.init() registered {} modules", MODULES.size());
    }

    /**
     * Registers a module, but if its constructor throws, logs it and keeps going instead of letting
     * the exception propagate out of init() and silently drop every module registered after it
     * (a single bad constructor used to be able to wipe out the entire rest of the Misc category).
     */
    private static void safeRegister(java.util.function.Supplier<Module> supplier) {
        try {
            register(supplier.get());
        } catch (Throwable t) {
            ChuklClient.LOG.error("Failed to construct/register a module, skipping it", t);
        }
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
