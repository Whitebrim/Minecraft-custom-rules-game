package com.deathgame.game;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.RuleManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    public static final String SCOREBOARD_OBJECTIVE = "deathgame_triggers";
    public static final int MAX_PARTICIPANTS = 3;
    
    private final RuleManager ruleManager;
    private final Set<UUID> participants = new HashSet<>();
    private MinecraftServer server;
    private boolean gameRunning = false;
    private ScoreboardObjective objective;
    
    public GameManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
        initScoreboard();
    }
    
    private void initScoreboard() {
        if (server == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        objective = scoreboard.getNullableObjective(SCOREBOARD_OBJECTIVE);
        
        if (objective == null) {
            objective = scoreboard.addObjective(
                SCOREBOARD_OBJECTIVE,
                ScoreboardCriterion.DUMMY,
                Text.literal("Death Game").formatted(Formatting.RED, Formatting.BOLD),
                ScoreboardCriterion.RenderType.INTEGER,
                true,
                null
            );
        }
        
        scoreboard.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
    }
    
    public boolean addParticipant(ServerPlayerEntity player) {
        if (gameRunning) {
            player.sendMessage(Text.literal("Игра уже запущена!").formatted(Formatting.RED));
            return false;
        }
        
        if (participants.size() >= MAX_PARTICIPANTS) {
            player.sendMessage(Text.literal("Максимальное количество участников: " + MAX_PARTICIPANTS).formatted(Formatting.RED));
            return false;
        }
        
        if (participants.contains(player.getUuid())) {
            player.sendMessage(Text.literal("Вы уже участвуете!").formatted(Formatting.YELLOW));
            return false;
        }
        
        participants.add(player.getUuid());
        updateScoreboardForPlayer(player, 0);
        
        broadcastMessage(Text.literal(player.getName().getString() + " присоединился к игре! (" + participants.size() + "/" + MAX_PARTICIPANTS + ")").formatted(Formatting.GREEN));
        return true;
    }
    
    public boolean removeParticipant(ServerPlayerEntity player) {
        if (gameRunning) {
            player.sendMessage(Text.literal("Нельзя выйти во время игры!").formatted(Formatting.RED));
            return false;
        }
        
        if (!participants.contains(player.getUuid())) {
            return false;
        }
        
        participants.remove(player.getUuid());
        removeFromScoreboard(player);
        
        broadcastMessage(Text.literal(player.getName().getString() + " вышел из игры.").formatted(Formatting.YELLOW));
        return true;
    }
    
    public boolean removeParticipantByName(String playerName) {
        if (server == null) return false;
        
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            return removeParticipant(player);
        }
        
        // Player offline - just remove from participants set
        // Score will be cleared on full game reset
        Scoreboard scoreboard = server.getScoreboard();
        for (UUID uuid : new HashSet<>(participants)) {
            for (var holder : scoreboard.getKnownScoreHolders()) {
                if (holder.getNameForScoreboard().equals(playerName)) {
                    participants.remove(uuid);
                    return true;
                }
            }
        }
        return false;
    }
    
    private void updateScoreboardForPlayer(ServerPlayerEntity player, int score) {
        if (server == null || objective == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);
        scoreAccess.setScore(score);
    }
    
    private void removeFromScoreboard(ServerPlayerEntity player) {
        // Score will remain on scoreboard until full game reset
        // This is fine since the objective is recreated on reset
    }
    
    public void incrementTriggerCount(ServerPlayerEntity player) {
        if (server == null || objective == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);
        scoreAccess.setScore(scoreAccess.getScore() + 1);
    }
    
    public boolean startGame() {
        if (gameRunning) return false;
        if (participants.isEmpty()) return false;
        
        gameRunning = true;
        ruleManager.resetAllRules();
        
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.DARK_RED));
        broadcastMessage(Text.literal("       DEATH GAME НАЧИНАЕТСЯ!").formatted(Formatting.RED, Formatting.BOLD));
        broadcastMessage(Text.literal("  Отгадайте все 10 правил или убейте Дракона!").formatted(Formatting.GOLD));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.DARK_RED));
        
        // Reset scores
        for (UUID uuid : participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                updateScoreboardForPlayer(player, 0);
            }
        }
        
        return true;
    }
    
    public boolean stopGame() {
        if (!gameRunning) return false;
        
        gameRunning = false;
        ruleManager.resetAllRules();
        
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GRAY));
        broadcastMessage(Text.literal("       ИГРА ОСТАНОВЛЕНА").formatted(Formatting.GRAY, Formatting.BOLD));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GRAY));
        
        return true;
    }
    
    public void resetGame() {
        gameRunning = false;
        participants.clear();
        ruleManager.resetAllRules();
        ruleManager.hideAllRules();
        
        if (server != null && objective != null) {
            Scoreboard scoreboard = server.getScoreboard();
            scoreboard.removeObjective(objective);
            initScoreboard();
        }
        
        broadcastMessage(Text.literal("Игра полностью сброшена.").formatted(Formatting.YELLOW));
    }
    
    public void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        // Check for Ender Dragon death
        if (entity.getType() == EntityType.ENDER_DRAGON && gameRunning) {
            triggerVictory("Дракон побеждён!");
        }
    }
    
    public void onRuleTriggered(ServerPlayerEntity player, int ruleId) {
        if (!isParticipant(player)) return;
        
        incrementTriggerCount(player);
        
        String ruleName = ruleManager.isRuleRevealed(ruleId) 
            ? ruleManager.getRuleDescription(ruleId)
            : "Правило #" + ruleId;
        
        broadcastMessage(Text.literal(player.getName().getString() + " погиб от: " + ruleName).formatted(Formatting.RED));
    }
    
    public void checkVictoryByRules() {
        if (!gameRunning) return;
        
        int revealedCount = ruleManager.getRevealedCount();
        if (revealedCount >= 10) {
            triggerVictory("Все правила отгаданы!");
        }
    }
    
    private void triggerVictory(String reason) {
        gameRunning = false;
        
        // Show victory title to all players
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("ПОБЕДА!").formatted(Formatting.GOLD, Formatting.BOLD)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                Text.literal(reason).formatted(Formatting.YELLOW)
            ));
            
            // Spawn fireworks
            spawnVictoryFireworks(player);
        }
        
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD));
        broadcastMessage(Text.literal("       ИГРОКИ ПОБЕДИЛИ!").formatted(Formatting.GREEN, Formatting.BOLD));
        broadcastMessage(Text.literal("       " + reason).formatted(Formatting.YELLOW));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD));
    }
    
    private void spawnVictoryFireworks(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        
        for (int i = 0; i < 5; i++) {
            ItemStack fireworkStack = new ItemStack(Items.FIREWORK_ROCKET);
            
            // Create explosion component
            FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.LARGE_BALL,
                IntList.of(0xFFFF00, 0x00FF00, 0xFF0000),
                IntList.of(),
                true,
                true
            );
            
            // Create fireworks component
            FireworksComponent fireworksComponent = new FireworksComponent(
                2,
                List.of(explosion)
            );
            
            fireworkStack.set(DataComponentTypes.FIREWORKS, fireworksComponent);
            
            double offsetX = (Math.random() - 0.5) * 10;
            double offsetZ = (Math.random() - 0.5) * 10;
            
            FireworkRocketEntity firework = new FireworkRocketEntity(
                world,
                player.getX() + offsetX,
                player.getY() + 1,
                player.getZ() + offsetZ,
                fireworkStack
            );
            world.spawnEntity(firework);
        }
    }
    
    public boolean isParticipant(ServerPlayerEntity player) {
        return participants.contains(player.getUuid());
    }
    
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    public Set<UUID> getParticipants() {
        return new HashSet<>(participants);
    }
    
    public MinecraftServer getServer() {
        return server;
    }
    
    private void broadcastMessage(Text message) {
        if (server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.sendMessage(message);
        }
    }
}
