package com.networkglitch.spekster.mixin;

import com.networkglitch.spekster.Spekster;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerManager.class)
public class BroadcastMessageMixin {

    @Shadow private static Logger LOGGER;
    @Shadow @Final private MinecraftServer server;

    /**
     *
     * Determines if the person died (from out of this world) while in specator mode
     * if true, suppress the message
     * Additionally suppress any message from the bot(fake player) entities.
     * @param message
     * @param type
     * @param sender
     * @param ci
     * @author PHELMS
     * @email Patrick@networkglitch.com
     *
     */
    @Inject(at = @At("INVOKE"), method = "broadcastChatMessage", cancellable = true)
    void filterBroadCastMessages(Text message, MessageType type, UUID sender, CallbackInfo ci) {

        if(message instanceof TranslatableText) {
            LOGGER.info(((TranslatableText) message).getKey());
            String Key = ((TranslatableText) message).getKey();
            if (Key.equals("death.attack.outOfWorld")) {
                if(Spekster.isNotNull(Spekster.Tracker.get(sender))) {
                    server.getPlayerManager().broadcastChatMessage(new TranslatableText("spekster.player.died"), MessageType.SYSTEM, sender);
                    ci.cancel();
                }
            }

            if (Spekster.isNotNull(Spekster.BotPlayerLink.get(sender))) {
                // Sender our bot, surpress the message.
                ci.cancel();
            }

        }


    }

}
