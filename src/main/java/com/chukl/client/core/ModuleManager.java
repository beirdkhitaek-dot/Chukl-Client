package com.chukl.client.core;

import com.chukl.client.modules.client.ClickGuiModule;
import com.chukl.client.modules.client.FontsModule;
import com.chukl.client.modules.client.HudModule;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();

    private ModuleManager() { }

    /** Add new modules here: register(new MyModule()); */
    public static void init() {
        register(new ClickGuiModule());
        register(new FontsModule());
        register(new HudModule());
    }

    public static void register(Module m) { MODULES.add(m); }
    public static List<Module> all() { return MODULES; }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> cls) {
        for (Module m : MODULES) if (m.getClass() == cls) return (T) m;
        return null;
    }

    public static Module byName(String n) {
        String k = n.replace(" ", "");
        for (Module m : MODULES) if (m.name.replace(" ", "").equalsIgnoreCase(k)) return m;
        return null;
    }

    public static void tick() {
        for (Module m : MODULES) if (m.isEnabled()) m.tickSafe();
    }

    public static void render2D(DrawContext ctx, float delta) {
        for (Module m : MODULES) if (m.isEnabled()) m.renderSafe(ctx, delta);
    }
}
