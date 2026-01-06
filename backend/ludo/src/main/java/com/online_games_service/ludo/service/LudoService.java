package com.online_games_service.ludo.service;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.common.messaging.GameFinishMessage;
import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.enums.PlayerColor;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
    
    private final Random random = new Random();
    private final ScheduledExecutorService turnTimeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> turnTimeouts = new ConcurrentHashMap<>();

    @Value("${ludo.amqp.exchange:game.events}")
    private String exchangeName;

    @Value("${ludo.amqp.routing.finish:ludo.finish}")
    private String finishRoutingKey;

    @Value("${ludo.turn-timeout-seconds:60}")
    private long turnTimeoutSeconds;

    private static final int BOARD_SIZE = 40;

    public void createGame(String roomId, List<String> playerIds, String hostUserId, Map<String, String> usernames) {
        if (gameRepository.existsById(roomId)) {
            log.info("Ludo game for room {} already exists", roomId);
            return;
        }

        LudoGame game = new LudoGame(roomId, playerIds, hostUserId, usernames);
        
        updateRollsCountForPlayer(game, game.getPlayers().get(0));
        
        saveAndBroadcast(game);
        scheduleTurnTimeout(game);
        
        log.info("Ludo game created for room: {}, gameId: {}", roomId, game.getGameId());
    }

    public LudoGame getGame(String roomId) {
        return gameRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Game not found for room: " + roomId));
    }

    // --- ROLL DICE ---
    public LudoGame rollDice(String roomId, String playerId) {
        LudoGame game = getGame(roomId);
        validateTurn(game, playerId);

        if (game.isDiceRolled() && game.isWaitingForMove()) {
            throw new IllegalStateException("You must move before rolling again!");
        }
        if (game.getRollsLeft() <= 0) {
             throw new IllegalStateException("No rolls left!");
        }

        int roll = random.nextInt(6) + 1;
        game.setLastDiceRoll(roll);
        game.setDiceRolled(true);
        game.setRollsLeft(game.getRollsLeft() - 1);

        if (roll == 6) {
            game.setWaitingForMove(true);
        } else {
            if (canPlayerMove(game, playerId, roll)) {
                game.setWaitingForMove(true);
            } else {
                if (game.getRollsLeft() > 0) {
                    game.setDiceRolled(false);
                    game.setWaitingForMove(false);
                } else {
                    passTurnToNextPlayer(game);
                }
            }
        }

        saveAndBroadcast(game);
        if (game.getStatus() == RoomStatus.PLAYING) {
            scheduleTurnTimeout(game);
        }
        return game;
    }

    // --- MOVE PAWN ---
    public LudoGame movePawn(String roomId, String playerId, int pawnIndex) {
        LudoGame game = getGame(roomId);
        validateTurn(game, playerId);

        if (!game.isDiceRolled()) {
            throw new IllegalStateException("Roll dice first!");
        }

        LudoPlayer player = game.getPlayerById(playerId);
        if (player == null) throw new IllegalArgumentException("Player not found in game");

        if (pawnIndex < 0 || pawnIndex >= player.getPawns().size()) {
             throw new IllegalArgumentException("Invalid pawn index");
        }
        
        LudoPawn pawn = player.getPawns().get(pawnIndex);
        int roll = game.getLastDiceRoll();

        performMoveLogic(game, player, pawn, roll);

        if (checkWinCondition(player)) {
            handleGameFinish(game, player);
        } else {
            if (roll == 6) {
                game.setDiceRolled(false);
                game.setWaitingForMove(false);
                game.setRollsLeft(1);
                saveAndBroadcast(game);
                scheduleTurnTimeout(game);
            } else {
                passTurnToNextPlayer(game);
            }
        }
        
        return game;
    }

    // --- MOVE LOGIC ---
    private void performMoveLogic(LudoGame game, LudoPlayer currentPlayer, LudoPawn pawn, int roll) {
        if (pawn.isInHome()) {
            throw new IllegalArgumentException("Pawn already in home");
        }

        if (pawn.isInBase()) {
            if (roll != 6) throw new IllegalArgumentException("Need 6 to start");
            int startPos = currentPlayer.getColor().getStartPosition();
            if (isFieldOccupiedBySelf(game, startPos, currentPlayer.getColor())) {
                throw new IllegalArgumentException("Start blocked by self");
            }
            handleCollision(game, startPos, currentPlayer);
            pawn.setInBase(false);
            pawn.setPosition(startPos);
            pawn.setStepsMoved(0);
        } else {
            int potentialSteps = pawn.getStepsMoved() + roll;
            if (potentialSteps >= BOARD_SIZE) {
                long inHomeCount = currentPlayer.getPawns().stream().filter(LudoPawn::isInHome).count();
                if (inHomeCount >= 4) throw new IllegalStateException("Home full");
                pawn.setInHome(true);
                pawn.setPosition((int) inHomeCount);
                pawn.setStepsMoved(BOARD_SIZE + (int) inHomeCount);
            } else {
                int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;
                if (isFieldOccupiedBySelf(game, nextPos, currentPlayer.getColor())) {
                    throw new IllegalArgumentException("Blocked by self");
                }
                if (isOpponentOnSafePos(game, nextPos, currentPlayer.getColor())) {
                    throw new IllegalArgumentException("Cannot capture on safe spot");
                }
                handleCollision(game, nextPos, currentPlayer);
                pawn.setPosition(nextPos);
                pawn.setStepsMoved(potentialSteps);
            }
        }
    }

    // --- HELPER FUNCTIONS ---

    private void validateTurn(LudoGame game, String playerId) {
        if (game.getActivePlayerId() == null || !game.getActivePlayerId().equals(playerId)) {
            throw new IllegalStateException("Not your turn!");
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
            
            saveAndBroadcast(game);
            scheduleTurnTimeout(game);
            
           
        }
    }

    private void updateRollsCountForPlayer(LudoGame game, LudoPlayer player) {
        boolean hasActive = player.getPawns().stream().anyMatch(p -> !p.isInBase() && !p.isInHome());
        game.setRollsLeft(hasActive ? 1 : 3);
    }

    private boolean canPlayerMove(LudoGame game, String playerId, int roll) {
        LudoPlayer p = game.getPlayerById(playerId);
        if (p == null) return false;
        
        if (roll == 6 && hasPawnInBase(p)) return true;
        
        return p.getPawns().stream()
                .filter(pawn -> !pawn.isInBase() && !pawn.isInHome())
                .anyMatch(pawn -> {
                    int potentialSteps = pawn.getStepsMoved() + roll;
                    if (potentialSteps >= BOARD_SIZE) return true;
                    
                    int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;
                    if (isFieldOccupiedBySelf(game, nextPos, p.getColor())) return false;
                    if (isOpponentOnSafePos(game, nextPos, p.getColor())) return false;
                    return true; 
                });
    }

    // --- COLLISION & SAFE SPOTS ---
    
    private boolean hasPawnInBase(LudoPlayer player) {
        return player.getPawns().stream().anyMatch(LudoPawn::isInBase);
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

    private void handleCollision(LudoGame game, int pos, LudoPlayer movingPlayer) {
        getPawnOnPosition(game, pos).ifPresent(enemy -> {
            if (enemy.getColor() != movingPlayer.getColor()) {
                enemy.setInBase(true);
                enemy.setPosition(-1);
                enemy.setStepsMoved(0);
                enemy.setInHome(false);
            }
        });
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

    // --- PERSISTENCE & BROADCAST ---

    private void saveAndBroadcast(LudoGame game) {
        gameRepository.save(game); 
        
        LudoGameStateMessage msg = new LudoGameStateMessage(
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
                game.getWinnerId()
        );
        
        messagingTemplate.convertAndSend("/topic/game/" + game.getRoomId(), msg);
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
                game.getPlacement() 
        );
        gameResultRepository.save(result);
        
        GameFinishMessage finishMsg = new GameFinishMessage(game.getRoomId(), RoomStatus.FINISHED);
        rabbitTemplate.convertAndSend(exchangeName, finishRoutingKey, finishMsg);
        
        saveAndBroadcast(game);
        
        if (game.getRoomId() != null) {
            gameRepository.deleteById(game.getRoomId());
        }
    }

    // --- TIMEOUT HANDLING ---

    private void scheduleTurnTimeout(LudoGame game) {
        if (game.getStatus() != RoomStatus.PLAYING) return;
        cancelTurnTimeout(game.getRoomId());
        
        ScheduledFuture<?> future = turnTimeoutScheduler.schedule(() -> {
            handleTurnTimeout(game.getRoomId(), game.getActivePlayerId());
        }, turnTimeoutSeconds, TimeUnit.SECONDS);
        
        turnTimeouts.put(game.getRoomId(), future);
    }

    private void handleTurnTimeout(String roomId, String timedOutPlayerId) {
        try {
            LudoGame game = gameRepository.findById(roomId).orElse(null);
            if (game == null || game.getStatus() != RoomStatus.PLAYING) return;
            if (!game.getActivePlayerId().equals(timedOutPlayerId)) return;

            log.info("Timeout for player {} in room {}", timedOutPlayerId, roomId);
            
            passTurnToNextPlayer(game);
            
        } catch (Exception e) {
            log.error("Error handling timeout", e);
        }
    }

    private void cancelTurnTimeout(String roomId) {
        ScheduledFuture<?> future = turnTimeouts.remove(roomId);
        if (future != null) future.cancel(false);
    }

    @PreDestroy
    public void shutdown() {
        turnTimeoutScheduler.shutdown();
    }
}