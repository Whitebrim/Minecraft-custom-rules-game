package com.deathgame.rule;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public interface Rule {
    
    int getId();
    
    String getDescription();
    
    void tick(MinecraftServer server);
    
    void reset();
    
    default void register() {}
    
    default void unregister() {}
    
    /**
     * Called when a participant respawns after death.
     * Rules can use this to reset per-player state.
     */
    default void onPlayerRespawn(ServerPlayerEntity player) {}
}
