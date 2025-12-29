import { motion } from "framer-motion";
import { Crown, Pencil } from "lucide-react";
import { LobbyPlayer } from "./types";
import { useState } from "react";

type Props = {
  player: LobbyPlayer;
  onAvatarClick: () => void;
  onToggleReady?: () => void;
};

export function BigPlayerCard({ player, onAvatarClick, onToggleReady }: Props) {
  const [hover, setHover] = useState(false);
  return (
    <motion.div
      initial={{ opacity: 0, x: 40 }}
      animate={{ opacity: 1, x: 0 }}
      className="relative h-full rounded-3xl p-10
                 bg-gradient-to-b from-[#1a1d3a] to-[#0b0d1a]
                 border border-purple-500/30
                 shadow-[0_0_60px_rgba(139,92,246,0.6)] flex flex-col items-center"
    >
      <div className="text-center">
        <button
          onClick={onAvatarClick}
          className="group relative mx-auto w-40 h-40 rounded-full
                     border-4 border-purple-500
                     overflow-hidden p-2"
        >
          <img src={player.avatar} className="w-full h-full object-cover" />

          {player.isReady && (
            <div
              className="absolute inset-0
                         shadow-[inset_0_0_40px_rgba(139,92,246,0.9)]"
            />
          )}

          <div
            className="absolute inset-0
                       bg-black/50
                       opacity-0
                       group-hover:opacity-100
                       transition"
          />

          <div
            className="absolute inset-0
               flex items-center justify-center
               opacity-0
               group-hover:opacity-100
               transition"
          >
            <div
              className="w-12 h-12 rounded-full
                 bg-purple-600
                 flex items-center justify-center
                 shadow-lg shadow-purple-600/40"
            >
              <Pencil size={20} className="text-white" />
            </div>
          </div>
        </button>

        <div className="mt-4">
          <div className="flex justify-center items-center gap-2">
            {player.isHost && <Crown className="text-yellow-400" size={18} />}
            <span className="text-xl font-bold tracking-wide">
              {player.username}
            </span>
          </div>

          <span
            className={`mt-2 flex items-center justify-center gap-2
                        text-sm tracking-widest
                        ${player.isReady ? "text-green-400" : "text-gray-500"}
            `}
          >
            <span
              className={`w-2 h-2 rounded-full ${
                player.isReady ? "bg-green-500" : "bg-gray-500"
              }`}
            />
            {player.isReady ? "READY" : "NOT READY"}
          </span>
        </div>
      </div>
      <motion.button
        onClick={onToggleReady}
        whileTap={{ scale: 0.96 }}
        className={`mt-20 w-[70%] py-3 rounded-xl
              font-bold tracking-widest text-sm
              transition relative`}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
      >
        <span
          className={`block w-full text-center
      transition-colors duration-200
      ${
        player.isReady
          ? hover
            ? "bg-red-600 text-white shadow-[0_0_20px_rgba(220,38,38,0.6)]"
            : "bg-green-600 text-white shadow-[0_0_20px_rgba(34,197,94,0.6)]"
          : "bg-white/5 text-gray-400 hover:bg-white/10"
      }
      py-3 rounded-xl
    `}
        >
          {player.isReady && hover
            ? "UNREADY"
            : player.isReady
            ? "READY"
            : "SET READY"}
        </span>
      </motion.button>
    </motion.div>
  );
}
