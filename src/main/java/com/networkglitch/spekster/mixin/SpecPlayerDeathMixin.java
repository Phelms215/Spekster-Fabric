package com.networkglitch.spekster.mixin;

import com.mojang.authlib.GameProfile;
import com.networkglitch.spekster.Spekster;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class SpecPlayerDeathMixin extends PlayerEntity {

    @Shadow
    private MinecraftServer server;
    public ServerPlayNetworkHandler networkHandler;


    public SpecPlayerDeathMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(at = @At("INVOKE"), method = "onDeath", cancellable = true)
    public void suppressDeathMessages(DamageSource source, CallbackInfo ci) {

        // If the person died while a spectator suppress the normal message, send ours, and continue with the rest of onDeath
        if (Spekster.isNotNull(Spekster.Tracker.get(this.getUuid()))) {

            this.server.getPlayerManager().broadcast(new LiteralText(this.getName().asString() + " died when their clone was killed."), MessageType.SYSTEM, Util.NIL_UUID);
            this.dropShoulderEntities();
            if (!this.isSpectator()) {
                this.drop(source);
            }

            this.getScoreboard().forEachScore(ScoreboardCriterion.DEATH_COUNT, this.getEntityName(), ScoreboardPlayerScore::incrementScore);
            LivingEntity livingEntity = this.getPrimeAdversary();
            if (livingEntity != null) {
                this.incrementStat(Stats.KILLED_BY.getOrCreateStat(livingEntity.getType()));
                livingEntity.updateKilledAdvancementCriterion(this, this.scoreAmount, source);
                this.onKilledBy(livingEntity);
            }

            this.world.sendEntityStatus(this, (byte) 3);
            this.incrementStat(Stats.DEATHS);
            this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_DEATH));
            this.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
            this.extinguish();
            this.setFrozenTicks(0);
            this.setOnFire(false);
            this.getDamageTracker().update();
            ci.cancel();
        }

    }

}
