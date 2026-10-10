package com.chukl.client.modules.client.hud;

import com.chukl.client.settings.BoolSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClockModule extends HudElement {
    public final BoolSetting hour24 = add(new BoolSetting("24 Hour", "Use 24 hour time", true));
    public final BoolSetting seconds = add(new BoolSetting("Show Seconds", "Add seconds", false));

    public ClockModule() {
        super("Clock", "Your computer's local time.", "Bottom Right", 4, 4, false);
    }

    @Override
    protected List<String> lines() {
        String p = (hour24.get() ? "HH:mm" : "hh:mm") + (seconds.get() ? ":ss" : "") + (hour24.get() ? "" : " a");
        return List.of(LocalTime.now().format(DateTimeFormatter.ofPattern(p)));
    }
}
