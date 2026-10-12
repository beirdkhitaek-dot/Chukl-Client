package com.chukl.client.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes a KeyBinding's currently bound key, for modules that need to know which physical key a binding uses. */
@Mixin(KeyBinding.class)
public interface KeyMappingAccessor {
    @Accessor("boundKey")
    InputUtil.Key chukl$getKey();
}
