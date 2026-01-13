import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate, useLocation } from "react-router-dom";
import {
  Gamepad2,
  Users,
  LogOut,
  Maximize2,
  ChevronUp,
  ChevronDown,
} from "lucide-react";
import { useLobby } from "../../context/LobbyContext";
import { lobbyService } from "../../services/lobbyService";
import { ConfirmationModal } from "./ConfirmationModal";

export function LobbyIndicator() {
  const navigate = useNavigate();
  const location = useLocation();
  const { currentLobby, isInLobby, clearLobby } = useLobby();
  const [isExpanded, setIsExpanded] = useState(true);
  const [isLeaving, setIsLeaving] = useState(false);
  const [showLeaveModal, setShowLeaveModal] = useState(false);

  // Don't show if not in a lobby or if already on the lobby page
  const lobbyPath =
    currentLobby?.gameType === "MAKAO" ? "/lobby/makao" : "/lobby/ludo";
  const gamePath =
    currentLobby?.gameType === "MAKAO" ? "/makao/game" : "/ludo/game";

  const isOnLobbyOrGamePage =
    location.pathname === lobbyPath ||
    location.pathname === gamePath ||
    location.pathname.includes("/lobby/");
  console.log(
    "LobbyIndicator - isInLobby:",
    isInLobby,
    "currentLobby:",
    currentLobby,
    "isOnLobbyOrGamePage:",
    isOnLobbyOrGamePage
  );

  if (!isInLobby || !currentLobby || isOnLobbyOrGamePage) {
    return null;
  }

  const playerCount = Object.keys(currentLobby.players).length;
  const isPlaying = currentLobby.status === "PLAYING";

  const handleOpenLobby = () => {
    if (isPlaying) {
      console.log(currentLobby);
      navigate(gamePath);
    } else {
      navigate(lobbyPath);
    }
  };

  const handleLeave = async () => {
    if (isLeaving) return;

    setIsLeaving(true);
    try {
      await lobbyService.leaveRoom();
      clearLobby();
    } catch (err) {
      console.error("Failed to leave:", err);
      // Force clear if API fails
      clearLobby();
    } finally {
      setIsLeaving(false);
    }
  };

  const handleLeaveClick = () => {
    setShowLeaveModal(true);
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: -100, y: 100 }}
      animate={{ opacity: 1, x: 0, y: 0 }}
      exit={{ opacity: 0, x: -100 }}
      transition={{ type: "spring", damping: 20, stiffness: 300 }}
      className="fixed bottom-12 left-12 z-[9999]"
    >
      <div className="bg-[#121018]/95 backdrop-blur-xl border border-purple-500/30 rounded-3xl shadow-2xl shadow-purple-900/40 overflow-hidden min-w-[500px]">
        {/* Header - Always visible - Scaled Up */}
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="w-full flex items-center justify-between gap-6 px-8 py-6 hover:bg-white/5 transition-colors"
        >
          <div className="flex items-center gap-6">
            <div className="relative">
              <div
                className={`w-20 h-20 rounded-2xl flex items-center justify-center ${
                  isPlaying
                    ? "bg-green-500/20 text-green-400"
                    : "bg-purple-500/20 text-purple-400"
                }`}
              >
                <Gamepad2 size={40} />
              </div>
              {/* Pulsing indicator */}
              <div
                className={`absolute -top-1.5 -right-1.5 w-5 h-5 rounded-full border-2 border-[#121018] ${
                  isPlaying
                    ? "bg-green-500 animate-pulse"
                    : "bg-yellow-500 animate-pulse"
                }`}
              />
            </div>
            <div className="text-left">
              <p className="text-lg font-bold text-white/90 uppercase tracking-wider">
                {isPlaying ? "In Game" : "In Lobby"}
              </p>
              <p className="text-base text-gray-500 font-medium">
                {currentLobby.gameType}
              </p>
            </div>
          </div>
          {isExpanded ? (
            <ChevronDown size={28} className="text-gray-500" />
          ) : (
            <ChevronUp size={28} className="text-gray-500" />
          )}
        </button>

        {/* Expanded content */}
        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.2 }}
            >
              <div className="px-8 pb-8 space-y-5 border-t border-white/5 pt-6">
                {/* Room info */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between text-base"></div>
                  <div className="flex items-center justify-between text-base">
                    <span className="text-gray-500">Players</span>
                    <span className="text-white font-medium flex items-center gap-2">
                      <Users size={20} />
                      {playerCount}/{currentLobby.maxPlayers}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-base">
                    <span className="text-gray-500">Status</span>
                    <span
                      className={`font-medium ${
                        isPlaying ? "text-green-400" : "text-yellow-400"
                      }`}
                    >
                      {isPlaying ? "Playing" : "Waiting"}
                    </span>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="flex gap-4 pt-4">
                  <button
                    onClick={handleOpenLobby}
                    className="flex-1 flex items-center justify-center gap-3 px-6 py-4 bg-purple-600 hover:bg-purple-500 rounded-xl text-white text-base font-bold uppercase tracking-wider transition-colors"
                  >
                    <Maximize2 size={22} />
                    Open
                  </button>
                  <button
                    onClick={handleLeaveClick}
                    disabled={isLeaving}
                    className="flex items-center justify-center gap-3 px-6 py-4 bg-red-500/20 hover:bg-red-500/30 border border-red-500/30 rounded-xl text-red-400 text-base font-bold uppercase tracking-wider transition-colors disabled:opacity-50"
                  >
                    <LogOut size={22} />
                    {isLeaving ? "..." : "Leave"}
                  </button>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Leave Confirmation Modal */}
      <ConfirmationModal
        isOpen={showLeaveModal}
        onClose={() => setShowLeaveModal(false)}
        onConfirm={handleLeave}
        title={isPlaying ? "Leave Game?" : "Leave Lobby?"}
        message={
          isPlaying
            ? "You're currently in a game. If you leave now, you may lose your progress and the game may be forfeited."
            : "Are you sure you want to leave this lobby? You'll need to rejoin if you want to play."
        }
        confirmText="Leave"
        variant={isPlaying ? "danger" : "warning"}
        isLoading={isLeaving}
      />
    </motion.div>
  );
}
