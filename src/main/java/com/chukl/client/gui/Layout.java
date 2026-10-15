package com.chukl.client.gui;

import com.chukl.client.core.Category;
import com.chukl.client.core.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Position and size of every panel, in GUI-logical units (before the Scale setting). */
public final class Layout {
    public static int HEADER = 16;
    public static final int MIN_W = 80, MIN_H = 40;
    private static final int UNSET = Integer.MIN_VALUE;

    public static final class Panel {
        public final Category cat;
        public int x = UNSET, y = UNSET, w = 110, h = 160;
        public boolean open = true;
        public int scroll;
        public final Set<String> expanded = new HashSet<>();

        /** Not persisted: drives the panel's fade-in-on-open animation (0..1). */
        public transient double bgFade;

        Panel(Category cat) { this.cat = cat; }
        public int totalHeight() { return HEADER + (open ? h : 0); }
    }

    public static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
    /** Draw order, last is on top. */
    public static final List<Category> ORDER = new ArrayList<>();

    static {
        for (Category c : Category.values()) {
            PANELS.put(c, new Panel(c));
            ORDER.add(c);
        }
    }

    private Layout() { }

    public static Panel get(Category c) { return PANELS.get(c); }

    public static void bringToFront(Category c) {
        ORDER.remove(c);
        ORDER.add(c);
    }

    public static void reset() {
        for (Panel p : PANELS.values()) {
            p.x = UNSET;
            p.y = UNSET;
            p.w = 110;
            p.h = 160;
            p.open = true;
            p.scroll = 0;
        }
        ORDER.clear();
        ORDER.addAll(List.of(Category.values()));
        Config.markDirty();
    }

    /** Places unplaced panels and keeps every panel reachable on screen. lw/lh = logical screen size. */
    public static void ensure(int lw, int lh) {
        int cx = 8, cy = 8, rowH = 0;
        for (Category c : Category.values()) {
            Panel p = PANELS.get(c);
            p.w = Math.max(MIN_W, Math.min(p.w, Math.max(MIN_W, lw - 4)));
            p.h = Math.max(MIN_H, Math.min(p.h, Math.max(MIN_H, lh - HEADER - 4)));
            if (p.x == UNSET || p.y == UNSET) {
                if (cx + p.w > lw - 8 && cx > 8) {
                    cx = 8;
                    cy += rowH + 8;
                    rowH = 0;
                }
                p.x = cx;
                p.y = cy;
                cx += p.w + 8;
                rowH = Math.max(rowH, p.totalHeight());
            }
            clamp(p, lw, lh);
        }
    }

    public static void clamp(Panel p, int lw, int lh) {
        p.x = Math.max(0, Math.min(p.x, Math.max(0, lw - p.w)));
        p.y = Math.max(0, Math.min(p.y, Math.max(0, lh - HEADER)));
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        for (Panel p : PANELS.values()) {
            JsonObject po = new JsonObject();
            po.addProperty("x", p.x);
            po.addProperty("y", p.y);
            po.addProperty("w", p.w);
            po.addProperty("h", p.h);
            po.addProperty("open", p.open);
            o.add(p.cat.name(), po);
        }
        JsonArray order = new JsonArray();
        for (Category c : ORDER) order.add(c.name());
        o.add("order", order);
        return o;
    }

    public static void fromJson(JsonObject o) {
        for (Panel p : PANELS.values()) {
            if (!o.has(p.cat.name())) continue;
            JsonObject po = o.getAsJsonObject(p.cat.name());
            if (po.has("x")) p.x = po.get("x").getAsInt();
            if (po.has("y")) p.y = po.get("y").getAsInt();
            if (po.has("w")) p.w = po.get("w").getAsInt();
            if (po.has("h")) p.h = po.get("h").getAsInt();
            if (po.has("open")) p.open = po.get("open").getAsBoolean();
        }
        if (o.has("order")) {
            List<Category> n = new ArrayList<>();
            for (var e : o.getAsJsonArray("order")) {
                try { n.add(Category.valueOf(e.getAsString())); } catch (Exception ignored) { }
            }
            for (Category c : Category.values()) if (!n.contains(c)) n.add(c);
            ORDER.clear();
            ORDER.addAll(n);
        }
    }
}
