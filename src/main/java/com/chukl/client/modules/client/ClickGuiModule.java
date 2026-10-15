package com.chukl.client.modules.client;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.gui.ClickGuiScreen;
import com.chukl.client.gui.Layout;
import com.chukl.client.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

public class ClickGuiModule extends Module {
    private static boolean openRequested;

    // Size
    public final NumberSetting scale = add(new NumberSetting("Scale", "Overall GUI size", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting rowHeight = add(new NumberSetting("Row Height", "Height of module rows", 16, 12, 24, 1));
    public final NumberSetting headerHeight = add(new NumberSetting("Header Height", "Height of panel headers", 16, 12, 24, 1));
    // Colors
    public final ColorSetting accent = add(new ColorSetting("Accent", "Headers, sliders, checkboxes", 0xFF5599FF));
    public final NumberSetting roundRadius = add(new NumberSetting("Round Corners", "Corner radius for panels and rows", 6, 0, 12, 1));
    public final BoolSetting rainbow = add(new BoolSetting("Rainbow", "Cycle module accent color through the rainbow", false));
    public final BoolSetting breathing = add(new BoolSetting("Breathing", "Pulse the accent color's brightness", false));
    public final ColorSetting headerText = add(new ColorSetting("Header Text", "Panel title color", 0xFFFFFFFF));
    public final ColorSetting panelColor = add(new ColorSetting("Panel Color", "Panel body color", 0xFF101018));
    public final NumberSetting opacity = add(new NumberSetting("Background", "Panel body opacity %", 85, 20, 100, 5));
    public final ColorSetting moduleText = add(new ColorSetting("Module Text", "Disabled module names", 0xFFCCCCCC));
    public final BoolSetting enabledUsesAccent = add(new BoolSetting("Enabled Uses Accent", "Enabled modules use the accent color", true));
    public final ColorSetting enabledText = add(new ColorSetting("Enabled Text", "Enabled module names", 0xFF5599FF));
    public final ColorSetting settingText = add(new ColorSetting("Setting Text", "Setting labels", 0xFFBBBBBB));
    public final ColorSetting hover = add(new ColorSetting("Hover Color", "Row highlight under the pointer", 0x30FFFFFF));
    public final ColorSetting track = add(new ColorSetting("Slider Track", "Slider background", 0xFF22222C));
    public final ColorSetting button = add(new ColorSetting("Button Color", "Button background", 0xFF333344));
    // Per-category header colors
    public final BoolSetting perCategory = add(new BoolSetting("Per-Category Colors", "Give each panel its own color", false));
    public final ColorSetting combatColor = add(new ColorSetting("Combat Color", "", 0xFFE55B5B));
    public final ColorSetting miscColor = add(new ColorSetting("Misc Color", "", 0xFF5599FF));
    public final ColorSetting donutColor = add(new ColorSetting("Donut Color", "", 0xFFF0A040));
    public final ColorSetting renderColor = add(new ColorSetting("Render Color", "", 0xFF8E6BEF));
    public final ColorSetting clientColor = add(new ColorSetting("Client Color", "", 0xFF4CC38A));
    // Extras
    public final BoolSetting shadow = add(new BoolSetting("Text Shadow", "Draw a shadow behind text", true));
    public final NumberSetting borderWidth = add(new NumberSetting("Border Width", "Panel outline, 0 = off", 0, 0, 3, 1));
    public final ColorSetting borderColor = add(new ColorSetting("Border Color", "Panel outline color", 0xFF000000));
    public final BoolSetting dim = add(new BoolSetting("Dim Screen", "Darken the game behind the GUI", true));
    public final NumberSetting dimStrength = add(new NumberSetting("Dim Strength", "How dark the dim is, %", 40, 0, 90, 5));
    public final ButtonSetting reset = add(new ButtonSetting("Reset Layout", "Put every panel back to default", Layout::reset));

    private FontsModule fonts() { return ModuleManager.get(FontsModule.class); }

    /** Text for the Click GUI, in the font chosen by the Fonts module. */
    public Text text(String s) {
        FontsModule f = fonts();
        return f != null ? f.text(s, false) : Text.literal(s);
    }

    public int width(TextRenderer tr, String s) { return tr.getWidth(text(s)); }

    public String fit(TextRenderer tr, String s, int max) {
        while (s.length() > 1 && width(tr, s) > max) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Header color for a panel, honoring Per-Category Colors. */
    public int headerColor(Category c) {
        if (!perCategory.get()) return accent.get() | 0xFF000000;
        int col = switch (c) {
            case COMBAT -> combatColor.get();
            case MISC -> miscColor.get();
            case DONUT -> donutColor.get();
            case RENDER -> renderColor.get();
            case CLIENT -> clientColor.get();
        };
        return col | 0xFF000000;
    }

    public int enabledColor(int fallback) {
        return enabledUsesAccent.get() ? fallback : enabledText.get();
    }

    /**
     * Argon-style per-module accent color: flat accent, a slow brightness "breathing" pulse, or a
     * rainbow cycle with a phase offset per row index so consecutive rows don't move in lockstep.
     */
    public int moduleColor(int index) {
        int base = accent.get();
        if (rainbow.get()) {
            float hue = (float) (((System.currentTimeMillis() % 3000L) / 3000.0) + index * 0.07) % 1.0F;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.65F, 1.0F);
            return (base & 0xFF000000) | (rgb & 0xFFFFFF);
        }
        if (breathing.get()) {
            float[] hsb = new float[3];
            java.awt.Color.RGBtoHSB((base >> 16) & 0xFF, (base >> 8) & 0xFF, base & 0xFF, hsb);
            float phase = Math.abs(((System.currentTimeMillis() % 2000L) / 1000F + index * 0.15F) % 2F - 1F);
            float bri = 0.35F + 0.65F * phase;
            int rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], bri);
            return (base & 0xFF000000) | (rgb & 0xFFFFFF);
        }
        return base;
    }

    public ClickGuiModule() {
        super(Category.CLIENT, "Click GUI", "The menu. Drag panel headers to move, drag the bottom-right corner to resize.");
        defaultBind(344); // Right Shift
        enabledText.visibleWhen(() -> !enabledUsesAccent.get());
        for (ColorSetting c : new ColorSetting[]{combatColor, miscColor, donutColor, renderColor, clientColor})
            c.visibleWhen(perCategory::get);
        borderColor.visibleWhen(() -> borderWidth.getInt() > 0);
        dimStrength.visibleWhen(dim::get);
    }

    public static void requestOpen() { openRequested = true; }

    public static boolean consumeOpenRequest() {
        boolean r = openRequested;
        openRequested = false;
        return r;
    }

    public static void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new ClickGuiScreen(com.chukl.client.core.ModuleManager.get(ClickGuiModule.class)));
    }

    @Override
    public void toggle() {
        if (!(mc.currentScreen instanceof ClickGuiScreen)) open();
    }

    @Override public void setEnabled(boolean on) { if (on) toggle(); }
    @Override public boolean persistEnabled() { return false; }
}
