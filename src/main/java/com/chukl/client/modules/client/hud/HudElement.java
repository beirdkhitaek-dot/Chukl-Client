package com.chukl.client.modules.client.hud;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.client.ClickGuiModule;
import com.chukl.client.modules.client.FontsModule;
import com.chukl.client.settings.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import java.util.List;

/** One box of text on the HUD with its own position, colors, background and border. */
public abstract class HudElement extends Module {
    public final ModeSetting anchor = add(new ModeSetting("Anchor", "Screen corner the offsets count from", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    public final NumberSetting offX = add(new NumberSetting("Offset X", "Distance from the anchor corner", 4, 0, 1000, 1));
    public final NumberSetting offY = add(new NumberSetting("Offset Y", "Distance from the anchor corner", 4, 0, 1000, 1));
    public final NumberSetting scale = add(new NumberSetting("Scale", "Size of this element", 1.0, 0.5, 3.0, 0.1));
    public final ModeSetting colorMode = add(new ModeSetting("Color Mode", "How the text is colored", "Static", "Static", "Accent", "Rainbow"));
    public final ColorSetting textColor = add(new ColorSetting("Text Color", "Used when Color Mode is Static", 0xFFFFFFFF));
    public final ColorSetting background = add(new ColorSetting("Background", "Box color, set opacity 0 for none", 0x00000000));
    public final NumberSetting padding = add(new NumberSetting("Padding", "Space between text and box edge", 2, 0, 10, 1));
    public final NumberSetting borderWidth = add(new NumberSetting("Border Width", "0 = no border", 0, 0, 4, 1));
    public final ColorSetting borderColor = add(new ColorSetting("Border Color", "", 0xFF000000));
    public final BoolSetting shadow = add(new BoolSetting("Text Shadow", "Draw a shadow behind text", true));

    protected HudElement(String name, String description, String defAnchor, int defX, int defY, boolean on) {
        super(Category.CLIENT, name, description);
        anchor.setDefault(defAnchor);
        offX.setDefault((double) defX);
        offY.setDefault((double) defY);
        if (on) defaultOn();
        textColor.visibleWhen(() -> colorMode.get().equals("Static"));
        borderColor.visibleWhen(() -> borderWidth.getInt() > 0);
    }

    @Override public boolean listed() { return false; }

    /** Text lines to show. Empty means draw nothing. */
    protected abstract List<String> lines();

    /** True to draw a thin bar beside each line in the line's color. */
    protected boolean bar() { return false; }

    private int lineColor(int i) {
        switch (colorMode.get()) {
            case "Accent": {
                ClickGuiModule g = ModuleManager.get(ClickGuiModule.class);
                return (g != null ? g.accent.get() : 0xFF5599FF) | 0xFF000000;
            }
            case "Rainbow": {
                float hue = ((System.currentTimeMillis() % 6000L) / 6000f + i * 0.05f) % 1f;
                return hsv(hue);
            }
            default:
                return textColor.get();
        }
    }

    private static int hsv(float h) {
        float s = 0.7f, v = 1f;
        int hi = (int) (h * 6f) % 6;
        float f = h * 6f - (int) (h * 6f);
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (hi) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    @Override
    protected void onRender2D(DrawContext ctx, float delta) {
        if (mc.options.hudHidden) return;
        List<String> ls = lines();
        if (ls == null || ls.isEmpty()) return;
        FontsModule fm = ModuleManager.get(FontsModule.class);
        TextRenderer tr = mc.textRenderer;

        int pad = padding.getInt();
        int lh = tr.fontHeight + 1;
        int tw = 0;
        for (String l : ls) tw = Math.max(tw, fm.width(tr, l, true));
        int barW = bar() ? 3 : 0;
        int w = tw + pad * 2 + barW;
        int h = ls.size() * lh - 1 + pad * 2;

        float sc = scale.getFloat();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        boolean right = anchor.get().contains("Right");
        boolean bottom = anchor.get().contains("Bottom");
        float px = right ? sw - offX.getInt() - w * sc : offX.getInt();
        float py = bottom ? sh - offY.getInt() - h * sc : offY.getInt();

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(px, py);
        m.scale(sc, sc);

        int bw = borderWidth.getInt();
        if (bw > 0) {
            int bc = borderColor.get();
            ctx.fill(-bw, -bw, w + bw, 0, bc);
            ctx.fill(-bw, h, w + bw, h + bw, bc);
            ctx.fill(-bw, 0, 0, h, bc);
            ctx.fill(w, 0, w + bw, h, bc);
        }
        if ((background.get() >>> 24) > 0) ctx.fill(0, 0, w, h, background.get());

        boolean sd = shadow.get();
        for (int i = 0; i < ls.size(); i++) {
            String l = ls.get(i);
            int lwid = fm.width(tr, l, true);
            int y = pad + i * lh;
            int x = right ? w - pad - barW - lwid : pad + barW;
            int col = lineColor(i);
            if (barW > 0) {
                if (right) ctx.fill(w - 2, y - 1, w, y + lh - 1, col | 0xFF000000);
                else ctx.fill(0, y - 1, 2, y + lh - 1, col | 0xFF000000);
            }
            ctx.drawText(tr, fm.text(l, true), x, y, col, sd);
        }
        m.popMatrix();
    }
}
