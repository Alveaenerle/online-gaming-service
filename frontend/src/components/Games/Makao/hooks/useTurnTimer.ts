import { useState, useEffect, useRef, useCallback } from "react";

const TURN_TIMEOUT_SECONDS = 60;

interface UseTurnTimerParams {
  /** The ID of the currently active player */
  activePlayerId: string | null | undefined;
  /** Server-provided remaining seconds (used to sync/reset the timer) */
  serverRemainingSeconds: number | null | undefined;
  /** Whether the active player is a bot (bots don't have timers) */
  isActivePlayerBot: boolean;
  /** Whether the game is currently in PLAYING status */
  isGamePlaying: boolean;
}

interface UseTurnTimerReturn {
  /** Current countdown value in seconds */
  remainingSeconds: number | null;
}

/**
 * Hook for managing a client-side turn countdown timer.
 * 
 * The timer:
 * - Resets when the active player changes
 * - Syncs with server-provided values when available
 * - Counts down every second locally for smooth UI
 * - Stops when a bot is active (bots don't have turn timers)
 * - Stops when game is not in PLAYING status
 */
export const useTurnTimer = ({
  activePlayerId,
  serverRemainingSeconds,
  isActivePlayerBot,
  isGamePlaying,
}: UseTurnTimerParams): UseTurnTimerReturn => {
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastActivePlayerRef = useRef<string | null>(null);
  const lastServerValueRef = useRef<number | null>(null);

  // Cleanup interval on unmount
  useEffect(() => {
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, []);

  // Handle timer logic
  useEffect(() => {
    // Clear existing interval
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }

    // Don't run timer if:
    // - No active player
    // - Active player is a bot
    // - Game is not playing
    if (!activePlayerId || isActivePlayerBot || !isGamePlaying) {
      setRemainingSeconds(null);
      lastActivePlayerRef.current = activePlayerId || null;
      return;
    }

    // Check if active player changed (turn changed)
    const playerChanged = lastActivePlayerRef.current !== activePlayerId;
    lastActivePlayerRef.current = activePlayerId;

    // Determine starting value for the timer
    let startValue: number;

    if (playerChanged) {
      // New turn - reset to full time or use server value if available
      startValue = serverRemainingSeconds != null && serverRemainingSeconds > 0 
        ? serverRemainingSeconds 
        : TURN_TIMEOUT_SECONDS;
    } else if (serverRemainingSeconds != null && serverRemainingSeconds !== lastServerValueRef.current) {
      // Same player but server sent a new value - sync to it
      startValue = serverRemainingSeconds;
    } else if (remainingSeconds != null && remainingSeconds > 0) {
      // Continue with existing countdown
      startValue = remainingSeconds;
    } else {
      // Fallback to full time
      startValue = TURN_TIMEOUT_SECONDS;
    }

    lastServerValueRef.current = serverRemainingSeconds ?? null;
    setRemainingSeconds(startValue);

    // Start countdown interval
    intervalRef.current = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null || prev <= 0) {
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [activePlayerId, isActivePlayerBot, isGamePlaying, serverRemainingSeconds]);

  return { remainingSeconds };
};

export default useTurnTimer;
