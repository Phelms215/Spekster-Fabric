package com.networkglitch.spekster.mixin;

import net.minecraft.text.Text;
import com.networkglitch.spekster.Spekster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class PlayerLeaveMixin {

    @Shadow public ServerPlayerEntity player;
    @Shadow private MinecraftServer server;

    /**
     * If a person disconnects from the server, put them back to where they were and in survival mode first.
     * @param reason
     * @param info
     */

    @Inject(at = @At("INVOKE"), method = "onDisconnected", cancellable = true)
    private void onPlayerLeave(Text reason, CallbackInfo info) {
        if(Spekster.isNotNull(Spekster.Tracker.get(player.getUuid()))) {
            Spekster.DeactiveSpec(Spekster.Tracker.get(player.getUuid()), server, player);
        }
    }
}