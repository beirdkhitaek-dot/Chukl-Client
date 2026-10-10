package com.chukl.client.modules.client.hud;

import com.chukl.client.ChuklClient;
import com.chukl.client.settings.BoolSetting;

import java.util.List;

public class WatermarkModule extends HudElement {
    public final BoolSetting showFps = add(new BoolSetting("Show FPS", "Add the frame rate", true));

    public WatermarkModule() {
        super("Watermark", "Client name, version and FPS.", "Top Left", 4, 4, true);
        textColor.setDefault(0xFF5599FF);
    }

    @Override
    protected List<String> lines() {
        String s = ChuklClient.NAME + " " + ChuklClient.VERSION;
        if (showFps.get()) s += "  " + mc.getCurrentFps() + " FPS";
        return List.of(s);
    }
}
