import { useState, useEffect, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Users, Loader2, X } from "lucide-react";
import Navbar from "../../Shared/Navbar";
import Card from "./Card";
import { Suit, Rank, GameStateMessage, PlayCardRequest } from "./types";
import { useAuth } from "../../../context/AuthContext";
import { socketService } from "../../../services/socketService";
import { makaoGameService } from "../../../services/makaoGameService";
import { lobbyService } from "../../../services/lobbyService";

// Backend enum mappings
const RANK_MAP: Record<string, string> = {
  TWO: "2", THREE: "3", FOUR: "4", FIVE: "5", SIX: "6",
  SEVEN: "7", EIGHT: "8", NINE: "9", TEN: "10",
  JACK: "J", QUEEN: "Q", KING: "K", ACE: "A"
};

const RANK_TO_BACKEND: Record<string, string> = {
  "2": "TWO", "3": "THREE", "4": "FOUR", "5": "FIVE", "6": "SIX",
  "7": "SEVEN", "8": "EIGHT", "9": "NINE", "10": "TEN",
  "J": "JACK", "Q": "QUEEN", "K": "KING", "A": "ACE"
};

const convertRank = (rank: string): string => RANK_MAP[rank] || rank;
const convertSuit = (suit: string): string => suit.toLowerCase();

// Backend sends PlayerCardView: { card: { suit, rank }, playable }
interface BackendCard {
  suit: string;
  rank: string;
}

interface PlayerCardView {
  card: BackendCard;
  playable: boolean;
}

// Flattened card for internal use
interface MyCard {
  suit: string;
  rank: string;
  isPlayable: boolean;
}

interface BackendGameState extends GameStateMessage {
  myCards: PlayerCardView[];
}

// Suit/Rank selection modal for Jack (J) and Ace (A)
interface DemandModalProps {
  type: "suit" | "rank";
  onSelect: (value: string) => void;
  onClose: () => void;
}

const DemandModal: React.FC<DemandModalProps> = ({ type, onSelect, onClose }) => {
  const suits = ["HEARTS", "DIAMONDS", "CLUBS", "SPADES"];
  const ranks = ["FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN"];
  const items = type === "suit" ? suits : ranks;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#1a1a27] p-6 rounded-2xl border border-purple-500/30 shadow-2xl max-w-sm w-full mx-4"
      >
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-white">
            {type === "suit" ? "Choose Suit (Ace)" : "Choose Rank (Jack)"}
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-white">
            <X size={20} />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {items.map((item) => (
            <motion.button
              key={item}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onSelect(item)}
              className="py-4 px-4 bg-purple-600/20 hover:bg-purple-600/40 border border-purple-500/30 rounded-xl text-white font-medium transition-colors"
            >
              {type === "suit" ? (
                <span className={`text-2xl ${item === "HEARTS" || item === "DIAMONDS" ? "text-red-500" : "text-white"}`}>
                  {item === "HEARTS" ? "♥" : item === "DIAMONDS" ? "♦" : item === "CLUBS" ? "♣" : "♠"}
                </span>
              ) : (
                convertRank(item)
              )}
            </motion.button>
          ))}
        </div>
      </motion.div>
    </motion.div>
  );
};

// Drawn card modal
interface DrawnCardModalProps {
  card: { suit: string; rank: string };
  isPlayable: boolean;
  onPlay: () => void;
  onSkip: () => void;
  loading: boolean;
}

const DrawnCardModal: React.FC<DrawnCardModalProps> = ({ card, isPlayable, onPlay, onSkip, loading }) => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#1a1a27] p-6 rounded-2xl border border-purple-500/30 shadow-2xl text-center"
      >
        <p className="text-gray-400 mb-4">You drew:</p>
        <div className="flex justify-center mb-4">
          <Card
            card={{
              suit: convertSuit(card.suit) as Suit,
              rank: convertRank(card.rank) as Rank,
              id: "drawn",
            }}
            size="lg"
          />
        </div>
        <div className="flex gap-3 justify-center">
          {isPlayable && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={onPlay}
              disabled={loading}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-500 rounded-xl font-bold text-white disabled:opacity-50"
            >
              {loading ? <Loader2 className="animate-spin" size={20} /> : "Play Card"}
            </motion.button>
          )}
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={onSkip}
            disabled={loading}
            className="px-6 py-3 bg-gray-600 hover:bg-gray-500 rounded-xl font-bold text-white disabled:opacity-50"
          >
            {loading ? <Loader2 className="animate-spin" size={20} /> : "Skip Turn"}
          </motion.button>
        </div>
      </motion.div>
    </motion.div>
  );
};

const MakaoGame: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  // Game state with flattened myCards
  interface LocalGameState extends Omit<GameStateMessage, 'myCards'> {
    myCards: MyCard[];
  }

  const [gameState, setGameState] = useState<LocalGameState | null>(null);
  const [playerNames, setPlayerNames] = useState<Record<string, string>>({});
  const [roomId, setRoomId] = useState<string | null>(null);
  const [message, setMessage] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [isConnecting, setIsConnecting] = useState(true);

  // Drawn card state
  const [drawnCard, setDrawnCard] = useState<{ suit: string; rank: string } | null>(null);
  const [drawnCardPlayable, setDrawnCardPlayable] = useState(false);

  // Demand modal state (for Jack/Ace)
  const [demandModal, setDemandModal] = useState<{ type: "suit" | "rank"; card: MyCard } | null>(null);

  // Handle game state update from WebSocket
  const handleGameUpdate = useCallback((data: BackendGameState) => {
    console.log("Game state update:", data);

    // Transform backend PlayerCardView[] to flat MyCard[]
    const flattenedCards: MyCard[] = data.myCards.map((pcv) => ({
      suit: pcv.card.suit,
      rank: pcv.card.rank,
      isPlayable: pcv.playable,
    }));

    const localState: LocalGameState = {
      ...data,
      myCards: flattenedCards,
    };

    setGameState(localState);
    setIsConnecting(false);

    if (data.status === "FINISHED") {
      setMessage("Game finished!");
    } else if (data.status === "WAITING") {
      navigate("/lobby/makao");
    }
  }, [navigate]);

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

        // Request game state after subscribing (in case we missed the initial broadcast)
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
        }, 500); // Small delay to ensure subscription is active

        // Set timeout - if no game state received in 15 seconds, show error
        timeoutId = setTimeout(() => {
          if (!gameState) {
            console.warn("No game state received after 15s");
            setMessage("Waiting for game data... If this persists, the game may not have started properly.");
            setIsConnecting(false);
          }
        }, 15000);

      } catch (err) {
        console.error("Socket connection failed:", err);
        setMessage("Connection failed. Please refresh.");
        setIsConnecting(false);
      }
    };

    initSocket();

    return () => {
      clearTimeout(timeoutId);
      const topic = `/topic/makao/${user.id}`;
      socketService.unsubscribe(topic);
    };
  }, [user?.id, roomId, handleGameUpdate, gameState]);

  // Play a card
  const handlePlayCard = async (card: MyCard) => {
    if (loading) return;

    // Check if card is Jack (J) - needs rank demand
    if (card.rank === "JACK" || card.rank === "J") {
      setDemandModal({ type: "rank", card });
      return;
    }

    // Check if card is Ace (A) - needs suit demand
    if (card.rank === "ACE" || card.rank === "A") {
      setDemandModal({ type: "suit", card });
      return;
    }

    await executePlayCard(card);
  };

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
      setMessage("Card played!");
    } catch (err: any) {
      setMessage(err.message || "Cannot play this card");
    } finally {
      setLoading(false);
    }
  };

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

  // Draw a card
  const handleDrawCard = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const response = await makaoGameService.drawCard();
      if (response.card) {
        setDrawnCard(response.card);
        setDrawnCardPlayable((response as any).isPlayable ?? false);
        setMessage(`Drew ${convertRank(response.card.rank)} of ${convertSuit(response.card.suit)}`);
      } else {
        setMessage("No cards to draw");
      }
    } catch (err: any) {
      setMessage(err.message || "Failed to draw card");
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
      setDrawnCard(null);
      setMessage("Played drawn card!");
    } catch (err: any) {
      setMessage(err.message || "Failed to play drawn card");
    } finally {
      setLoading(false);
    }
  };

  // Skip after drawing
  const handleSkipDrawnCard = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await makaoGameService.skipDrawnCard();
      setDrawnCard(null);
      setMessage("Turn skipped");
    } catch (err: any) {
      setMessage(err.message || "Failed to skip");
    } finally {
      setLoading(false);
    }
  };

  // Accept special effect (e.g., draw cards from 2/3)
  const handleAcceptEffect = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await makaoGameService.acceptEffect();
      setMessage("Effect accepted");
    } catch (err: any) {
      setMessage(err.message || "Failed to accept effect");
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
    if (playerId === user?.id) return "You";
    return playerNames[playerId] || `Player ${playerId.slice(0, 4)}`;
  };

  // Loading state
  if (isConnecting || !gameState) {
    return (
      <div className="min-h-screen bg-[#0a0a0f] text-white flex flex-col">
        <Navbar />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <Loader2 className="animate-spin w-12 h-12 text-purple-500 mx-auto mb-4" />
            <p className="text-gray-400">Connecting to game...</p>
          </div>
        </div>
      </div>
    );
  }

  const isMyTurn = gameState.activePlayerId === user?.id;
  const otherPlayers = Object.entries(gameState.playersCardsAmount)
    .filter(([id]) => id !== user?.id);

  // Distribute players around table
  const distributePlayersAroundTable = () => {
    const total = otherPlayers.length;
    const top: typeof otherPlayers = [];
    const left: typeof otherPlayers = [];
    const right: typeof otherPlayers = [];

    if (total === 1) {
      top.push(otherPlayers[0]);
    } else if (total === 2) {
      left.push(otherPlayers[0]);
      right.push(otherPlayers[1]);
    } else if (total === 3) {
      left.push(otherPlayers[0]);
      top.push(otherPlayers[1]);
      right.push(otherPlayers[2]);
    } else if (total >= 4) {
      const perSide = Math.ceil(total / 3);
      otherPlayers.forEach((p, i) => {
        if (i < perSide) left.push(p);
        else if (i < perSide * 2) top.push(p);
        else right.push(p);
      });
    }

    return { top, left, right };
  };

  const { top: topPlayers, left: leftPlayers, right: rightPlayers } = distributePlayersAroundTable();

  // Render player component
  const renderPlayer = (playerId: string, cardCount: number, position: "top" | "left" | "right") => {
    const isActive = playerId === gameState.activePlayerId;
    const skipTurns = gameState.playersSkipTurns[playerId] || 0;

    return (
      <div
        key={playerId}
        className={`bg-[#1a1a27] p-3 rounded-xl border transition-all ${
          isActive ? "border-purple-500 shadow-lg shadow-purple-500/20" : "border-white/10"
        }`}
      >
        <div className="flex items-center gap-2 mb-2">
          <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
            isActive ? "bg-purple-600" : "bg-gray-600"
          }`}>
            {getPlayerName(playerId).charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="text-sm font-medium text-white">{getPlayerName(playerId)}</p>
            <p className="text-xs text-gray-400">{cardCount} cards</p>
          </div>
          {isActive && (
            <motion.div
              animate={{ opacity: [0.5, 1, 0.5] }}
              transition={{ duration: 1.5, repeat: Infinity }}
              className="w-2 h-2 rounded-full bg-green-500 ml-auto"
            />
          )}
        </div>
        {skipTurns > 0 && (
          <p className="text-xs text-red-400">Skips: {skipTurns}</p>
        )}
        <div className="flex gap-0.5 mt-2">
          {Array.from({ length: Math.min(cardCount, 5) }).map((_, i) => (
            <div
              key={i}
              className="w-6 h-9 bg-gradient-to-br from-purple-600 to-purple-900 rounded border border-white/10"
            />
          ))}
          {cardCount > 5 && (
            <span className="text-xs text-gray-400 ml-1">+{cardCount - 5}</span>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-white antialiased overflow-hidden">
      <Navbar />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)]" />

      <main className="pt-24 pb-4 px-4 h-[calc(100vh-96px)]">
        <div className="h-full max-w-[1800px] mx-auto flex gap-4">
          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center">
            <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purple-500/20 shadow-2xl shadow-black/50 p-6">
              {/* Table glow */}
              <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.08),_transparent_60%)]" />

              {/* Top Players */}
              {topPlayers.length > 0 && (
                <div className="absolute top-4 left-1/2 -translate-x-1/2 flex gap-3">
                  {topPlayers.map(([id, count]) => renderPlayer(id, count, "top"))}
                </div>
              )}

              {/* Left Players */}
              {leftPlayers.length > 0 && (
                <div className="absolute left-4 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {leftPlayers.map(([id, count]) => renderPlayer(id, count, "left"))}
                </div>
              )}

              {/* Right Players */}
              {rightPlayers.length > 0 && (
                <div className="absolute right-4 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {rightPlayers.map(([id, count]) => renderPlayer(id, count, "right"))}
                </div>
              )}

              {/* Center - Deck & Discard */}
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center gap-12">
                {/* Draw Pile */}
                <motion.div
                  whileHover={isMyTurn && !gameState.specialEffectActive && !drawnCard ? { scale: 1.05 } : {}}
                  className={`text-center ${
                    isMyTurn && !gameState.specialEffectActive && !drawnCard
                      ? "cursor-pointer"
                      : "cursor-not-allowed opacity-70"
                  }`}
                  onClick={isMyTurn && !gameState.specialEffectActive && !drawnCard ? handleDrawCard : undefined}
                >
                  <div className="relative">
                    <div className="absolute -top-0.5 -left-0.5 w-[62px] h-[87px] rounded-lg bg-purple-600/30" />
                    <div className="absolute -top-1 -left-1 w-[62px] h-[87px] rounded-lg bg-purple-600/20" />
                    <div className="w-[60px] h-[85px] rounded-lg bg-gradient-to-br from-purple-600 to-purple-900 border border-white/20 flex items-center justify-center">
                      <span className="text-white/40 font-bold">OG</span>
                    </div>
                  </div>
                  <p className="text-xs text-white/60 mt-2">Draw ({gameState.drawDeckCardsAmount})</p>
                </motion.div>

                {/* Discard Pile */}
                <div className="text-center">
                  {gameState.currentCard && (
                    <Card
                      card={{
                        suit: convertSuit(gameState.currentCard.suit) as Suit,
                        rank: convertRank(gameState.currentCard.rank) as Rank,
                        id: "discard",
                      }}
                      size="lg"
                    />
                  )}
                  <p className="text-xs text-white/60 mt-2">Discard ({gameState.discardDeckCardsAmount})</p>
                </div>
              </div>

              {/* Bottom - My Cards */}
              <div className="absolute bottom-4 left-1/2 -translate-x-1/2 max-w-[85%]">
                <div className={`p-4 rounded-2xl transition-all ${
                  isMyTurn
                    ? "bg-purple-600/20 border-2 border-purple-500"
                    : "bg-white/5 border border-white/10"
                }`}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center font-bold">
                        {user?.username?.charAt(0).toUpperCase() || "Y"}
                      </div>
                      <div>
                        <p className="text-sm font-medium">You</p>
                        <p className="text-xs text-gray-400">{gameState.myCards.length} cards</p>
                      </div>
                    </div>
                    {isMyTurn && (
                      <span className="px-3 py-1 bg-green-500/20 text-green-400 text-xs font-bold rounded-full animate-pulse">
                        YOUR TURN
                      </span>
                    )}
                  </div>

                  <div className="flex gap-2 flex-wrap justify-center">
                    {gameState.myCards.map((card, index) => {
                      const isPlayable = isMyTurn && !gameState.specialEffectActive && card.isPlayable;
                      return (
                        <motion.div
                          key={index}
                          whileHover={isPlayable ? { y: -10, scale: 1.05 } : {}}
                          className={isPlayable ? "cursor-pointer" : "cursor-not-allowed"}
                          onClick={() => isPlayable && handlePlayCard(card)}
                        >
                          <Card
                            card={{
                              suit: convertSuit(card.suit) as Suit,
                              rank: convertRank(card.rank) as Rank,
                              id: `my-${index}`,
                            }}
                            size="md"
                            isPlayable={isPlayable}
                          />
                        </motion.div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Side Panel */}
          <div className="w-72 flex flex-col gap-3 flex-shrink-0">
            {/* Navigation */}
            <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleLeaveGame}
                className="w-full py-3 rounded-xl bg-red-500/20 hover:bg-red-500/30 text-red-400 font-medium transition-colors flex items-center justify-center gap-2 border border-red-500/30"
              >
                <ArrowLeft size={18} />
                Leave Game
              </motion.button>
            </div>

            {/* Game Info */}
            <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
              <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Room</h3>
              <p className="text-white font-mono text-sm">{gameState.roomId?.slice(0, 8)}...</p>
              <div className="flex items-center gap-2 mt-2 text-gray-400 text-sm">
                <Users size={14} />
                <span>{Object.keys(gameState.playersCardsAmount).length} players</span>
              </div>
            </div>

            {/* Activity */}
            <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
              <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Activity</h3>
              <AnimatePresence mode="wait">
                <motion.p
                  key={message}
                  initial={{ opacity: 0, y: 5 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -5 }}
                  className="text-white text-sm"
                >
                  {message || "Game in progress..."}
                </motion.p>
              </AnimatePresence>
            </div>

            {/* Game Status */}
            <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10 flex-1">
              <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Status</h3>
              <div className="space-y-3 text-sm">
                {/* Current player */}
                <div className="flex justify-between items-center">
                  <span className="text-gray-400">Current Turn</span>
                  <span className={`font-medium ${isMyTurn ? "text-green-400" : "text-white"}`}>
                    {getPlayerName(gameState.activePlayerId)}
                  </span>
                </div>

                {/* Demanded suit/rank */}
                {gameState.demandedSuit && (
                  <div className="flex justify-between items-center">
                    <span className="text-gray-400">Demanded Suit</span>
                    <span className={`text-xl ${
                      gameState.demandedSuit === "HEARTS" || gameState.demandedSuit === "DIAMONDS"
                        ? "text-red-500"
                        : "text-white"
                    }`}>
                      {gameState.demandedSuit === "HEARTS" ? "♥" :
                       gameState.demandedSuit === "DIAMONDS" ? "♦" :
                       gameState.demandedSuit === "CLUBS" ? "♣" : "♠"}
                    </span>
                  </div>
                )}

                {gameState.demandedRank && (
                  <div className="flex justify-between items-center">
                    <span className="text-gray-400">Demanded Rank</span>
                    <span className="text-purple-400 font-bold">{convertRank(gameState.demandedRank)}</span>
                  </div>
                )}

                {/* Special effect */}
                {gameState.specialEffectActive && (
                  <div className="mt-4 p-3 bg-orange-500/20 border border-orange-500/30 rounded-xl">
                    <p className="text-orange-400 text-sm font-medium mb-2">Special Effect Active!</p>
                    {isMyTurn && (
                      <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={handleAcceptEffect}
                        disabled={loading}
                        className="w-full py-2 bg-orange-500 hover:bg-orange-400 text-white rounded-lg font-medium text-sm disabled:opacity-50"
                      >
                        {loading ? <Loader2 className="animate-spin mx-auto" size={18} /> : "Accept Effect"}
                      </motion.button>
                    )}
                  </div>
                )}

                {/* Ranking */}
                {Object.keys(gameState.ranking).length > 0 && (
                  <div className="mt-4">
                    <p className="text-gray-400 text-xs mb-2">Finished:</p>
                    {Object.entries(gameState.ranking)
                      .sort(([, a], [, b]) => a - b)
                      .map(([id, place]) => (
                        <div key={id} className="flex justify-between text-sm">
                          <span className="text-white">{getPlayerName(id)}</span>
                          <span className="text-yellow-400">#{place}</span>
                        </div>
                      ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Modals */}
      <AnimatePresence>
        {drawnCard && (
          <DrawnCardModal
            card={drawnCard}
            isPlayable={drawnCardPlayable}
            onPlay={handlePlayDrawnCard}
            onSkip={handleSkipDrawnCard}
            loading={loading}
          />
        )}

        {demandModal && (
          <DemandModal
            type={demandModal.type}
            onSelect={handleDemandSelect}
            onClose={() => setDemandModal(null)}
          />
        )}
      </AnimatePresence>

      {/* Game Finished Modal */}
      <AnimatePresence>
        {gameState.status === "FINISHED" && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-[#121018] p-10 rounded-2xl border border-purple-500/50 shadow-2xl text-center max-w-md"
            >
              <h2 className="text-4xl font-bold mb-4 bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">
                🎉 Game Finished!
              </h2>

              {/* Final ranking */}
              <div className="mb-6">
                {Object.entries(gameState.ranking)
                  .sort(([, a], [, b]) => a - b)
                  .map(([id, place]) => (
                    <div key={id} className={`flex justify-between items-center py-2 px-4 rounded-lg mb-2 ${
                      place === 1 ? "bg-yellow-500/20" : "bg-white/5"
                    }`}>
                      <span className="text-white font-medium">{getPlayerName(id)}</span>
                      <span className={`font-bold ${
                        place === 1 ? "text-yellow-400" : "text-gray-400"
                      }`}>
                        {place === 1 ? "🥇" : place === 2 ? "🥈" : place === 3 ? "��" : `#${place}`}
                      </span>
                    </div>
                  ))}
              </div>

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleLeaveGame}
                className="px-8 py-3 bg-purple-600 hover:bg-purple-500 rounded-xl font-bold text-white"
              >
                Return to Home
              </motion.button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default MakaoGame;
