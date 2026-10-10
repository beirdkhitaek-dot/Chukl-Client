package com.chukl.client.core;

import com.chukl.client.ChuklClient;
import com.chukl.client.settings.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for every module. Extend it, add settings, override the hooks, register in ModuleManager. */
public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected boolean enabled;
    private int bind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(Category category, String name, String description) {
        this.category = category;
        this.name = name;
        this.description = description;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public boolean isEnabled() { return enabled; }
    public int getBind() { return bind; }

    public void setBind(int key) {
        this.bind = key;
        Config.markDirty();
    }

    /** Used by defaults (e.g. ClickGUI key) without marking config dirty. */
    protected void defaultBind(int key) { this.bind = key; }
    /** Starts enabled without calling onEnable. */
    protected void defaultOn() { this.enabled = true; }
    public boolean persistEnabled() { return true; }
    /** False to keep this module out of the on-screen Module List. */
    public boolean listed() { return true; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        try {
            if (on) onEnable(); else onDisable();
        } catch (Throwable t) {
            ChuklClient.LOG.error("Module {} failed, disabling", name, t);
            enabled = false;
        }
        Config.markDirty();
    }

    public final void tickSafe() {
        try { onTick(); } catch (Throwable t) { fail(t); }
    }

    public final void renderSafe(DrawContext ctx, float delta) {
        try { onRender2D(ctx, delta); } catch (Throwable t) { fail(t); }
    }

    public final void render3DSafe(Render3D r) {
        try { onRender3D(r); } catch (Throwable t) { fail(t); }
    }

    public final void packetSafe(net.minecraft.network.packet.Packet<?> p) {
        try { onPacket(p); } catch (Throwable t) { fail(t); }
    }

    private void fail(Throwable t) {
        ChuklClient.LOG.error("Module {} crashed, disabling", name, t);
        enabled = false;
        try { onDisable(); } catch (Throwable ignored) { }
    }

    /** A key went down while in-world with no screen open. Return true if handled. */
    public boolean onKeyPress(int keyCode) { return false; }

    protected void onEnable() { }
    protected void onDisable() { }
    protected void onTick() { }
    protected void onRender2D(DrawContext ctx, float delta) { }
    /** Drawing in the 3D world (through walls). */
    protected void onRender3D(Render3D r) { }
    /** Called after the game processed an incoming packet (chunk data, light, block updates, join, respawn and a few more). */
    protected void onPacket(net.minecraft.network.packet.Packet<?> packet) { }
    /** Called each frame while drawing the world. cam is the camera position; draw relative to it. */
    protected void onRender3D(VertexConsumerProvider.Immediate buffers, MatrixStack matrices, Vec3d cam) { }
    /** Packet arrived from the server (on the network thread). */
    protected void onPacketReceive(Packet<?> packet) { }
    /** Packet about to be sent. Return true to cancel it. */
    protected boolean onPacketSend(Packet<?> packet) { return false; }

    public final void render3DSafe(VertexConsumerProvider.Immediate b, MatrixStack m, Vec3d cam) {
        try { onRender3D(b, m, cam); } catch (Throwable t) { fail(t); }
    }

    public final void packetReceiveSafe(Packet<?> p) {
        try { onPacketReceive(p); } catch (Throwable t) { fail(t); }
    }

    public final boolean packetSendSafe(Packet<?> p) {
        try { return onPacketSend(p); } catch (Throwable t) { fail(t); return false; }
    }
}
