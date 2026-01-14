import React from "react";
import { motion } from "framer-motion";
import type { PlayerView } from "../types";

interface GameOverModalProps {
  players: PlayerView[];
  myUserId: string;
  onPlayAgain: () => void;
  onExitToMenu: () => void;
}

/**
 * Modal shown when game is finished - displays rankings and options
 * Two actions: Play Again (return to lobby) and Exit to Menu (go home)
 */
const GameOverModal: React.FC<GameOverModalProps> = ({
  players,
  myUserId: _myUserId,
  onPlayAgain,
  onExitToMenu,
}) => {
  // Sort players by placement
  const sortedPlayers = [...players]
    .filter((p) => p.placement !== null)
    .sort((a, b) => (a.placement || 99) - (b.placement || 99));

  const myPlayer = players.find((p) => p.isMe);
  const myPlacement = myPlayer?.placement;
  const isWinner = myPlacement === 1;

  const getMedalEmoji = (place: number): string => {
    switch (place) {
      case 1:
        return "🥇";
      case 2:
        return "🥈";
      case 3:
        return "🥉";
      default:
        return `#${place}`;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#121018] rounded-2xl p-8 max-w-md w-full mx-4 border border-purpleEnd/50 shadow-2xl text-center"
      >
        {/* Title */}
        <motion.h2
          initial={{ y: -20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.1 }}
          className="text-4xl font-bold mb-3 bg-gradient-to-r from-purpleStart to-purpleEnd bg-clip-text text-transparent"
        >
          Game Over!
        </motion.h2>

        {/* Winner announcement */}
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
          className="mb-6"
        >
          {isWinner ? (
            <div className="text-yellow-400 text-2xl font-bold">
              🎉 You Won! 🎉
            </div>
          ) : (
            <div className="text-gray-300 text-xl">
              You finished{" "}
              <span className="text-purpleEnd font-bold">
                {getMedalEmoji(myPlacement || 0)}
              </span>
            </div>
          )}
        </motion.div>

        {/* Rankings */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="space-y-2 mb-6 max-h-48 overflow-y-auto"
        >
          <h3 className="text-xs font-medium text-gray-500 mb-3 uppercase tracking-wider">
            Final Rankings
          </h3>
          {sortedPlayers.map((player, index) => (
            <motion.div
              key={player.id}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.4 + index * 0.1 }}
              className={`
                flex items-center justify-between p-3 rounded-xl
                ${player.placement === 1 ? "bg-yellow-600/20 border border-yellow-500/30" : "bg-[#1a1a27]"}
                ${player.isMe ? "ring-2 ring-purpleEnd" : ""}
              `}
            >
              <div className="flex items-center gap-3">
                <span className="text-xl w-8">
                  {getMedalEmoji(player.placement || 0)}
                </span>
                <span className="text-white font-medium">
                  {player.username}
                  {player.isMe && (
                    <span className="text-purpleEnd text-sm ml-2">(You)</span>
                  )}
                </span>
              </div>
              <span className="text-gray-400 text-sm">
                {player.cardCount} cards left
              </span>
            </motion.div>
          ))}
        </motion.div>

        {/* Action buttons */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.6 }}
          className="flex flex-col gap-3"
        >
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onPlayAgain}
            className="w-full py-3 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold shadow-neon"
          >
            🔄 Play Again
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onExitToMenu}
            className="w-full py-3 rounded-xl bg-[#1a1a27] hover:bg-[#252532] text-white font-medium border border-white/10 transition-colors"
          >
            🏠 Exit to Menu
          </motion.button>
        </motion.div>
      </motion.div>
    </motion.div>
  );
};

export default GameOverModal;
