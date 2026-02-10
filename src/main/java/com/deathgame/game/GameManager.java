package com.deathgame.game;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.RuleManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    public static final String SCOREBOARD_OBJECTIVE = "deathgame_triggers";
    public static final int MAX_PARTICIPANTS = 3;
    
    private final RuleManager ruleManager;
    private final Set<UUID> participants = new HashSet<>();
    private final Map<UUID, Integer> lastDeathRule = new HashMap<>(); // Track which rule killed a player
    private MinecraftServer server;
    private boolean gameRunning = false;
    private ScoreboardObjective objective;
    private long elapsedTicks = 0;
    private ServerBossBar timerBossBar;
    
    public GameManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
        initScoreboard();
        initBossBar();
        loadState();
    }
    
    private void initBossBar() {
        if (server == null) return;
        timerBossBar = new ServerBossBar(
            Text.literal("⏱ 00:00:00").formatted(Formatting.AQUA),
            BossBar.Color.BLUE,
            BossBar.Style.PROGRESS
        );
        timerBossBar.setPercent(0.0f);
    }
    
    /**
     * Called every server tick. Increments game timer and updates bossbar.
     */
    public void tick() {
        if (!gameRunning || server == null) return;
        
        elapsedTicks++;
        
        // Update bossbar every 20 ticks (1 second)
        if (elapsedTicks % 20 == 0) {
            updateTimerBossBar();
        }
        
        // Sync bossbar players (add new, remove gone)
        if (elapsedTicks % 100 == 0) {
            syncBossBarPlayers();
        }
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
    
    /**
     * Load saved game state from disk
     */
    private void loadState() {
        GameStateSaver.GameState state = GameStateSaver.loadState(server);
        if (state == null) return;
        
        // Restore game running status
        gameRunning = state.gameRunning;
        
        // Restore elapsed ticks
        elapsedTicks = state.elapsedTicks;
        
        // Restore participants
        participants.clear();
        for (String uuidStr : state.participants) {
            try {
                participants.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                DeathGameMod.LOGGER.warn("Invalid UUID in saved state: {}", uuidStr);
            }
        }
        
        // Restore rule states
        ruleManager.restoreState(state.enabledRules, state.revealedRules);
        
        // Restore player scores
        restoreScores(state.playerScores);
        
        if (gameRunning) {
            DeathGameMod.LOGGER.info("Restored running game with {} participants, elapsed {}",
                participants.size(), formatTime(elapsedTicks));
            broadcastMessage(Text.literal("Игра восстановлена после перезапуска сервера.").formatted(Formatting.GREEN));
            showBossBar();
        }
    }
    
    /**
     * Restore player scores from saved state
     */
    private void restoreScores(Map<String, Integer> playerScores) {
        if (server == null || objective == null) return;
        
        Scoreboard scoreboard = server.getScoreboard();
        
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            String playerName = entry.getKey();
            int score = entry.getValue();
            
            // Try to find the player
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) {
                ScoreAccess scoreAccess = scoreboard.getOrCreateScore(player, objective);
                scoreAccess.setScore(score);
            } else {
                // Player not online, but we need to restore their score
                // The scoreboard will keep it when they rejoin
                scoreboard.getOrCreateScore(() -> playerName, objective).setScore(score);
            }
        }
    }
    
    /**
     * Save current game state to disk
     */
    public void saveState() {
        GameStateSaver.saveState(server, this, ruleManager);
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
        
        saveState();
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
        
        saveState();
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
                    saveState();
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
        
        saveState();
    }
    
    public boolean startGame() {
        if (gameRunning) return false;
        if (participants.isEmpty()) return false;
        
        gameRunning = true;
        elapsedTicks = 0;
        lastDeathRule.clear();
        ruleManager.resetAllRules();
        
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.DARK_RED));
        broadcastMessage(Text.literal("       DEATH GAME НАЧИНАЕТСЯ!").formatted(Formatting.RED, Formatting.BOLD));
        broadcastMessage(Text.literal("  Отгадайте все " + RuleManager.TOTAL_RULES + " правил или убейте Дракона!").formatted(Formatting.GOLD));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.DARK_RED));
        
        // Show timer bossbar
        showBossBar();
        
        // Reset scores
        for (UUID uuid : participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                updateScoreboardForPlayer(player, 0);
            }
        }
        
        saveState();
        return true;
    }
    
    public boolean stopGame() {
        if (!gameRunning) return false;
        
        gameRunning = false;
        ruleManager.resetAllRules();
        hideBossBar();
        
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GRAY));
        broadcastMessage(Text.literal("       ИГРА ОСТАНОВЛЕНА").formatted(Formatting.GRAY, Formatting.BOLD));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GRAY));
        
        saveState();
        return true;
    }
    
    public void resetGame() {
        gameRunning = false;
        elapsedTicks = 0;
        lastDeathRule.clear();
        participants.clear();
        ruleManager.resetAllRules();
        ruleManager.hideAllRules();
        hideBossBar();
        
        if (server != null && objective != null) {
            Scoreboard scoreboard = server.getScoreboard();
            scoreboard.removeObjective(objective);
            initScoreboard();
        }
        
        // Delete saved state
        GameStateSaver.deleteState(server);
        
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
        
        // Track which rule killed the player (for actionbar on respawn)
        lastDeathRule.put(player.getUuid(), ruleId);
        
        String ruleName = ruleManager.isRuleRevealed(ruleId) 
            ? ruleManager.getRuleDescription(ruleId)
            : "Правило #" + ruleId;
        
        broadcastMessage(Text.literal(player.getName().getString() + " погиб от: " + ruleName).formatted(Formatting.RED));
    }
    
    public void checkVictoryByRules() {
        if (!gameRunning) return;
        
        int revealedCount = ruleManager.getRevealedCount();
        if (revealedCount >= RuleManager.TOTAL_RULES) {
            triggerVictory("Все правила отгаданы!");
        }
    }
    
    private void triggerVictory(String reason) {
        String timeStr = formatTime(elapsedTicks);
        gameRunning = false;
        hideBossBar();
        
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
        broadcastMessage(Text.literal("       ⏱ Время: " + timeStr).formatted(Formatting.AQUA));
        broadcastMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD));
        
        saveState();
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
    
    public List<ServerPlayerEntity> getOnlineParticipants() {
        List<ServerPlayerEntity> online = new ArrayList<>();
        if (server == null) return online;
        
        for (UUID uuid : participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
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
    
    // ---- Timer & BossBar ----
    
    private void updateTimerBossBar() {
        if (timerBossBar == null) return;
        timerBossBar.setName(
            Text.literal("⏱ " + formatTime(elapsedTicks)).formatted(Formatting.AQUA)
        );
        // No max time, so keep percent at 1.0 as a full bar
        timerBossBar.setPercent(1.0f);
    }
    
    private void syncBossBarPlayers() {
        if (timerBossBar == null || server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            timerBossBar.addPlayer(player);
        }
    }
    
    private void showBossBar() {
        if (timerBossBar == null || server == null) return;
        updateTimerBossBar();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            timerBossBar.addPlayer(player);
        }
    }
    
    private void hideBossBar() {
        if (timerBossBar == null) return;
        timerBossBar.clearPlayers();
    }
    
    /**
     * Add a newly connected player to the bossbar if game is running.
     */
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (gameRunning && timerBossBar != null) {
            timerBossBar.addPlayer(player);
        }
    }
    
    public static String formatTime(long ticks) {
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public long getElapsedTicks() {
        return elapsedTicks;
    }
    
    // ---- Death reason actionbar ----
    
    /**
     * Called on player respawn to show actionbar with death cause.
     */
    public void onPlayerRespawn(ServerPlayerEntity player) {
        Integer ruleId = lastDeathRule.remove(player.getUuid());
        if (ruleId != null) {
            if (ruleManager.isRuleRevealed(ruleId)) {
                // Revealed rule — actionbar with description
                player.sendMessage(
                    Text.literal("☠ Причина смерти: " + ruleManager.getRuleDescription(ruleId)).formatted(Formatting.RED),
                    true // actionbar overlay
                );
            } else {
                // Unknown rule — subtitle (more prominent)
                player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.empty() // empty title required for subtitle to show
                ));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("Правило #" + ruleId).formatted(Formatting.RED)
                ));
            }
        }
    }
}
