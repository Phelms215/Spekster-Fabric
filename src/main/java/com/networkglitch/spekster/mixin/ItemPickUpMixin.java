package com.networkglitch.spekster.mixin;

import com.networkglitch.spekster.Spekster;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemPickUpMixin {

    @Inject(at = @At("INVOKE"), method = "onPlayerCollision", cancellable = true)
    private void playerCollisionMixin(PlayerEntity player, CallbackInfo info) {
        if (Spekster.isNotNull(Spekster.BotPlayerLink.get(player.getUuid()))) {
            info.cancel();
        }
    }

}