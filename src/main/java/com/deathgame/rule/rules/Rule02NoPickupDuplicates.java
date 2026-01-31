package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 2: Can't pick up items you already have in inventory
 * If a player picks up an item that they already have anywhere in their inventory, they die.
 */
public class Rule02NoPickupDuplicates extends AbstractRule {
    
    public Rule02NoPickupDuplicates() {
        super(2, "Нельзя подбирать предметы которые уже есть в инвентаре");
    }
    
    /**
     * Check if player should be killed for picking up an item
     * Called BEFORE the item is actually added to inventory
     */
    public boolean shouldKillOnPickup(ServerPlayerEntity player, ItemStack pickedUpStack) {
        if (!isValidTarget(player)) return false;
        
        Item pickedItem = pickedUpStack.getItem();
        
        // Check entire inventory (main + armor + offhand)
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack invStack = player.getInventory().getStack(i);
            if (!invStack.isEmpty() && invStack.getItem() == pickedItem) {
                DeathGameMod.LOGGER.info("[Rule2] Player {} picked up duplicate item: {}", 
                    player.getName().getString(), pickedItem.toString());
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
