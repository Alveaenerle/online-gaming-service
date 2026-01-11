import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
  useRef,
} from "react";
import { lobbyService } from "../services/lobbyService";
import { socketService } from "../services/socketService";
import { LobbyInfoRaw } from "../components/Games/utils/types";
import { useAuth } from './AuthContext';

interface LobbyContextType {
  currentLobby: LobbyInfoRaw | null;
  isInLobby: boolean;
  isLoading: boolean;
  refreshLobbyStatus: () => Promise<void>;
  clearLobby: () => void;
  setCurrentLobby: (lobby: LobbyInfoRaw | null) => void;
}

const LobbyContext = createContext<LobbyContextType | undefined>(undefined);

export const LobbyProvider: React.FC<{ children: ReactNode }> = ({
  children,
}) => {
  const { user } = useAuth(); // Monitor user state
  const [currentLobby, setCurrentLobby] = useState<LobbyInfoRaw | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const subscriptionRef = useRef<string | null>(null);

  const refreshLobbyStatus = useCallback(async () => {
    setIsLoading(true);
    try {
      const lobbyInfo = await lobbyService.getRoomInfo();
      setCurrentLobby(lobbyInfo);
    } catch {
      // User is not in any lobby
      setCurrentLobby(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearLobby = useCallback(() => {
    // Unsubscribe from WebSocket when clearing lobby
    if (subscriptionRef.current) {
      socketService.unsubscribe(subscriptionRef.current);
      subscriptionRef.current = null;
    }
    setCurrentLobby(null);
  }, []);

  // Effect: Clear lobby when user logs out
  useEffect(() => {
    if (!user) {
      clearLobby();
    }
  }, [user, clearLobby]);

  // Handle WebSocket updates for the current lobby
  const handleLobbyUpdate = useCallback((data: LobbyInfoRaw) => {
    if (!data) return;
    console.log("LobbyContext: Received lobby update", data);
    setCurrentLobby(data);
  }, []);

  // Subscribe to WebSocket updates when lobby changes
  useEffect(() => {
    if (!currentLobby?.id) {
      // No lobby, unsubscribe if subscribed
      if (subscriptionRef.current) {
        socketService.unsubscribe(subscriptionRef.current);
        subscriptionRef.current = null;
      }
      return;
    }

    const topic = `/topic/room/${currentLobby.id}`;
    
    // Only subscribe if not already subscribed to this topic
    if (subscriptionRef.current === topic) {
      return;
    }

    // Unsubscribe from previous topic if any
    if (subscriptionRef.current) {
      socketService.unsubscribe(subscriptionRef.current);
    }

    const connectAndSubscribe = async () => {
      try {
        await socketService.connect();
        console.log(`LobbyContext: Subscribing to ${topic}`);
        socketService.subscribe(topic, handleLobbyUpdate);
        subscriptionRef.current = topic;
        
        // Refresh lobby status to ensure we have the latest data after connection
        const latestInfo = await lobbyService.getRoomInfo();
        setCurrentLobby(latestInfo);
      } catch (err) {
        console.error("LobbyContext: Failed to connect to WebSocket:", err);
      }
    };

    connectAndSubscribe();

    return () => {
      // Keep subscription active
    };
  }, [currentLobby?.id, handleLobbyUpdate]);

  // Check lobby status on mount
  useEffect(() => {
    refreshLobbyStatus();
  }, [refreshLobbyStatus]);

  return (
    <LobbyContext.Provider
      value={{
        currentLobby,
        isInLobby: !!currentLobby,
        isLoading,
        refreshLobbyStatus,
        clearLobby,
        setCurrentLobby,
      }}
    >
      {children}
    </LobbyContext.Provider>
  );
};

export const useLobby = (): LobbyContextType => {
  const context = useContext(LobbyContext);
  if (!context) {
    throw new Error("useLobby must be used within a LobbyProvider");
  }
  return context;
};
