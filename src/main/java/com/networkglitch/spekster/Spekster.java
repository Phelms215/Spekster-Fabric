package com.networkglitch.spekster;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.networkglitch.spekster.carpet.EntityPlayerMPFake;
import com.networkglitch.spekster.commands.SpecCommand;
import com.networkglitch.spekster.datasets.SpecDetails;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
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
        log(Level.INFO, "Initializing mode, registering commands");
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

                for(Map.Entry<UUID, SpecDetails> entry : Tracker.entrySet()) {
                    UUID uuid = entry.getKey();
                    SpecDetails details = entry.getValue();
                    if(isNotNull(server.getPlayerManager().getPlayer(uuid))) {
                        Spekster.DeactiveSpec(details, server, Objects.requireNonNull(server.getPlayerManager().getPlayer(uuid)));
                    }
                }
            });
        } catch (Exception e) {
            Spekster.log(Level.ERROR, "An exception was called during the Stop server event!");
            Spekster.log(Level.INFO, e.getMessage());
            e.printStackTrace();
        }


    }

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }
    public static boolean isNotNull(Object o) { return o!=null; }
    public static boolean isNull(Object o)    { return o==null; }

    public static void onBotDeath(UUID botUUID, MinecraftServer server) {
        if(isNotNull(Spekster.BotPlayerLink.get(botUUID))) {
            UUID playerUUID = Spekster.BotPlayerLink.get(botUUID);
            SpecDetails Details = Spekster.Tracker.get(playerUUID);
            ServerPlayerEntity thisPlayer = server.getPlayerManager().getPlayer(playerUUID);
            if(isNotNull(thisPlayer)) {
                thisPlayer.sendMessage(new LiteralText("You died while in spectator mode!").formatted(Formatting.DARK_RED), false);
                thisPlayer.teleport(Details.getX(), Details.getY(), Details.getZ(), false);
                thisPlayer.changeGameMode(GameMode.SURVIVAL);
                thisPlayer.kill();
                Spekster.BotPlayerLink.remove(Details.getBotUUID());
                Spekster.Tracker.remove(playerUUID);
            }
        }
    }

    public static void ActivateSpec(SpecDetails details, MinecraftServer server, ServerPlayerEntity player) {

        player.changeGameMode(GameMode.SPECTATOR);

        // Spawn in entity
        Vec2f facing = player.getRotationClient();
        RegistryKey<World> dimType = player.getServerWorld().getRegistryKey();
        String BotName = player.getName().asString() + "-SPEC";
        if(BotName.length() > 12) {
            BotName = player.getName().asString().substring(0, 8) + "-SPEC";
        }

        PlayerEntity bot = EntityPlayerMPFake.createFake(BotName, server, details.getX(), details.getY(), details.getZ(), facing.y, facing.x, dimType, GameMode.SURVIVAL, false);

        ServerPlayerEntity botServerEntity =   server.getPlayerManager().getPlayer(bot.getUuid());
        assert botServerEntity != null;
        botServerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, botServerEntity));

        details.setBotUUID(bot.getUuid());
        details.setBot(bot);
        Spekster.Tracker.put(player.getUuid(), details);
        Spekster.BotPlayerLink.put(bot.getUuid(),player.getUuid());

        player.sendMessage(new LiteralText("You are now in Spectator Mode").formatted(Formatting.BLUE), false);
        player.sendMessage(new LiteralText("If your clone below takes damage, so will you.").formatted(Formatting.BLUE), false);
        Spekster.log(Level.INFO, player.getName().asString() + " is now in spectator");
    }

    public static void DeactiveSpec(SpecDetails details, MinecraftServer server, ServerPlayerEntity player) {
       player.teleport(details.getX(), details.getY(), details.getZ());
       player.changeGameMode(GameMode.SURVIVAL);

        Spekster.Tracker.remove(player.getUuid());
        Spekster.BotPlayerLink.remove(details.getBotUUID());
        details.getBot().remove(Entity.RemovalReason.DISCARDED);
        player.sendMessage(new LiteralText("Sending back to Survival mode.").formatted(Formatting.GREEN), false);
        Spekster.log(Level.INFO,player.getName().asString() + " is now back in survival.");
    }

}