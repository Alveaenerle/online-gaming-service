package com.online_games_service.social.exception;

/**
 * Exception class for game invite related errors.
 */
public class GameInviteException extends RuntimeException {

    public enum ErrorCode {
        SELF_INVITE("Cannot invite yourself to a game"),
        INVITE_ALREADY_PENDING("An invite to this user for this lobby is already pending"),
        LOBBY_NOT_AVAILABLE("The lobby is no longer available or has already started"),
        INVITE_NOT_FOUND("Game invite not found");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorCode errorCode;

    public GameInviteException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GameInviteException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public GameInviteException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
