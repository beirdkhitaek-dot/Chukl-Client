package com.chukl.client.modules.client;

import com.chukl.client.ChuklClient;
import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.settings.BoolSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudModule extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", "Name, version and FPS", true));
    public final BoolSetting list = add(new BoolSetting("Module List", "Enabled modules, top right", true));

    public final BoolSetting followAccent = add(new BoolSetting("Follow Accent", "Use the Click GUI accent color", true));
    public final com.chukl.client.settings.ColorSetting textColor = add(new com.chukl.client.settings.ColorSetting("Text Color", "HUD text color", 0xFF5599FF));
    public final BoolSetting shadow = add(new BoolSetting("Text Shadow", "Draw a shadow behind text", true));

    public HudModule() {
        super(Category.CLIENT, "HUD", "On-screen info.");
        defaultOn();
        textColor.visibleWhen(() -> !followAccent.get());
    }

    @Override
    protected void onRender2D(DrawContext ctx, float delta) {
        if (mc.options.hudHidden || mc.currentScreen != null && mc.currentScreen instanceof com.chukl.client.gui.ClickGuiScreen) return;
        TextRenderer tr = mc.textRenderer;
        int accent = (followAccent.get() ? ModuleManager.get(ClickGuiModule.class).accent.get() : textColor.get()) | 0xFF000000;
        boolean sh = shadow.get();
        FontsModule gui = ModuleManager.get(FontsModule.class);
        if (watermark.get()) {
            ctx.drawText(tr, gui.text(ChuklClient.NAME + " " + ChuklClient.VERSION + "  " + mc.getCurrentFps() + " FPS", true), 4, 4, accent, sh);
        }
        if (list.get()) {
            List<Module> on = new ArrayList<>();
            for (Module m : ModuleManager.all()) if (m.isEnabled() && m != this) on.add(m);
            on.sort(Comparator.comparingInt((Module m) -> gui.width(tr, m.name, true)).reversed());
            int sw = mc.getWindow().getScaledWidth();
            int y = 4;
            for (Module m : on) {
                ctx.drawText(tr, gui.text(m.name, true), sw - 4 - gui.width(tr, m.name, true), y, accent, sh);
                y += tr.fontHeight + 1;
            }
        }
    }
}
