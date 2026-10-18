package com.chukl.client;

import com.chukl.client.core.AddonManager;
import com.chukl.client.core.Category;
import com.chukl.client.core.Config;
import com.chukl.client.core.Keys;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.core.Render3D;
import com.chukl.client.gui.Layout;
import com.chukl.client.modules.client.ClickGuiModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChuklClient implements ClientModInitializer {
    public static final String ID = "chuklclient";
    public static final String NAME = "Chukl Client";
    public static final Logger LOG = LoggerFactory.getLogger(NAME);
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ID)
        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    @Override
    public void onInitializeClient() {
        Config.init();
        ModuleManager.init();
        AddonManager.init();
        // Picks up any Category an addon registered for its own panel, even if Layout's class (and its
        // snapshot of Category.values()) already loaded before AddonManager ran.
        for (Category c : Category.values()) Layout.ensureTracked(c);
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            try {
                Keys.tick(mc);
                if (ClickGuiModule.consumeOpenRequest() && mc.currentScreen == null) ClickGuiModule.open();
                ModuleManager.tick();
                Config.tick();
            } catch (Throwable t) {
                LOG.error("Tick error", t);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            try {
                ModuleManager.render2D(ctx, tickCounter.getTickProgress(false));
            } catch (Throwable t) {
                LOG.error("HUD error", t);
            }
        });

        // Runs every frame regardless of what's under the crosshair (unlike the block-outline hook this
        // used to piggyback on, which only fired while actually looking at a block in range, verified
        // against the fabric-api 0.141.6+1.21.11 source rather than a newer/mismatched version).
        WorldRenderEvents.END_MAIN.register(context -> {
            try {
                var matrices = context.matrices();
                var consumers = context.consumers();
                var cameraState = context.worldState().cameraRenderState;
                if (matrices != null && cameraState != null && consumers instanceof VertexConsumerProvider.Immediate immediate) {
                    ModuleManager.render3D(new Render3D(immediate, matrices, cameraState.pos));
                }
            } catch (Throwable t) {
                LOG.error("World render error", t);
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Config.save());

        // Chat commands: touch fallback if you cannot press Right Shift.
        ClientSendMessageEvents.ALLOW_CHAT.register(ChuklClient::handleChat);

        LOG.info("{} {} loaded with {} modules", NAME, VERSION, ModuleManager.all().size());
    }

    private static boolean handleChat(String msg) {
        if (!msg.startsWith(".")) return true;
        String[] a = msg.substring(1).trim().split("\\s+");
        MinecraftClient mc = MinecraftClient.getInstance();
        switch (a[0].toLowerCase()) {
            case "gui" -> ClickGuiModule.requestOpen();
            case "t", "toggle" -> {
                var m = a.length > 1 ? ModuleManager.byName(a[1]) : null;
                if (m == null) info(mc, "Unknown module. Use .modules");
                else { m.toggle(); info(mc, m.name + (m.isEnabled() ? " on" : " off")); }
            }
            case "bind" -> {
                var m = a.length > 1 ? ModuleManager.byName(a[1]) : null;
                if (m == null || a.length < 3) info(mc, "Usage: .bind <module> <key|none>");
                else {
                    int k = a[2].equalsIgnoreCase("none") ? -1 : com.chukl.client.core.KeyNames.parse(a[2]);
                    if (k == -2) info(mc, "Unknown key");
                    else { m.setBind(k); info(mc, m.name + " bound to " + com.chukl.client.core.KeyNames.name(k)); }
                }
            }
            case "modules" -> {
                StringBuilder sb = new StringBuilder();
                for (var m : ModuleManager.all()) sb.append(m.name.replace(" ", "")).append(' ');
                info(mc, sb.toString().trim());
            }
            default -> info(mc, ".gui  .toggle <module>  .bind <module> <key|none>  .modules");
        }
        return false;
    }

    private static void info(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Chukl] " + s), false);
    }
}
