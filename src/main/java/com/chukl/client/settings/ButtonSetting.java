package com.chukl.client.settings;

import com.google.gson.JsonElement;

/** A clickable action row in the GUI. Not saved. */
public class ButtonSetting extends Setting<Boolean> {
    private final Runnable action;

    public ButtonSetting(String name, String description, Runnable action) {
        super(name, description, false);
        this.action = action;
    }

    public void run() { action.run(); }
    @Override public JsonElement toJson() { return null; }
    @Override public void fromJson(JsonElement e) { }
}
