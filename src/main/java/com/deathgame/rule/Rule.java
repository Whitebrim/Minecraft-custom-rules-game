package com.deathgame.rule;

import net.minecraft.server.MinecraftServer;

public interface Rule {
    
    int getId();
    
    String getDescription();
    
    void tick(MinecraftServer server);
    
    void reset();
    
    default void register() {}
    
    default void unregister() {}
}
