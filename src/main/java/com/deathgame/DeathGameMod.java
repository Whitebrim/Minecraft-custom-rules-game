package com.deathgame;

import com.deathgame.command.GameCommands;
import com.deathgame.command.ParticipateCommand;
import com.deathgame.command.RuleCommand;
import com.deathgame.game.GameManager;
import com.deathgame.rule.RuleManager;
import com.deathgame.rule.rules.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathGameMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "deathgame";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static DeathGameMod instance;
    private GameManager gameManager;
    private RuleManager ruleManager;

    @Override
    public void onInitializeServer() {
        instance = this;
        LOGGER.info("Death Game mod initializing...");
        
        ruleManager = new RuleManager();
        gameManager = new GameManager(ruleManager);
        
        registerRules();
        registerCommands();
        registerEvents();
        
        LOGGER.info("Death Game mod initialized with {} rules!", RuleManager.TOTAL_RULES);
    }
    
    private void registerRules() {
        // Season 2 Rules
        ruleManager.registerRule(new Rule01EatLookingAtBlock());
        ruleManager.registerRule(new Rule02NoPickupDuplicates());
        ruleManager.registerRule(new Rule03NoSameGUI());
        ruleManager.registerRule(new Rule04NoSynchronizedAction());
        ruleManager.registerRule(new Rule05Halal());
        ruleManager.registerRule(new Rule06NoBreakBelowIfOthersHave());
        ruleManager.registerRule(new Rule07LoveTriangle());
        ruleManager.registerRule(new Rule08MaxDistance());
        ruleManager.registerRule(new Rule09FibonacciHotbar());
        ruleManager.registerRule(new Rule10NoCraftDuplicates());
        ruleManager.registerRule(new Rule11NoAFK());
        ruleManager.registerRule(new Rule12NoLookAtSky());
        ruleManager.registerRule(new Rule13NoTreeShadow());
        ruleManager.registerRule(new Rule14NoAttackPlayers());
        ruleManager.registerRule(new Rule15NoChat());
        ruleManager.registerRule(new Rule16NoWalkBackward());
        ruleManager.registerRule(new Rule17NoSimultaneousMining());
        ruleManager.registerRule(new Rule18XpSlotsLimit());
        ruleManager.registerRule(new Rule19NoSwitchHostileMob());
        ruleManager.registerRule(new Rule20JumpLimit());
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ParticipateCommand.register(dispatcher);
            RuleCommand.register(dispatcher);
            GameCommands.register(dispatcher);
        });
    }
    
    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            gameManager.setServer(server);
            ruleManager.setServer(server);
        });
        
        // Save state when server is stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping, saving game state...");
            gameManager.saveState();
        });
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (gameManager.isGameRunning()) {
                ruleManager.tick(server);
            }
        });
        
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            gameManager.onEntityDeath(entity, damageSource);
        });
        
        // Handle player respawn - notify rules to reset per-player state
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (gameManager.isParticipant(newPlayer)) {
                ruleManager.onPlayerRespawn(newPlayer);
            }
        });
    }
    
    public static DeathGameMod getInstance() {
        return instance;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public RuleManager getRuleManager() {
        return ruleManager;
    }
}
