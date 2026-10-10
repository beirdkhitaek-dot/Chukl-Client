package com.chukl.client.modules.client.hud;

import com.chukl.client.settings.NumberSetting;

import java.util.List;
import java.util.Locale;

public class CoordinatesModule extends HudElement {
    public final NumberSetting decimals = add(new NumberSetting("Decimals", "Digits after the point", 1, 0, 2, 1));

    public CoordinatesModule() {
        super("Coordinates", "Your X, Y and Z position.", "Bottom Left", 4, 4, false);
    }

    @Override
    protected List<String> lines() {
        if (mc.player == null) return List.of();
        String f = "%." + decimals.getInt() + "f";
        return List.of(String.format(Locale.ROOT, "XYZ  " + f + "  " + f + "  " + f, mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }
}
