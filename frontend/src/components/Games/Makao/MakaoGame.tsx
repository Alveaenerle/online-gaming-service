import React, { useState, useMemo, useCallback, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import Navbar from "../../Shared/Navbar";
import Card from "./Card";
import Player from "./components/Player";
import DemandPicker from "./components/DemandPicker";
import DrawnCardModal from "./components/DrawnCardModal";
import GameOverModal from "./components/GameOverModal";
import TimeoutModal from "./components/TimeoutModal";
import SidebarNotifications from "./components/SidebarNotifications";
import DiscardPile from "./components/DiscardPile";
import { ChatWidget } from "../shared/ChatWidget";
import { useCardAnimations, CardAnimationManager, AnimatedCardPile } from "./components/AnimatedCard";
import { useMakaoSocket } from "./hooks/useMakaoSocket";
import { useMakaoActions } from "./hooks/useMakaoActions";
import { useTurnTimer } from "./hooks/useTurnTimer";
import { useGameSounds } from "./utils/soundEffects";
import { useAuth } from "../../../context/AuthContext";
import { useLobby } from "../../../context/LobbyContext";
import makaoGameService from "../../../services/makaoGameService";
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

  // Sounds
  const {
    playCardSound,
    drawCardSound,
    playTurnStartSound,
    playMakaoSound
  } = useGameSounds();

  // WebSocket connection and game state
  const { gameState, isConnected, connectionError, resetState, wasKickedByTimeout, clearTimeoutStatus, reconnect } = useMakaoSocket();


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
  const [previousCardPlayed, setPreviousCardPlayed] = useState<CardType | null>(null);
  const [showTimeoutModal, setShowTimeoutModal] = useState(false);
  const [wasReplacedByBot, setWasReplacedByBot] = useState(false);

  // Track previous active player to know who played the card
  const prevActivePlayerIdRef = useRef<string | null>(null);
  // Track previous turn to detecting turn start sfx
  const wasMyTurnRef = useRef<boolean>(false);
  // Track if user was previously in the game (to detect being kicked)
  const wasInGameRef = useRef<boolean>(false);

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

  // Handle browser close/refresh - notify backend to clean up player state
  useEffect(() => {
    const handleBeforeUnload = () => {
      // Use fetch with keepalive for reliable delivery during page unload
      // keepalive allows the request to outlive the page
      try {
        fetch("/api/makao/leave-game", {
          method: "POST",
          credentials: "include",
          keepalive: true,
          headers: {
            "Content-Type": "application/json",
          },
        });
      } catch {
        // Ignore errors during unload
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, []);

  // Handle Game State Changes (Animations, Sound)
  useEffect(() => {
    if (!gameState) return;

    // 1. Detect card plays for animation
    if (gameState.currentCard && previousCardPlayed) {
      const cardChanged =
        gameState.currentCard.rank !== previousCardPlayed.rank ||
        gameState.currentCard.suit !== previousCardPlayed.suit;

      if (cardChanged) {
        // The player who played is likely the one who WAS active
        // If I am the one who played, I handled it in handleCardClick
        // But for consistency and opponents, we can trigger here if not already handled
        // Note: handleCardClick triggers animation immediately for better UX.
        // We should avoid double animation for self.

        const whoPlayed = prevActivePlayerIdRef.current;
        if (whoPlayed && whoPlayed !== user?.id) {
            addPlayAnimation(gameState.currentCard, undefined, whoPlayed);
            playCardSound();
        }
      }
    }

    // Update trackers
    setPreviousCardPlayed(gameState.currentCard || null);
    prevActivePlayerIdRef.current = gameState.activePlayerId;

    // 2. Detect Turn Start
    const isNowMyTurn = gameState.activePlayerId === user?.id;
    if (isNowMyTurn && !wasMyTurnRef.current) {
        playTurnStartSound();
    }
    wasMyTurnRef.current = isNowMyTurn;

    // 3. Detect Makao
    if (gameState.makaoPlayerId) {
        playMakaoSound();
    }

    // 4. Detect if user was replaced by a bot (kicked due to timeout) - fallback detection
    // Primary detection is via wasKickedByTimeout from WebSocket, this is a backup
    const isUserInGame = gameState.playersCardsAmount && user?.id && user.id in gameState.playersCardsAmount;
    const isUserInLosers = gameState.losers?.includes(user?.id || "");

    if (wasInGameRef.current && !isUserInGame && !isGameOver && isUserInLosers && !wasKickedByTimeout) {
      // User was in game but now they're not (and game isn't over) - they got replaced
      setWasReplacedByBot(true);
      setShowTimeoutModal(true);
    }

    // Update tracking ref
    if (isUserInGame) {
      wasInGameRef.current = true;
    }

  }, [gameState, previousCardPlayed, user?.id, addPlayAnimation, playCardSound, playTurnStartSound, playMakaoSound, wasKickedByTimeout]);

  // Show timeout modal when kicked via WebSocket notification
  useEffect(() => {
    if (wasKickedByTimeout) {
      setShowTimeoutModal(true);
      setWasReplacedByBot(true);
    }
  }, [wasKickedByTimeout]);

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

  // Determine if active player is a bot
  const isActivePlayerBot = gameState?.activePlayerId ? isBot(gameState.activePlayerId) : false;

  // Turn timer - uses local countdown for smooth UI
  const { remainingSeconds: turnRemainingSeconds } = useTurnTimer({
    activePlayerId: gameState?.activePlayerId,
    serverRemainingSeconds: gameState?.turnRemainingSeconds,
    isActivePlayerBot,
    isGamePlaying: gameState?.status === "PLAYING",
  });

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

      // Trigger play animation immediately for self
      addPlayAnimation(card, undefined, user?.id);
      playCardSound();

      // Play card directly
      await playCard(card);
    },
    [canPlayCard, playCard, addPlayAnimation, user?.id, playCardSound]
  );

  // Handle demand selection (for Ace/Jack)
  const handleDemandSelect = useCallback(
    async (value: CardSuit | CardRank) => {
      if (!pendingCard) return;

      // Trigger play animation for the pending card
      addPlayAnimation(pendingCard, undefined, user?.id);
      playCardSound();

      if (demandType === "suit") {
        await playCard(pendingCard, value as CardSuit, null);
      } else if (demandType === "rank") {
        await playCard(pendingCard, null, value as CardRank);
      }

      setPendingCard(null);
      setDemandType(null);

    },
    [pendingCard, demandType, playCard, playCardSound, addPlayAnimation, user?.id]
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
      await acceptEffect();
      return;
    }

    const result = await drawCard();
    if (result) {
      // Trigger draw animation
      addDrawAnimation(result.drawnCard, undefined, user?.id);
      drawCardSound();

      setDrawnCardInfo({
        card: result.drawnCard,
        canPlay: result.playable,
      });
    }
  }, [isMyTurn, isLoading, drawnCardInfo, hasSpecialEffect, acceptEffect, drawCard, addDrawAnimation, user?.id, drawCardSound]);

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

    // Trigger animation for playing the drawn card
    addPlayAnimation(card, undefined, user?.id);
    playCardSound();
    await playDrawnCard();
    setDrawnCardInfo(null);
  }, [drawnCardInfo, playDrawnCard, playCardSound, addPlayAnimation, user?.id]);

  // Handle skipping after draw
  const handleSkipDrawnCard = useCallback(async () => {
    await skipDrawnCard();
    setDrawnCardInfo(null);
  }, [skipDrawnCard]);

  // Handle leaving game (mid-game)
  const handleLeaveGame = useCallback(async () => {
    console.log("[MakaoGame] Leaving game mid-session");
    try {
      // Notify backend so player gets replaced by bot
      await makaoGameService.leaveGame();
    } catch (err) {
      console.error("[MakaoGame] Failed to notify backend of leave:", err);
      // Continue with navigation even if API call fails
    }
    resetState();
    clearLobby();
    navigate("/makao");
  }, [resetState, clearLobby, navigate]);

  // Reset all local UI state (for new game)
  const resetLocalState = useCallback(() => {
    console.log("[MakaoGame] Resetting local UI state");
    setPendingCard(null);
    setDemandType(null);
    setDrawnCardInfo(null);
    setPreviousCardPlayed(null);
    setShowTimeoutModal(false);
    setWasReplacedByBot(false);
    prevActivePlayerIdRef.current = null;
    wasMyTurnRef.current = false;
    wasInGameRef.current = false;
    playerRefsMap.current.clear();
  }, []);

  // Handle "Play Again" - return to lobby with same players
  const handlePlayAgain = useCallback(() => {
    console.log("[MakaoGame] Play Again clicked - resetting for new game");
    // Reset game-specific state
    resetLocalState();
    resetState(); // Reset WebSocket game state
    // Clear the lobby so user can join a new one
    clearLobby();
    // Navigate back to makao page to join/create new lobby
    navigate("/makao");
  }, [resetLocalState, resetState, clearLobby, navigate]);

  // Handle "Exit to Menu" - leave completely
  const handleExitToMenu = useCallback(() => {
    console.log("[MakaoGame] Exit to Menu clicked");
    resetLocalState();
    resetState();
    clearLobby(); // Clear the lobby context entirely
    navigate("/home");
  }, [resetLocalState, resetState, clearLobby, navigate]);

  // Warning for errors
  useEffect(() => {
    if (actionError) {
       // We rely on standard toast or console for now, or could pass to sidebar
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
        <div className="h-full max-w-[2000px] mx-auto flex gap-4 justify-center">

          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center" ref={gameTableRef}>
            <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purpleEnd/20 shadow-2xl shadow-black/50 p-8">
              {/* Table glow */}
              <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.1),_transparent_60%)]" />

              {/* Other Players - Circular Layout for 2-8 players */}

              {/* Top Position */}
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

              {/* Top-Left Position */}
              {others.filter((p) => p.position === "top-left").length > 0 && (
                <div className="absolute top-6 left-[15%] flex gap-2">
                  {others
                    .filter((p) => p.position === "top-left")
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

              {/* Top-Right Position */}
              {others.filter((p) => p.position === "top-right").length > 0 && (
                <div className="absolute top-6 right-[15%] flex gap-2">
                  {others
                    .filter((p) => p.position === "top-right")
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

              {/* Left Position */}
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

              {/* Right Position */}
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

              {/* Bottom-Left Position */}
              {others.filter((p) => p.position === "bottom-left").length > 0 && (
                <div className="absolute bottom-20 left-[10%] flex gap-2">
                  {others
                    .filter((p) => p.position === "bottom-left")
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

              {/* Bottom-Right Position */}
              {others.filter((p) => p.position === "bottom-right").length > 0 && (
                <div className="absolute bottom-20 right-[10%] flex gap-2">
                  {others
                    .filter((p) => p.position === "bottom-right")
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
                    isClickable={isMyTurn && !drawnCardInfo && !hasSpecialEffect}
                    showGlow={isMyTurn && !drawnCardInfo && !hasSpecialEffect}
                    onClick={handleDrawCard}
                  />
                  <p className="text-xs text-white/60 mt-2">Draw Deck</p>
                </div>

                {/* Discard Pile - with animation support */}
                <div ref={discardRef} className="text-center">
                  <DiscardPile
                    topCard={gameState.currentCard}
                    count={gameState.discardDeckCardsAmount}
                  />
                  <p className="text-xs text-white/60 mt-2">Discard Pile</p>
                </div>
              </div>

              {/* Accept Effect HUD (Positioned lower) */}
              {hasSpecialEffect && isMyTurn && (
                <div className="absolute top-[65%] left-1/2 -translate-x-1/2 z-50">
                  <motion.div
                    initial={{ y: 20, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    className="flex flex-col items-center gap-3"
                  >
                    {/* Penalty Indicator */}
                    <div className={`px-6 py-2 rounded-full font-bold uppercase tracking-wider text-sm shadow-xl border ${
                      gameState.effectNotification?.includes("draw")
                        ? "bg-red-500/90 border-red-400 text-white"
                        : "bg-amber-500/90 border-amber-400 text-black"
                    }`}>
                      {gameState.effectNotification || "Special Effect Active"}
                    </div>

                    {/* Accept Button */}
                    <motion.button
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => acceptEffect()}
                      disabled={isLoading}
                      className="px-8 py-3 rounded-xl bg-gray-900/90 hover:bg-gray-800 text-white font-bold border border-white/20 shadow-lg backdrop-blur mx-auto"
                    >
                      Accept & Continue
                    </motion.button>
                  </motion.div>
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
          <div className="w-80 flex flex-col gap-3 flex-shrink-0 h-full overflow-hidden">
            {/* Top Section: Navigation & Status */}
            <div className="flex flex-col gap-3 flex-shrink-0">
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

               {/* Current Turn with Timer */}
               <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
                 <h3 className="text-xs font-medium text-gray-500 mb-2">
                   Current Turn
                 </h3>
                 <div className="flex items-center gap-2">
                   {/* Avatar with Timer Ring */}
                   <div className="relative">
                     {/* Timer Ring - only show for human players */}
                     {turnRemainingSeconds != null && !isActivePlayerBot && (
                       <svg
                         className="absolute inset-0 -rotate-90"
                         width={36}
                         height={36}
                         viewBox="0 0 36 36"
                       >
                         {/* Background ring */}
                         <circle
                           cx={18}
                           cy={18}
                           r={16}
                           fill="none"
                           stroke="rgba(255,255,255,0.1)"
                           strokeWidth={2.5}
                         />
                         {/* Progress ring */}
                         <motion.circle
                           cx={18}
                           cy={18}
                           r={16}
                           fill="none"
                           stroke={turnRemainingSeconds <= 5 ? "#ef4444" : turnRemainingSeconds <= 10 ? "#f97316" : "#8b5cf6"}
                           strokeWidth={2.5}
                           strokeLinecap="round"
                           strokeDasharray={2 * Math.PI * 16}
                           strokeDashoffset={(2 * Math.PI * 16) * (1 - Math.max(0, Math.min(1, turnRemainingSeconds / 60)))}
                           animate={{
                             strokeDashoffset: (2 * Math.PI * 16) * (1 - Math.max(0, Math.min(1, turnRemainingSeconds / 60))),
                             stroke: turnRemainingSeconds <= 5 ? "#ef4444" : turnRemainingSeconds <= 10 ? "#f97316" : "#8b5cf6",
                           }}
                           transition={{ duration: 0.3, ease: "linear" }}
                         />
                       </svg>
                     )}
                     <div
                       className={`w-9 h-9 rounded-full overflow-hidden border-2 ${
                         turnRemainingSeconds != null && turnRemainingSeconds <= 10
                           ? "border-red-500"
                           : isActivePlayerBot
                           ? "border-cyan-500"
                           : "border-purpleEnd"
                       }`}
                     >
                       <img
                         src={
                           players.find((p) => p.id === gameState.activePlayerId)?.avatarUrl ||
                           (isActivePlayerBot ? "/avatars/bot_avatar.svg" : "/avatars/avatar_1.png")
                         }
                         alt="Active Player"
                         className="w-full h-full object-cover"
                         onError={(e) => {
                           const target = e.target as HTMLImageElement;
                           target.src = isActivePlayerBot
                             ? "/avatars/bot_avatar.svg"
                             : "/avatars/avatar_1.png";
                         }}
                       />
                     </div>
                   </div>
                   <div className="flex-1 min-w-0">
                     <p className="text-white text-sm font-medium truncate">
                       {isMyTurn
                         ? "Your Turn"
                         : players.find((p) => p.id === gameState.activePlayerId)?.username ||
                           getPlayerDisplayName(gameState.activePlayerId)}
                     </p>
                     <p className="text-[10px] text-gray-500">
                       {isMyTurn ? "Play a card or draw" : isActivePlayerBot ? "Bot thinking..." : "Waiting..."}
                     </p>
                   </div>
                   {/* Timer display / Active indicator */}
                   {turnRemainingSeconds != null && !isActivePlayerBot ? (
                     <motion.div
                       animate={turnRemainingSeconds <= 10 ? { opacity: [1, 0.5, 1] } : {}}
                       transition={{ duration: 0.5, repeat: Infinity }}
                       className={`text-sm font-mono font-bold ml-auto ${
                         turnRemainingSeconds <= 5
                           ? "text-red-400"
                           : turnRemainingSeconds <= 10
                           ? "text-orange-400"
                           : "text-gray-400"
                       }`}
                     >
                       {turnRemainingSeconds}s
                     </motion.div>
                   ) : isMyTurn ? (
                     <motion.div
                       animate={{ opacity: [0.5, 1, 0.5] }}
                       transition={{ duration: 1.5, repeat: Infinity }}
                       className="w-2.5 h-2.5 rounded-full bg-purpleEnd ml-auto"
                     />
                   ) : null}
                 </div>
               </div>
            </div>

            {/* Bottom Section: Notifications & Alerts */}
            {/* "New Alert Position: Move all game alerts/notifications to the right-hand side, positioned vertically below the side panels." */}
            <div className="flex-1 min-h-0 flex flex-col justify-end">
               <SidebarNotifications
                  effectNotification={gameState.effectNotification}
                  specialEffectActive={hasSpecialEffect || false}
                  demandedSuit={gameState.demandedSuit}
                  demandedRank={gameState.demandedRank}
                  isMyTurn={isMyTurn || false}
                  lastMoveLog={gameState.lastMoveLog}
               />
            </div>
          </div>
        </div>
      </main>

      {/* Chat Widget - preserves chat history from lobby */}
      <ChatWidget />

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
            onPlayAgain={handlePlayAgain}
            onExitToMenu={handleExitToMenu}
          />
        )}
      </AnimatePresence>

      {/* Timeout/Kicked Modal */}
      <TimeoutModal
        isOpen={showTimeoutModal}
        onClose={() => {
          setShowTimeoutModal(false);
          clearTimeoutStatus();
        }}
        onReturnToLobby={() => {
          setShowTimeoutModal(false);
          clearTimeoutStatus();
          clearLobby();
          navigate("/makao");
        }}
      />
    </div>
  );
};

export default MakaoGame;