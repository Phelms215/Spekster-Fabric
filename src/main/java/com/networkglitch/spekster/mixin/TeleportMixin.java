package com.networkglitch.spekster.mixin;

import com.networkglitch.spekster.Spekster;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public abstract class TeleportMixin extends LivingEntity {

    @Final
    @Shadow
    private static Logger LOGGER;

    protected TeleportMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }


    /**
     * Confirm the person isn't in 'spec' mode trying to use the teleport functions.
     * @param targetWorld
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     * @param ci
     */
    @Inject(at = @At("INVOKE"), method = "teleport", cancellable = true)
    void validateTeleport(ServerWorld targetWorld, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {

        if(Spekster.isNotNull(Spekster.Tracker.get(this.getUuid()))) {
            LOGGER.info("Player " + this.getName().asString() + " tried to teleport while in 'spec' mode.");
            ci.cancel();
        }


    }
}
