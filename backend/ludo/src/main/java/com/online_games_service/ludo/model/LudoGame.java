package com.online_games_service.ludo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.ludo.enums.PlayerColor;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.*;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LudoGame implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String roomId;
    private String gameId;

    private RoomStatus status;
    private String hostUserId;
    private int maxPlayers;

    private List<LudoPlayer> players = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, String> playersUsernames = new HashMap<>();

    private PlayerColor currentPlayerColor;
    private String activePlayerId;

    private int lastDiceRoll;
    private boolean diceRolled;
    private boolean waitingForMove;
    private int rollsLeft;

    private String winnerId;
    private Map<String, Integer> placement = new HashMap<>();

    private int botCounter = 0;

    // Turn timer - start timestamp in milliseconds (for accurate client-side timer calculation)
    private Long turnStartTime;

    // Turn timeout in seconds (default 60)
    private static final long TURN_TIMEOUT_SECONDS = 60;

    // Player avatars - playerId -> avatarId (e.g., "avatar_1.png" or "bot_avatar.svg")
    private Map<String, String> playersAvatars = new HashMap<>();

    /**
     * Checks if the current turn has expired (exceeded 60 seconds).
     * @return true if turn has expired, false otherwise (including when turnStartTime is null)
     */
    public boolean isTurnExpired() {
        if (turnStartTime == null) {
            return false;
        }
        long elapsedMs = System.currentTimeMillis() - turnStartTime;
        return elapsedMs >= (TURN_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Gets the remaining seconds for the current turn.
     * @return remaining seconds, or null if turnStartTime is not set
     */
    public Integer getTurnRemainingSeconds() {
        if (turnStartTime == null) {
            return null;
        }
        long elapsedMs = System.currentTimeMillis() - turnStartTime;
        int elapsedSeconds = (int) (elapsedMs / 1000);
        return Math.max(0, (int) TURN_TIMEOUT_SECONDS - elapsedSeconds);
    }

    /**
     * Legacy constructor (uses playerIds.size() as maxPlayers).
     * @deprecated Use {@link #LudoGame(String, List, String, Map, int)} to support bot filling.
     */
    @Deprecated
    public LudoGame(String roomId, List<String> playerIds, String hostUserId, Map<String, String> usernames) {
        this(roomId, playerIds, hostUserId, usernames, playerIds != null ? playerIds.size() : 0);
    }

    /**
     * Creates a new Ludo game with bot filling support.
     * If the number of human players is less than maxPlayers,
     * the remaining slots are filled with bot players.
     *
     * @param roomId the room identifier
     * @param playerIds list of human player IDs
     * @param hostUserId the host user ID
     * @param usernames map of player IDs to usernames
     * @param maxPlayers maximum number of players (used for bot filling)
     */
    public LudoGame(String roomId, List<String> playerIds, String hostUserId, Map<String, String> usernames, int maxPlayers) {
        this.roomId = roomId;
        this.gameId = "LUDO-" + UUID.randomUUID().toString();

        this.status = RoomStatus.PLAYING;
        this.hostUserId = hostUserId;
        this.playersUsernames = usernames != null ? new HashMap<>(usernames) : new HashMap<>();
        this.maxPlayers = maxPlayers;
        this.rollsLeft = 1;

        PlayerColor[] availableColors = PlayerColor.values();
        int colorIndex = 0;

        // Add human players first
        for (int i = 0; i < playerIds.size() && colorIndex < availableColors.length; i++) {
            this.players.add(new LudoPlayer(playerIds.get(i), availableColors[colorIndex]));
            colorIndex++;
        }

        // Fill remaining slots with bots if fewer human players than maxPlayers
        int humanCount = playerIds.size();
        int missing = Math.max(0, maxPlayers - humanCount);
        while (missing > 0 && colorIndex < availableColors.length) {
            botCounter++;
            String botId = "bot-" + botCounter;
            LudoPlayer botPlayer = new LudoPlayer(botId, availableColors[colorIndex]);
            botPlayer.setBot(true);
            this.players.add(botPlayer);
            this.playersUsernames.put(botId, "Bot " + botCounter);
            this.playersAvatars.put(botId, "bot_avatar.svg");
            colorIndex++;
            missing--;
        }

        if (!players.isEmpty()) {
            this.currentPlayerColor = players.get(0).getColor();
            this.activePlayerId = players.get(0).getUserId();
        }
    }

    public Map<String, String> getPlayersUsernames() {
        return new HashMap<>(this.playersUsernames);
    }

    public void setPlayersUsernames(Map<String, String> playersUsernames) {
        this.playersUsernames = playersUsernames != null ? new HashMap<>(playersUsernames) : new HashMap<>();
    }

    public LudoPlayer getPlayerById(String userId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    public LudoPlayer getPlayerByColor(PlayerColor color) {
        return players.stream()
                .filter(p -> p.getColor() == color)
                .findFirst()
                .orElse(null);
    }
}