import { useState, useEffect, useCallback, useRef } from "react";
import { useAuth } from "../../../../context/AuthContext";
import { GameStateMessage } from "../types";
import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

/** Message sent by backend when player is kicked due to timeout */
interface PlayerTimeoutMessage {
  roomId: string;
  playerId: string;
  replacedByBotId: string;
  message: string;
  type: "PLAYER_TIMEOUT";
}

interface UseMakaoSocketReturn {
  gameState: GameStateMessage | null;
  isConnected: boolean;
  connectionError: string | null;
  wasKickedByTimeout: boolean;
  timeoutMessage: string | null;
  resetState: () => void;
  clearTimeoutStatus: () => void;
}

/**
 * Hook for managing WebSocket connection and game state updates for Makao
 * Uses dedicated Makao WebSocket endpoint (/api/makao/ws)
 */
export const useMakaoSocket = (): UseMakaoSocketReturn => {
  const { user } = useAuth();
  const [gameState, setGameState] = useState<GameStateMessage | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);
  const [wasKickedByTimeout, setWasKickedByTimeout] = useState(false);
  const [timeoutMessage, setTimeoutMessage] = useState<string | null>(null);
  const clientRef = useRef<StompJs.Client | null>(null);
  const subscriptionRef = useRef<StompJs.Subscription | null>(null);
  const timeoutSubscriptionRef = useRef<StompJs.Subscription | null>(null);

  // Handle incoming game state update
  const handleGameUpdate = useCallback((message: StompJs.Message) => {
    try {
      const data: GameStateMessage = JSON.parse(message.body);
      console.log("[Makao WS] Game state update:", data);
      setGameState(data);
      setConnectionError(null);
    } catch (err) {
      console.error("[Makao WS] Failed to parse message:", err);
    }
  }, []);

  // Handle timeout notification
  const handleTimeoutNotification = useCallback((message: StompJs.Message) => {
    try {
      const data: PlayerTimeoutMessage = JSON.parse(message.body);
      console.log("[Makao WS] Timeout notification received:", data);
      setWasKickedByTimeout(true);
      setTimeoutMessage(data.message);
    } catch (err) {
      console.error("[Makao WS] Failed to parse timeout message:", err);
    }
  }, []);

  // Connect and subscribe to WebSocket
  useEffect(() => {
    if (!user?.id) {
      setConnectionError("User not authenticated");
      return;
    }

    const topic = `/topic/makao/${user.id}`;
    const timeoutTopic = `/topic/makao/${user.id}/timeout`;

    const connect = () => {
      try {
        console.log("[Makao WS] Connecting to Makao WebSocket...");
        const socket = new SockJS("http://localhost/api/makao/ws");
        const client = StompJs.over(socket);
        client.debug = () => {}; // Disable debug logs

        client.connect(
          {},
          () => {
            console.log("[Makao WS] Connected successfully");
            setIsConnected(true);
            setConnectionError(null);

            console.log(`[Makao WS] Subscribing to ${topic}`);
            subscriptionRef.current = client.subscribe(topic, handleGameUpdate);

            console.log(`[Makao WS] Subscribing to timeout topic ${timeoutTopic}`);
            timeoutSubscriptionRef.current = client.subscribe(timeoutTopic, handleTimeoutNotification);
          },
          (error: string | StompJs.Frame) => {
            console.error("[Makao WS] Connection error:", error);
            setConnectionError("Failed to connect to game server");
            setIsConnected(false);
          }
        );

        clientRef.current = client;
      } catch (err) {
        console.error("[Makao WS] Connection failed:", err);
        setConnectionError("Failed to connect to game server");
        setIsConnected(false);
      }
    };

    connect();

    // Cleanup on unmount
    return () => {
      console.log(`[Makao WS] Cleaning up WebSocket connection`);
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
      if (timeoutSubscriptionRef.current) {
        timeoutSubscriptionRef.current.unsubscribe();
        timeoutSubscriptionRef.current = null;
      }
      if (clientRef.current?.connected) {
        clientRef.current.disconnect(() => {
          console.log("[Makao WS] Disconnected");
        });
        clientRef.current = null;
      }
    };
  }, [user?.id, handleGameUpdate, handleTimeoutNotification]);

  // Reset state (e.g., when leaving game)
  const resetState = useCallback(() => {
    setGameState(null);
    setConnectionError(null);
    setWasKickedByTimeout(false);
    setTimeoutMessage(null);
  }, []);

  // Clear timeout status (e.g., after user acknowledges the modal)
  const clearTimeoutStatus = useCallback(() => {
    setWasKickedByTimeout(false);
    setTimeoutMessage(null);
  }, []);

  return {
    gameState,
    isConnected,
    connectionError,
    wasKickedByTimeout,
    timeoutMessage,
    resetState,
    clearTimeoutStatus,
  };
};

export default useMakaoSocket;
