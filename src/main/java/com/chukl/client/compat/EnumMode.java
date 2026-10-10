package com.chukl.client.compat;

import com.chukl.client.settings.ModeSetting;

/** A ModeSetting backed by an enum. Use value() to read the enum constant. */
public class EnumMode<E extends Enum<E>> extends ModeSetting {
    private final Class<E> type;

    public EnumMode(String name, String description, E def, Class<E> type) {
        super(name, description, def.name(), names(type));
        this.type = type;
    }

    private static <E extends Enum<E>> String[] names(Class<E> t) {
        E[] c = t.getEnumConstants();
        String[] out = new String[c.length];
        for (int i = 0; i < c.length; i++) out[i] = c[i].name();
        return out;
    }

    public E value() { return Enum.valueOf(type, get()); }
    public void setValue(E e) { set(e.name()); }
}
