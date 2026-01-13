import { useCallback } from "react";

// Sound file paths (placeholders as requested)
const SOUNDS = {
  PLAY_CARD: "/sounds/play-card.mp3",
  DRAW_CARD: "/sounds/draw-card.mp3",
  TURN_START: "/sounds/turn-start.mp3",
  MAKAO: "/sounds/makao.mp3",
  GAME_OVER: "/sounds/game-over.mp3",
  SHUFFLE: "/sounds/shuffle.mp3",
};

export const useGameSounds = () => {
  const playSound = useCallback((soundPath: string, volume = 0.5) => {
    try {
      const audio = new Audio(soundPath);
      audio.volume = volume;
      audio.play().catch((e) => {
        // Ignore auto-play errors or missing files
        console.warn("Audio play failed:", e);
      });
    } catch (e) {
      console.warn("Audio initialization failed:", e);
    }
  }, []);

  return {
    playCardSound: () => playSound(SOUNDS.PLAY_CARD),
    drawCardSound: () => playSound(SOUNDS.DRAW_CARD),
    playTurnStartSound: () => playSound(SOUNDS.TURN_START),
    playMakaoSound: () => playSound(SOUNDS.MAKAO),
    playGameOverSound: () => playSound(SOUNDS.GAME_OVER),
    playShuffleSound: () => playSound(SOUNDS.SHUFFLE),
  };
};
