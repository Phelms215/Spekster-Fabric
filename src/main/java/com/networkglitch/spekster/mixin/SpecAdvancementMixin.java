package com.networkglitch.spekster.mixin;

import com.networkglitch.spekster.Spekster;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public class SpecAdvancementMixin {

    @Shadow private ServerPlayerEntity owner;

    @Inject(at = @At("HEAD"), method = "grantCriterion",cancellable = true)
    private void suppressBotAdvancements(Advancement advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if(Spekster.isNotNull(Spekster.BotPlayerLink.get(owner.getUuid()))) {
            cir.cancel();
        }
    }


}
