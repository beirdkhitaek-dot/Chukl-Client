package com.chukl.client.water;

/** Water Client's module categories, mapped onto Chukl's. */
public enum Category {
    COMBAT(com.chukl.client.core.Category.COMBAT),
    MISC(com.chukl.client.core.Category.MISC),
    DONUT(com.chukl.client.core.Category.DONUT),
    RENDER(com.chukl.client.core.Category.RENDER),
    CLIENT(com.chukl.client.core.Category.CLIENT);

    public final com.chukl.client.core.Category target;

    Category(com.chukl.client.core.Category target) { this.target = target; }
}
