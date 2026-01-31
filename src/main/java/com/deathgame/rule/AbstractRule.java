package com.deathgame.rule;

import com.deathgame.DeathGameMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public abstract class AbstractRule implements Rule {
    protected final int id;
    protected final String description;
    
    public AbstractRule(int id, String description) {
        this.id = id;
        this.description = description;
    }
    
    @Override
    public int getId() {
        return id;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public void tick(MinecraftServer server) {
    }
    
    @Override
    public void reset() {
    }
    
    protected void killPlayer(ServerPlayerEntity player) {
        if (!DeathGameMod.getInstance().getGameManager().isParticipant(player)) {
            return;
        }
        
        if (!DeathGameMod.getInstance().getRuleManager().isRuleEnabled(id)) {
            return;
        }
        
        // Notify game manager first
        DeathGameMod.getInstance().getGameManager().onRuleTriggered(player, id);
        
        // Kill the player with generic damage
        player.damage(player.getServerWorld(), player.getDamageSources().magic(), Float.MAX_VALUE);
    }
    
    protected boolean isValidTarget(ServerPlayerEntity player) {
        if (player == null || player.isSpectator() || player.isCreative() || player.isDead()) {
            return false;
        }
        
        return DeathGameMod.getInstance().getGameManager().isParticipant(player) 
            && DeathGameMod.getInstance().getGameManager().isGameRunning()
            && DeathGameMod.getInstance().getRuleManager().isRuleEnabled(id);
    }
}
