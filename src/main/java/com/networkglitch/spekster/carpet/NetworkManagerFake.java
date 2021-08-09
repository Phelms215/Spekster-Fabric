package com.networkglitch.spekster.carpet;
/*
    Straight carpet rip.
    All credit to carpet author - gnembon
    https://github.com/gnembon/fabric-carpet/tree/master/src/main/java/carpet/
    TODO: Fix fake player entity to be more precise for needs, removing specific carpet implementation
    TODO: Check if carpet mod is installed and use method from carpet instead of custom one.
 */

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class NetworkManagerFake extends ClientConnection
{
    public NetworkManagerFake(NetworkSide p)
    {
        super(p);
    }

    @Override
    public void disableAutoRead()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}