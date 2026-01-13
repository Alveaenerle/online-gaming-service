package com.online_games_service.ludo.service;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.dto.PlayerTimeoutMessage;
import com.online_games_service.ludo.enums.PlayerColor;
import com.online_games_service.ludo.exception.GameLogicException;
import com.online_games_service.ludo.exception.InvalidMoveException;
import com.online_games_service.ludo.model.LudoGame;
import com.online_games_service.ludo.model.LudoGameResult;
import com.online_games_service.ludo.model.LudoPawn;
import com.online_games_service.ludo.model.LudoPlayer;
import com.online_games_service.ludo.repository.mongo.LudoGameResultRepository;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LudoService {

    private final LudoGameRedisRepository gameRepository;
    private final LudoGameResultRepository gameResultRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private final ScheduledExecutorService turnTimeoutScheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> turnTimeouts = new ConcurrentHashMap<>();

    @Value("${ludo.amqp.exchange:game.events}")
    private String exchangeName;

    @Value("${ludo.amqp.routing.finish:ludo.finish}")
    private String finishRoutingKey;

    @Value("${ludo.turn-timeout-seconds:60}")
    private long turnTimeoutSeconds;

    private static final int BOARD_SIZE = 44;
    private static final String USER_GAME_KEY_PREFIX = "ludo:user-game:";

    // --- API FUNCTIONS ---

    public void createGame(String roomId, List<String> playerIds, String hostUserId, Map<String, String> usernames, Map<String, String> avatars, int maxPlayers) {
        if (playerIds == null || playerIds.isEmpty()) {
            log.warn("Cannot create Ludo game for room {}: playerIds is null or empty", roomId);
            return;
        }

        // Use the new constructor with maxPlayers for bot filling
        LudoGame game = new LudoGame(roomId, playerIds, hostUserId, usernames, maxPlayers);

        // Initialize avatars from lobby
        if (avatars != null && !avatars.isEmpty()) {
            game.setPlayersAvatars(new HashMap<>(avatars));
        }

        // Set bot avatars for any players that are bots
        Map<String, String> gameAvatars = game.getPlayersAvatars();
        for (LudoPlayer player : game.getPlayers()) {
            if (player.isBot()) {
                gameAvatars.put(player.getUserId(), "bot_avatar.svg");
            }
        }
        game.setPlayersAvatars(gameAvatars);

        if (!game.getPlayers().isEmpty()) {
            updateRollsCountForPlayer(game, game.getPlayers().get(0));
        } else {
            log.warn("Created Ludo game for room {} has no players; skipping initial rolls count update", roomId);
        }

        boolean created = gameRepository.createGameIfAbsent(game);

        if (!created) {
            log.info("Ludo game for room {} already exists (race condition prevented)", roomId);
            return;
        }

        for (String playerId : playerIds) {
            saveUserGameMapping(playerId, roomId);
        }

        // Set initial turn start time before broadcasting
        game.setTurnStartTime(System.currentTimeMillis());

        saveAndBroadcast(game, null);

        scheduleTurnTimeout(game);

        log.info("Ludo game created for room: {}, gameId: {}", roomId, game.getGameId());
    }

    public LudoGameStateMessage getGameState(String userId) {
        LudoGame game = getGameByUserId(userId);
        return mapToDTO(game, null);
    }

    public void rollDice(String userId) {
        LudoGame game = getGameByUserId(userId);

        validateTurn(game, userId);

        if (game.isDiceRolled() && game.isWaitingForMove()) {
            throw new GameLogicException("You must move before rolling again!");
        }
        if (game.getRollsLeft() <= 0) {
            throw new IllegalStateException("No rolls left!");
        }

        performRollLogic(game, userId);
    }

    public void movePawn(String userId, int pawnIndex) {
        LudoGame game = getGameByUserId(userId);

        validateTurn(game, userId);
        if (!game.isDiceRolled()) throw new IllegalStateException("Roll dice first!");

        LudoPlayer player = game.getPlayerById(userId);
        if (player == null) throw new IllegalArgumentException("Player not found in game");

        if (pawnIndex < 0 || pawnIndex >= player.getPawns().size()) {
            throw new InvalidMoveException("Invalid pawn index");
        }

        LudoPawn pawn = player.getPawns().get(pawnIndex);
        int roll = game.getLastDiceRoll();

        String capturedId = performMoveLogic(game, player, pawn, roll);

        handlePostMove(game, player, roll, capturedId);
    }

    /**
     * Handle a player voluntarily leaving the game.
     * The player will be replaced by a bot.
     */
    public void handlePlayerLeave(String userId) {
        LudoGame game;
        try {
            game = getGameByUserId(userId);
        } catch (IllegalArgumentException e) {
            log.info("Player {} tried to leave but is not in any game", userId);
            return;
        }

        if (game.getStatus() != RoomStatus.PLAYING) {
            log.info("Player {} tried to leave game {} but it's not in PLAYING status", userId, game.getRoomId());
            return;
        }

        LudoPlayer player = game.getPlayerById(userId);
        if (player == null || player.isBot()) {
            log.info("Player {} not found or is already a bot in game {}", userId, game.getRoomId());
            return;
        }

        log.info("Player {} voluntarily leaving game {}", userId, game.getRoomId());

        // Remove user-game mapping
        removeUserGameMapping(userId);

        // Replace player with bot
        int nextBotNum = game.getBotCounter() + 1;
        game.setBotCounter(nextBotNum);

        String oldId = player.getUserId();
        String botId = "bot-" + nextBotNum;

        player.setUserId(botId);
        player.setBot(true);

        Map<String, String> usernames = game.getPlayersUsernames();
        usernames.remove(oldId);
        usernames.put(botId, "Bot " + player.getColor().name());
        game.setPlayersUsernames(usernames);

        // Update avatars - remove old player's avatar and set bot avatar
        Map<String, String> avatars = game.getPlayersAvatars() != null
                ? new HashMap<>(game.getPlayersAvatars())
                : new HashMap<>();
        avatars.remove(oldId);
        avatars.put(botId, "bot_avatar.svg");
        game.setPlayersAvatars(avatars);

        // If it was this player's turn, let the bot take over
        if (game.getActivePlayerId().equals(oldId)) {
            game.setActivePlayerId(botId);
            game.setDiceRolled(false);
            game.setWaitingForMove(false);
            game.setLastDiceRoll(0);
            updateRollsCountForPlayer(game, player);
            cancelTurnTimeout(game.getRoomId());
        }

        // Check if all humans left
        if (checkAndAbortIfNoHumans(game)) {
            return;
        }

        saveAndBroadcast(game, null);

        // If it's now the bot's turn, trigger bot logic
        if (game.getActivePlayerId().equals(botId)) {
            handleBotTurn(game, botId);
        }
    }

    /**
     * Request the current game state to be sent via WebSocket for a user.
     */
    public void requestStateForUser(String userId) {
        LudoGame game = getGameByUserId(userId);
        saveAndBroadcast(game, null);
    }

    // --- LOOKUP & MAPPING METHODS (DODANE) ---

    private LudoGame getGameByUserId(String userId) {
        String roomId = stringRedisTemplate.opsForValue().get(USER_GAME_KEY_PREFIX + userId);
        if (roomId == null) {
            throw new IllegalArgumentException("User " + userId + " is not currently in any Ludo game");
        }
        return gameRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found for room: " + roomId));
    }

    private void saveUserGameMapping(String userId, String roomId) {
        stringRedisTemplate.opsForValue().set(USER_GAME_KEY_PREFIX + userId, roomId, Duration.ofHours(2));
    }

    private void removeUserGameMapping(String userId) {
        stringRedisTemplate.delete(USER_GAME_KEY_PREFIX + userId);
    }

    private void removeAllUserMappings(LudoGame game) {
        for (LudoPlayer player : game.getPlayers()) {
            if (!player.isBot()) {
                removeUserGameMapping(player.getUserId());
            }
        }
    }

    // --- ROLL AND MOVE LOGIC ---

    private void performRollLogic(LudoGame game, String playerId) {
        int roll = random.nextInt(6) + 1;
        game.setLastDiceRoll(roll);
        game.setDiceRolled(true);
        game.setRollsLeft(game.getRollsLeft() - 1);

        log.debug("Player {} rolled {}", playerId, roll);

        if (canPlayerMove(game, playerId, roll)) {
            game.setWaitingForMove(true);
        } else {
            if (game.getRollsLeft() > 0) {
                game.setDiceRolled(false);
                game.setWaitingForMove(false);
            } else {
                passTurnToNextPlayer(game);
                return;
            }
        }

        saveAndBroadcast(game, null);

        if (game.getStatus() == RoomStatus.PLAYING && !isBot(game.getActivePlayerId())) {
            scheduleTurnTimeout(game);
        }
    }

    private String performMoveLogic(LudoGame game, LudoPlayer currentPlayer, LudoPawn pawn, int roll) {
        String capturedId = null;

        if (pawn.isInHome()) {
            throw new InvalidMoveException("Pawn already in home");
        }

        if (pawn.isInBase()) {
            if (roll != 6) throw new InvalidMoveException("Need 6 to start");

            int startPos = currentPlayer.getColor().getStartPosition();
            if (isFieldOccupiedBySelf(game, startPos, currentPlayer.getColor())) {
                throw new InvalidMoveException("Start position blocked by self");
            }

            capturedId = handleCollision(game, startPos, currentPlayer);

            pawn.setInBase(false);
            pawn.setPosition(startPos);
            pawn.setStepsMoved(0);
        } else {
            int potentialSteps = pawn.getStepsMoved() + roll;

            if (potentialSteps >= BOARD_SIZE) {
                long inHomeCount = currentPlayer.getPawns().stream().filter(LudoPawn::isInHome).count();

                pawn.setInHome(true);
                pawn.setPosition(-2);
                pawn.setStepsMoved(BOARD_SIZE + (3 - (int) inHomeCount));
            } else {
                int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;

                if (isFieldOccupiedBySelf(game, nextPos, currentPlayer.getColor())) {
                    throw new InvalidMoveException("Field occupied by your own pawn");
                }
                if (isOpponentOnSafePos(game, nextPos, currentPlayer.getColor())) {
                    throw new InvalidMoveException("Cannot capture opponent on safe spot");
                }

                capturedId = handleCollision(game, nextPos, currentPlayer);
                pawn.setPosition(nextPos);
                pawn.setStepsMoved(potentialSteps);
            }
        }
        return capturedId;
    }

    private void handlePostMove(LudoGame game, LudoPlayer player, int roll, String capturedId) {
        if (checkWinCondition(player)) {
            handleGameFinish(game, player);
        } else {
            if (roll == 6) {
                game.setDiceRolled(false);
                game.setWaitingForMove(false);
                game.setRollsLeft(1);

                saveAndBroadcast(game, capturedId);

                if (!isBot(player.getUserId())) {
                    scheduleTurnTimeout(game);
                } else {
                    handleBotTurn(game, player.getUserId());
                }
            } else {
                if (capturedId != null) {
                    saveAndBroadcast(game, capturedId);
                }
                passTurnToNextPlayer(game);
            }
        }
    }

    private void passTurnToNextPlayer(LudoGame game) {
        game.setDiceRolled(false);
        game.setWaitingForMove(false);

        PlayerColor nextColor = game.getCurrentPlayerColor().next();
        LudoPlayer nextPlayer = null;

        int attempts = 0;
        while (nextPlayer == null && attempts < 4) {
            nextPlayer = game.getPlayerByColor(nextColor);
            if (nextPlayer == null) {
                nextColor = nextColor.next();
            }
            attempts++;
        }

        if (nextPlayer != null) {
            game.setCurrentPlayerColor(nextColor);
            game.setActivePlayerId(nextPlayer.getUserId());
            updateRollsCountForPlayer(game, nextPlayer);

            saveAndBroadcast(game, null);

            if (isBot(nextPlayer.getUserId())) {
                handleBotTurn(game, nextPlayer.getUserId());
            } else {
                scheduleTurnTimeout(game);
            }
        }
    }

    // --- BOT LOGIC ---

    private boolean isBot(String playerId) {
        return playerId != null && playerId.startsWith("bot-");
    }

    private void handleBotTurn(LudoGame gameParam, String botId) {
        turnTimeoutScheduler.schedule(() -> processBotStep(gameParam.getRoomId(), botId), 1500, TimeUnit.MILLISECONDS);
    }

    private void processBotStep(String roomId, String botId) {
        try {
            LudoGame game = gameRepository.findById(roomId).orElse(null);
            if (game == null || game.getStatus() != RoomStatus.PLAYING) return;

            if (checkAndAbortIfNoHumans(game)) {
                return;
            }

            if (!botId.equals(game.getActivePlayerId())) return;

            LudoPlayer bot = game.getPlayerById(botId);
            if (bot == null) return;

            int roll = random.nextInt(6) + 1;
            game.setLastDiceRoll(roll);
            game.setDiceRolled(true);
            game.setRollsLeft(game.getRollsLeft() - 1);

            log.info("Bot {} rolled {}", botId, roll);

            boolean canMove = (roll == 6) || canMoveAnyPawn(game, bot, roll);

            if (canMove) {
                game.setWaitingForMove(true);
            } else {
                if (game.getRollsLeft() > 0) {
                    game.setDiceRolled(false);
                    game.setWaitingForMove(false);
                    saveAndBroadcast(game, null);

                    turnTimeoutScheduler.schedule(() -> processBotStep(roomId, botId), 1000, TimeUnit.MILLISECONDS);
                    return;
                } else {
                    passTurnToNextPlayer(game);
                    return;
                }
            }

            saveAndBroadcast(game, null);

            if (canMove) {
                turnTimeoutScheduler.schedule(() -> executeBotMove(roomId, botId, roll), 1000, TimeUnit.MILLISECONDS);
            }

        } catch (Exception e) {
            log.error("Bot logic error in room " + roomId, e);
            tryToRecoverTurn(roomId);
        }
    }

    private void executeBotMove(String roomId, String botId, int roll) {
        try {
            LudoGame game = gameRepository.findById(roomId).orElse(null);
            if (game == null) return;

            LudoPlayer bot = game.getPlayerById(botId);

            int pawnToMoveIndex = chooseBestPawnToMove(game, bot, roll);

            if (pawnToMoveIndex != -1) {
                LudoPawn pawn = bot.getPawns().get(pawnToMoveIndex);

                String capturedId = performMoveLogic(game, bot, pawn, roll);

                if (checkWinCondition(bot)) {
                    handleGameFinish(game, bot);
                    return;
                }

                if (roll == 6) {
                    game.setDiceRolled(false);
                    game.setWaitingForMove(false);
                    game.setRollsLeft(1);
                    saveAndBroadcast(game, capturedId);

                    turnTimeoutScheduler.schedule(() -> processBotStep(roomId, botId), 1000, TimeUnit.MILLISECONDS);
                } else {
                    if (capturedId != null) {
                        saveAndBroadcast(game, capturedId);
                    }
                    passTurnToNextPlayer(game);
                }
            } else {
                passTurnToNextPlayer(game);
            }
        } catch (Exception e) {
            log.error("Error executing bot move", e);
            tryToRecoverTurn(roomId);
        }
    }

    private int chooseBestPawnToMove(LudoGame game, LudoPlayer bot, int roll) {
        if (roll == 6) {
            for (LudoPawn pawn : bot.getPawns()) {
                if (pawn.isInBase() && canPawnMoveSimple(game, bot, pawn, roll)) {
                    log.info("Bot {} leaves base", bot.getUserId());
                    return pawn.getId();
                }
            }
        }
        for (LudoPawn pawn : bot.getPawns()) {
            if (canPawnMoveSimple(game, bot, pawn, roll)) {
                return pawn.getId();
            }
        }
        return -1;
    }

    private void tryToRecoverTurn(String roomId) {
        try {
            LudoGame game = gameRepository.findById(roomId).orElse(null);
            if (game != null) passTurnToNextPlayer(game);
        } catch (Exception ex) {
            log.error("Critical error recovering bot turn", ex);
        }
    }

    private boolean canMoveAnyPawn(LudoGame game, LudoPlayer player, int roll) {
        for (LudoPawn pawn : player.getPawns()) {
            if (canPawnMoveSimple(game, player, pawn, roll)) {
                return true;
            }
        }
        return false;
    }

    private boolean canPawnMoveSimple(LudoGame game, LudoPlayer player, LudoPawn pawn, int roll) {
        if (pawn.isInHome()) return false;

        if (pawn.isInBase()) {
            if (roll != 6) return false;
            int startPos = player.getColor().getStartPosition();
            return !isFieldOccupiedBySelf(game, startPos, player.getColor());
        } else {
            int potentialSteps = pawn.getStepsMoved() + roll;
            if (potentialSteps >= BOARD_SIZE) return true;

            int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;
            if (isFieldOccupiedBySelf(game, nextPos, player.getColor())) return false;
            if (isOpponentOnSafePos(game, nextPos, player.getColor())) return false;
            return true;
        }
    }

    // --- TIMEOUT ---

    private void scheduleTurnTimeout(LudoGame game) {
        if (game.getStatus() != RoomStatus.PLAYING) return;
        cancelTurnTimeout(game.getRoomId());

        if (isBot(game.getActivePlayerId())) {
            // Clear turn start time for bots
            game.setTurnStartTime(null);
            return;
        }

        // Set turn start time for accurate client-side timer calculation
        game.setTurnStartTime(System.currentTimeMillis());

        ScheduledFuture<?> future = turnTimeoutScheduler.schedule(() -> {
            handleTurnTimeout(game.getRoomId(), game.getActivePlayerId());
        }, turnTimeoutSeconds, TimeUnit.SECONDS);

        turnTimeouts.put(game.getRoomId(), future);
    }

    private void handleTurnTimeout(String roomId, String timedOutPlayerId) {
        // Remove the triggered timer so a new one can be scheduled without being cancelled
        turnTimeouts.remove(roomId);
        try {
            LudoGame game = gameRepository.findById(roomId).orElse(null);
            if (game == null || game.getStatus() != RoomStatus.PLAYING) return;
            if (!game.getActivePlayerId().equals(timedOutPlayerId)) return;

            log.info("Timeout for player {} in room {}", timedOutPlayerId, roomId);

            LudoPlayer player = game.getPlayerById(timedOutPlayerId);
            if (player != null) {
                removeUserGameMapping(player.getUserId());

                int nextBotNum = game.getBotCounter() + 1;
                game.setBotCounter(nextBotNum);

                String oldId = player.getUserId();
                String botId = "bot-" + nextBotNum;

                player.setUserId(botId);
                player.setBot(true);

                Map<String, String> usernames = game.getPlayersUsernames();
                usernames.remove(oldId);
                usernames.put(botId, "Bot " + nextBotNum);
                game.setPlayersUsernames(usernames);

                // Update avatars - remove old player's avatar and set bot avatar
                Map<String, String> avatars = game.getPlayersAvatars() != null
                        ? new HashMap<>(game.getPlayersAvatars())
                        : new HashMap<>();
                avatars.remove(oldId);
                avatars.put(botId, "bot_avatar.svg");
                game.setPlayersAvatars(avatars);

                game.setActivePlayerId(botId);

                // Notify the timed-out player before checking for humans
                notifyPlayerTimeout(oldId, roomId, botId);

                if (checkAndAbortIfNoHumans(game)) {
                    return;
                }

                game.setDiceRolled(false);
                game.setWaitingForMove(false);
                game.setLastDiceRoll(0);
                updateRollsCountForPlayer(game, player);

                saveAndBroadcast(game, null);

                handleBotTurn(game, botId);
            } else {
                passTurnToNextPlayer(game);
            }

        } catch (Exception e) {
            log.error("Error handling timeout", e);
        }
    }

    /**
     * Sends a timeout notification to a player who was kicked due to inactivity.
     * This message is sent directly to the player's personal topic so they receive it
     * even though they are no longer in the game.
     */
    private void notifyPlayerTimeout(String playerId, String roomId, String replacedByBotId) {
        if (playerId == null || isBot(playerId)) {
            return;
        }

        PlayerTimeoutMessage timeoutMessage = new PlayerTimeoutMessage(
                roomId,
                playerId,
                replacedByBotId,
                "You have been replaced by a bot due to inactivity. You did not make a move within the time limit."
        );

        log.info("Sending timeout notification to player {} in room {}", playerId, roomId);
        messagingTemplate.convertAndSend("/topic/ludo/" + playerId + "/timeout", timeoutMessage);
    }

    // --- HELPER FUNCTIONS ---

    private void updateRollsCountForPlayer(LudoGame game, LudoPlayer player) {
        boolean hasActivePawns = player.getPawns().stream()
                .anyMatch(p -> !p.isInBase() && !p.isInHome());
        game.setRollsLeft(hasActivePawns ? 1 : 3);
    }

    private boolean canPlayerMove(LudoGame game, String playerId, int roll) {
        LudoPlayer p = game.getPlayerById(playerId);
        if (p == null) return false;
        return canMoveAnyPawn(game, p, roll);
    }

    private boolean isFieldOccupiedBySelf(LudoGame game, int pos, PlayerColor myColor) {
        return getPawnOnPosition(game, pos)
                .map(p -> p.getColor() == myColor)
                .orElse(false);
    }

    private boolean isOpponentOnSafePos(LudoGame game, int pos, PlayerColor myColor) {
        return getPawnOnPosition(game, pos)
                .filter(p -> p.getColor() != myColor)
                .map(p -> p.getPosition() == p.getColor().getStartPosition())
                .orElse(false);
    }

    private String handleCollision(LudoGame game, int pos, LudoPlayer movingPlayer) {
        final String[] capturedId = {null};
        getPawnOnPosition(game, pos).ifPresent(enemy -> {
            if (enemy.getColor() != movingPlayer.getColor()) {
                enemy.setInBase(true);
                enemy.setPosition(-1);
                enemy.setStepsMoved(0);
                enemy.setInHome(false);

                capturedId[0] = game.getPlayers().stream()
                        .filter(p -> p.getColor() == enemy.getColor())
                        .findFirst()
                        .map(LudoPlayer::getUserId)
                        .orElse(null);
            }
        });
        return capturedId[0];
    }

    private Optional<LudoPawn> getPawnOnPosition(LudoGame game, int pos) {
        return game.getPlayers().stream()
                .flatMap(p -> p.getPawns().stream())
                .filter(p -> !p.isInBase() && !p.isInHome())
                .filter(p -> p.getPosition() == pos)
                .findFirst();
    }

    private boolean checkWinCondition(LudoPlayer player) {
        return player.getPawns().stream().allMatch(LudoPawn::isInHome);
    }

    private void validateTurn(LudoGame game, String playerId) {
        if (game.getActivePlayerId() == null || !game.getActivePlayerId().equals(playerId)) {
            throw new GameLogicException("Not your turn!");
        }
    }

    private void saveAndBroadcast(LudoGame game, String capturedUserId) {
        gameRepository.save(game);
        LudoGameStateMessage msg = mapToDTO(game, capturedUserId);
        // Send to each human player's personal topic (like Makao does)
        for (LudoPlayer player : game.getPlayers()) {
            if (!player.isBot()) {
                messagingTemplate.convertAndSend("/topic/ludo/" + player.getUserId(), msg);
            }
        }
    }

    private LudoGameStateMessage mapToDTO(LudoGame game, String capturedUserId) {
        // Calculate remaining seconds for timer (null for bots)
        Integer turnRemainingSeconds = null;
        if (game.getTurnStartTime() != null && !isBot(game.getActivePlayerId())) {
            long elapsedMs = System.currentTimeMillis() - game.getTurnStartTime();
            int elapsedSeconds = (int) (elapsedMs / 1000);
            turnRemainingSeconds = Math.max(0, (int) turnTimeoutSeconds - elapsedSeconds);
        }

        return new LudoGameStateMessage(
                game.getRoomId(),
                game.getStatus(),
                game.getCurrentPlayerColor(),
                game.getActivePlayerId(),
                game.getLastDiceRoll(),
                game.isDiceRolled(),
                game.isWaitingForMove(),
                game.getRollsLeft(),
                game.getPlayers(),
                game.getPlayersUsernames(),
                game.getWinnerId(),
                capturedUserId,
                turnRemainingSeconds,
                game.getTurnStartTime(),
                game.getPlayersAvatars()
        );
    }

    private void handleGameFinish(LudoGame game, LudoPlayer winner) {
        cancelTurnTimeout(game.getRoomId());
        game.setStatus(RoomStatus.FINISHED);
        game.setWinnerId(winner.getUserId());

        LudoGameResult result = new LudoGameResult(
                game.getGameId(),
                game.getMaxPlayers(),
                game.getPlayersUsernames(),
                winner.getUserId(),
                calculatePlacement(game, winner)
        );
        gameResultRepository.save(result);

        GameFinishMessage finishMsg = new GameFinishMessage(game.getRoomId(), RoomStatus.FINISHED);
        rabbitTemplate.convertAndSend(exchangeName, finishRoutingKey, finishMsg);

        saveAndBroadcast(game, null);

        removeAllUserMappings(game);

        if (game.getRoomId() != null) {
            gameRepository.deleteById(game.getRoomId());
        }
    }

    private boolean checkAndAbortIfNoHumans(LudoGame game) {
        boolean hasHumans = game.getPlayers().stream()
                .anyMatch(p -> !p.isBot());

        if (!hasHumans) {
            log.info("No humans left in game {}. Aborting to save resources.", game.getRoomId());
            removeAllUserMappings(game);
            gameRepository.deleteById(game.getRoomId());
            cancelTurnTimeout(game.getRoomId());
            return true;
        }
        return false;
    }

    private Map<String, Integer> calculatePlacement(LudoGame game, LudoPlayer winner) {
        Map<String, Integer> placement = new HashMap<>();
        for (String pid : game.getPlayersUsernames().keySet()) {
            placement.put(pid, pid.equals(winner.getUserId()) ? 1 : 2);
        }
        return placement;
    }

    private void cancelTurnTimeout(String roomId) {
        ScheduledFuture<?> future = turnTimeouts.remove(roomId);
        if (future != null) future.cancel(false);
    }

    @PreDestroy
    public void shutdown() {
        if (turnTimeoutScheduler != null && !turnTimeoutScheduler.isShutdown()) {
            for (ScheduledFuture<?> future : turnTimeouts.values()) {
                if (future != null && !future.isDone()) {
                    future.cancel(false);
                }
            }
            turnTimeouts.clear();

            turnTimeoutScheduler.shutdown();
            try {
                if (!turnTimeoutScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    turnTimeoutScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                turnTimeoutScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}