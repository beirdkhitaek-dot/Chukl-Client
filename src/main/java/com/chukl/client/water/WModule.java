package com.chukl.client.water;

import com.chukl.client.core.Module;
import com.chukl.client.core.Render3D;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;

/**
 * Base class that lets Water Client modules run inside Chukl. It keeps Water's hooks (onEnable, onDisable,
 * onTick, onRender, onPacketReceive, onPacketSend) and its addSetting() call.
 */
public abstract class WModule extends Module {
    private final MatrixStack identity = new MatrixStack();

    protected WModule(String name, Category category) {
        super(category.target, name, name + " (from Water Client)");
    }

    protected void addSetting(Setting<?> s) {
        com.chukl.client.settings.Setting<?> built = s.build();
        if (built != null) add(built);
    }

    // ---- Water hooks (public to match Water's signatures) ----
    @Override public void onEnable() { }
    @Override public void onDisable() { }
    @Override public void onTick() { }
    public void onRender(MatrixStack matrices, float tickDelta) { }
    @Override public void onPacketReceive(Packet<?> packet) { }
    @Override public boolean onPacketSend(Packet<?> packet) { return false; }

    // ---- wiring ----
    @Override
    protected void onRender3D(Render3D r) {
        RenderUtils.begin(r);
        try {
            onRender(identity, mc.getRenderTickCounter().getTickProgress(false));
        } finally {
            RenderUtils.end();
        }
    }

    @Override
    protected void onPacket(Packet<?> packet) {
        onPacketReceive(packet);
    }
}
