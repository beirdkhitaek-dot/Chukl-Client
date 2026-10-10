package com.chukl.client;

import com.chukl.client.core.Config;
import com.chukl.client.core.Keys;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.client.ClickGuiModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
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
