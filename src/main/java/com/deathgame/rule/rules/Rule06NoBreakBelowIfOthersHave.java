package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Rule 6: Can't break blocks below you if another player has them in inventory
 * "Below" means any Y level lower than player's feet, including diagonally.
 * Checks the drop result without silk touch.
 */
public class Rule06NoBreakBelowIfOthersHave extends AbstractRule {
    
    public Rule06NoBreakBelowIfOthersHave() {
        super(6, "Нельзя ломать блоки ниже себя, если они есть у другого игрока в инвентаре");
    }
    
    /**
     * Called when a player breaks a block
     */
    public void onBlockBroken(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (!isValidTarget(player)) return;
        
        // Check if block is below player (Y level of block < Y level of player's feet)
        double playerFeetY = player.getY();
        int blockY = pos.getY();
        
        if (blockY >= playerFeetY) {
            // Block is at or above player's feet, allowed
            return;
        }
        
        // Get what this block would drop (without silk touch)
        List<Item> dropItems = getBlockDropItems(player, pos, state);
        
        if (dropItems.isEmpty()) {
            return;
        }
        
        // Check if any other participant has these items
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        for (ServerPlayerEntity other : gameManager.getOnlineParticipants()) {
            if (other.equals(player)) continue;
            if (!isValidTarget(other)) continue;
            
            for (Item dropItem : dropItems) {
                if (playerHasItem(other, dropItem)) {
                    DeathGameMod.LOGGER.info("[Rule6] Player {} broke block {} below them, but {} has it", 
                        player.getName().getString(), 
                        state.getBlock().getName().getString(),
                        other.getName().getString());
                    killPlayer(player);
                    return;
                }
            }
        }
    }
    
    private List<Item> getBlockDropItems(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        ServerWorld world = player.getServerWorld();
        
        // Create loot context without silk touch
        LootWorldContext.Builder builder = new LootWorldContext.Builder(world)
            .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
            .add(LootContextParameters.TOOL, ItemStack.EMPTY) // No silk touch
            .addOptional(LootContextParameters.THIS_ENTITY, player)
            .addOptional(LootContextParameters.BLOCK_ENTITY, world.getBlockEntity(pos));
        
        try {
            List<ItemStack> drops = state.getDroppedStacks(builder);
            return drops.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .distinct()
                .toList();
        } catch (Exception e) {
            // Fallback: just use the block's item
            Item blockItem = state.getBlock().asItem();
            if (blockItem != null) {
                return List.of(blockItem);
            }
            return List.of();
        }
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
}
