package com.networkglitch.spekster.datasets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class SpecDetails {

    private UUID uuid;
    private UUID botUUID;
    private double X;
    private double Y;
    private double Z;

    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

    public double getZ() {
        return Z;
    }

    public void setCords(double x, double y, double z) {
        X = x;
        Y = y;
        Z = z;
    }

    public void setPlayerUUID(UUID PlyerUUID) {
        uuid = PlyerUUID;
    }

    public UUID getPlayerUUID() {
        return uuid;
    }

    public void setBotUUID(UUID botID) {
        botUUID = botID;
    }

    public UUID getBotUUID() {
        return botUUID;
    }

}
