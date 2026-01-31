package com.deathgame.game;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.RuleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles saving and loading game state to/from disk.
 * State is saved in the world folder as deathgame_state.json
 */
public class GameStateSaver {
    
    private static final String STATE_FILE_NAME = "deathgame_state.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Data class representing the saved game state
     */
    public static class GameState {
        public boolean gameRunning = false;
        public List<String> participants = new ArrayList<>(); // UUIDs as strings
        public Map<Integer, Boolean> enabledRules = new HashMap<>();
        public Map<Integer, Boolean> revealedRules = new HashMap<>();
        public Map<String, Integer> playerScores = new HashMap<>(); // Player name -> score
        
        public GameState() {
            // Initialize default rule states
            for (int i = 1; i <= RuleManager.TOTAL_RULES; i++) {
                enabledRules.put(i, true);
                revealedRules.put(i, false);
            }
        }
    }
    
    /**
     * Get the path to the state file in the world folder
     */
    private static Path getStatePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(STATE_FILE_NAME);
    }
    
    /**
     * Save the current game state to disk
     */
    public static void saveState(MinecraftServer server, GameManager gameManager, RuleManager ruleManager) {
        if (server == null) return;
        
        try {
            GameState state = new GameState();
            
            // Save game running status
            state.gameRunning = gameManager.isGameRunning();
            
            // Save participants
            for (UUID uuid : gameManager.getParticipants()) {
                state.participants.add(uuid.toString());
            }
            
            // Save rule states
            for (int i = 1; i <= RuleManager.TOTAL_RULES; i++) {
                state.enabledRules.put(i, ruleManager.isRuleEnabled(i));
                state.revealedRules.put(i, ruleManager.isRuleRevealed(i));
            }
            
            // Save player scores from scoreboard
            var scoreboard = server.getScoreboard();
            var objective = scoreboard.getNullableObjective(GameManager.SCOREBOARD_OBJECTIVE);
            if (objective != null) {
                for (var holder : scoreboard.getKnownScoreHolders()) {
                    var scoreAccess = scoreboard.getScore(holder, objective);
                    if (scoreAccess != null) {
                        state.playerScores.put(holder.getNameForScoreboard(), scoreAccess.getScore());
                    }
                }
            }
            
            // Write to file
            Path statePath = getStatePath(server);
            String json = GSON.toJson(state);
            Files.writeString(statePath, json);
            
            DeathGameMod.LOGGER.info("Game state saved to {}", statePath);
            
        } catch (Exception e) {
            DeathGameMod.LOGGER.error("Failed to save game state", e);
        }
    }
    
    /**
     * Load game state from disk
     * Returns null if no state file exists or loading fails
     */
    public static GameState loadState(MinecraftServer server) {
        if (server == null) return null;
        
        Path statePath = getStatePath(server);
        
        if (!Files.exists(statePath)) {
            DeathGameMod.LOGGER.info("No saved game state found");
            return null;
        }
        
        try {
            String json = Files.readString(statePath);
            GameState state = GSON.fromJson(json, GameState.class);
            
            DeathGameMod.LOGGER.info("Game state loaded from {}", statePath);
            return state;
            
        } catch (Exception e) {
            DeathGameMod.LOGGER.error("Failed to load game state", e);
            return null;
        }
    }
    
    /**
     * Delete the state file (used on full reset)
     */
    public static void deleteState(MinecraftServer server) {
        if (server == null) return;
        
        try {
            Path statePath = getStatePath(server);
            Files.deleteIfExists(statePath);
            DeathGameMod.LOGGER.info("Game state file deleted");
        } catch (Exception e) {
            DeathGameMod.LOGGER.error("Failed to delete game state file", e);
        }
    }
}
