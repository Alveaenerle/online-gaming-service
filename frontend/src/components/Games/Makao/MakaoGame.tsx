import React, { useState, useMemo, useCallback, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import Navbar from "../../Shared/Navbar";
import Card from "./Card";
import Player from "./components/Player";
import DemandPicker from "./components/DemandPicker";
import DrawnCardModal from "./components/DrawnCardModal";
import GameOverModal from "./components/GameOverModal";
import MoveHistoryPanel from "./components/MoveHistoryPanel";
import EffectToast from "./components/EffectToast";
import { useCardAnimations, CardAnimationManager, AnimatedCardPile } from "./components/AnimatedCard";
import { useMakaoSocket } from "./hooks/useMakaoSocket";
import { useMakaoActions } from "./hooks/useMakaoActions";
import { useAuth } from "../../../context/AuthContext";
import { useLobby } from "../../../context/LobbyContext";
import {
  Card as CardType,
  CardSuit,
  CardRank,
  PlayerCardView,
  PlayerView,
  DemandType,
} from "./types";
import {
  buildPlayerViews,
  distributePlayersAroundTable,
  RANK_DISPLAY,
  SUIT_INFO,
  requiresDemand,
  getPlayerDisplayName,
  isBot,
} from "./utils/cardHelpers";

// ============================================
// Main Makao Game Component
// ============================================

const MakaoGame: React.FC = () => {
  const { user } = useAuth();
  const { clearLobby } = useLobby();
  const navigate = useNavigate();

  // WebSocket connection and game state
  const { gameState, isConnected, connectionError } = useMakaoSocket();

  // Game actions
  const {
    playCard,
    drawCard,
    playDrawnCard,
    skipDrawnCard,
    acceptEffect,
    isLoading,
    error: actionError,
    clearError,
  } = useMakaoActions();

  // Card animations hook
  const {
    animations,
    addPlayAnimation,
    addDrawAnimation,
    removeAnimation,
  } = useCardAnimations();

  // Refs for animation positions
  const gameTableRef = useRef<HTMLDivElement>(null);
  const deckRef = useRef<HTMLDivElement>(null);
  const discardRef = useRef<HTMLDivElement>(null);
  const handRef = useRef<HTMLDivElement>(null);
  const playerRefsMap = useRef<Map<string, React.RefObject<HTMLDivElement | null>>>(new Map());

  // Local UI state
  const [pendingCard, setPendingCard] = useState<CardType | null>(null);
  const [demandType, setDemandType] = useState<DemandType | null>(null);
  const [drawnCardInfo, setDrawnCardInfo] = useState<{
    card: CardType;
    canPlay: boolean;
  } | null>(null);
  const [message, setMessage] = useState<string>("");
  const [previousCardPlayed, setPreviousCardPlayed] = useState<CardType | null>(null);

  // Helper to get or create a ref for a player
  const getPlayerRef = useCallback((playerId: string) => {
    if (!playerRefsMap.current.has(playerId)) {
      playerRefsMap.current.set(playerId, React.createRef<HTMLDivElement>());
    }
    return playerRefsMap.current.get(playerId)!;
  }, []);

  // Redirect if not authenticated
  useEffect(() => {
    if (!user?.id) {
      navigate("/login");
      return;
    }
  }, [user?.id, navigate]);

  // Detect card plays for animation (when currentCard changes)
  useEffect(() => {
    if (gameState?.currentCard && previousCardPlayed) {
      const cardChanged =
        gameState.currentCard.rank !== previousCardPlayed.rank ||
        gameState.currentCard.suit !== previousCardPlayed.suit;

      if (cardChanged) {
        // Trigger play animation for the new card
        addPlayAnimation(gameState.currentCard);
      }
    }
    setPreviousCardPlayed(gameState?.currentCard || null);
  }, [gameState?.currentCard]);

  // Build player views from game state
  const players = useMemo<PlayerView[]>(() => {
    if (!gameState || !user?.id) return [];
    return buildPlayerViews(gameState, user.id);
  }, [gameState, user?.id]);

  // Distribute players around table
  const { me, others } = useMemo(() => {
    return distributePlayersAroundTable(players, user?.id || "");
  }, [players, user?.id]);

  // Game state derived values
  const isMyTurn = gameState?.activePlayerId === user?.id;
  const isGameOver = gameState?.status === "FINISHED";
  const hasSpecialEffect = gameState?.specialEffectActive;

  // Get bot thinking player ID from game state
  const botThinkingPlayerId = gameState?.botThinkingPlayerId || null;

  // Get MAKAO player ID from game state
  const makaoPlayerId = gameState?.makaoPlayerId || null;

  // Get turn timer
  const turnRemainingSeconds = gameState?.turnRemainingSeconds ?? null;

  // Check if a card can be played
  const canPlayCard = useCallback(
    (cardView: PlayerCardView): boolean => {
      if (!isMyTurn || isLoading || drawnCardInfo) return false;
      return cardView.playable;
    },
    [isMyTurn, isLoading, drawnCardInfo]
  );

  // Handle card click from hand
  const handleCardClick = useCallback(
    async (cardView: PlayerCardView) => {
      if (!canPlayCard(cardView)) return;

      const card = cardView.card;
      const demand = requiresDemand(card);

      if (demand) {
        // Card requires demand selection - show picker
        setPendingCard(card);
        setDemandType(demand);
        return;
      }

      // Trigger play animation (positions calculated by CardAnimationManager using refs)
      addPlayAnimation(card, undefined, user?.id);

      // Play card directly
      const success = await playCard(card);
      if (success) {
        setMessage(`Played ${RANK_DISPLAY[card.rank]} of ${SUIT_INFO[card.suit].name}`);
      }
    },
    [canPlayCard, playCard, addPlayAnimation, user?.id]
  );

  // Handle demand selection (for Ace/Jack)
  const handleDemandSelect = useCallback(
    async (value: CardSuit | CardRank) => {
      if (!pendingCard) return;

      let success = false;

      if (demandType === "suit") {
        success = await playCard(pendingCard, value as CardSuit, null);
      } else if (demandType === "rank") {
        success = await playCard(pendingCard, null, value as CardRank);
      }

      if (success) {
        const demandDisplay =
          demandType === "suit"
            ? SUIT_INFO[value as CardSuit].name
            : RANK_DISPLAY[value as CardRank];
        setMessage(
          `Played ${RANK_DISPLAY[pendingCard.rank]} - demanded ${demandDisplay}`
        );
      }

      setPendingCard(null);
      setDemandType(null);
    },
    [pendingCard, demandType, playCard]
  );

  // Cancel demand selection
  const handleDemandCancel = useCallback(() => {
    setPendingCard(null);
    setDemandType(null);
  }, []);

  // Handle draw card
  const handleDrawCard = useCallback(async () => {
    if (!isMyTurn || isLoading || drawnCardInfo) return;

    // If special effect is active, need to accept it first
    if (hasSpecialEffect) {
      const success = await acceptEffect();
      if (success) {
        setMessage("Effect accepted");
      }
      return;
    }

    const result = await drawCard();
    if (result) {
      // Trigger draw animation (positions calculated by CardAnimationManager using refs)
      addDrawAnimation(result.drawnCard, undefined, user?.id);

      setDrawnCardInfo({
        card: result.drawnCard,
        canPlay: result.playable,
      });
      setMessage(
        `Drew ${RANK_DISPLAY[result.drawnCard.rank]} of ${SUIT_INFO[result.drawnCard.suit].name}`
      );
    }
  }, [isMyTurn, isLoading, drawnCardInfo, hasSpecialEffect, acceptEffect, drawCard, addDrawAnimation, user?.id]);

  // Handle playing drawn card
  const handlePlayDrawnCard = useCallback(async () => {
    if (!drawnCardInfo) return;

    const card = drawnCardInfo.card;
    const demand = requiresDemand(card);

    if (demand) {
      // Need to select demand first
      setPendingCard(card);
      setDemandType(demand);
      setDrawnCardInfo(null);
      return;
    }

    const success = await playDrawnCard();
    if (success) {
      setMessage(`Played drawn card: ${RANK_DISPLAY[card.rank]}`);
    }
    setDrawnCardInfo(null);
  }, [drawnCardInfo, playDrawnCard]);

  // Handle skipping after draw
  const handleSkipDrawnCard = useCallback(async () => {
    const success = await skipDrawnCard();
    if (success) {
      setMessage("Kept card, turn ended");
    }
    setDrawnCardInfo(null);
  }, [skipDrawnCard]);

  // Handle leaving game
  const handleLeaveGame = useCallback(() => {
    clearLobby();
    navigate("/makao");
  }, [clearLobby, navigate]);

  // Clear action error after timeout
  useEffect(() => {
    if (actionError) {
      setMessage(actionError);
      const timeout = setTimeout(() => {
        clearError();
      }, 5000);
      return () => clearTimeout(timeout);
    }
  }, [actionError, clearError]);

  // ============================================
  // Loading State
  // ============================================
  if (!gameState) {
    return (
      <div className="min-h-screen bg-bg text-white antialiased flex items-center justify-center">
        <Navbar />
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purpleEnd mx-auto mb-4" />
          <p className="text-white text-lg">
            {isConnected ? "Waiting for game to start..." : "Connecting to game server..."}
          </p>
          {connectionError && (
            <p className="text-red-400 mt-2 text-sm">{connectionError}</p>
          )}
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => navigate("/makao")}
            className="mt-6 px-6 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-white text-sm"
          >
            Back to Lobby
          </motion.button>
        </div>
      </div>
    );
  }

  // ============================================
  // Game Render
  // ============================================
  return (
    <div className="min-h-screen bg-bg text-white antialiased overflow-hidden">
      <Navbar />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)]" />

      {/* Effect Toast Notifications */}
      <EffectToast
        effectNotification={gameState.effectNotification}
        specialEffectActive={hasSpecialEffect || false}
        demandedSuit={gameState.demandedSuit}
        demandedRank={gameState.demandedRank ? RANK_DISPLAY[gameState.demandedRank] : null}
        isMyTurn={isMyTurn || false}
      />

      {/* Card Animation Layer - uses refs for accurate positioning */}
      <CardAnimationManager
        animations={animations}
        onAnimationComplete={removeAnimation}
        deckRef={deckRef}
        discardRef={discardRef}
        handRef={handRef}
        playerRefs={playerRefsMap.current}
      />

      <main className="pt-36 pb-4 px-4 h-[calc(100vh-144px)]">
        <div className="h-full max-w-[2000px] mx-auto flex gap-2 justify-center">
          {/* Move History Panel - Left Sidebar */}
          <div className="w-64 flex-shrink-0 hidden xl:block">
            <MoveHistoryPanel
              moveHistory={gameState.moveHistory || []}
              lastMoveLog={gameState.lastMoveLog}
            />
          </div>

          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center" ref={gameTableRef}>
            <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purpleEnd/20 shadow-2xl shadow-black/50 p-8">
              {/* Table glow */}
              <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.1),_transparent_60%)]" />

              {/* Other Players - Top */}
              {others.filter((p) => p.position === "top").length > 0 && (
                <div className="absolute top-3 left-1/2 -translate-x-1/2 flex gap-3">
                  {others
                    .filter((p) => p.position === "top")
                    .map((player) => (
                      <Player
                        key={player.id}
                        ref={getPlayerRef(player.id)}
                        player={player}
                        isBot={isBot(player.id)}
                        isBotThinking={botThinkingPlayerId === player.id}
                        hasMakao={makaoPlayerId === player.id}
                        turnRemainingSeconds={player.isActive ? turnRemainingSeconds : null}
                      />
                    ))}
                </div>
              )}

              {/* Other Players - Left */}
              {others.filter((p) => p.position === "left").length > 0 && (
                <div className="absolute left-3 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {others
                    .filter((p) => p.position === "left")
                    .map((player) => (
                      <Player
                        key={player.id}
                        ref={getPlayerRef(player.id)}
                        player={player}
                        isBot={isBot(player.id)}
                        isBotThinking={botThinkingPlayerId === player.id}
                        hasMakao={makaoPlayerId === player.id}
                        turnRemainingSeconds={player.isActive ? turnRemainingSeconds : null}
                      />
                    ))}
                </div>
              )}

              {/* Other Players - Right */}
              {others.filter((p) => p.position === "right").length > 0 && (
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {others
                    .filter((p) => p.position === "right")
                    .map((player) => (
                      <Player
                        key={player.id}
                        ref={getPlayerRef(player.id)}
                        player={player}
                        isBot={isBot(player.id)}
                        isBotThinking={botThinkingPlayerId === player.id}
                        hasMakao={makaoPlayerId === player.id}
                        turnRemainingSeconds={player.isActive ? turnRemainingSeconds : null}
                      />
                    ))}
                </div>
              )}

              {/* Center - Deck & Discard */}
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center gap-12">
                {/* Draw Pile - with animation support */}
                <div
                  ref={deckRef}
                  className="text-center"
                  onClick={
                    isMyTurn && !drawnCardInfo && !hasSpecialEffect
                      ? handleDrawCard
                      : undefined
                  }
                >
                  <AnimatedCardPile
                    count={gameState.drawDeckCardsAmount}
                    type="draw"
                    isClickable={isMyTurn && !drawnCardInfo && !hasSpecialEffect}
                    showGlow={isMyTurn && !drawnCardInfo && !hasSpecialEffect}
                    onClick={handleDrawCard}
                  />
                  <p className="text-xs text-white/60 mt-2">Draw Deck</p>
                </div>

                {/* Discard Pile - with animation support */}
                <div ref={discardRef} className="text-center">
                  <AnimatedCardPile
                    count={gameState.discardDeckCardsAmount}
                    type="discard"
                    topCard={gameState.currentCard}
                  />
                  <p className="text-xs text-white/60 mt-2">Discard Pile</p>
                </div>
              </div>

              {/* Accept Effect Button (centered above discard) */}
              {hasSpecialEffect && isMyTurn && (
                <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 mt-20">
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => acceptEffect()}
                    disabled={isLoading}
                    className="px-6 py-3 rounded-xl bg-orange-500/80 hover:bg-orange-500 text-white font-bold shadow-lg animate-pulse"
                  >
                    Accept Effect
                  </motion.button>
                </div>
              )}

              {/* My Cards - Bottom */}
              {me && (
                <div
                  ref={handRef}
                  className="absolute bottom-3 left-1/2 -translate-x-1/2 max-w-[90%]"
                >
                  <motion.div
                    layout
                    className={`p-3 rounded-xl ${
                      isMyTurn
                        ? "bg-purpleEnd/20 border border-purpleEnd shadow-lg shadow-purpleEnd/20"
                        : "bg-[#1a1a27]"
                    } transition-all duration-300`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <p className="text-xs text-gray-400">
                        {isMyTurn ? (
                          <span className="flex items-center gap-1.5">
                            <motion.span
                              animate={{ scale: [1, 1.2, 1] }}
                              transition={{ duration: 1, repeat: Infinity }}
                              className="w-2 h-2 rounded-full bg-green-500"
                            />
                            Your turn!
                          </span>
                        ) : (
                          "Waiting for your turn..."
                        )}
                      </p>
                      <p className="text-xs text-gray-500">
                        {gameState.myCards.length} cards
                      </p>
                    </div>
                    <div className="flex gap-1 flex-wrap justify-center">
                      <AnimatePresence mode="popLayout">
                        {gameState.myCards.map((cardView, index) => (
                          <motion.div
                            key={`${cardView.card.suit}-${cardView.card.rank}-${index}`}
                            layout
                            initial={{ opacity: 0, scale: 0.8, y: 20 }}
                            animate={{ opacity: 1, scale: 1, y: 0 }}
                            exit={{ opacity: 0, scale: 0.5, y: -20 }}
                            transition={{
                              type: "spring",
                              stiffness: 400,
                              damping: 25,
                            }}
                            whileHover={canPlayCard(cardView) ? { scale: 1.1, y: -12 } : {}}
                            className={
                              canPlayCard(cardView)
                                ? "cursor-pointer"
                                : ""
                            }
                            onClick={() => handleCardClick(cardView)}
                          >
                            <Card
                              card={cardView.card}
                              size="md"
                              isPlayable={canPlayCard(cardView)}
                              showEffect
                            />
                          </motion.div>
                        ))}
                      </AnimatePresence>
                    </div>
                  </motion.div>
                </div>
              )}
            </div>
          </div>

          {/* Side Panel */}
          <div className="w-64 flex flex-col gap-3 flex-shrink-0">
            {/* Navigation */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleLeaveGame}
                className="w-full py-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-red-400 text-sm font-medium transition-colors flex items-center justify-center gap-2 border border-red-500/30"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                  />
                </svg>
                Leave Game
              </motion.button>
            </div>

            {/* Latest Activity - Compact */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <h3 className="text-xs font-medium text-gray-500 mb-2">
                Latest Action
              </h3>
              <div className="min-h-[40px] flex items-center">
                <AnimatePresence mode="wait">
                  <motion.p
                    key={gameState.lastMoveLog || message}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 10 }}
                    className={`text-sm ${
                      actionError ? "text-red-400" : "text-white"
                    }`}
                  >
                    {gameState.lastMoveLog || message || "Game in progress..."}
                  </motion.p>
                </AnimatePresence>
              </div>
            </div>

            {/* Game Status */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <h3 className="text-xs font-medium text-gray-500 mb-3">
                Game Status
              </h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">Room</span>
                  <span className="text-white font-mono">
                    {gameState.roomId?.slice(0, 8)}
                  </span>
                </div>

                {gameState.specialEffectActive && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Special Effect</span>
                    <span className="text-orange-400 animate-pulse">
                      Active!
                    </span>
                  </div>
                )}

                {gameState.demandedSuit && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Demanded Suit</span>
                    <span
                      className={`font-bold ${SUIT_INFO[gameState.demandedSuit].color}`}
                    >
                      {SUIT_INFO[gameState.demandedSuit].symbol}{" "}
                      {SUIT_INFO[gameState.demandedSuit].name}
                    </span>
                  </div>
                )}

                {gameState.demandedRank && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Demanded Rank</span>
                    <span className="text-purpleEnd font-bold">
                      {RANK_DISPLAY[gameState.demandedRank]}
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Current Turn */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <h3 className="text-xs font-medium text-gray-500 mb-2">
                Current Turn
              </h3>
              <div className="flex items-center gap-2">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm ${
                    isMyTurn
                      ? "bg-gradient-to-br from-purpleStart to-purpleEnd text-white"
                      : "bg-gray-700 text-gray-300"
                  }`}
                >
                  {(
                    players.find((p) => p.id === gameState.activePlayerId)
                      ?.username || "?"
                  )
                    .charAt(0)
                    .toUpperCase()}
                </div>
                <div>
                  <p className="text-white text-sm font-medium">
                    {isMyTurn
                      ? "Your Turn"
                      : getPlayerDisplayName(gameState.activePlayerId)}
                  </p>
                  <p className="text-[10px] text-gray-500">
                    {isMyTurn ? "Play a card or draw" : "Waiting..."}
                  </p>
                </div>
                {isMyTurn && (
                  <motion.div
                    animate={{ opacity: [0.5, 1, 0.5] }}
                    transition={{ duration: 1.5, repeat: Infinity }}
                    className="w-2.5 h-2.5 rounded-full bg-purpleEnd ml-auto"
                  />
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* ============================================ */}
      {/* Modals */}
      {/* ============================================ */}

      {/* Demand Picker Modal */}
      <AnimatePresence>
        {demandType && (
          <DemandPicker
            type={demandType}
            onSelect={handleDemandSelect}
            onCancel={handleDemandCancel}
            isLoading={isLoading}
          />
        )}
      </AnimatePresence>

      {/* Drawn Card Modal */}
      <AnimatePresence>
        {drawnCardInfo && (
          <DrawnCardModal
            card={drawnCardInfo.card}
            canPlay={drawnCardInfo.canPlay}
            onPlay={handlePlayDrawnCard}
            onSkip={handleSkipDrawnCard}
            isLoading={isLoading}
          />
        )}
      </AnimatePresence>

      {/* Game Over Modal */}
      <AnimatePresence>
        {isGameOver && (
          <GameOverModal
            players={players}
            myUserId={user?.id || ""}
            onPlayAgain={() => navigate("/makao")}
          />
        )}
      </AnimatePresence>
    </div>
  );
};

export default MakaoGame;