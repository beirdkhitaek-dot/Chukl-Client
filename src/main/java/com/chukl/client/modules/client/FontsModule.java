package com.chukl.client.modules.client;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ModeSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Picks the font used by the client's own text. Turn the module off to go back to the normal font. */
public class FontsModule extends Module {
    public final ModeSetting font = add(new ModeSetting("Font", "Built-in fonts, or Custom from a resource pack",
        "Default", "Default", "Unifont", "Lemon Milk", "Arial Bold", "Xuong", "Custom"));
    public final BoolSetting applyGui = add(new BoolSetting("Apply To GUI", "Use the font in the Click GUI", true));
    public final BoolSetting applyHud = add(new BoolSetting("Apply To HUD", "Use the font in the HUD", true));

    public FontsModule() {
        super(Category.CLIENT, "Fonts", "Lemon Milk, Arial Bold, Xuong, Unifont or your own font for the client's text.");
        defaultOn();
    }

    private boolean applies(boolean hud) {
        return enabled && (hud ? applyHud.get() : applyGui.get());
    }

    private Identifier fontId() {
        return switch (font.get()) {
            case "Unifont" -> Identifier.ofVanilla("uniform");
            case "Lemon Milk" -> Identifier.of("chuklclient", "lemonmilk");
            case "Arial Bold" -> Identifier.of("chuklclient", "arialbold");
            case "Xuong" -> Identifier.of("chuklclient", "xuong");
            case "Custom" -> Identifier.of("chuklclient", "custom");
            default -> null;
        };
    }

    public Text text(String s, boolean hud) {
        MutableText t = Text.literal(s);
        Identifier id = applies(hud) ? fontId() : null;
        if (id != null) t.setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(id)));
        return t;
    }

    public int width(TextRenderer tr, String s, boolean hud) {
        return tr.getWidth(text(s, hud));
    }

    public String fit(TextRenderer tr, String s, int max, boolean hud) {
        while (s.length() > 1 && width(tr, s, hud) > max) s = s.substring(0, s.length() - 1);
        return s;
    }
}
