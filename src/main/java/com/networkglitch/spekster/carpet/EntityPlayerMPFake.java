package com.networkglitch.spekster.carpet;
/*
    Straight carpet rip.
    All credit to carpet author - gnembon
    https://github.com/gnembon/fabric-carpet/tree/master/src/main/java/carpet/
    TODO: Fix fake player entity to be more precise for needs, removing specific carpet implementation
    TODO: Check if carpet mod is installed and use method from carpet instead of custom one.
 */

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.networkglitch.spekster.Spekster;
import com.networkglitch.spekster.datasets.SkinDetails;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("EntityConstructor")
public class EntityPlayerMPFake extends ServerPlayerEntity
{
    public Runnable fixStartingPosition = () -> {};

    public static EntityPlayerMPFake createFake(MinecraftServer server, double d0, double d1, double d2, UUID playerUUID) throws Exception {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUUID);
        if(Spekster.isNull(player)) {
            throw new Exception("The user does not exist! Cannot clone them.");
        }
        RegistryKey<World> key = player.getServerWorld().getRegistryKey();
        ServerWorld worldIn = player.getServerWorld();
        if(Spekster.isNull(worldIn)) {
            Spekster.log(Level.ERROR, "World is null, cannot create a fake");
            Spekster.log(Level.ERROR, key.toString());
            return null;
        }

        UUID randomUUID = UUID.randomUUID();
        GameProfile gameprofile;
        double yaw;
        double pitch;
        Vec2f facing = player.getRotationClient();
        yaw = facing.y;
        pitch = facing.x;

        String BotName = player.getName().asString() + "-SPEC";
        if (BotName.length() > 11) {
            BotName = player.getName().asString().substring(0, 8) + "-SPEC";
        }
        gameprofile = new GameProfile(randomUUID, BotName);


        // Grab the skin of the playerUUID and set the clone to, ya know look like them
        SkinDetails playerSkin = Spekster.FetchSkin(playerUUID);
        if (Spekster.isNotNull(playerSkin)) {
            //TODO: Build a static grab for a default texture (config?)
            gameprofile.getProperties().put("textures", new Property("textures", playerSkin.getProperties().getTexture(), playerSkin.getProperties().getSignature()));
        }

        if (gameprofile.getProperties().containsKey("textures")) {
            AtomicReference<GameProfile> result = new AtomicReference<>();
            SkullBlockEntity.loadProperties(gameprofile, result::set);
            gameprofile = result.get();
        }

        EntityPlayerMPFake instance = new EntityPlayerMPFake(server, worldIn, gameprofile);
        server.getPlayerManager().onPlayerConnect(new NetworkManagerFake(NetworkSide.SERVERBOUND), instance);
        instance.setHealth(20.0F);
        instance.interactionManager.changeGameMode(GameMode.SURVIVAL);
        instance.teleport(worldIn, d0, d1, d2, (float)yaw, (float)pitch);


        worldIn.getChunkManager().updatePosition(instance);
        instance.dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); // show all model layers (incl. capes)
        instance.getAbilities().flying = false;
        return instance;
    }

    private EntityPlayerMPFake(MinecraftServer server, ServerWorld worldIn, GameProfile profile)
    {
        super(server, worldIn, profile);
    }

    @Override
    protected void onEquipStack(ItemStack stack)
    {
        if (!isUsingItem()) super.onEquipStack(stack);
    }

    @Override
    public void kill()
    {
        kill(Messenger.s("Killed"));
    }

    public void kill(Text reason)
    {
        shakeOff();
        this.server.send(new ServerTask(this.server.getTicks(), () -> {
            this.networkHandler.onDisconnected(reason);
        }));
    }

    @Override
    public void tick()
    {

        if (this.getServer().getTicks() % 10 == 0)
        {
            this.networkHandler.syncWithPlayerPosition();
            if(Spekster.isNotNull(this.getServerWorld().getChunkManager())) {
             this.getServerWorld().getChunkManager().updatePosition(this);
            }
            onTeleportationDone(); //<- causes hard crash but would need to be done to enable portals // not as of 1.17
        }

        // Confirm player is active.
        // Spekster.ConfirmPlayer(server, this.getUuid());

        super.tick();
        this.playerTick();
    }

    private void shakeOff()
    {
        if (getVehicle() instanceof PlayerEntity) stopRiding();
        for (Entity passenger : getPassengersDeep())
        {
            if (passenger instanceof PlayerEntity) passenger.stopRiding();
        }
    }

    @Override
    public void onDeath(DamageSource cause)
    {
        // Capture the death and kill the dude who caused this mess.
        Spekster.onBotDeath(this.getUuid(), this.getServer());
        server.getPlayerManager().remove(this);
    }

    @Override
    public String getIp()
    {
        return "127.0.0.1";
    }
}