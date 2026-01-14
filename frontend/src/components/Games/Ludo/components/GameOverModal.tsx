import React from "react";
import { motion } from "framer-motion";
import { Trophy, Home, RotateCcw } from "lucide-react";
import type { LudoPlayer } from "../types";
import type { Color } from "../Board/constants";

interface PlayerResult {
  id: string;
  username: string;
  color: Color;
  pawnsInHome: number;
  isWinner: boolean;
  isMe: boolean;
  avatarUrl: string;
}

interface GameOverModalProps {
  players: LudoPlayer[];
  usernames: Record<string, string>;
  winnerId: string | null;
  myUserId: string;
  onPlayAgain: () => void;
  onExitToMenu: () => void;
}

const colorStyles: Record<Color, { text: string; bg: string; border: string; glow: string }> = {
  RED: {
    text: "text-red-400",
    bg: "bg-red-500/20",
    border: "border-red-500/50",
    glow: "shadow-red-500/30",
  },
  BLUE: {
    text: "text-blue-400",
    bg: "bg-blue-500/20",
    border: "border-blue-500/50",
    glow: "shadow-blue-500/30",
  },
  YELLOW: {
    text: "text-yellow-400",
    bg: "bg-yellow-500/20",
    border: "border-yellow-500/50",
    glow: "shadow-yellow-500/30",
  },
  GREEN: {
    text: "text-green-400",
    bg: "bg-green-500/20",
    border: "border-green-500/50",
    glow: "shadow-green-500/30",
  },
};

/**
 * Modal shown when Ludo game is finished - displays winner and rankings
 * Styled to match the Ludo game's futuristic/glassmorphism aesthetic
 */
const GameOverModal: React.FC<GameOverModalProps> = ({
  players,
  usernames,
  winnerId,
  myUserId,
  onPlayAgain,
  onExitToMenu,
}) => {
  // Build player results with ranking info
  const playerResults: PlayerResult[] = players.map((player, index) => ({
    id: player.userId,
    username: usernames[player.userId] || "Unknown",
    color: player.color,
    pawnsInHome: player.pawns.filter((p) => p.inHome).length,
    isWinner: player.userId === winnerId,
    isMe: player.userId === myUserId,
    avatarUrl: `/avatars/avatar_${(index % 4) + 1}.png`,
  }));

  // Sort by winner first, then by pawns in home
  const sortedPlayers = [...playerResults].sort((a, b) => {
    if (a.isWinner) return -1;
    if (b.isWinner) return 1;
    return b.pawnsInHome - a.pawnsInHome;
  });

  const winner = sortedPlayers.find((p) => p.isWinner);
  const isMyWin = winner?.isMe ?? false;

  const getPlacementEmoji = (index: number): string => {
    switch (index) {
      case 0:
        return "🥇";
      case 1:
        return "🥈";
      case 2:
        return "🥉";
      default:
        return `#${index + 1}`;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="fixed inset-0 bg-black/85 backdrop-blur-sm flex items-center justify-center z-[100]"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        transition={{ type: "spring", damping: 20, stiffness: 300 }}
        className="bg-gradient-to-br from-[#1a1825] to-[#0d0c12] rounded-[2.5rem] p-8 max-w-md w-full mx-4 border border-purple-500/30 shadow-2xl shadow-purple-500/20"
      >
        {/* Trophy Icon */}
        <div className="flex justify-center mb-6">
          <motion.div
            initial={{ rotate: -20, scale: 0 }}
            animate={{ rotate: 0, scale: 1 }}
            transition={{ type: "spring", delay: 0.2 }}
            className={`w-24 h-24 rounded-full flex items-center justify-center ${
              winner ? colorStyles[winner.color].bg : "bg-purple-500/20"
            } border-2 ${
              winner ? colorStyles[winner.color].border : "border-purple-500/50"
            } shadow-lg ${winner ? colorStyles[winner.color].glow : "shadow-purple-500/30"}`}
          >
            <Trophy className={`w-12 h-12 ${winner ? colorStyles[winner.color].text : "text-purple-400"}`} />
          </motion.div>
        </div>

        {/* Title */}
        <motion.h2
          initial={{ y: -20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.1 }}
          className="text-4xl font-black text-center mb-3 bg-gradient-to-r from-purple-400 to-purple-600 bg-clip-text text-transparent uppercase tracking-wider"
        >
          Game Over!
        </motion.h2>

        {/* Winner announcement */}
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
          className="text-center mb-6"
        >
          {isMyWin ? (
            <div className="text-yellow-400 text-2xl font-black uppercase tracking-wider">
              🎉 Victory is Yours! 🎉
            </div>
          ) : winner ? (
            <div className="text-gray-300 text-xl">
              <span className={`font-bold ${colorStyles[winner.color].text}`}>
                {winner.username}
              </span>{" "}
              wins the game!
            </div>
          ) : (
            <div className="text-gray-400 text-lg">Game Concluded</div>
          )}
        </motion.div>

        {/* Rankings */}
        <motion.div
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="space-y-2 mb-8 max-h-52 overflow-y-auto scrollbar-hide"
        >
          <h3 className="text-[10px] font-black text-gray-500 mb-3 uppercase tracking-[0.3em] text-center">
            Final Rankings
          </h3>
          {sortedPlayers.map((player, index) => (
            <motion.div
              key={player.id}
              initial={{ x: -20, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              transition={{ delay: 0.4 + index * 0.1 }}
              className={`
                flex items-center justify-between p-3 rounded-2xl border
                ${player.isWinner ? `${colorStyles[player.color].bg} ${colorStyles[player.color].border}` : "bg-white/5 border-white/10"}
                ${player.isMe ? "ring-2 ring-purple-500" : ""}
              `}
            >
              <div className="flex items-center gap-3">
                <span className="text-xl w-8">{getPlacementEmoji(index)}</span>

                {/* Avatar */}
                <div className={`w-10 h-10 rounded-full overflow-hidden border-2 ${colorStyles[player.color].border}`}>
                  <img
                    src={player.avatarUrl}
                    alt={player.username}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.src = "/avatars/avatar_1.png";
                    }}
                  />
                </div>

                <div className="flex flex-col">
                  <span className={`font-bold ${colorStyles[player.color].text}`}>
                    {player.username}
                    {player.isMe && (
                      <span className="text-purple-400 text-xs ml-2">(You)</span>
                    )}
                  </span>
                  <span className="text-[10px] text-gray-500 uppercase tracking-wider">
                    {player.color} Team
                  </span>
                </div>
              </div>
              <div className="text-right">
                <span className="text-gray-400 text-sm font-mono">
                  {player.pawnsInHome}/4
                </span>
                <p className="text-[9px] text-gray-600 uppercase">Home</p>
              </div>
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
            className="w-full py-4 rounded-2xl bg-gradient-to-r from-purple-600 to-purple-500 text-white font-black uppercase tracking-widest shadow-lg shadow-purple-500/30 flex items-center justify-center gap-3"
          >
            <RotateCcw size={18} />
            Play Again
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={onExitToMenu}
            className="w-full py-4 rounded-2xl bg-white/5 hover:bg-white/10 text-white font-bold border border-white/10 transition-colors flex items-center justify-center gap-3"
          >
            <Home size={18} />
            Exit to Menu
          </motion.button>
        </motion.div>
      </motion.div>
    </motion.div>
  );
};

export default GameOverModal;
