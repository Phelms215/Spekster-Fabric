package com.networkglitch.spekster;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.networkglitch.spekster.carpet.EntityPlayerMPFake;
import com.networkglitch.spekster.commands.SpecCommand;
import com.networkglitch.spekster.datasets.SkinDetails;
import com.networkglitch.spekster.datasets.SpecDetails;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Spekster implements ModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_NAME = "Spekster";

    // Players to monitor by this plugin
    public static HashMap<UUID, SpecDetails> Tracker = new HashMap<>();
    public static HashMap<UUID, UUID> BotPlayerLink = new HashMap<>();

    @Override
    public void onInitialize() {
        log(Level.INFO, "Initializing mod, registering commands");
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralCommandNode<ServerCommandSource> SpectatorNode = CommandManager
                    .literal("spec")
                    .executes(SpecCommand::Trigger)
                    .build();

            //usage: /spec
            dispatcher.getRoot().addChild(SpectatorNode);
        });

        //
        // Server Stop Event
        try {
            //TODO: Mixin?
            ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
                Spekster.log(Level.INFO, "Returning all players back to survival mode before server shutdown");

                for (Map.Entry<UUID, SpecDetails> entry : Tracker.entrySet()) {
                    UUID uuid = entry.getKey();
                    SpecDetails details = entry.getValue();
                    if (isNotNull(server.getPlayerManager().getPlayer(uuid))) {
                        Spekster.DeactivateSpec(details, server, Objects.requireNonNull(server.getPlayerManager().getPlayer(uuid)));
                    }
                }
            });
        } catch (Exception e) {
            Spekster.log(Level.ERROR, "An exception was called during the Stop server event!");
            Spekster.log(Level.INFO, e.getMessage());
            e.printStackTrace();
        }


    }

    public static void log(Level level, String message) {
        LOGGER.log(level, "[" + MOD_NAME + "] " + message);
    }

    public static boolean isNotNull(Object o) {
        return o != null;
    }

    public static boolean isNull(Object o) {
        return o == null;
    }

    public static void ActivateSpec(SpecDetails details, MinecraftServer server, ServerPlayerEntity player) {

        try {

            PlayerEntity bot = EntityPlayerMPFake.createFake(server, details.getX(), details.getY(), details.getZ(), player.getUuid());
            details.setBotUUID(bot.getUuid());
            Spekster.Tracker.put(player.getUuid(), details);
            Spekster.BotPlayerLink.put(bot.getUuid(), player.getUuid());

            player.changeGameMode(GameMode.SPECTATOR);
            player.sendMessage(new LiteralText("You are now in Spectator Mode").formatted(Formatting.BLUE), false);
            player.sendMessage(new LiteralText("If your clone below takes damage, so will you.").formatted(Formatting.BLUE), false);
            Spekster.log(Level.INFO, player.getName().asString() + " is now in spectator");

        } catch (Exception e) {
            Spekster.log(Level.ERROR, "An error occurred while activating spectator mode.");
            Spekster.log(Level.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    public static void DeactivateSpec(SpecDetails details, MinecraftServer server, ServerPlayerEntity player) {
        player.teleport(details.getX(), details.getY(), details.getZ());
        player.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity theBot = server.getPlayerManager().getPlayer(details.getBotUUID());
        if (isNotNull(theBot)) {
            player.sendMessage(new LiteralText("Sending back to Survival mode.").formatted(Formatting.GREEN), false);
            Spekster.log(Level.INFO, player.getName().asString() + " is now back in survival.");
            Spekster.Tracker.remove(player.getUuid());
            Spekster.BotPlayerLink.remove(details.getBotUUID());
            server.getPlayerManager().remove(theBot);
        } else {
            Spekster.log(Level.ERROR, "Bot does not exist when it should!");
        }
    }

    public static void onBotDeath(UUID botUUID, MinecraftServer server) {
        // Capture the death and kill the dude who caused this mess.
        ServerPlayerEntity theBot = server.getPlayerManager().getPlayer(botUUID);

        if (Spekster.isNotNull(Spekster.BotPlayerLink.get(botUUID))) {
            UUID playerUUID = Spekster.BotPlayerLink.get(botUUID);
            SpecDetails Details = Spekster.Tracker.get(playerUUID);
            ServerPlayerEntity thisPlayer = server.getPlayerManager().getPlayer(playerUUID);
            if (Spekster.isNotNull(thisPlayer)) {
                server.getPlayerManager().remove(theBot);
                thisPlayer.sendMessage(new LiteralText("You died while in spectator mode!").formatted(Formatting.DARK_RED), false);
                thisPlayer.teleport(Details.getX(), Details.getY(), Details.getZ(), false);
                thisPlayer.changeGameMode(GameMode.SURVIVAL);
                thisPlayer.kill();
                Spekster.BotPlayerLink.remove(Details.getBotUUID());
                Spekster.Tracker.remove(playerUUID);
            }
        }
    }
}