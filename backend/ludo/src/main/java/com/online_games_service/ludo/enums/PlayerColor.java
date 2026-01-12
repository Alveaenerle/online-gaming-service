package com.online_games_service.ludo.enums;

public enum PlayerColor {
    RED(0),
    BLUE(10),
    GREEN(20),
    YELLOW(30);

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