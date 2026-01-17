package com.deathgame;

import com.deathgame.command.GameCommands;
import com.deathgame.command.ParticipateCommand;
import com.deathgame.command.RuleCommand;
import com.deathgame.game.GameManager;
import com.deathgame.rule.RuleManager;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
        
        registerCommands();
        registerEvents();
        
        LOGGER.info("Death Game mod initialized!");
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
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (gameManager.isGameRunning()) {
                ruleManager.tick(server);
            }
        });
        
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            gameManager.onEntityDeath(entity, damageSource);
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
