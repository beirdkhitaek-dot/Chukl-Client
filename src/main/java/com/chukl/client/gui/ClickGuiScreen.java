package com.chukl.client.gui;

import com.chukl.client.core.Category;
import com.chukl.client.core.KeyNames;
import com.chukl.client.core.Module;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.gui.Layout.Panel;
import com.chukl.client.modules.client.ClickGuiModule;
import com.chukl.client.core.Config;
import com.chukl.client.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
    private static final int GRIP = 12, SB = 5;
    private int ROW = 16, SET = 14, NUM = 18;

    private enum Kind { MODULE, BOOL, NUMBER, MODE, COLOR, COLOR_CH, BIND, BUTTON, STRING, KEY, EMPTY }

    private static final class Row {
        Kind kind;
        Module m;
        Setting<?> s;
        int ch;
        int y, h;
    }

    private final ClickGuiModule mod;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private float activeScale = 1f;
    private int lw, lh;

    private boolean bindKeyReleased;
    private Module bindTarget;
    private StringSetting editing;
    private KeybindSetting keyTarget;

    // drag state
    private Panel dragPanel;
    private double dragOffX, dragOffY, dragStartX, dragStartY;
    private boolean dragMoved;
    private Panel resizePanel;
    private int resizeW, resizeH;
    private double resizeStartX, resizeStartY;
    private Panel scrollPanel;
    private double scrollStartY;
    private int scrollStart;
    private Row sliderRow;
    private Panel sliderPanel;

    private String tooltip;
    private int tipX, tipY;

    public ClickGuiScreen(ClickGuiModule mod) {
        super(Text.literal("Chukl Client"));
        this.mod = mod;
        this.activeScale = mod.scale.getFloat();
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) { }

    @Override
    public void removed() {
        Config.markDirty();
        Config.save();
    }

    // ---------- helpers ----------

    private int accent() { return mod.accent.get() | 0xFF000000; }

    private int bgColor() {
        int c = mod.panelColor.get();
        int a = (c >>> 24) * mod.opacity.getInt() / 100;
        return (a << 24) | (c & 0xFFFFFF);
    }

    private void syncMetrics() {
        ROW = mod.rowHeight.getInt();
        SET = Math.max(10, ROW - 2);
        NUM = ROW + 2;
        Layout.HEADER = mod.headerHeight.getInt();
    }

    private void txt(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color) {
        ctx.drawText(tr, mod.text(s), x, y, color, mod.shadow.get());
    }
    private double mx() { return mc.mouse.getScaledX(mc.getWindow()) / activeScale; }
    private double my() { return mc.mouse.getScaledY(mc.getWindow()) / activeScale; }

    private List<Row> rows(Panel p) {
        List<Row> out = new ArrayList<>();
        int y = 2;
        List<Module> mods = ModuleManager.in(p.cat);
        if (mods.isEmpty()) {
            out.add(row(Kind.EMPTY, null, null, 0, y, ROW));
            return out;
        }
        for (Module m : mods) {
            out.add(row(Kind.MODULE, m, null, 0, y, ROW));
            y += ROW;
            if (!p.expanded.contains(m.name)) continue;
            for (Setting<?> s : m.getSettings()) {
                if (!s.visible()) continue;
                if (s instanceof BoolSetting) { out.add(row(Kind.BOOL, m, s, 0, y, SET)); y += SET; }
                else if (s instanceof NumberSetting) { out.add(row(Kind.NUMBER, m, s, 0, y, NUM)); y += NUM; }
                else if (s instanceof ModeSetting) { out.add(row(Kind.MODE, m, s, 0, y, SET)); y += SET; }
                else if (s instanceof ButtonSetting) { out.add(row(Kind.BUTTON, m, s, 0, y, SET)); y += SET; }
                else if (s instanceof StringSetting) { out.add(row(Kind.STRING, m, s, 0, y, SET)); y += SET; }
                else if (s instanceof KeybindSetting) { out.add(row(Kind.KEY, m, s, 0, y, SET)); y += SET; }
                else if (s instanceof ColorSetting) {
                    out.add(row(Kind.COLOR, m, s, 0, y, SET));
                    y += SET;
                    if (p.expanded.contains("C:" + m.name + ":" + s.name)) {
                        for (int c = 0; c < 4; c++) { out.add(row(Kind.COLOR_CH, m, s, c, y, NUM)); y += NUM; }
                    }
                }
            }
            out.add(row(Kind.BIND, m, null, 0, y, SET));
            y += SET;
        }
        return out;
    }

    private static Row row(Kind k, Module m, Setting<?> s, int ch, int y, int h) {
        Row r = new Row();
        r.kind = k; r.m = m; r.s = s; r.ch = ch; r.y = y; r.h = h;
        return r;
    }

    private static int contentH(List<Row> rows) {
        if (rows.isEmpty()) return 0;
        Row l = rows.get(rows.size() - 1);
        return l.y + l.h + 2;
    }

    private static void clampScroll(Panel p, int content) {
        p.scroll = Math.max(0, Math.min(p.scroll, Math.max(0, content - p.h)));
    }

    private static boolean in(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && y >= ry && x < rx + rw && y < ry + rh;
    }

    // ---------- render ----------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        syncMetrics();
        if (sliderRow == null) activeScale = mod.scale.getFloat();
        float s = activeScale;
        lw = (int) (width / s);
        lh = (int) (height / s);
        Layout.ensure(lw, lh);

        if (!bindKeyReleased && mod.getBind() >= 0
            && GLFW.glfwGetKey(mc.getWindow().getHandle(), mod.getBind()) != GLFW.GLFW_PRESS) bindKeyReleased = true;

        if (mod.dim.get()) ctx.fill(0, 0, width, height, (mod.dimStrength.getInt() * 255 / 100) << 24);
        com.chukl.client.modules.client.hud.LogoModule lg = com.chukl.client.core.ModuleManager.get(com.chukl.client.modules.client.hud.LogoModule.class);
        if (lg != null && lg.isEnabled() && lg.inGui.get()) {
            float ls = Math.max(24, lg.size.getInt() * 1.5f);
            lg.draw(ctx, width - ls - 8, height - ls - 8, ls);
        }

        Matrix3x2fStack st = ctx.getMatrices();
        st.pushMatrix();
        st.scale(s, s);
        double lmx = mouseX / s, lmy = mouseY / s;
        tooltip = null;
        TextRenderer tr = mc.textRenderer;

        for (Category c : new ArrayList<>(Layout.ORDER)) renderPanel(ctx, tr, Layout.get(c), lmx, lmy);

        // close button (touch friendly)
        boolean hc = in(lmx, lmy, lw - 20, 4, 16, 16);
        ctx.fill(lw - 20, 4, lw - 4, 20, hc ? 0xFFCC3333 : 0xCC222222);
        txt(ctx, tr, "X", lw - 15, 8, 0xFFFFFFFF);

        if (bindTarget != null) {
            String t = "Press a key for " + bindTarget.name + "  (Esc cancel, Backspace clear)";
            int w = mod.width(tr, t);
            ctx.fill(lw / 2 - w / 2 - 6, lh - 26, lw / 2 + w / 2 + 6, lh - 10, 0xEE000000);
            txt(ctx, tr, t, lw / 2 - w / 2, lh - 22, 0xFFFFFFFF);
        } else if (tooltip != null) {
            int w = mod.width(tr, tooltip);
            int x = Math.min(tipX + 8, lw - w - 6), y = Math.min(tipY + 10, lh - 14);
            ctx.fill(x - 3, y - 3, x + w + 3, y + 11, 0xF0000000);
            txt(ctx, tr, tooltip, x, y, 0xFFFFFFFF);
        }
        st.popMatrix();
    }

    private void renderPanel(DrawContext ctx, TextRenderer tr, Panel p, double lmx, double lmy) {
        int acc = mod.headerColor(p.cat);
        int x = p.x, y = p.y, w = p.w;
        int bw = mod.borderWidth.getInt();
        if (bw > 0) {
            int bc = mod.borderColor.get();
            int th = p.totalHeight();
            ctx.fill(x - bw, y - bw, x + w + bw, y, bc);
            ctx.fill(x - bw, y + th, x + w + bw, y + th + bw, bc);
            ctx.fill(x - bw, y, x, y + th, bc);
            ctx.fill(x + w, y, x + w + bw, y + th, bc);
        }
        int ht = mod.headerText.get();
        ctx.fill(x, y, x + w, y + Layout.HEADER, acc);
        txt(ctx, tr, p.cat.displayName, x + 5, y + (Layout.HEADER - tr.fontHeight) / 2 + 1, ht);
        txt(ctx, tr, p.open ? "-" : "+", x + w - 11, y + (Layout.HEADER - tr.fontHeight) / 2 + 1, ht);
        if (!p.open) return;

        int by = y + Layout.HEADER;
        ctx.fill(x, by, x + w, by + p.h, bgColor());

        List<Row> rows = rows(p);
        int content = contentH(rows);
        clampScroll(p, content);
        boolean bar = content > p.h;
        int cw = w - (bar ? SB : 0);

        ctx.enableScissor(x, by, x + w, by + p.h);
        boolean panelHover = in(lmx, lmy, x, by, cw, p.h) && topPanelAt(lmx, lmy) == p;
        for (Row r : rows) {
            int ry = by + r.y - p.scroll;
            if (ry + r.h < by || ry > by + p.h) continue;
            boolean hover = panelHover && lmy >= ry && lmy < ry + r.h;
            drawRow(ctx, tr, p, r, x, ry, cw, hover, acc);
            if (hover && r.kind == Kind.MODULE) { tooltip = r.m.description; tipX = (int) lmx; tipY = (int) lmy; }
            else if (hover && r.s != null && !r.s.description.isEmpty()) { tooltip = r.s.description; tipX = (int) lmx; tipY = (int) lmy; }
        }
        if (bar) {
            int th = Math.max(10, (int) ((float) p.h * p.h / content));
            int ty = by + (int) ((float) (p.h - th) * p.scroll / Math.max(1, content - p.h));
            ctx.fill(x + w - SB, by, x + w, by + p.h, 0x55000000);
            ctx.fill(x + w - SB + 1, ty, x + w - 1, ty + th, acc);
        }
        ctx.disableScissor();

        // resize grip
        ctx.fill(x + w - 8, by + p.h - 2, x + w, by + p.h, acc);
        ctx.fill(x + w - 2, by + p.h - 8, x + w, by + p.h, acc);
    }

    private void drawRow(DrawContext ctx, TextRenderer tr, Panel p, Row r, int x, int ry, int cw, boolean hover, int acc) {
        int ty = ry + (r.h - tr.fontHeight) / 2 + 1;
        int right = x + cw - 4;
        if (hover) ctx.fill(x, ry, x + cw, ry + r.h, mod.hover.get());
        switch (r.kind) {
            case EMPTY -> txt(ctx, tr, "No modules yet", x + 5, ty, 0xFF777777);
            case MODULE -> {
                boolean on = r.m.isEnabled();
                int onc = mod.enabledColor(acc);
                if (on) ctx.fill(x, ry, x + 2, ry + r.h, onc);
                txt(ctx, tr, mod.fit(tr, r.m.name, cw - 24), x + 6, ty, on ? onc : mod.moduleText.get());
                txt(ctx, tr, p.expanded.contains(r.m.name) ? "-" : "+", right - 4, ty, 0xFF888888);
            }
            case BOOL -> {
                BoolSetting b = (BoolSetting) r.s;
                txt(ctx, tr, mod.fit(tr, b.name, cw - 30), x + 10, ty, mod.settingText.get());
                ctx.fill(right - 8, ry + 3, right, ry + 11, 0xFF333333);
                if (b.get()) ctx.fill(right - 7, ry + 4, right - 1, ry + 10, acc);
            }
            case MODE -> {
                ModeSetting ms = (ModeSetting) r.s;
                String t = ms.name + ": " + ms.get();
                txt(ctx, tr, mod.fit(tr, t, cw - 14), x + 10, ty, mod.settingText.get());
            }
            case STRING -> {
                StringSetting st = (StringSetting) r.s;
                boolean ed = editing == st;
                String v = st.get();
                String t = st.name + ": " + (v.isEmpty() && !ed ? st.placeholder : v) + (ed && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
                txt(ctx, tr, mod.fit(tr, t, cw - 14), x + 10, ty, ed ? 0xFFFFFFFF : mod.settingText.get());
            }
            case KEY -> {
                KeybindSetting ks = (KeybindSetting) r.s;
                String t = ks.name + ": " + (keyTarget == ks ? "..." : ks.keyName());
                txt(ctx, tr, mod.fit(tr, t, cw - 14), x + 10, ty, mod.settingText.get());
            }
            case BUTTON -> {
                ctx.fill(x + 8, ry + 1, x + cw - 4, ry + r.h - 1, mod.button.get());
                String t = r.s.name;
                txt(ctx, tr, t, x + (cw - mod.width(tr, t)) / 2, ty, 0xFFFFFFFF);
            }
            case COLOR -> {
                ColorSetting c = (ColorSetting) r.s;
                txt(ctx, tr, mod.fit(tr, c.name, cw - 30), x + 10, ty, mod.settingText.get());
                ctx.fill(right - 10, ry + 2, right, ry + 12, 0xFF000000);
                ctx.fill(right - 9, ry + 3, right - 1, ry + 11, c.get() | 0xFF000000);
            }
            case BIND -> {
                String t = bindTarget == r.m ? "Bind: ..." : "Bind: " + KeyNames.name(r.m.getBind());
                txt(ctx, tr, t, x + 10, ty, mod.settingText.get());
            }
            case NUMBER, COLOR_CH -> {
                double f;
                String label;
                if (r.kind == Kind.NUMBER) {
                    NumberSetting n = (NumberSetting) r.s;
                    f = n.fraction();
                    label = n.name + ": " + n.display();
                } else {
                    ColorSetting c = (ColorSetting) r.s;
                    f = c.channel(r.ch) / 255.0;
                    label = "RGBA".charAt(r.ch) + ": " + c.channel(r.ch);
                }
                int sx = x + 4, sw = cw - 8;
                ctx.fill(sx, ry + 1, sx + sw, ry + r.h - 1, mod.track.get());
                ctx.fill(sx, ry + 1, sx + (int) (sw * f), ry + r.h - 1, (acc & 0x00FFFFFF) | 0x99000000);
                txt(ctx, tr, mod.fit(tr, label, sw - 4), sx + 4, ty, mod.settingText.get() | 0xFF000000);
            }
        }
    }

    // ---------- input ----------

    private Panel topPanelAt(double lx, double ly) {
        for (int i = Layout.ORDER.size() - 1; i >= 0; i--) {
            Panel p = Layout.get(Layout.ORDER.get(i));
            if (in(lx, ly, p.x, p.y, p.w, p.totalHeight())) return p;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        syncMetrics();
        double lx = mx(), ly = my();
        int btn = click.button();

        if (bindTarget != null) { bindTarget = null; return true; }
        if (keyTarget != null) { keyTarget = null; return true; }
        editing = null;
        if (in(lx, ly, lw - 20, 4, 16, 16)) { close(); return true; }

        Panel p = topPanelAt(lx, ly);
        if (p == null) return true;
        Layout.bringToFront(p.cat);

        if (ly < p.y + Layout.HEADER) {
            dragPanel = p;
            dragOffX = lx - p.x;
            dragOffY = ly - p.y;
            dragStartX = lx;
            dragStartY = ly;
            dragMoved = false;
            return true;
        }

        int by = p.y + Layout.HEADER;
        if (lx >= p.x + p.w - GRIP && ly >= by + p.h - GRIP) {
            resizePanel = p;
            resizeW = p.w;
            resizeH = p.h;
            resizeStartX = lx;
            resizeStartY = ly;
            return true;
        }

        List<Row> rows = rows(p);
        int content = contentH(rows);
        boolean bar = content > p.h;
        if (bar && lx >= p.x + p.w - SB) {
            scrollPanel = p;
            scrollStartY = ly;
            scrollStart = p.scroll;
            return true;
        }

        int cw = p.w - (bar ? SB : 0);
        double rel = ly - by + p.scroll;
        for (Row r : rows) {
            if (rel < r.y || rel >= r.y + r.h) continue;
            clickRow(p, r, btn, lx, cw);
            break;
        }
        return true;
    }

    private void clickRow(Panel p, Row r, int btn, double lx, int cw) {
        switch (r.kind) {
            case MODULE -> {
                boolean arrow = lx >= p.x + cw - 18;
                if (btn == 1 || arrow) toggleKey(p, r.m.name);
                else if (btn == 0) r.m.toggle();
            }
            case BOOL -> { if (btn == 0) { BoolSetting b = (BoolSetting) r.s; b.set(!b.get()); } }
            case MODE -> { ModeSetting ms = (ModeSetting) r.s; ms.cycle(btn == 1 ? -1 : 1); }
            case BUTTON -> { if (btn == 0) ((ButtonSetting) r.s).run(); }
            case STRING -> { if (btn == 0) { editing = (StringSetting) r.s; keyTarget = null; bindTarget = null; } }
            case KEY -> {
                if (btn == 1) ((KeybindSetting) r.s).set(-1);
                else { keyTarget = (KeybindSetting) r.s; editing = null; bindTarget = null; }
            }
            case COLOR -> toggleKey(p, "C:" + r.m.name + ":" + r.s.name);
            case BIND -> { if (btn == 1) r.m.setBind(-1); else bindTarget = r.m; }
            case NUMBER, COLOR_CH -> {
                if (btn == 0) {
                    sliderRow = r;
                    sliderPanel = p;
                    applySlider(lx);
                }
            }
            default -> { }
        }
    }

    private void toggleKey(Panel p, String key) {
        if (!p.expanded.remove(key)) p.expanded.add(key);
    }

    private void applySlider(double lx) {
        Panel p = sliderPanel;
        Row r = sliderRow;
        if (p == null || r == null) return;
        int content = contentH(rows(p));
        int cw = p.w - (content > p.h ? SB : 0);
        double sx = p.x + 4, sw = cw - 8;
        double f = Math.max(0, Math.min(1, (lx - sx) / sw));
        if (r.kind == Kind.NUMBER) {
            NumberSetting n = (NumberSetting) r.s;
            n.set(n.min + f * (n.max - n.min));
        } else {
            ((ColorSetting) r.s).setChannel(r.ch, (int) Math.round(f * 255));
        }
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        double lx = mx(), ly = my();
        if (sliderRow != null) { applySlider(lx); return true; }
        if (dragPanel != null) {
            if (!dragMoved && (Math.abs(lx - dragStartX) > 2 || Math.abs(ly - dragStartY) > 2)) dragMoved = true;
            if (dragMoved) {
                dragPanel.x = (int) (lx - dragOffX);
                dragPanel.y = (int) (ly - dragOffY);
                Layout.clamp(dragPanel, lw, lh);
            }
            return true;
        }
        if (resizePanel != null) {
            Panel p = resizePanel;
            p.w = Math.max(Layout.MIN_W, Math.min(lw - p.x, resizeW + (int) (lx - resizeStartX)));
            p.h = Math.max(Layout.MIN_H, Math.min(lh - p.y - Layout.HEADER, resizeH + (int) (ly - resizeStartY)));
            return true;
        }
        if (scrollPanel != null) {
            Panel p = scrollPanel;
            int content = contentH(rows(p));
            double range = Math.max(1, content - p.h);
            p.scroll = scrollStart + (int) ((ly - scrollStartY) * content / Math.max(1, p.h));
            clampScroll(p, content);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragPanel != null && !dragMoved) dragPanel.open = !dragPanel.open;
        if (dragPanel != null || resizePanel != null) Config.markDirty();
        dragPanel = null;
        resizePanel = null;
        scrollPanel = null;
        sliderRow = null;
        sliderPanel = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        syncMetrics();
        Panel p = topPanelAt(mx(), my());
        if (p != null && p.open) {
            p.scroll -= (int) (vertical * 14);
            clampScroll(p, contentH(rows(p)));
        }
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (editing != null && input.isValidChar()) {
            editing.set(editing.get() + input.asString());
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.getKeycode();
        if (editing != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) editing = null;
            else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String v = editing.get();
                if (!v.isEmpty()) editing.set(v.substring(0, v.length() - 1));
            }
            return true;
        }
        if (keyTarget != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) keyTarget.set(-1);
            else if (key != GLFW.GLFW_KEY_ESCAPE) keyTarget.set(key);
            keyTarget = null;
            return true;
        }
        if (bindTarget != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { /* cancel */ }
            else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) bindTarget.setBind(-1);
            else bindTarget.setBind(key);
            bindTarget = null;
            bindKeyReleased = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == mod.getBind() && bindKeyReleased) { close(); return true; }
        return super.keyPressed(input);
    }
}
