package com.chukl.client.sixseven;

/** Maps 67Client's dev.sixseven.module.Category onto Chukl's categories. */
public enum Category {
    COMBAT(com.chukl.client.core.Category.COMBAT),
    MISC(com.chukl.client.core.Category.MISC),
    RENDER(com.chukl.client.core.Category.RENDER),
    VISUALS(com.chukl.client.core.Category.RENDER),
    CLIENT(com.chukl.client.core.Category.CLIENT);

    public final com.chukl.client.core.Category target;

    Category(com.chukl.client.core.Category target) { this.target = target; }
}
