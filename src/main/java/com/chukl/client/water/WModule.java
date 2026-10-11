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

    /** Secondary held-key trigger (Water's ActivatableModule), separate from the module's main toggle bind. */
    private int activationKey = -1;

    protected WModule(String name, Category category) {
        super(category.target, name, name + " (from Water Client)");
    }

    protected void addSetting(Setting<?> s) {
        com.chukl.client.settings.Setting<?> built = s.build();
        if (built != null) add(built);
    }

    public int getActivationKey() { return activationKey; }

    public void setActivationKey(int key) {
        this.activationKey = key;
        com.chukl.client.core.Config.markDirty();
    }

    /** Fired on key-down of the activation key, while the module is enabled. Default toggles the module. */
    public void onActivationKeyPressed() { toggle(); }

    /** Fired on key-down of the module's main bind. Chukl toggles via Keys already; override hooks stay no-ops here. */
    public void onBindPressed() { }

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
