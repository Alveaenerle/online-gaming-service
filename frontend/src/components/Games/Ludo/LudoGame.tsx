import { useState, useEffect, useCallback, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { UserPlus, UserMinus } from "lucide-react";

// Shared Components
import Navbar from "../../Shared/Navbar";
import { SocialCenter } from "../../Shared/SocialCenter";
import { ConfirmationModal } from "../../Shared/ConfirmationModal";
import { ChatWidget } from "../shared/ChatWidget";

// Ludo Components
import { GameNotification } from "./GameNotification";
import { LudoBoard } from "./Board/LudoBoard";
import {
  TimeoutModal,
  GameOverModal,
  DiceWidget,
  CornerPlayerCard,
  CornerPlayerCardProps,
} from "./components";

// Hooks
import { useTurnTimer } from "./hooks/useTurnTimer";

// Context & Services
import { useLudo } from "../../../context/LudoGameContext";
import { useAuth } from "../../../context/AuthContext";
import { useLobby } from "../../../context/LobbyContext";
import { useSocial } from "../../../context/SocialContext";
import { useToast } from "../../../context/ToastContext";
import { lobbyService } from "../../../services/lobbyService";

import { RoomStatus, LudoPlayer } from "./types";

// ============================================
// Helper Functions
// ============================================

/**
 * Builds player views for corner positioning based on player color
 * Red = top-left, Blue = top-right, Yellow = bottom-right, Green = bottom-left
 */
function buildCornerPlayers(
  players: LudoPlayer[],
  usernames: Record<string, string>,
  currentUserId: string,
  currentPlayerId: string,
  hostUserId?: string
): CornerPlayerCardProps[] {
  return players.map((p, index) => ({
    id: p.userId,
    username: usernames[p.userId] || "Unknown",
    color: p.color,
    isActive: p.userId === currentPlayerId,
    isBot: p.isBot,
    isHost: p.userId === hostUserId,
    isMe: p.userId === currentUserId,
    pawnsInBase: p.pawns.filter((pawn) => pawn.inBase).length,
    pawnsOnBoard: p.pawns.filter((pawn) => !pawn.inBase && !pawn.inHome).length,
    pawnsInHome: p.pawns.filter((pawn) => pawn.inHome).length,
    avatarUrl: `/avatars/avatar_${(index % 4) + 1}.png`,
  }));
}

// ============================================
// Main Ludo Game Component
// ============================================

export function LudoArenaPage() {
  const { user } = useAuth();
  const { currentLobby, clearLobby } = useLobby();
  const { friends, sendFriendRequest } = useSocial();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const {
    gameState,
    movePawn,
    isMyTurn,
    isRolling,
    notification,
    notificationType,
  } = useLudo();

  // Determine if current user is the host
  const isHost = currentLobby?.hostUserId === user?.id;

  // Local UI state
  const [showMessage, setShowMessage] = useState(true);
  const [showTimeoutModal, setShowTimeoutModal] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);
  const [playerContextMenu, setPlayerContextMenu] = useState<{
    playerId: string;
    username: string;
    x: number;
    y: number;
  } | null>(null);

  // Game status flags
  const isGameOver = gameState?.status === RoomStatus.FINISHED || !!gameState?.winnerId;
  const isGamePlaying = gameState?.status === RoomStatus.IN_GAME;

  // Determine if active player is a bot
  const activePlayer = gameState?.players.find(p => p.userId === gameState?.currentPlayerId);
  const isActivePlayerBot = activePlayer?.isBot ?? false;

  // Turn timer hook
  const { remainingSeconds: turnRemainingSeconds } = useTurnTimer({
    activePlayerId: gameState?.currentPlayerId,
    isActivePlayerBot,
    isGamePlaying,
  });

  // Build corner player views (positioned by color)
  const cornerPlayers = useMemo(() => {
    if (!gameState || !user?.id) {
      return [];
    }
    return buildCornerPlayers(
      gameState.players,
      gameState.usernames,
      user.id,
      gameState.currentPlayerId,
      currentLobby?.hostUserId
    );
  }, [gameState, user?.id, currentLobby?.hostUserId]);

  // Get current user's player data
  const myPlayer = useMemo(() => {
    return cornerPlayers.find((p) => p.isMe) || null;
  }, [cornerPlayers]);

  // Redirect if not authenticated
  useEffect(() => {
    if (!user?.id) {
      navigate("/login");
    }
  }, [user?.id, navigate]);

  // Show notification when it changes
  useEffect(() => {
    if (notification) {
      setShowMessage(true);
    }
  }, [notification]);

  // Handle pawn click
  const handlePawnClick = useCallback(async (pawnId: number) => {
    await movePawn(pawnId);
  }, [movePawn]);

  const handlePawnMoveFinished = useCallback((pawnId: number) => {
    console.log(`[LudoGame] Pawn ${pawnId} move completed`);
  }, []);

  // Handle leaving game
  const handleLeaveGame = useCallback(async () => {
    console.log("[LudoGame] Leaving game");
    try {
      await lobbyService.leaveRoom();
    } catch (err) {
      console.error("[LudoGame] Failed to leave:", err);
    }
    clearLobby();
    navigate("/ludo");
  }, [clearLobby, navigate]);

  // Handle play again
  const handlePlayAgain = useCallback(() => {
    console.log("[LudoGame] Play Again clicked");
    clearLobby();
    navigate("/ludo");
  }, [clearLobby, navigate]);

  // Handle exit to menu
  const handleExitToMenu = useCallback(() => {
    console.log("[LudoGame] Exit to Menu clicked");
    clearLobby();
    navigate("/home");
  }, [clearLobby, navigate]);

  // Handle player click (for context menu)
  const handlePlayerClick = useCallback(
    (e: React.MouseEvent, playerId: string, username: string) => {
      if (playerId === user?.id) return;
      if (username.startsWith("Guest_")) return;

      e.preventDefault();
      setPlayerContextMenu({
        playerId,
        username,
        x: e.clientX,
        y: e.clientY,
      });
    },
    [user?.id]
  );

  // Check if a player is already a friend
  const isFriend = useCallback(
    (userId: string) => friends.some((f) => f.id === userId),
    [friends]
  );

  // Handle adding friend from player context menu
  const handleAddFriendFromMenu = useCallback(async () => {
    if (!playerContextMenu) return;
    await sendFriendRequest(playerContextMenu.playerId);
    setPlayerContextMenu(null);
  }, [playerContextMenu, sendFriendRequest]);

  // Handle kicking player from player context menu
  const handleKickFromMenu = useCallback(async () => {
    if (!playerContextMenu) return;
    try {
      await lobbyService.kickPlayer(playerContextMenu.playerId);
      showToast(`${playerContextMenu.username} has been kicked`, "info");
    } catch {
      showToast("Failed to kick player", "error");
    }
    setPlayerContextMenu(null);
  }, [playerContextMenu, showToast]);

  // Dice rolling conditions
  const canRoll = isMyTurn &&
    gameState &&
    gameState.rollsLeft > 0 &&
    !gameState.waitingForMove &&
    !gameState.winnerId;

  // ============================================
  // Loading State
  // ============================================
  if (!gameState) {
    return (
      <div className="min-h-screen bg-bg text-white antialiased flex items-center justify-center">
        <Navbar />
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-purple-500 mx-auto mb-4" />
          <p className="text-white text-lg font-bold uppercase tracking-wider">
            Initializing Ludo Game...
          </p>
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => navigate("/ludo")}
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

      {/* Background gradient effects */}
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)]" />

      <main className="pt-24 pb-4 px-4 h-[calc(100vh-96px)]">
        <div className="h-full max-w-[2000px] mx-auto flex gap-4 justify-center">

          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center">
            <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purple-500/20 shadow-2xl shadow-black/50 p-6">
              {/* Table glow */}
              <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.1),_transparent_60%)]" />

              {/* Game Notification */}
              <GameNotification
                message={notification}
                type={notificationType}
                isVisible={showMessage}
                onClose={() => setShowMessage(false)}
              />

              {/* Corner Player Cards - Positioned by Color */}
              {cornerPlayers.map((player) => (
                <CornerPlayerCard
                  key={player.id}
                  {...player}
                  turnRemainingSeconds={player.isActive ? turnRemainingSeconds : null}
                  onPlayerClick={handlePlayerClick}
                />
              ))}

              {/* Center - Ludo Board */}
              <div className="absolute inset-0 flex items-center justify-center p-16">
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="relative z-10"
                >
                  <LudoBoard
                    players={gameState.players}
                    usernames={gameState.usernames}
                    currentPlayerId={gameState.currentPlayerId}
                    diceValue={gameState.lastDiceRoll}
                    waitingForMove={gameState.waitingForMove}
                    onPawnMoveComplete={handlePawnMoveFinished}
                    onPawnClick={handlePawnClick}
                    loggedPlayerId={user?.id || ""}
                    winnerId={gameState.winnerId}
                  />
                </motion.div>
              </div>
            </div>
          </div>

          {/* Side Panel */}
          <div className="w-80 flex flex-col gap-3 flex-shrink-0 h-full overflow-hidden">
            {/* Navigation - Leave Game Button */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setShowLeaveConfirm(true)}
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
              <h3 className="text-xs font-medium text-gray-500 mb-2">Current Turn</h3>
              <div className="flex items-center gap-2">
                {/* Avatar with Timer Ring */}
                <div className="relative">
                  {turnRemainingSeconds != null && !isActivePlayerBot && (
                    <svg
                      className="absolute inset-0 -rotate-90"
                      width={36}
                      height={36}
                      viewBox="0 0 36 36"
                    >
                      <circle
                        cx={18}
                        cy={18}
                        r={16}
                        fill="none"
                        stroke="rgba(255,255,255,0.1)"
                        strokeWidth={2.5}
                      />
                      <motion.circle
                        cx={18}
                        cy={18}
                        r={16}
                        fill="none"
                        stroke={turnRemainingSeconds <= 5 ? "#ef4444" : turnRemainingSeconds <= 10 ? "#f97316" :
                          gameState.currentPlayerColor === "RED" ? "#ef4444" :
                          gameState.currentPlayerColor === "BLUE" ? "#3b82f6" :
                          gameState.currentPlayerColor === "YELLOW" ? "#eab308" :
                          gameState.currentPlayerColor === "GREEN" ? "#22c55e" : "#8b5cf6"
                        }
                        strokeWidth={2.5}
                        strokeLinecap="round"
                        strokeDasharray={2 * Math.PI * 16}
                        strokeDashoffset={(2 * Math.PI * 16) * (1 - Math.max(0, Math.min(1, turnRemainingSeconds / 60)))}
                        animate={{
                          strokeDashoffset: (2 * Math.PI * 16) * (1 - Math.max(0, Math.min(1, turnRemainingSeconds / 60))),
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
                        : gameState.currentPlayerColor === "RED" ? "border-red-500" :
                          gameState.currentPlayerColor === "BLUE" ? "border-blue-500" :
                          gameState.currentPlayerColor === "YELLOW" ? "border-yellow-500" :
                          gameState.currentPlayerColor === "GREEN" ? "border-green-500" : "border-purple-500"
                    }`}
                  >
                    <img
                      src={`/avatars/avatar_${(gameState.players.findIndex(p => p.userId === gameState.currentPlayerId) % 4) + 1}.png`}
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
                  <p className={`text-sm font-medium truncate ${
                    isMyTurn ? "text-white" :
                    gameState.currentPlayerColor === "RED" ? "text-red-400" :
                    gameState.currentPlayerColor === "BLUE" ? "text-blue-400" :
                    gameState.currentPlayerColor === "YELLOW" ? "text-yellow-400" :
                    gameState.currentPlayerColor === "GREEN" ? "text-green-400" : "text-white"
                  }`}>
                    {isMyTurn
                      ? "Your Turn"
                      : gameState.usernames[gameState.currentPlayerId] || "Unknown"}
                  </p>
                  <div className="flex items-center gap-1 text-[10px] text-gray-500">
                    <span className={`font-bold ${
                      gameState.currentPlayerColor === "RED" ? "text-red-400" :
                      gameState.currentPlayerColor === "BLUE" ? "text-blue-400" :
                      gameState.currentPlayerColor === "YELLOW" ? "text-yellow-400" :
                      gameState.currentPlayerColor === "GREEN" ? "text-green-400" : "text-gray-400"
                    }`}>
                      {gameState.currentPlayerColor}
                    </span>
                    <span>•</span>
                    <span>{isMyTurn ? "Roll dice or move pawn" : isActivePlayerBot ? "Bot thinking..." : "Waiting..."}</span>
                  </div>
                </div>
                {/* Timer display */}
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
                    className="w-2.5 h-2.5 rounded-full bg-purple-500 ml-auto"
                  />
                ) : null}
              </div>
            </div>

            {/* Dice Widget */}
            <DiceWidget isMyTurn={isMyTurn} canRoll={canRoll} />

            {/* Game Status Info */}
            <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10 flex-shrink-0">
              <h3 className="text-xs font-medium text-gray-500 mb-3">Game Info</h3>
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400">Last Roll</span>
                  <span className="text-purple-400 font-bold">
                    {gameState.lastDiceRoll || "-"}
                  </span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400">Rolls Left</span>
                  <span className="text-purple-400 font-bold">
                    {gameState.rollsLeft}
                  </span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-400">Players</span>
                  <span className="text-purple-400 font-bold">
                    {gameState.players.length}
                  </span>
                </div>
              </div>
            </div>

            {/* Spacer to push chat to bottom */}
            <div className="flex-1" />
          </div>
        </div>
      </main>

      {/* Chat Widget */}
      <ChatWidget isHost={isHost} />

      {/* Player Context Menu */}
      <AnimatePresence>
        {playerContextMenu && (
          <>
            <div
              className="fixed inset-0 z-[60]"
              onClick={() => setPlayerContextMenu(null)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="fixed z-[70] bg-black/90 border border-purple-500/30 rounded-xl shadow-xl shadow-purple-500/20 py-1 min-w-[150px]"
              style={{ left: playerContextMenu.x, top: playerContextMenu.y }}
            >
              <div className="px-3 py-2 text-sm text-gray-400 border-b border-white/10">
                {playerContextMenu.username}
              </div>
              {!user?.isGuest && !playerContextMenu.username.startsWith("Guest_") && !isFriend(playerContextMenu.playerId) && (
                <button
                  onClick={handleAddFriendFromMenu}
                  className="w-full px-3 py-2 text-left text-sm text-white hover:bg-purple-500/20 flex items-center gap-2"
                >
                  <UserPlus className="w-4 h-4" />
                  Add Friend
                </button>
              )}
              {isHost && (
                <button
                  onClick={handleKickFromMenu}
                  className="w-full px-3 py-2 text-left text-sm text-red-400 hover:bg-red-500/20 flex items-center gap-2"
                >
                  <UserMinus className="w-4 h-4" />
                  Kick Player
                </button>
              )}
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* ============================================ */}
      {/* Modals */}
      {/* ============================================ */}

      {/* Game Over Modal */}
      <AnimatePresence>
        {isGameOver && (
          <GameOverModal
            players={gameState.players}
            usernames={gameState.usernames}
            winnerId={gameState.winnerId}
            myUserId={user?.id || ""}
            onPlayAgain={handlePlayAgain}
            onExitToMenu={handleExitToMenu}
          />
        )}
      </AnimatePresence>

      {/* Timeout Modal */}
      <TimeoutModal
        isOpen={showTimeoutModal}
        onClose={() => setShowTimeoutModal(false)}
        onReturnToLobby={() => {
          setShowTimeoutModal(false);
          clearLobby();
          navigate("/ludo");
        }}
      />

      {/* Leave Game Confirmation Modal */}
      <ConfirmationModal
        isOpen={showLeaveConfirm}
        onClose={() => setShowLeaveConfirm(false)}
        onConfirm={() => {
          setShowLeaveConfirm(false);
          handleLeaveGame();
        }}
        title="Leave Game?"
        message="Are you sure you want to leave? Your spot will be replaced by a bot."
        confirmText="Leave Game"
        cancelText="Stay"
        variant="danger"
      />

      {/* Social Center - Friends Drawer */}
      <SocialCenter />
    </div>
  );
}
