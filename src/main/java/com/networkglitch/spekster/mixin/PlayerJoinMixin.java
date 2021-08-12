package com.networkglitch.spekster.mixin;

import com.networkglitch.spekster.Spekster;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerJoinMixin {

    @Final
    @Shadow private MinecraftServer server;

    @Inject(at = @At("INVOKE"), method = "onPlayerConnect", cancellable = true)
    public void clearOldSpecSession(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {

        if(Spekster.isNotNull(Spekster.Tracker.get(player.getUuid()))) {
            Spekster.DeactivateSpec(Spekster.Tracker.get(player.getUuid()), server, player);
            player.sendMessage(new LiteralText("Old spectator session was cleared.").formatted(Formatting.GOLD), true);
        }

    }

}
