import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
  useRef,
} from "react";
import { ludoService } from "../services/ludoGameService";
import { ludoSocketService } from "../services/ludoSocketService";
import { LudoGameStateMessage } from "../components/Games/Ludo/types";
import { NotificationType } from "../components/Games/Ludo/GameNotification";
import { useAuth } from "./AuthContext";

/** Message sent by backend when player is kicked due to timeout */
interface PlayerTimeoutMessage {
  roomId: string;
  playerId: string;
  replacedByBotId: string;
  message: string;
  type: "PLAYER_TIMEOUT";
}

interface LudoContextType {
  gameState: LudoGameStateMessage | null;
  isLoading: boolean;
  isRolling: boolean;
  isMyTurn: boolean;
  notification: string;
  notificationType: NotificationType;
  wasKickedByTimeout: boolean;
  timeoutMessage: string | null;
  setGameNotification: (message: string, type: NotificationType) => void;
  rollDice: () => Promise<void>;
  movePawn: (pawnId: number) => Promise<void>;
  refreshGameState: () => Promise<void>;
  clearTimeoutStatus: () => void;
  resetState: () => void;
}

const LudoContext = createContext<LudoContextType | undefined>(undefined);

export const LudoProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth();
  const [gameState, setGameState] = useState<LudoGameStateMessage | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isRolling, setIsRolling] = useState(false);
  const [wasKickedByTimeout, setWasKickedByTimeout] = useState(false);
  const [timeoutMessage, setTimeoutMessage] = useState<string | null>(null);

  const [notification, setNotification] = useState("Welcome to Ludo!");
  const [notificationType, setNotificationType] =
    useState<NotificationType>("INFO");

  const subscriptionRef = useRef<string | null>(null);
  const timeoutSubscriptionRef = useRef<string | null>(null);

  const setGameNotification = useCallback(
    (message: string, type: NotificationType) => {
      setNotification(message);
      setNotificationType(type);
    },
    []
  );

  const clearTimeoutStatus = useCallback(() => {
    setWasKickedByTimeout(false);
    setTimeoutMessage(null);
  }, []);

  const resetState = useCallback(() => {
    setGameState(null);
    clearTimeoutStatus();
  }, [clearTimeoutStatus]);

  const handleGameUpdate = useCallback(
    (data: LudoGameStateMessage) => {
      console.log("!!! SOCKET MESSAGE RECEIVED !!!", data);

      if (data.diceRolled || data.lastDiceRoll > 0) {
        setIsRolling(false);
      }

      if (data.capturedUserId) {
        setGameNotification(
          "Unit captured! Tactical advantage gained.",
          "COMBAT"
        );
      }

      setGameState(data);
    },
    [setGameNotification]
  );

  const refreshGameState = useCallback(async () => {
    if (!user?.id) return;
    try {
      const state = await ludoService.getGameState();
      setGameState(state);
    } catch (err) {
      console.warn("Silent refresh failed");
    }
  }, [user?.id]);

  const rollDice = async () => {
    try {
      setIsRolling(true);
      await ludoService.rollDice();

      setTimeout(() => setIsRolling(false), 4000);
    } catch (err) {
      setIsRolling(false);
      setGameNotification("Dice failure.", "ERROR");
    }
  };

  const movePawn = async (pawnId: number) => {
    try {
      setGameNotification(`Pawn ${pawnId} moving...`, "MOVING");
      await ludoService.movePawn(pawnId);
    } catch (err) {
      console.error("Move pawn error:", err);
      setGameNotification(`Movement intercepted. ${err}`, "ERROR");
    }
  };

  useEffect(() => {
    if (!user?.id) return;

    // Subscribe to personal topic (like Makao does)
    const topic = `/topic/ludo/${user.id}`;
    const timeoutTopic = `/topic/ludo/${user.id}/timeout`;

    const startSocket = async () => {
      try {
        await ludoSocketService.connect();

        // Subscribe to game state updates
        if (subscriptionRef.current !== topic) {
          if (subscriptionRef.current) {
            ludoSocketService.unsubscribe(subscriptionRef.current);
          }
          ludoSocketService.subscribe(topic, handleGameUpdate);
          subscriptionRef.current = topic;
          console.log("[LudoGameContext] Subscribed to game state:", topic);
        }

        // Subscribe to timeout notifications
        if (timeoutSubscriptionRef.current !== timeoutTopic) {
          if (timeoutSubscriptionRef.current) {
            ludoSocketService.unsubscribe(timeoutSubscriptionRef.current);
          }
          ludoSocketService.subscribe(timeoutTopic, (data: PlayerTimeoutMessage) => {
            console.log("[LudoGameContext] Received timeout notification:", data);
            setWasKickedByTimeout(true);
            setTimeoutMessage(data.message);
          });
          timeoutSubscriptionRef.current = timeoutTopic;
          console.log("[LudoGameContext] Subscribed to timeout:", timeoutTopic);
        }

        // Request initial state
        try {
          await ludoService.requestState();
          console.log("[LudoGameContext] Requested initial state");
        } catch (err) {
          console.warn("[LudoGameContext] Initial state request failed, will get via WebSocket");
        }
      } catch (err) {
        setGameNotification("Connection failed. Reconnecting...", "ERROR");
      }
    };

    startSocket();

    // Cleanup: unsubscribe when component unmounts or dependencies change
    return () => {
      if (subscriptionRef.current) {
        console.log("[LudoGameContext] Cleaning up game state subscription");
        ludoSocketService.unsubscribe(subscriptionRef.current);
        subscriptionRef.current = null;
      }
      if (timeoutSubscriptionRef.current) {
        console.log("[LudoGameContext] Cleaning up timeout subscription");
        ludoSocketService.unsubscribe(timeoutSubscriptionRef.current);
        timeoutSubscriptionRef.current = null;
      }
    };
  }, [user?.id, handleGameUpdate, setGameNotification]);

  return (
    <LudoContext.Provider
      value={{
        gameState,
        isLoading,
        isRolling,
        notification,
        notificationType,
        wasKickedByTimeout,
        timeoutMessage,
        setGameNotification,
        isMyTurn: gameState?.currentPlayerId === user?.id,
        rollDice,
        movePawn,
        refreshGameState,
        clearTimeoutStatus,
        resetState,
      }}
    >
      {children}
    </LudoContext.Provider>
  );
};

export const useLudo = () => {
  const c = useContext(LudoContext);
  if (!c) throw new Error("useLudo missing");
  return c;
};
