import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, Users, Loader2 } from "lucide-react";
import { LocalGameState } from "./types";
import { convertRank, SUIT_SYMBOLS, SUIT_COLORS } from "./constants";

// ============================================
// Side Panel Component
// ============================================

interface SidePanelProps {
  gameState: LocalGameState;
  message: string;
  loading: boolean;
  isMyTurn: boolean;
  getPlayerName: (playerId: string) => string;
  onLeaveGame: () => void;
  onAcceptEffect: () => void;
}

export const SidePanel: React.FC<SidePanelProps> = ({
  gameState,
  message,
  loading,
  isMyTurn,
  getPlayerName,
  onLeaveGame,
  onAcceptEffect,
}) => {
  return (
    <div className="w-72 flex flex-col gap-3 flex-shrink-0">
      {/* Navigation */}
      <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onLeaveGame}
          className="w-full py-3 rounded-xl bg-red-500/20 hover:bg-red-500/30 text-red-400 font-medium transition-colors flex items-center justify-center gap-2 border border-red-500/30"
        >
          <ArrowLeft size={18} />
          Opuść grę
        </motion.button>
      </div>

      {/* Game Info */}
      <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Pokój</h3>
        <p className="text-white font-mono text-sm">{gameState.roomId?.slice(0, 8)}...</p>
        <div className="flex items-center gap-2 mt-2 text-gray-400 text-sm">
          <Users size={14} />
          <span>{Object.keys(gameState.playersCardsAmount).length} graczy</span>
        </div>
      </div>

      {/* Activity */}
      <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10">
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Aktywność</h3>
        <AnimatePresence mode="wait">
          <motion.p
            key={message}
            initial={{ opacity: 0, y: 5 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -5 }}
            className="text-white text-sm"
          >
            {message || "Gra w toku..."}
          </motion.p>
        </AnimatePresence>
      </div>

      {/* Game Status */}
      <div className="bg-[#121018]/80 backdrop-blur p-4 rounded-xl border border-white/10 flex-1">
        <h3 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Status</h3>
        <div className="space-y-3 text-sm">
          {/* Current player */}
          <div className="flex justify-between items-center">
            <span className="text-gray-400">Aktualny gracz</span>
            <span className={`font-medium ${isMyTurn ? "text-green-400" : "text-white"}`}>
              {getPlayerName(gameState.activePlayerId)}
            </span>
          </div>

          {/* Demanded suit */}
          {gameState.demandedSuit && (
            <div className="flex justify-between items-center">
              <span className="text-gray-400">Żądany kolor</span>
              <span className={`text-xl ${SUIT_COLORS[gameState.demandedSuit]}`}>
                {SUIT_SYMBOLS[gameState.demandedSuit]}
              </span>
            </div>
          )}

          {/* Demanded rank */}
          {gameState.demandedRank && (
            <div className="flex justify-between items-center">
              <span className="text-gray-400">Żądana figura</span>
              <span className="text-purple-400 font-bold">{convertRank(gameState.demandedRank)}</span>
            </div>
          )}

          {/* Special effect */}
          {gameState.specialEffectActive && (
            <div className="mt-4 p-3 bg-orange-500/20 border border-orange-500/30 rounded-xl">
              <p className="text-orange-400 text-sm font-medium mb-2">Efekt specjalny aktywny!</p>
              {isMyTurn && (
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={onAcceptEffect}
                  disabled={loading}
                  className="w-full py-2 bg-orange-500 hover:bg-orange-400 text-white rounded-lg font-medium text-sm disabled:opacity-50 flex items-center justify-center"
                >
                  {loading ? <Loader2 className="animate-spin" size={18} /> : "Akceptuj efekt"}
                </motion.button>
              )}
            </div>
          )}

          {/* Ranking */}
          {Object.keys(gameState.ranking).length > 0 && (
            <div className="mt-4">
              <p className="text-gray-400 text-xs mb-2">Zakończyli:</p>
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
  );
};
