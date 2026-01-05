package com.online_games_service.ludo.service;

import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import com.online_games_service.ludo.model.LudoGame;
import com.online_games_service.ludo.model.LudoPawn;
import com.online_games_service.ludo.model.LudoPlayer;
import com.online_games_service.ludo.repository.redis.LudoGameRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class LudoService {

    private final LudoGameRedisRepository ludoRepository;
    private final Random random = new Random();

    private static final int BOARD_SIZE = 40; 

    public LudoGame createGameWithId(String gameId, List<String> playerIds) {
        LudoGame game = new LudoGame(gameId, playerIds);
        
        if (!game.getPlayers().isEmpty()) {
            updateRollsCountForPlayer(game, game.getPlayers().get(0));
        }
        
        return ludoRepository.save(game);
    }

    public LudoGame getGame(String gameId) {
        return ludoRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + gameId));
    }

    public LudoGame rollDice(String gameId, String playerId) {
        LudoGame game = getGame(gameId);
        validateTurn(game, playerId);
        
        if (game.isDiceRolled() && game.isWaitingForMove()) {
            throw new IllegalStateException("You must move before rolling again!");
        }

        if (game.getRollsLeft() <= 0) {
             throw new IllegalStateException("No rolls left in this turn!");
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

        return ludoRepository.save(game);
    }

    public LudoGame movePawn(String gameId, String playerId, int pawnIndex) {
        LudoGame game = getGame(gameId);
        validateTurn(game, playerId);
        
        if (!game.isDiceRolled()) {
            throw new IllegalStateException("You must roll the dice first!");
        }
        
        int roll = game.getLastDiceRoll();
        LudoPlayer currentPlayer = getPlayerById(game, playerId);
        
        if (pawnIndex < 0 || pawnIndex >= currentPlayer.getPawns().size()) {
            throw new IllegalArgumentException("Invalid pawn index");
        }
        
        LudoPawn pawn = currentPlayer.getPawns().get(pawnIndex);

        if (pawn.isInHome()) {
            throw new IllegalArgumentException("This pawn is already in home!");
        }

        // MOVE LOGIC 

        if (pawn.isInBase()) {
            if (roll != 6) {
                throw new IllegalArgumentException("You must roll a 6 to leave the base!");
            }

            int startPos = currentPlayer.getColor().getStartPosition();

            if (isFieldOccupiedBySelf(game, startPos, currentPlayer.getColor())) {
                throw new IllegalArgumentException("Start position blocked by self!");
            }
            
            handleCollision(game, startPos, currentPlayer);

            pawn.setInBase(false);
            pawn.setPosition(startPos);
            pawn.setStepsMoved(0); 

        } else {
            int potentialSteps = pawn.getStepsMoved() + roll;
            
            if (potentialSteps >= BOARD_SIZE) {
                long pawnsAlreadyInHome = currentPlayer.getPawns().stream()
                        .filter(LudoPawn::isInHome)
                        .count();

                if (pawnsAlreadyInHome >= 4) {
                     throw new IllegalStateException("Home is full!");
                }
                
                pawn.setInHome(true);
                pawn.setPosition((int) pawnsAlreadyInHome); 
                pawn.setStepsMoved(BOARD_SIZE + (int) pawnsAlreadyInHome); 

            } else {
                int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;

                if (isFieldOccupiedBySelf(game, nextPos, currentPlayer.getColor())) {
                    throw new IllegalArgumentException("Field occupied by your own pawn!");
                }

                if (isOpponentOnTheirSafePosition(game, nextPos, currentPlayer.getColor())) {
                    throw new IllegalArgumentException("Cannot capture opponent on their safe start field!");
                }

                handleCollision(game, nextPos, currentPlayer);
                
                pawn.setPosition(nextPos);
                pawn.setStepsMoved(potentialSteps);
            }
        }

        // AFTER MOVE

        if (checkWinCondition(currentPlayer)) {
            game.setStatus(RoomStatus.FINISHED);
            game.setWinnerId(currentPlayer.getUserId());
        } else {
            if (roll == 6) {
                game.setDiceRolled(false);
                game.setWaitingForMove(false);
                game.setRollsLeft(1); 
            } else {
                passTurnToNextPlayer(game);
            }
        }

        return ludoRepository.save(game);
    }

    // HELPER METHODS

    private void validateTurn(LudoGame game, String playerId) {
        LudoPlayer current = getPlayerByColor(game, game.getCurrentPlayerColor());
        if (!current.getUserId().equals(playerId)) {
            throw new IllegalStateException("It is not your turn!");
        }
    }

    private void passTurnToNextPlayer(LudoGame game) {
        game.setDiceRolled(false);
        game.setWaitingForMove(false);
        
        PlayerColor nextColor = game.getCurrentPlayerColor().next();
        game.setCurrentPlayerColor(nextColor);

        LudoPlayer nextPlayer = getPlayerByColor(game, nextColor);
        updateRollsCountForPlayer(game, nextPlayer);
    }

    private void updateRollsCountForPlayer(LudoGame game, LudoPlayer player) {
        boolean hasActivePawns = player.getPawns().stream()
                .anyMatch(p -> !p.isInBase() && !p.isInHome()); 

        if (!hasActivePawns) {
            game.setRollsLeft(3);
        } else {
            game.setRollsLeft(1);
        }
    }

    public boolean canPlayerMove(LudoGame game, String playerId, int roll) {
        LudoPlayer p = getPlayerById(game, playerId);
        
        if (roll == 6 && hasPawnInBase(p)) return true;
        
        return p.getPawns().stream()
                .filter(pawn -> !pawn.isInBase() && !pawn.isInHome())
                .anyMatch(pawn -> {
                    int potentialSteps = pawn.getStepsMoved() + roll;
                    
                    if (potentialSteps >= BOARD_SIZE) return true;
                    
                    int nextPos = (pawn.getPosition() + roll) % BOARD_SIZE;
                    
                    if (isFieldOccupiedBySelf(game, nextPos, p.getColor())) return false;
                    
                    if (isOpponentOnTheirSafePosition(game, nextPos, p.getColor())) return false;

                    return true; 
                });
    }

    public boolean hasPawnInBase(LudoPlayer player) {
        return player.getPawns().stream().anyMatch(LudoPawn::isInBase);
    }

    private boolean isFieldOccupiedBySelf(LudoGame game, int pos, PlayerColor myColor) {
        return getPawnOnPosition(game, pos)
                .map(p -> p.getColor() == myColor)
                .orElse(false);
    }
    
    private boolean isOpponentOnTheirSafePosition(LudoGame game, int pos, PlayerColor myColor) {
        return getPawnOnPosition(game, pos)
                .filter(p -> p.getColor() != myColor) 
                .map(p -> p.getPosition() == p.getColor().getStartPosition())
                .orElse(false);
    }
    
    private void handleCollision(LudoGame game, int pos, LudoPlayer movingPlayer) {
        Optional<LudoPawn> targetPawn = getPawnOnPosition(game, pos);
        
        if (targetPawn.isPresent()) {
            LudoPawn enemyPawn = targetPawn.get();
            if (enemyPawn.getColor() != movingPlayer.getColor()) {
                enemyPawn.setInBase(true);
                enemyPawn.setPosition(-1);
                enemyPawn.setStepsMoved(0); 
                enemyPawn.setInHome(false);
            }
        }
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
    
    private LudoPlayer getPlayerById(LudoGame game, String playerId) {
        return game.getPlayers().stream()
                .filter(p -> p.getUserId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player not found in game"));
    }
    
    private LudoPlayer getPlayerByColor(LudoGame game, PlayerColor color) {
        return game.getPlayers().stream()
                .filter(p -> p.getColor() == color)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Color not found in game"));
    }
}