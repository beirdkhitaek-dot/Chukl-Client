package com.chukl.client.mixin;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes Input's movementVector so Freecam can drive the player's body without real key presses. */
@Mixin(Input.class)
public interface ClientInputAccessor {
    @Accessor("movementVector")
    void chukl$setMoveVector(Vec2f v);
}
