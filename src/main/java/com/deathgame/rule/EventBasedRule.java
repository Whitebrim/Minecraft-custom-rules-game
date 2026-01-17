package com.deathgame.rule;

import net.minecraft.server.MinecraftServer;

public abstract class EventBasedRule extends AbstractRule {
    
    protected boolean registered = false;
    
    public EventBasedRule(int id, String description) {
        super(id, description);
    }
    
    @Override
    public void tick(MinecraftServer server) {
    }
    
    @Override
    public abstract void register();
    
    @Override
    public void unregister() {
        registered = false;
    }
    
    protected boolean shouldProcess() {
        return registered;
    }
}
