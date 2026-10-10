package com.chukl.client.modules.client.hud;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ModeSetting;
import com.chukl.client.settings.NumberSetting;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/** The client logo on the HUD and in the Click GUI corner. Pick which logo to use. */
public class LogoModule extends Module {
    private static final Identifier CH = Identifier.of("chuklclient", "textures/logo/ch.png");
    private static final Identifier TROLL = Identifier.of("chuklclient", "textures/logo/troll.png");

    public final ModeSetting logo = add(new ModeSetting("Logo", "Which logo to show", "Ch", "Ch", "Troll"));
    public final NumberSetting size = add(new NumberSetting("Size", "Logo size in pixels", 32, 8, 160, 1));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Logo opacity", 100, 10, 100, 1));
    public final ModeSetting anchor = add(new ModeSetting("Anchor", "Screen corner the offsets count from", "Top Right", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    public final NumberSetting offX = add(new NumberSetting("Offset X", "Distance from the anchor corner", 4, 0, 1000, 1));
    public final NumberSetting offY = add(new NumberSetting("Offset Y", "Distance from the anchor corner", 4, 0, 1000, 1));
    public final BoolSetting onHud = add(new BoolSetting("Show On HUD", "Draw the logo while playing", true));
    public final BoolSetting inGui = add(new BoolSetting("Show In Click GUI", "Draw the logo in the bottom right of the Click GUI", true));

    public LogoModule() {
        super(Category.CLIENT, "Logo", "Client logo. Choose between the Ch logo and the Troll logo.");
        defaultOn();
    }

    @Override public boolean listed() { return false; }

    public Identifier texture() { return logo.get().equals("Troll") ? TROLL : CH; }

    /** Draws the selected logo with its top-left corner at x, y. */
    public void draw(DrawContext ctx, float x, float y, float px) {
        int a = Math.round(opacity.getInt() * 2.55f);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(px / 256f, px / 256f);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture(), 0, 0, 0f, 0f, 256, 256, 256, 256, (a << 24) | 0xFFFFFF);
        m.popMatrix();
    }

    @Override
    protected void onRender2D(DrawContext ctx, float delta) {
        if (!onHud.get() || mc.options.hudHidden) return;
        float s = size.getInt();
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        float x = anchor.get().contains("Right") ? sw - offX.getInt() - s : offX.getInt();
        float y = anchor.get().contains("Bottom") ? sh - offY.getInt() - s : offY.getInt();
        draw(ctx, x, y, s);
    }
}
