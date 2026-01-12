package com.online_games_service.social.exception;

public class FriendRequestException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public enum ErrorCode {
        SELF_REFERENTIAL_REQUEST("Cannot send friend request to yourself"),
        USER_NOT_FOUND("Target user does not exist"),
        REQUEST_NOT_FOUND("Friend request not found"),
        REQUEST_NOT_OWNED("Friend request does not belong to current user"),
        ALREADY_FRIENDS("Users are already friends"),
        REQUEST_ALREADY_PENDING("Friend request already pending"),
        REQUEST_ALREADY_ACCEPTED("Friend request has already been accepted"),
        DATABASE_ERROR("Database operation failed");
        
        private final String defaultMessage;
        
        ErrorCode(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
    
    public FriendRequestException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
    
    public FriendRequestException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public FriendRequestException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
