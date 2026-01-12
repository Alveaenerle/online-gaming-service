package com.online_games_service.social.controller;

import com.online_games_service.social.dto.GameInviteDto;
import com.online_games_service.social.dto.RespondGameInviteRequest;
import com.online_games_service.social.dto.SendGameInviteRequest;
import com.online_games_service.social.exception.GameInviteException;
import com.online_games_service.social.service.GameInviteService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for game invite operations.
 * All endpoints require authentication via SessionUserFilter.
 */
@RestController
@RequestMapping("/invites")
public class GameInviteController {

    private static final Logger logger = LoggerFactory.getLogger(GameInviteController.class);

    private final GameInviteService gameInviteService;

    public GameInviteController(GameInviteService gameInviteService) {
        this.gameInviteService = gameInviteService;
    }

    /**
     * Send a game invite to another user.
     *
     * POST /invites/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendGameInvite(
            @Valid @RequestBody SendGameInviteRequest request,
            @RequestAttribute(value = "userId", required = false) String userId,
            @RequestAttribute(value = "username", required = false) String username) {

        if (userId == null) {
            logger.warn("Attempt to send game invite without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String senderName = username != null ? username : userId;

        logger.info("User {} sending game invite to {} for lobby {}",
                userId, request.getTargetUserId(), request.getLobbyId());

        GameInviteDto result = gameInviteService.sendGameInvite(
                userId,
                senderName,
                request.getTargetUserId(),
                request.getLobbyId(),
                request.getLobbyName(),
                request.getGameType()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get all pending game invites for the current user.
     * This filters out invites for lobbies that have started or ended.
     *
     * GET /invites/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<GameInviteDto>> getPendingInvites(
            @RequestAttribute(value = "userId", required = false) String userId) {

        if (userId == null) {
            logger.warn("Attempt to get pending invites without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} fetching pending game invites", userId);
        List<GameInviteDto> invites = gameInviteService.getPendingInvites(userId);

        return ResponseEntity.ok(invites);
    }

    /**
     * Accept a game invite.
     *
     * POST /invites/accept
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvite(
            @Valid @RequestBody RespondGameInviteRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {

        if (userId == null) {
            logger.warn("Attempt to accept invite without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} accepting game invite {}", userId, request.getInviteId());
        GameInviteDto result = gameInviteService.acceptInvite(userId, request.getInviteId());

        return ResponseEntity.ok(result);
    }

    /**
     * Decline a game invite.
     *
     * POST /invites/decline
     */
    @PostMapping("/decline")
    public ResponseEntity<?> declineInvite(
            @Valid @RequestBody RespondGameInviteRequest request,
            @RequestAttribute(value = "userId", required = false) String userId) {

        if (userId == null) {
            logger.warn("Attempt to decline invite without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} declining game invite {}", userId, request.getInviteId());
        GameInviteDto result = gameInviteService.declineInvite(userId, request.getInviteId());

        return ResponseEntity.ok(result);
    }

    /**
     * Get all invites sent by the current user.
     *
     * GET /invites/sent
     */
    @GetMapping("/sent")
    public ResponseEntity<List<GameInviteDto>> getSentInvites(
            @RequestAttribute(value = "userId", required = false) String userId) {

        if (userId == null) {
            logger.warn("Attempt to get sent invites without authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("User {} fetching sent game invites", userId);
        List<GameInviteDto> invites = gameInviteService.getSentInvites(userId);

        return ResponseEntity.ok(invites);
    }

    /**
     * Exception handler for GameInviteException.
     */
    @ExceptionHandler(GameInviteException.class)
    public ResponseEntity<Map<String, String>> handleGameInviteException(GameInviteException ex) {
        logger.warn("Game invite error: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = switch (ex.getErrorCode()) {
            case SELF_INVITE, INVITE_ALREADY_PENDING -> HttpStatus.BAD_REQUEST;
            case INVITE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case LOBBY_NOT_AVAILABLE -> HttpStatus.CONFLICT;
        };

        return ResponseEntity.status(status).body(Map.of(
                "error", ex.getErrorCode().name(),
                "message", ex.getMessage()
        ));
    }
}
