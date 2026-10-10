package com.chukl.client.modules.client.hud;

import com.chukl.client.core.Module;
import com.chukl.client.core.ModuleManager;
import com.chukl.client.modules.client.FontsModule;
import com.chukl.client.settings.BoolSetting;
import com.chukl.client.settings.ModeSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleListModule extends HudElement {
    public final ModeSetting sort = add(new ModeSetting("Sort", "Order of the list", "Length", "Length", "Alphabetical"));
    public final BoolSetting sideBar = add(new BoolSetting("Side Bar", "Draw a colored bar beside each name", false));

    public ModuleListModule() {
        super("Module List", "Enabled modules.", "Top Right", 4, 4, true);
        textColor.setDefault(0xFF5599FF);
    }

    @Override protected boolean bar() { return sideBar.get(); }

    @Override
    protected List<String> lines() {
        List<Module> on = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (m.isEnabled() && m.listed()) on.add(m);
        if (sort.get().equals("Alphabetical")) {
            on.sort(Comparator.comparing((Module m) -> m.name.toLowerCase()));
        } else {
            FontsModule fm = ModuleManager.get(FontsModule.class);
            on.sort(Comparator.comparingInt((Module m) -> fm.width(mc.textRenderer, m.name, true)).reversed());
        }
        List<String> out = new ArrayList<>();
        for (Module m : on) out.add(m.name);
        return out;
    }
}
