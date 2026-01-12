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
import { useAuth } from "./AuthContext";

interface LudoContextType {
  gameState: LudoGameStateMessage | null;
  isLoading: boolean;
  isRolling: boolean;
  isMyTurn: boolean;
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

  const subscriptionRef = useRef<string | null>(null);

  // LOGOWANIE KAŻDEJ ZMIANY STANU - Sprawdź to w konsoli!
  useEffect(() => {
    console.log("DEBUG: Current GameState in Context:", gameState);
  }, [gameState]);

  const handleGameUpdate = useCallback((data: LudoGameStateMessage) => {
    console.log("!!! SOCKET MESSAGE RECEIVED !!!", data); // Jeśli tego nie widzisz, socket nie działa

    // Kluczowe: zatrzymujemy kostkę jeśli serwer mówi, że rzut się odbył
    if (data.diceRolled || data.lastDiceRoll > 0) {
      setIsRolling(false);
    }

    setGameState(data);
  }, []);

  const refreshGameState = useCallback(async () => {
    if (!user?.id) return;
    try {
      const state = await ludoService.getGameState();
      setGameState(state);
    } catch (err) {
      console.warn("Silent refresh failed - probably not in game yet");
    }
  }, [user?.id]);

  const rollDice = async () => {
    try {
      setIsRolling(true);
      console.log("Sending Roll Request...");
      await ludoService.rollDice();

      // Safety stop: jeśli po 4s nic nie przyjdzie, zatrzymaj kostkę
      setTimeout(() => setIsRolling(false), 4000);
    } catch (err) {
      setIsRolling(false);
      console.error("Roll API Error:", err);
    }
  };

  const movePawn = async (pawnId: number) => {
    try {
      await ludoService.movePawn(pawnId);
    } catch (err) {
      console.error("Move API Error:", err);
    }
  };

  // ZARZĄDZANIE SOCKETEM
  useEffect(() => {
    const gameId = gameState?.gameId;
    if (!gameId || !user?.id) return;

    const topic = `/topic/game/${gameId}`;

    // Blokada wielokrotnej subskrypcji
    if (subscriptionRef.current === topic) return;

    const startSocket = async () => {
      try {
        console.log("Attempting socket connection...");
        await ludoSocketService.connect();

        if (subscriptionRef.current) {
          ludoSocketService.unsubscribe(subscriptionRef.current);
        }

        console.log(`SUBSCRIBING TO: ${topic}`);
        ludoSocketService.subscribe(topic, handleGameUpdate);
        subscriptionRef.current = topic;
      } catch (err) {
        console.error("Socket flow failed:", err);
      }
    };

    startSocket();

    return () => {
      // Nie czyścimy tu subskrypcji przy każdym renderze!
      // Tylko jeśli gameId się faktycznie zmieni.
    };
  }, [gameState?.gameId, user?.id, handleGameUpdate]);

  useEffect(() => {
    refreshGameState();
  }, [refreshGameState]);

  return (
    <LudoContext.Provider
      value={{
        gameState,
        isLoading,
        isRolling,
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
