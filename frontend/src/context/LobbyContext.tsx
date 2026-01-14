import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
  useCallback,
  useRef,
} from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../services/lobbyService";
import { socketService } from "../services/socketService";
import { LobbyInfoRaw } from "../components/Games/utils/types";
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

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
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [currentLobby, setCurrentLobby] = useState<LobbyInfoRaw | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const subscriptionRef = useRef<string | null>(null);
  const kickSubscriptionRef = useRef<string | null>(null);

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
    if (kickSubscriptionRef.current) {
      socketService.unsubscribe(kickSubscriptionRef.current);
      kickSubscriptionRef.current = null;
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

  // Handle kick notification
  const handleKickNotification = useCallback((data: { type: string; kickedBy: string; message: string }) => {
    console.log("LobbyContext: Received kick notification", data);
    if (data.type === 'KICKED') {
      showToast(data.message || `You have been kicked by ${data.kickedBy}`, 'error');
      // Clear lobby state and navigate to home
      if (subscriptionRef.current) {
        socketService.unsubscribe(subscriptionRef.current);
        subscriptionRef.current = null;
      }
      if (kickSubscriptionRef.current) {
        socketService.unsubscribe(kickSubscriptionRef.current);
        kickSubscriptionRef.current = null;
      }
      setCurrentLobby(null);
      navigate('/home');
    }
  }, [showToast, navigate]);

  // Subscribe to WebSocket updates when lobby changes
  useEffect(() => {
    if (!currentLobby?.id || !user?.id) {
      // No lobby, unsubscribe if subscribed
      if (subscriptionRef.current) {
        socketService.unsubscribe(subscriptionRef.current);
        subscriptionRef.current = null;
      }
      if (kickSubscriptionRef.current) {
        socketService.unsubscribe(kickSubscriptionRef.current);
        kickSubscriptionRef.current = null;
      }
      return;
    }

    const topic = `/topic/room/${currentLobby.id}`;
    const kickTopic = `/topic/room/${currentLobby.id}/kicked/${user.id}`;
    
    // Only subscribe if not already subscribed to this topic
    if (subscriptionRef.current === topic) {
      return;
    }

    // Unsubscribe from previous topic if any
    if (subscriptionRef.current) {
      socketService.unsubscribe(subscriptionRef.current);
    }
    if (kickSubscriptionRef.current) {
      socketService.unsubscribe(kickSubscriptionRef.current);
    }

    const connectAndSubscribe = async () => {
      try {
        await socketService.connect();
        console.log(`LobbyContext: Subscribing to ${topic}`);
        socketService.subscribe(topic, handleLobbyUpdate);
        subscriptionRef.current = topic;
        
        // Subscribe to kick notifications for this user
        console.log(`LobbyContext: Subscribing to kick notifications ${kickTopic}`);
        socketService.subscribe(kickTopic, handleKickNotification);
        kickSubscriptionRef.current = kickTopic;
        
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
  }, [currentLobby?.id, user?.id, handleLobbyUpdate, handleKickNotification]);

  // Fallback polling: periodically check lobby status in case WebSocket misses updates
  // This ensures players don't get stuck in lobby when game starts
  useEffect(() => {
    if (!currentLobby?.id || currentLobby?.status === "PLAYING") {
      return;
    }

    const pollInterval = setInterval(async () => {
      try {
        const latestInfo = await lobbyService.getRoomInfo();
        // Only update if status changed to PLAYING (game started)
        if (latestInfo?.status === "PLAYING" && currentLobby?.status !== "PLAYING") {
          console.log("LobbyContext: Polling detected game start, updating state");
          setCurrentLobby(latestInfo);
        }
      } catch (err) {
        // Ignore errors - WebSocket is primary, this is just a fallback
      }
    }, 3000); // Poll every 3 seconds

    return () => clearInterval(pollInterval);
  }, [currentLobby?.id, currentLobby?.status]);

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
