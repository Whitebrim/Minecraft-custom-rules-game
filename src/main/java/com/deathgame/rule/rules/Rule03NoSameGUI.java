package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.screen.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 3: Two players can't have the same GUI type open simultaneously
 * If two players have the same type of screen open (inventory, chest, crafting table, furnace, etc.), both die.
 * Note: PlayerScreenHandler when no GUI is open doesn't count - only when player pressed E to open inventory.
 */
public class Rule03NoSameGUI extends AbstractRule {
    
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    // Track players who have actively opened their inventory (pressed E)
    private final Set<UUID> hasOpenInventory = new HashSet<>();
    
    // Map screen handler class to readable name
    private static final Map<Class<? extends ScreenHandler>, String> GUI_NAMES = new HashMap<>();
    
    static {
        GUI_NAMES.put(PlayerScreenHandler.class, "Инвентарь");
        GUI_NAMES.put(GenericContainerScreenHandler.class, "Сундук");
        GUI_NAMES.put(CraftingScreenHandler.class, "Верстак");
        GUI_NAMES.put(FurnaceScreenHandler.class, "Печка");
        GUI_NAMES.put(BlastFurnaceScreenHandler.class, "Плавильня");
        GUI_NAMES.put(SmokerScreenHandler.class, "Коптильня");
        GUI_NAMES.put(AnvilScreenHandler.class, "Наковальня");
        GUI_NAMES.put(EnchantmentScreenHandler.class, "Стол зачарований");
        GUI_NAMES.put(BrewingStandScreenHandler.class, "Зельеварка");
        GUI_NAMES.put(BeaconScreenHandler.class, "Маяк");
        GUI_NAMES.put(ShulkerBoxScreenHandler.class, "Шалкер");
        GUI_NAMES.put(HopperScreenHandler.class, "Воронка");
        GUI_NAMES.put(CartographyTableScreenHandler.class, "Картографический стол");
        GUI_NAMES.put(GrindstoneScreenHandler.class, "Точило");
        GUI_NAMES.put(LoomScreenHandler.class, "Ткацкий станок");
        GUI_NAMES.put(StonecutterScreenHandler.class, "Камнерез");
        GUI_NAMES.put(SmithingScreenHandler.class, "Кузнечный стол");
        GUI_NAMES.put(MerchantScreenHandler.class, "Торговля");
    }
    
    public Rule03NoSameGUI() {
        super(3, "Нельзя двоим открывать один тип GUI одновременно");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        List<ServerPlayerEntity> players = gameManager.getOnlineParticipants()
            .stream()
            .filter(this::isValidTarget)
            .toList();
        
        if (players.size() < 2) return;
        
        // Group players by their open screen type
        // Only count screens that are NOT the default PlayerScreenHandler
        // (i.e., player has actually opened something)
        Map<Class<? extends ScreenHandler>, List<ServerPlayerEntity>> screenGroups = new HashMap<>();
        
        for (ServerPlayerEntity player : players) {
            ScreenHandler handler = player.currentScreenHandler;
            if (handler == null) continue;
            
            // Skip if this is the default player screen handler (nothing opened)
            // Player has a GUI open only if currentScreenHandler != playerScreenHandler
            if (handler == player.playerScreenHandler) {
                continue;
            }
            
            Class<? extends ScreenHandler> screenClass = handler.getClass();
            screenGroups.computeIfAbsent(screenClass, k -> new ArrayList<>()).add(player);
        }
        
        // Check for duplicates
        Set<ServerPlayerEntity> toKill = new HashSet<>();
        
        for (Map.Entry<Class<? extends ScreenHandler>, List<ServerPlayerEntity>> entry : screenGroups.entrySet()) {
            if (entry.getValue().size() >= 2) {
                String guiName = GUI_NAMES.getOrDefault(entry.getKey(), entry.getKey().getSimpleName());
                DeathGameMod.LOGGER.info("[Rule3] Multiple players have {} open", guiName);
                toKill.addAll(entry.getValue());
            }
        }
        
        if (!toKill.isEmpty()) {
            lastTriggerTime = currentTime;
            for (ServerPlayerEntity player : toKill) {
                killPlayer(player);
            }
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
        hasOpenInventory.clear();
    }
}
