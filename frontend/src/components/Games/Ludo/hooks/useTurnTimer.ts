import { useState, useEffect, useRef } from "react";

const TURN_TIMEOUT_SECONDS = 60;

interface UseTurnTimerParams {
  /** The ID of the currently active player */
  activePlayerId: string | null | undefined;
  /** Server-provided remaining seconds (used to sync/reset the timer) */
  serverRemainingSeconds?: number | null;
  /** Server-provided turn start timestamp in milliseconds (for accurate calculation) */
  turnStartTime?: number | null;
  /** Whether the active player is a bot (bots don't have timers) */
  isActivePlayerBot?: boolean;
  /** Whether the game is currently in PLAYING status */
  isGamePlaying: boolean;
}

interface UseTurnTimerReturn {
  /** Current countdown value in seconds */
  remainingSeconds: number | null;
}

/**
 * Calculates remaining seconds from turnStartTime.
 * Returns null if turnStartTime is not available.
 */
const calculateRemainingFromStartTime = (turnStartTime: number | null | undefined): number | null => {
  if (turnStartTime == null) {
    return null;
  }
  const elapsedMs = Date.now() - turnStartTime;
  const elapsedSeconds = Math.floor(elapsedMs / 1000);
  const remaining = Math.max(0, TURN_TIMEOUT_SECONDS - elapsedSeconds);
  return remaining;
};

/**
 * Hook for managing a client-side turn countdown timer for Ludo.
 *
 * The timer:
 * - Calculates remaining time from turnStartTime for accuracy (primary)
 * - Falls back to serverRemainingSeconds if turnStartTime unavailable
 * - Resets when the active player changes
 * - Counts down every second locally for smooth UI
 * - Stops when a bot is active (bots don't have turn timers)
 * - Stops when game is not in PLAYING status
 */
export const useTurnTimer = ({
  activePlayerId,
  serverRemainingSeconds,
  turnStartTime,
  isActivePlayerBot = false,
  isGamePlaying,
}: UseTurnTimerParams): UseTurnTimerReturn => {
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const lastActivePlayerRef = useRef<string | null>(null);
  const lastTurnStartTimeRef = useRef<number | null>(null);

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
      lastTurnStartTimeRef.current = null;
      return;
    }

    // Check if active player changed (turn changed)
    const playerChanged = lastActivePlayerRef.current !== activePlayerId;
    const turnStartTimeChanged = lastTurnStartTimeRef.current !== turnStartTime;

    lastActivePlayerRef.current = activePlayerId;
    lastTurnStartTimeRef.current = turnStartTime ?? null;

    // Determine starting value for the timer
    let startValue: number;

    // Primary: Calculate from turnStartTime (most accurate)
    const calculatedFromStartTime = calculateRemainingFromStartTime(turnStartTime);

    if (calculatedFromStartTime != null) {
      // Use calculated value from turnStartTime - this is the most accurate
      startValue = calculatedFromStartTime;
    } else if (serverRemainingSeconds != null && serverRemainingSeconds > 0) {
      // Fallback to server-provided remaining seconds
      startValue = serverRemainingSeconds;
    } else if (playerChanged || turnStartTimeChanged) {
      // Turn changed but no server data - start fresh
      startValue = TURN_TIMEOUT_SECONDS;
    } else if (remainingSeconds != null && remainingSeconds > 0) {
      // Continue with existing countdown (same turn, no new data)
      startValue = remainingSeconds;
    } else {
      // Fallback to full time
      startValue = TURN_TIMEOUT_SECONDS;
    }

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
  }, [activePlayerId, isActivePlayerBot, isGamePlaying, serverRemainingSeconds, turnStartTime]);

  return { remainingSeconds };
};

export default useTurnTimer;
