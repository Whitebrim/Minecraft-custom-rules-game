package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.block.Block;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 10: Can't mine the same block (1 block buffer)
 * Each player has a memory of the last block they mined.
 * If another player mines a block that matches any other player's last mined block,
 * that player dies.
 */
public class Rule10SameBlockMining extends AbstractRule {
    
    private final Map<UUID, Block> lastMinedBlock = new HashMap<>();
    
    public Rule10SameBlockMining() {
        super(10, "Нельзя добывать тот же блок, что последним добыл другой игрок");
    }
    
    /**
     * Called when a player breaks a block
     * Returns true if player should die
     */
    public boolean onBlockBroken(ServerPlayerEntity player, Block block) {
        if (!isValidTarget(player)) return false;
        
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        UUID playerId = player.getUuid();
        
        // Check if this block matches any other participant's last mined block
        for (UUID uuid : gameManager.getParticipants()) {
            if (uuid.equals(playerId)) continue;
            
            Block otherLastBlock = lastMinedBlock.get(uuid);
            if (otherLastBlock != null && otherLastBlock.equals(block)) {
                // Player mined same block as someone else's last block - they die
                // DO NOT update their memory - this would overwrite the "forbidden" block
                killPlayer(player);
                return true;
            }
        }
        
        // Safe - update memory
        lastMinedBlock.put(playerId, block);
        return false;
    }
    
    @Override
    public void reset() {
        lastMinedBlock.clear();
    }
}
