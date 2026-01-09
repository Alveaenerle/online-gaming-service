import { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import { socketService } from "../../../services/socketService";
import { makaoGameService } from "../../../services/makaoGameService";
import { lobbyService } from "../../../services/lobbyService";
import {
  GameStateMessage,
  LocalGameState,
  MyCard,
  PlayCardRequest,
  DemandModalState,
  DrawnCardState,
  PlayerCardView
} from "./types";
import { RANK_TO_BACKEND } from "./constants";

interface BackendGameState extends GameStateMessage {
  myCards: PlayerCardView[];
}

interface UseMakaoGameReturn {
  // State
  gameState: LocalGameState | null;
  playerNames: Record<string, string>;
  roomId: string | null;
  message: string;
  loading: boolean;
  isConnecting: boolean;
  drawnCard: DrawnCardState | null;
  drawnCardPlayable: boolean;
  demandModal: DemandModalState | null;

  // Computed
  isMyTurn: boolean;
  user: ReturnType<typeof useAuth>["user"];

  // Actions
  handlePlayCard: (card: MyCard) => Promise<void>;
  handleDrawCard: () => Promise<void>;
  handlePlayDrawnCard: () => Promise<void>;
  handleSkipDrawnCard: () => Promise<void>;
  handleAcceptEffect: () => Promise<void>;
  handleLeaveGame: () => Promise<void>;
  handleDemandSelect: (value: string) => Promise<void>;
  closeDemandModal: () => void;
  getPlayerName: (playerId: string) => string;
}

export const useMakaoGame = (): UseMakaoGameReturn => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Core game state
  const [gameState, setGameState] = useState<LocalGameState | null>(null);
  const [playerNames, setPlayerNames] = useState<Record<string, string>>({});
  const [roomId, setRoomId] = useState<string | null>(null);
  const [message, setMessage] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [isConnecting, setIsConnecting] = useState(true);

  // Drawn card state
  const [drawnCard, setDrawnCard] = useState<DrawnCardState | null>(null);
  const [drawnCardPlayable, setDrawnCardPlayable] = useState(false);

  // Demand modal state (for Jack/Ace)
  const [demandModal, setDemandModal] = useState<DemandModalState | null>(null);

  // Stable reference to navigate
  const navigateRef = useRef(navigate);
  navigateRef.current = navigate;

  // Transform backend state to local state
  const transformGameState = useCallback((data: BackendGameState): LocalGameState => {
    const flattenedCards: MyCard[] = data.myCards.map((pcv) => ({
      suit: pcv.card.suit,
      rank: pcv.card.rank,
      isPlayable: pcv.playable,
    }));

    return {
      ...data,
      myCards: flattenedCards,
    };
  }, []);

  // Handle game state update from WebSocket
  const handleGameUpdate = useCallback((data: BackendGameState) => {
    console.log("Game state update:", data);

    const localState = transformGameState(data);
    setGameState(localState);
    setIsConnecting(false);

    if (data.status === "FINISHED") {
      setMessage("Gra zakończona!");
    } else if (data.status === "WAITING") {
      navigateRef.current("/lobby/makao");
    }
  }, [transformGameState]);

  // Load player names and room ID from lobby info
  useEffect(() => {
    lobbyService.getRoomInfo()
      .then((info) => {
        setPlayerNames(info.players);
        setRoomId(info.id);
        console.log("Room info loaded:", info.id, info.players);
      })
      .catch((err) => {
        console.error("Failed to load room info:", err);
        navigate("/home");
      });
  }, [navigate]);

  // Connect to WebSocket for game updates
  useEffect(() => {
    if (!user?.id || !roomId) return;

    let timeoutId: NodeJS.Timeout;
    let stateRequested = false;

    const initSocket = async () => {
      try {
        setIsConnecting(true);
        console.log("Connecting to Makao WebSocket...");
        await socketService.connectMakao();
        console.log("WebSocket connected!");

        const topic = `/topic/makao/${user.id}`;
        socketService.subscribe(topic, handleGameUpdate);
        console.log("Subscribed to", topic);

        // Request game state after subscribing
        setTimeout(async () => {
          if (!stateRequested) {
            stateRequested = true;
            console.log("Requesting game state for room:", roomId);
            try {
              await makaoGameService.requestState(roomId);
              console.log("Game state requested successfully");
            } catch (err) {
              console.error("Failed to request game state:", err);
            }
          }
        }, 500);

        // Timeout - if no game state received in 15 seconds
        timeoutId = setTimeout(() => {
          setMessage("Oczekiwanie na dane gry...");
          setIsConnecting(false);
        }, 15000);

      } catch (err) {
        console.error("Socket connection failed:", err);
        setMessage("Połączenie nie powiodło się. Odśwież stronę.");
        setIsConnecting(false);
      }
    };

    initSocket();

    return () => {
      clearTimeout(timeoutId);
      const topic = `/topic/makao/${user.id}`;
      socketService.unsubscribe(topic);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, roomId]);

  // Execute play card request
  const executePlayCard = async (card: MyCard, demandedRank?: string, demandedSuit?: string) => {
    setLoading(true);
    try {
      const request: PlayCardRequest = {
        cardSuit: card.suit.toUpperCase(),
        cardRank: RANK_TO_BACKEND[card.rank] || card.rank.toUpperCase(),
        ...(demandedRank && { requestRank: demandedRank }),
        ...(demandedSuit && { requestSuit: demandedSuit }),
      };
      console.log("Playing card:", request);
      await makaoGameService.playCard(request);
      setMessage("Karta zagrana!");
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Nie można zagrać tej karty";
      setMessage(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Play a card (with Jack/Ace special handling)
  const handlePlayCard = async (card: MyCard) => {
    if (loading) return;

    // Jack (J) - needs rank demand
    if (card.rank === "JACK" || card.rank === "J") {
      setDemandModal({ type: "rank", card });
      return;
    }

    // Ace (A) - needs suit demand
    if (card.rank === "ACE" || card.rank === "A") {
      setDemandModal({ type: "suit", card });
      return;
    }

    await executePlayCard(card);
  };

  // Handle demand selection from modal
  const handleDemandSelect = async (value: string) => {
    if (!demandModal) return;

    const { type, card } = demandModal;
    setDemandModal(null);

    if (type === "rank") {
      await executePlayCard(card, value, undefined);
    } else {
      await executePlayCard(card, undefined, value);
    }
  };

  // Close demand modal
  const closeDemandModal = () => setDemandModal(null);

  // Draw a card
  const handleDrawCard = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const response = await makaoGameService.drawCard();
      if (response.card) {
        setDrawnCard(response.card);
        setDrawnCardPlayable(response.isPlayable ?? false);
        setMessage(`Dobrano kartę`);
      } else {
        setMessage("Brak kart do dobrania");
      }
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Nie można dobrać karty";
      setMessage(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Play drawn card
  const handlePlayDrawnCard = async () => {
    if (!drawnCard || loading) return;
    setLoading(true);
    try {
      const request: PlayCardRequest = {
        cardSuit: drawnCard.suit,
        cardRank: drawnCard.rank,
      };
      await makaoGameService.playDrawnCard(request);
      setMessage("Zagrano dobraną kartę!");
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : "Nie można zagrać karty";
      setMessage(errorMessage);
    } finally {
      setDrawnCard(null); // Always close modal, even on error
      setLoading(false);
    }
  };

  // Skip after drawing
  const handleSkipDrawnCard = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await makaoGameService.skipDrawnCard();
      setMessage("Tura pominięta");
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error ? err.message : "Nie można pominąć tury";
      setMessage(errorMessage);
    } finally {
      setDrawnCard(null); // Always close modal, even on error
      setLoading(false);
    }
  };

  // Accept special effect (e.g., draw cards from 2/3)
  const handleAcceptEffect = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await makaoGameService.acceptEffect();
      setMessage("Efekt zaakceptowany");
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Nie można zaakceptować efektu";
      setMessage(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Leave game
  const handleLeaveGame = async () => {
    try {
      await lobbyService.leaveRoom();
      socketService.disconnect();
      navigate("/home");
    } catch (err) {
      console.error("Failed to leave:", err);
      navigate("/home");
    }
  };

  // Get player name
  const getPlayerName = (playerId: string): string => {
    if (playerId === user?.id) return "Ty";
    return playerNames[playerId] || `Gracz ${playerId.slice(0, 4)}`;
  };

  // Computed values
  const isMyTurn = gameState?.activePlayerId === user?.id;

  return {
    // State
    gameState,
    playerNames,
    roomId,
    message,
    loading,
    isConnecting,
    drawnCard,
    drawnCardPlayable,
    demandModal,

    // Computed
    isMyTurn,
    user,

    // Actions
    handlePlayCard,
    handleDrawCard,
    handlePlayDrawnCard,
    handleSkipDrawnCard,
    handleAcceptEffect,
    handleLeaveGame,
    handleDemandSelect,
    closeDemandModal,
    getPlayerName,
  };
};
