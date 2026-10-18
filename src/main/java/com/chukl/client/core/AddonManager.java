package com.chukl.client.core;

import com.chukl.client.ChuklClient;
import com.chukl.client.api.ChuklAddon;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.List;

/**
 * Loads addons the same way Meteor Client does: an addon is its own Fabric mod, not a jar dropped in some
 * Chukl-specific folder, so it goes through Fabric Loader's normal mod discovery (and gets normal Fabric
 * things for free -- mixins, access wideners, its own resources). It just has to depend on Chukl Client and
 * declare a "chukl.addon" entrypoint (see {@link ChuklAddon}) pointing at a class that implements it.
 * <p>
 * This class finds every such entrypoint and calls {@link ChuklAddon#onInitialize()} on each, with its own
 * try/catch per addon so one broken addon can't crash the base client or any other addon.
 */
public final class AddonManager {
    public static final String ENTRYPOINT_KEY = "chukl.addon";

    private AddonManager() { }

    public static void init() {
        List<EntrypointContainer<ChuklAddon>> containers =
            FabricLoader.getInstance().getEntrypointContainers(ENTRYPOINT_KEY, ChuklAddon.class);

        for (EntrypointContainer<ChuklAddon> container : containers) {
            String modId = container.getProvider().getMetadata().getId();
            ChuklAddon addon;
            try {
                addon = container.getEntrypoint();
            } catch (Throwable t) {
                ChuklClient.LOG.error("Addon mod '{}' failed to construct its chukl.addon entrypoint, skipping it", modId, t);
                continue;
            }

            String name = modId;
            try { name = addon.name(); } catch (Throwable ignored) { }

            try {
                addon.onInitialize();
                ChuklClient.LOG.info("Loaded addon '{}' ({})", name, modId);
            } catch (Throwable t) {
                ChuklClient.LOG.error("Addon '{}' ({}) failed to initialize, skipping it", name, modId, t);
            }
        }

        if (!containers.isEmpty()) {
            ChuklClient.LOG.info("AddonManager loaded {} addon(s)", containers.size());
        }
    }
}
