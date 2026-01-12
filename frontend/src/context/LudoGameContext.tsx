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
import { socketService } from "../services/socketService";
import { LudoGameStateMessage } from "../components/Games/Ludo/types";
import { useAuth } from "./AuthContext";

interface LudoContextType {
  gameState: LudoGameStateMessage | null;
  isLoading: boolean;
  isMyTurn: boolean;
  refreshGameState: () => Promise<void>;
  rollDice: () => Promise<void>;
  movePawn: (pawnId: number) => Promise<void>;
}

const LudoContext = createContext<LudoContextType | undefined>(undefined);

export const LudoProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth();
  const [gameState, setGameState] = useState<LudoGameStateMessage | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const subscriptionRef = useRef<string | null>(null);

  // Sprawdzenie czy jest tura lokalnego gracza
  const isMyTurn = gameState?.currentPlayerId === user?.id;

  // Pobieranie stanu gry z API
  const refreshGameState = useCallback(async () => {
    setIsLoading(true);
    try {
      const state = await ludoService.getGameState();
      setGameState(state);
    } catch (err) {
      console.error("LudoContext: Failed to fetch game state", err);
      setGameState(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Akcja rzutu kostką
  const rollDice = async () => {
    try {
      await ludoService.rollDice();
      // Nie musimy tu odświeżać stanu, bo przyjdzie przez WebSocket
    } catch (err) {
      console.error("LudoContext: Roll error", err);
    }
  };

  // Akcja ruchu pionkiem
  const movePawn = async (pawnId: number) => {
    try {
      await ludoService.movePawn(pawnId);
    } catch (err) {
      console.error("LudoContext: Move error", err);
    }
  };

  // Obsługa aktualizacji z WebSocketu
  const handleGameUpdate = useCallback((data: LudoGameStateMessage) => {
    if (!data) return;
    console.log("LudoContext: Received real-time update", data);
    setGameState(data);
  }, []);

  // Zarządzanie subskrypcją WebSocket
  useEffect(() => {
    // Subskrybujemy tylko jeśli mamy gameId (czyli gra się toczy)
    if (!gameState?.gameId || !user?.id) {
      if (subscriptionRef.current) {
        socketService.unsubscribe(subscriptionRef.current);
        subscriptionRef.current = null;
      }
      return;
    }

    const topic = `/topic/ludo/${gameState.gameId}`;

    if (subscriptionRef.current === topic) return;

    if (subscriptionRef.current) {
      socketService.unsubscribe(subscriptionRef.current);
    }

    const connectAndSubscribe = async () => {
      try {
        await socketService.connect();
        console.log(`LudoContext: Subscribing to ${topic}`);
        socketService.subscribe(topic, handleGameUpdate);
        subscriptionRef.current = topic;

        // Pobieramy najświeższy stan zaraz po podłączeniu
        const latest = await ludoService.getGameState();
        setGameState(latest);
      } catch (err) {
        console.error("LudoContext: WebSocket subscription failed", err);
      }
    };

    connectAndSubscribe();

    return () => {
      // Subskrypcja zostaje przy zmianach komponentów,
      // zostanie wyczyszczona tylko przy zmianie gameId lub wylogowaniu
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
        isMyTurn,
        refreshGameState,
        rollDice,
        movePawn,
      }}
    >
      {children}
    </LudoContext.Provider>
  );
};

export const useLudo = (): LudoContextType => {
  const context = useContext(LudoContext);
  if (!context) {
    throw new Error("useLudo must be used within a LudoProvider");
  }
  return context;
};
