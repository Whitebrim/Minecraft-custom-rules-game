package com.deathgame.rule;

import com.deathgame.DeathGameMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleManager {
    private final List<Rule> rules = new ArrayList<>();
    private final Map<Integer, Boolean> enabledRules = new HashMap<>();
    private final Map<Integer, Boolean> revealedRules = new HashMap<>();
    private MinecraftServer server;
    
    public RuleManager() {
        // Initialize 10 placeholder rules
        for (int i = 1; i <= 10; i++) {
            enabledRules.put(i, true);
            revealedRules.put(i, false);
        }
    }
    
    public void registerRule(Rule rule) {
        rules.add(rule);
        enabledRules.put(rule.getId(), true);
        revealedRules.put(rule.getId(), false);
        rule.register();
        DeathGameMod.LOGGER.info("Registered rule #{}: {}", rule.getId(), rule.getDescription());
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    public void tick(MinecraftServer server) {
        for (Rule rule : rules) {
            if (enabledRules.getOrDefault(rule.getId(), false)) {
                rule.tick(server);
            }
        }
    }
    
    public boolean enableRule(int id) {
        if (id < 1 || id > 10) return false;
        enabledRules.put(id, true);
        
        Rule rule = getRuleById(id);
        if (rule != null) {
            rule.reset();
        }
        
        return true;
    }
    
    public boolean disableRule(int id) {
        if (id < 1 || id > 10) return false;
        enabledRules.put(id, false);
        return true;
    }
    
    public boolean isRuleEnabled(int id) {
        return enabledRules.getOrDefault(id, false);
    }
    
    public boolean revealRule(int id) {
        if (id < 1 || id > 10) return false;
        if (revealedRules.getOrDefault(id, false)) return false;
        
        revealedRules.put(id, true);
        
        String description = getRuleDescription(id);
        
        if (server != null) {
            Text message = Text.literal("═══════════════════════════════").formatted(Formatting.GOLD);
            Text ruleText = Text.literal("ПРАВИЛО #" + id + " РАСКРЫТО!").formatted(Formatting.GREEN, Formatting.BOLD);
            Text descText = Text.literal(description).formatted(Formatting.YELLOW);
            Text separator = Text.literal("═══════════════════════════════").formatted(Formatting.GOLD);
            
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(message);
                player.sendMessage(ruleText);
                player.sendMessage(descText);
                player.sendMessage(separator);
            }
        }
        
        DeathGameMod.getInstance().getGameManager().checkVictoryByRules();
        
        return true;
    }
    
    public boolean isRuleRevealed(int id) {
        return revealedRules.getOrDefault(id, false);
    }
    
    public String getRuleDescription(int id) {
        Rule rule = getRuleById(id);
        if (rule != null) {
            return rule.getDescription();
        }
        return "Правило #" + id + " (не реализовано)";
    }
    
    public int getRevealedCount() {
        int count = 0;
        for (boolean revealed : revealedRules.values()) {
            if (revealed) count++;
        }
        return count;
    }
    
    public Rule getRuleById(int id) {
        for (Rule rule : rules) {
            if (rule.getId() == id) {
                return rule;
            }
        }
        return null;
    }
    
    public void resetAllRules() {
        for (Rule rule : rules) {
            rule.reset();
        }
        
        for (int i = 1; i <= 10; i++) {
            enabledRules.put(i, true);
        }
    }
    
    /**
     * Called when a participant respawns after death.
     * Notifies all rules so they can reset per-player state.
     */
    public void onPlayerRespawn(ServerPlayerEntity player) {
        for (Rule rule : rules) {
            rule.onPlayerRespawn(player);
        }
    }
    
    public void hideAllRules() {
        for (int i = 1; i <= 10; i++) {
            revealedRules.put(i, false);
        }
    }
    
    public List<Rule> getAllRules() {
        return new ArrayList<>(rules);
    }
}
