package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 10: Can't craft items that another player already has in their inventory
 * When crafting, if the result item is already in another participant's inventory, crafter dies.
 */
public class Rule10NoCraftDuplicates extends AbstractRule {
    
    public Rule10NoCraftDuplicates() {
        super(10, "Нельзя крафтить предметы которые уже есть у другого игрока");
    }
    
    /**
     * Called when player takes a crafted item
     * Returns true if player should be killed
     */
    public boolean shouldKillOnCraft(ServerPlayerEntity crafter, ItemStack craftedItem) {
        if (!isValidTarget(crafter)) return false;
        if (craftedItem.isEmpty()) return false;
        
        Item craftedItemType = craftedItem.getItem();
        
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        for (ServerPlayerEntity other : gameManager.getOnlineParticipants()) {
            if (other.equals(crafter)) continue;
            
            // Check if other player has this item
            if (playerHasItem(other, craftedItemType)) {
                DeathGameMod.LOGGER.info("[Rule10] Player {} crafted {} which {} already has", 
                    crafter.getName().getString(),
                    craftedItemType.toString(),
                    other.getName().getString());
                return true;
            }
        }
        
        return false;
    }
    
    private boolean playerHasItem(ServerPlayerEntity player, Item item) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }
    
    public void killPlayerForViolation(ServerPlayerEntity player) {
        if (!isValidTarget(player)) return;
        killPlayer(player);
    }
}
