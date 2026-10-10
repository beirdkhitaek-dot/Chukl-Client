package com.chukl.client.compat;

import com.chukl.client.core.Category;
import com.chukl.client.core.Module;
import net.minecraft.text.Text;

/** Base for modules ported from Meteor-style addons. Gives them info/warning/error chat messages. */
public abstract class MeteorModule extends Module {
    protected MeteorModule(Category category, String name, String description) {
        super(category, name, description);
    }

    protected void info(String msg, Object... args) { say(msg, args); }
    protected void warning(String msg, Object... args) { say(msg, args); }
    protected void error(String msg, Object... args) { say(msg, args); }

    private void say(String msg, Object... args) {
        String text = args.length == 0 ? msg : String.format(msg, args);
        if (mc.player != null) mc.player.sendMessage(Text.literal("[" + name + "] " + text), false);
    }
}
