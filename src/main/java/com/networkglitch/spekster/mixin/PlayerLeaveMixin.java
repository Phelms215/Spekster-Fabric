package com.networkglitch.spekster.mixin;

import net.minecraft.network.MessageType;
import net.minecraft.text.Text;
import com.networkglitch.spekster.Spekster;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Level;
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

    @Inject(at = @At("HEAD"), method = "onDisconnected", cancellable = true)
    private void botLeaveCleanUp(Text reason, CallbackInfo info) {
        if(Spekster.isNotNull(Spekster.BotPlayerLink.get(player.getUuid()))) {
            Spekster.log(Level.INFO, "Bot leaving detected, suppress leave message");
            // Bot account - do what the ondisconnected does then kill it.
            // Can't think of a better way to suppress this logout
            this.server.forcePlayerSampleUpdate();
            this.player.onDisconnect();
            this.server.getPlayerManager().remove(this.player);
            this.player.getTextStream().onDisconnect();
            info.cancel();
        }
        if(Spekster.isNotNull(Spekster.Tracker.get(player.getUuid()))) {
            Spekster.DeactivateSpec(Spekster.Tracker.get(player.getUuid()), server, player);
        }
    }

}