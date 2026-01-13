import { useState, useCallback } from "react";
import makaoGameService, {
  PlayCardPayload,
  PlayDrawnCardPayload,
} from "../../../../services/makaoGameService";
import { Card, CardSuit, CardRank, DrawCardResponse } from "../types";

interface ActionState {
  isLoading: boolean;
  error: string | null;
}

interface UseMakaoActionsReturn {
  // Actions
  playCard: (
    card: Card,
    requestSuit?: CardSuit | null,
    requestRank?: CardRank | null
  ) => Promise<boolean>;
  drawCard: () => Promise<DrawCardResponse | null>;
  playDrawnCard: (
    requestSuit?: CardSuit | null,
    requestRank?: CardRank | null
  ) => Promise<boolean>;
  skipDrawnCard: () => Promise<boolean>;
  acceptEffect: () => Promise<boolean>;

  // State
  isLoading: boolean;
  error: string | null;
  clearError: () => void;
}

/**
 * Hook for managing Makao game actions (API calls)
 */
export const useMakaoActions = (): UseMakaoActionsReturn => {
  const [actionState, setActionState] = useState<ActionState>({
    isLoading: false,
    error: null,
  });

  /**
   * Execute an action with loading state and error handling
   */
  const executeAction = useCallback(
    async <T>(
      action: () => Promise<T>,
      errorMessage: string
    ): Promise<T | null> => {
      setActionState({ isLoading: true, error: null });

      try {
        const result = await action();
        setActionState({ isLoading: false, error: null });
        return result;
      } catch (err: unknown) {
        const message =
          err instanceof Error ? err.message : errorMessage;
        console.error(`[Makao Action] ${errorMessage}:`, err);
        setActionState({ isLoading: false, error: message });
        return null;
      }
    },
    []
  );

  /**
   * Play a card from hand
   */
  const playCard = useCallback(
    async (
      card: Card,
      requestSuit?: CardSuit | null,
      requestRank?: CardRank | null
    ): Promise<boolean> => {
      const payload: PlayCardPayload = {
        cardSuit: card.suit,
        cardRank: card.rank,
        requestSuit: requestSuit ?? null,
        requestRank: requestRank ?? null,
      };

      const result = await executeAction(
        () => makaoGameService.playCard(payload),
        "Failed to play card"
      );

      return result !== null;
    },
    [executeAction]
  );

  /**
   * Draw a card from deck
   */
  const drawCard = useCallback(async (): Promise<DrawCardResponse | null> => {
    return executeAction(
      () => makaoGameService.drawCard(),
      "Failed to draw card"
    );
  }, [executeAction]);

  /**
   * Play the drawn card
   */
  const playDrawnCard = useCallback(
    async (
      requestSuit?: CardSuit | null,
      requestRank?: CardRank | null
    ): Promise<boolean> => {
      const payload: PlayDrawnCardPayload = {
        requestSuit: requestSuit ?? null,
        requestRank: requestRank ?? null,
      };

      const result = await executeAction(
        () => makaoGameService.playDrawnCard(payload),
        "Failed to play drawn card"
      );

      return result !== null;
    },
    [executeAction]
  );

  /**
   * Skip turn after drawing (keep card)
   */
  const skipDrawnCard = useCallback(async (): Promise<boolean> => {
    const result = await executeAction(
      () => makaoGameService.skipDrawnCard(),
      "Failed to skip turn"
    );

    return result !== null;
  }, [executeAction]);

  /**
   * Accept special effect (draw penalty cards or skip turns)
   */
  const acceptEffect = useCallback(async (): Promise<boolean> => {
    const result = await executeAction(
      () => makaoGameService.acceptEffect(),
      "Failed to accept effect"
    );

    return result !== null;
  }, [executeAction]);

  /**
   * Clear current error
   */
  const clearError = useCallback(() => {
    setActionState((prev) => ({ ...prev, error: null }));
  }, []);

  return {
    playCard,
    drawCard,
    playDrawnCard,
    skipDrawnCard,
    acceptEffect,
    isLoading: actionState.isLoading,
    error: actionState.error,
    clearError,
  };
};

export default useMakaoActions;
