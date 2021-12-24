package com.networkglitch.spekster.mixin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.networkglitch.spekster.Spekster;
import com.networkglitch.spekster.carpet.EntityPlayerMPFake;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(PlayerManager.class)
public abstract class SpecPlayerJoinMixin {

    @Final @Shadow private MinecraftServer server;
    @Final @Shadow private static Logger LOGGER;
    @Final @Shadow private List<ServerPlayerEntity> players;
    @Final @Shadow private Map<UUID, ServerPlayerEntity> playerMap;
    @Final @Shadow private WorldSaveHandler saveHandler;
    @Final @Shadow private DynamicRegistryManager.Impl registryManager;
    @Shadow protected int maxPlayers;
    @Shadow private int viewDistance;

    @Shadow public abstract MinecraftServer getServer();

    public void sendCommandTree(ServerPlayerEntity player) {
        GameProfile gameProfile = player.getGameProfile();
        int i = this.server.getPermissionLevel(gameProfile);
        this.sendCommandTree(player, i);
    }

    private void sendCommandTree(ServerPlayerEntity player, int permissionLevel) {
        if (player.networkHandler != null) {
            byte d;
            if (permissionLevel <= 0) {
                d = 24;
            } else if (permissionLevel >= 4) {
                d = 28;
            } else {
                d = (byte)(24 + permissionLevel);
            }

            player.networkHandler.sendPacket(new EntityStatusS2CPacket(player, d));
        }

        this.server.getCommandManager().sendCommandTree(player);
    }

    public void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
        WorldBorder worldBorder = this.server.getOverworld().getWorldBorder();
        player.networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(worldBorder));
        player.networkHandler.sendPacket(new WorldTimeUpdateS2CPacket(world.getTime(), world.getTimeOfDay(), world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)));
        player.networkHandler.sendPacket(new PlayerSpawnPositionS2CPacket(world.getSpawnPos(), world.getSpawnAngle()));
        if (world.isRaining()) {
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_STARTED, 0.0F));
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, world.getRainGradient(1.0F)));
            player.networkHandler.sendPacket(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, world.getThunderGradient(1.0F)));
        }

    }

    public void sendToAll(Packet<?> packet) {
        Iterator var2 = this.players.iterator();

        while(var2.hasNext()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var2.next();
            serverPlayerEntity.networkHandler.sendPacket(packet);
        }

    }

    @Nullable
    public NbtCompound loadPlayerData(ServerPlayerEntity player) {
        NbtCompound nbtCompound = this.server.getSaveProperties().getPlayerData();
        NbtCompound nbtCompound3;
        if (player.getName().getString().equals(this.server.getSinglePlayerName()) && nbtCompound != null) {
            nbtCompound3 = nbtCompound;
            player.readNbt(nbtCompound);
            LOGGER.debug("loading single player");
        } else {
            nbtCompound3 = this.saveHandler.loadPlayerData(player);
        }

        return nbtCompound3;
    }

    public int getMaxPlayerCount() {
        return this.maxPlayers;
    }

    @Inject(at = @At("HEAD"), method = "onPlayerConnect", cancellable = true)
    public void suppressBotMessages(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {

       if(player instanceof EntityPlayerMPFake) {
            // we got a bot here
            GameProfile gameProfile = player.getGameProfile();
            UserCache userCache = this.server.getUserCache();
            Optional<GameProfile> optional = userCache.getByUuid(gameProfile.getId());
            String string = (String)optional.map(GameProfile::getName).orElse(gameProfile.getName());
            userCache.add(gameProfile);

           /*
            NbtCompound nbtCompound = this.loadPlayerData(player);
            RegistryKey var23;
            if (nbtCompound != null) {
                DataResult var10000 = DimensionType.worldFromDimensionNbt(new Dynamic(NbtOps.INSTANCE, nbtCompound.get("Dimension")));
                Logger var10001 = LOGGER;
                Objects.requireNonNull(var10001);
                var23 = (RegistryKey)var10000.resultOrPartial(var10001::error).orElse(World.OVERWORLD);
            } else {
                var23 = World.OVERWORLD;
            }

            RegistryKey<World> registryKey = var23;
            ServerWorld serverWorld = this.server.getWorld(registryKey);
            ServerWorld serverWorld3;
            */

           NbtCompound nbtCompound = this.loadPlayerData(player);
           @SuppressWarnings("deprecation") RegistryKey<World> registryKey = nbtCompound != null ? DimensionType.worldFromDimensionNbt(new Dynamic<NbtElement>(NbtOps.INSTANCE, nbtCompound.get("Dimension"))).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD) : World.OVERWORLD;
           ServerWorld serverWorld3 = this.server.getWorld(registryKey);
            if (serverWorld3 == null) {
                LOGGER.warn("Unknown respawn dimension {}, defaulting to overworld", World.OVERWORLD);
                serverWorld3 = this.server.getOverworld();
            }
            player.setWorld(serverWorld3);

            String string2 = "local";
            if (connection.getAddress() != null) {
                string2 = connection.getAddress().toString();
            }
            WorldProperties worldProperties = serverWorld3.getLevelProperties();
            player.setGameMode(nbtCompound);
            ServerPlayNetworkHandler serverPlayNetworkHandler = new ServerPlayNetworkHandler(this.server, connection, player);
            GameRules gameRules = serverWorld3.getGameRules();
            boolean bl = gameRules.getBoolean(GameRules.DO_IMMEDIATE_RESPAWN);
            boolean bl2 = gameRules.getBoolean(GameRules.REDUCED_DEBUG_INFO);

           serverPlayNetworkHandler.sendPacket(new GameJoinS2CPacket(
                           player.getId(), // player id
                           worldProperties.isHardcore(),
                           player.interactionManager.getPreviousGameMode(), // old game mode
                           player.interactionManager.getGameMode(), // game mode
                           this.server.getWorldRegistryKeys(), // world keys
                           this.registryManager, // registry manager?
                           serverWorld3.getDimension(),
                           serverWorld3.getRegistryKey(),
                           BiomeAccess.hashSeed(serverWorld3.getSeed()),
                           this.getMaxPlayerCount(),
                           this.viewDistance,
                           1, // i
                           bl2,
                           !bl,
                           serverWorld3.isDebugWorld(),
                           serverWorld3.isFlat()
                   )
           );
            serverPlayNetworkHandler.sendPacket(new CustomPayloadS2CPacket(CustomPayloadS2CPacket.BRAND, (new PacketByteBuf(Unpooled.buffer())).writeString(server.getServerModName())));
            serverPlayNetworkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
            serverPlayNetworkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
            serverPlayNetworkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(player.getInventory().selectedSlot));
            serverPlayNetworkHandler.sendPacket(new SynchronizeRecipesS2CPacket(this.server.getRecipeManager().values()));
            serverPlayNetworkHandler.sendPacket(new SynchronizeTagsS2CPacket(this.server.getTagManager().toPacket(this.registryManager)));
            this.sendCommandTree(player);
            player.getStatHandler().updateStatSet();
            player.getRecipeBook().sendInitRecipesPacket(player);
            this.server.forcePlayerSampleUpdate();

            serverPlayNetworkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            this.players.add(player);
            this.playerMap.put(player.getUuid(), player);
            this.sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, new ServerPlayerEntity[]{player}));

            player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
            player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));


            serverWorld3.onPlayerConnected(player);
            this.server.getBossBarManager().onPlayerConnect(player);
            this.sendWorldInfo(player, serverWorld3);
            if (!this.server.getResourcePackUrl().isEmpty()) {
                player.sendResourcePackUrl(this.server.getResourcePackUrl(), this.server.getResourcePackHash(), this.server.requireResourcePack(), this.server.getResourcePackPrompt());
            }

            Iterator var24 = player.getStatusEffects().iterator();

            while(var24.hasNext()) {
                StatusEffectInstance statusEffectInstance = (StatusEffectInstance)var24.next();
                serverPlayNetworkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), statusEffectInstance));
            }

            if (nbtCompound != null && nbtCompound.contains("RootVehicle", 10)) {
                NbtCompound nbtCompound2 = nbtCompound.getCompound("RootVehicle");
                ServerWorld finalServerWorld = serverWorld3;
                Entity entity = EntityType.loadEntityWithPassengers(nbtCompound2.getCompound("Entity"), serverWorld3, (vehicle) -> {
                    return !finalServerWorld.tryLoadEntity(vehicle) ? null : vehicle;
                });
                if (entity != null) {
                    UUID uUID2;
                    if (nbtCompound2.containsUuid("Attach")) {
                        uUID2 = nbtCompound2.getUuid("Attach");
                    } else {
                        uUID2 = null;
                    }

                    Iterator var21;
                    Entity entity3;
                    if (entity.getUuid().equals(uUID2)) {
                        player.startRiding(entity, true);
                    } else {
                        var21 = entity.getPassengersDeep().iterator();

                        while(var21.hasNext()) {
                            entity3 = (Entity)var21.next();
                            if (entity3.getUuid().equals(uUID2)) {
                                player.startRiding(entity3, true);
                                break;
                            }
                        }
                    }

                    if (!player.hasVehicle()) {
                        LOGGER.warn("Couldn't reattach entity to player");
                        entity.discard();
                        var21 = entity.getPassengersDeep().iterator();

                        while(var21.hasNext()) {
                            entity3 = (Entity)var21.next();
                            entity3.discard();
                        }
                    }
                }
            }

            player.onSpawn();
            ci.cancel();
        }


    }

}
