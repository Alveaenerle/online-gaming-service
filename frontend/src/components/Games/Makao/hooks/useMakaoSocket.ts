import { useState, useEffect, useCallback, useRef } from "react";
import { useAuth } from "../../../../context/AuthContext";
import { GameStateMessage } from "../types";
import makaoGameService from "../../../../services/makaoGameService";
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
  reconnect: () => void;
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

  // Connection management refs
  const clientRef = useRef<StompJs.Client | null>(null);
  const subscriptionRef = useRef<StompJs.Subscription | null>(null);
  const timeoutSubscriptionRef = useRef<StompJs.Subscription | null>(null);
  const isMountedRef = useRef(true);
  const connectionIdRef = useRef(0); // Track connection attempts to avoid stale closures

  // Cleanup function to properly disconnect and clear subscriptions
  const cleanup = useCallback(() => {
    console.log("[Makao WS] Running cleanup...");

    if (subscriptionRef.current) {
      try {
        console.log("[Makao WS] Unsubscribing from game state topic");
        subscriptionRef.current.unsubscribe();
      } catch (e) {
        console.warn("[Makao WS] Error unsubscribing from game state:", e);
      }
      subscriptionRef.current = null;
    }

    if (timeoutSubscriptionRef.current) {
      try {
        console.log("[Makao WS] Unsubscribing from timeout topic");
        timeoutSubscriptionRef.current.unsubscribe();
      } catch (e) {
        console.warn("[Makao WS] Error unsubscribing from timeout:", e);
      }
      timeoutSubscriptionRef.current = null;
    }

    if (clientRef.current) {
      try {
        if (clientRef.current.connected) {
          console.log("[Makao WS] Disconnecting STOMP client");
          clientRef.current.disconnect(() => {
            console.log("[Makao WS] STOMP client disconnected");
          });
        }
      } catch (e) {
        console.warn("[Makao WS] Error disconnecting:", e);
      }
      clientRef.current = null;
    }

    setIsConnected(false);
  }, []);

  // Connect to WebSocket
  const connect = useCallback(() => {
    if (!user?.id) {
      console.log("[Makao WS] Cannot connect: no user ID");
      setConnectionError("User not authenticated");
      return;
    }

    // Increment connection ID to invalidate any pending callbacks from previous connections
    const currentConnectionId = ++connectionIdRef.current;
    console.log(`[Makao WS] Starting connection attempt #${currentConnectionId} for user ${user.id}`);

    // Clean up any existing connection first
    cleanup();

    const topic = `/topic/makao/${user.id}`;
    const timeoutTopic = `/topic/makao/${user.id}/timeout`;
    
    // Build WebSocket URL - use current origin for production, env var, or fallback to relative path
    const getWsUrl = () => {
      if (import.meta.env.VITE_MAKAO_WS_URL) {
        return import.meta.env.VITE_MAKAO_WS_URL;
      }
      // Use current origin to ensure correct protocol (http/https)
      return `${window.location.origin}/api/makao/ws`;
    };
    const wsUrl = getWsUrl();

    try {
      console.log("[Makao WS] Creating SockJS connection...");
      const socket = new SockJS(wsUrl);
      const client = StompJs.over(socket);

      // Disable verbose STOMP debug logs but keep our custom ones
      client.debug = () => {};

      client.connect(
        {},
        () => {
          // Check if this connection is still valid (component still mounted, same connection attempt)
          if (!isMountedRef.current || currentConnectionId !== connectionIdRef.current) {
            console.log(`[Makao WS] Connection #${currentConnectionId} completed but is stale, disconnecting`);
            try {
              client.disconnect(() => {});
            } catch (e) {
              // Ignore
            }
            return;
          }

          console.log(`[Makao WS] Connection #${currentConnectionId} established successfully`);
          setIsConnected(true);
          setConnectionError(null);
          clientRef.current = client;

          // Subscribe to game state updates
          console.log(`[Makao WS] Subscribing to game state: ${topic}`);
          subscriptionRef.current = client.subscribe(topic, (message: StompJs.Message) => {
            if (!isMountedRef.current || currentConnectionId !== connectionIdRef.current) {
              console.log("[Makao WS] Ignoring message from stale connection");
              return;
            }
            try {
              const data: GameStateMessage = JSON.parse(message.body);
              console.log("[Makao WS] Received game state update:", {
                roomId: data.roomId,
                status: data.status,
                activePlayerId: data.activePlayerId,
                playerCount: Object.keys(data.playersCardsAmount || {}).length,
              });
              setGameState(data);
              setConnectionError(null);
            } catch (err) {
              console.error("[Makao WS] Failed to parse game state message:", err);
            }
          });

          // Subscribe to timeout notifications
          console.log(`[Makao WS] Subscribing to timeout notifications: ${timeoutTopic}`);
          timeoutSubscriptionRef.current = client.subscribe(timeoutTopic, (message: StompJs.Message) => {
            if (!isMountedRef.current || currentConnectionId !== connectionIdRef.current) {
              return;
            }
            try {
              const data: PlayerTimeoutMessage = JSON.parse(message.body);
              console.log("[Makao WS] Received timeout notification:", data);
              setWasKickedByTimeout(true);
              setTimeoutMessage(data.message);
            } catch (err) {
              console.error("[Makao WS] Failed to parse timeout message:", err);
            }
          });

          // Request the current game state after subscribing with retry logic
          // The game might still be initializing on the backend (race condition with RabbitMQ)
          const requestStateWithRetry = async (attempt: number, maxAttempts: number, delayMs: number) => {
            if (!isMountedRef.current || currentConnectionId !== connectionIdRef.current) {
              return;
            }

            try {
              console.log(`[Makao WS] Requesting game state (attempt ${attempt}/${maxAttempts})...`);
              await makaoGameService.requestState();
              console.log("[Makao WS] State request sent successfully");
            } catch (err) {
              console.log(`[Makao WS] State request failed (attempt ${attempt}/${maxAttempts}):`, err);

              if (attempt < maxAttempts) {
                // Retry with exponential backoff
                const nextDelay = delayMs * 1.5;
                console.log(`[Makao WS] Retrying in ${delayMs}ms...`);
                setTimeout(() => {
                  requestStateWithRetry(attempt + 1, maxAttempts, nextDelay);
                }, delayMs);
              } else {
                console.log("[Makao WS] All state request attempts failed. Waiting for WebSocket broadcast.");
              }
            }
          };

          // Start retry sequence: 5 attempts, starting with 500ms delay
          requestStateWithRetry(1, 5, 500);
        },
        (error: string | StompJs.Frame) => {
          if (!isMountedRef.current || currentConnectionId !== connectionIdRef.current) {
            return;
          }
          console.error(`[Makao WS] Connection #${currentConnectionId} error:`, error);
          setConnectionError("Failed to connect to game server");
          setIsConnected(false);
          clientRef.current = null;
        }
      );
    } catch (err) {
      console.error("[Makao WS] Failed to create connection:", err);
      setConnectionError("Failed to connect to game server");
      setIsConnected(false);
    }
  }, [user?.id, cleanup]);

  // Initial connection on mount
  useEffect(() => {
    isMountedRef.current = true;
    console.log("[Makao WS] Hook mounted, initiating connection");
    connect();

    return () => {
      console.log("[Makao WS] Hook unmounting, cleaning up");
      isMountedRef.current = false;
      cleanup();
    };
  }, [connect, cleanup]);

  // Reset state (e.g., when leaving game or playing again)
  const resetState = useCallback(() => {
    console.log("[Makao WS] Resetting state");
    setGameState(null);
    setConnectionError(null);
    setWasKickedByTimeout(false);
    setTimeoutMessage(null);
  }, []);

  // Force reconnect (useful when starting a new game)
  const reconnect = useCallback(() => {
    console.log("[Makao WS] Force reconnect requested");
    resetState();
    connect();
  }, [resetState, connect]);

  // Clear timeout status (e.g., after user acknowledges the modal)
  const clearTimeoutStatus = useCallback(() => {
    console.log("[Makao WS] Clearing timeout status");
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
    reconnect,
  };
};

export default useMakaoSocket;
