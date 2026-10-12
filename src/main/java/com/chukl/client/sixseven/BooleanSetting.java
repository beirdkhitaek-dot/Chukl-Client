package com.chukl.client.sixseven;

import com.chukl.client.settings.BoolSetting;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public void toggle() { set(!get()); }

    @Override protected void pull() { if (backing != null) value = (Boolean) backing.get(); }
    @Override protected void pushToBacking(Boolean v) { if (backing != null) ((BoolSetting) backing).set(v); }

    @Override
    public com.chukl.client.settings.Setting<?> build() {
        BoolSetting b = new BoolSetting(getName(), getDescription(), value);
        b.visibleWhen(this::isVisible);
        backing = b;
        return b;
    }
}
