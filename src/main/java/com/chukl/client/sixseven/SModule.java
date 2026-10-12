package com.chukl.client.sixseven;

import com.chukl.client.core.Module;

/**
 * Base class that lets 67Client modules (dev.sixseven.module.Module) run inside Chukl. Keeps 67Client's
 * hooks (onEnable, onDisable, onTick, onKeyPress) and its addSetting() call, which now builds a real Chukl
 * setting under the hood.
 */
public abstract class SModule extends Module {
    protected SModule(String name, String description, Category category) {
        super(category.target, name, description);
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        com.chukl.client.settings.Setting<?> built = setting.build();
        if (built != null) add(built);
        return setting;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    // onEnable/onDisable/onTick are already protected no-ops on core.Module with matching
    // signatures, so 67Client modules' "@Override protected void onEnable()" etc. just work.
}
