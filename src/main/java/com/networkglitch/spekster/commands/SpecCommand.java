package com.networkglitch.spekster.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.networkglitch.spekster.Spekster;
import com.networkglitch.spekster.datasets.SpecDetails;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.Level;

import java.util.UUID;

public class SpecCommand {

    /**
     * Command to run the actual spectator, put user in and out of spectator mode as required.
     * @param context
     * @return
     * @throws CommandSyntaxException
     */
    public static int Trigger(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();

        UUID playerUUUID = source.getPlayer().getUuid();
        Spekster.log(Level.INFO, source.getPlayer().getName().asString() + " ran spectator command");

        try {
            if (Spekster.isNotNull(Spekster.Tracker.get(playerUUUID))) {

                // Player is in Spec mode
                SpecDetails details = Spekster.Tracker.get(playerUUUID);
                Spekster.DeactivateSpec(details, source.getServer(), source.getPlayer());

            } else {
                // Register HashMap Entry
                SpecDetails newDetails = new SpecDetails();
                newDetails.setCords(source.getPlayer().getX(), source.getPlayer().getY(), source.getPlayer().getZ());
                newDetails.setPlayerUUID(source.getPlayer().getUuid());

                Spekster.ActivateSpec(newDetails, source.getServer(), source.getPlayer());
                return 1;
            }
        } catch (Exception e) {
            Spekster.log(Level.ERROR, "Exception occurred!");
            Spekster.log(Level.ERROR, e.getMessage());
            e.printStackTrace();
            return -1;
        }
        return -1;
    }
}
