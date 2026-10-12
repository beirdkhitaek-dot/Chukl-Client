package com.chukl.client.sixseven;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = List.of(modes);
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode '" + defaultValue + "' not in modes for " + name);
        }
    }

    public List<String> getModes() { return modes; }

    public boolean check(String mode) { return get().equals(mode); }

    public void cycle() {
        int next = (modes.indexOf(get()) + 1) % modes.size();
        set(modes.get(next));
    }

    @Override protected void pull() { if (backing != null) value = (String) backing.get(); }
    @Override protected void pushToBacking(String v) { if (backing != null) ((com.chukl.client.settings.ModeSetting) backing).set(v); }

    @Override
    public com.chukl.client.settings.Setting<?> build() {
        com.chukl.client.settings.ModeSetting m =
            new com.chukl.client.settings.ModeSetting(getName(), getDescription(), value, modes.toArray(new String[0]));
        m.visibleWhen(this::isVisible);
        backing = m;
        return m;
    }
}
