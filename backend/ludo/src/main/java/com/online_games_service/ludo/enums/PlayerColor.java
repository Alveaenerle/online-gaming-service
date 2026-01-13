package com.online_games_service.ludo.enums;

public enum PlayerColor {
    RED(0),
    BLUE(11),
    YELLOW(22),
    GREEN(33);

    private final int startPosition;

    PlayerColor(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public PlayerColor next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}