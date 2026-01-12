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

interface LudoContextType {
  gameState: LudoGameStateMessage | null;
  isLoading: boolean;
  isRolling: boolean;
  isMyTurn: boolean;
  notification: string;
  notificationType: NotificationType;
  setGameNotification: (message: string, type: NotificationType) => void;
  rollDice: () => Promise<void>;
  movePawn: (pawnId: number) => Promise<void>;
  refreshGameState: () => Promise<void>;
}

const LudoContext = createContext<LudoContextType | undefined>(undefined);

export const LudoProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth();
  const [gameState, setGameState] = useState<LudoGameStateMessage | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isRolling, setIsRolling] = useState(false);

  const [notification, setNotification] = useState("Welcome to Ludo!");
  const [notificationType, setNotificationType] =
    useState<NotificationType>("INFO");

  const subscriptionRef = useRef<string | null>(null);

  const setGameNotification = useCallback(
    (message: string, type: NotificationType) => {
      setNotification(message);
      setNotificationType(type);
    },
    []
  );

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
      setGameNotification(
        "Movement intercepted. Invalid coordinates.",
        "ERROR"
      );
    }
  };

  useEffect(() => {
    const gameId = gameState?.gameId;
    if (!gameId || !user?.id) return;

    const topic = `/topic/game/${gameId}`;
    if (subscriptionRef.current === topic) return;

    const startSocket = async () => {
      try {
        await ludoSocketService.connect();
        if (subscriptionRef.current) {
          ludoSocketService.unsubscribe(subscriptionRef.current);
        }
        ludoSocketService.subscribe(topic, handleGameUpdate);
        subscriptionRef.current = topic;
      } catch (err) {
        setGameNotification("Reconnecting...", "ERROR");
      }
    };

    startSocket();
  }, [gameState?.gameId, user?.id, handleGameUpdate, setGameNotification]);

  useEffect(() => {
    refreshGameState();
  }, [refreshGameState]);

  return (
    <LudoContext.Provider
      value={{
        gameState,
        isLoading,
        isRolling,
        notification,
        notificationType,
        setGameNotification,
        isMyTurn: gameState?.currentPlayerId === user?.id,
        rollDice,
        movePawn,
        refreshGameState,
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
