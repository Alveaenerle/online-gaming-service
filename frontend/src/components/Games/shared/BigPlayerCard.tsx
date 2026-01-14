import { motion, AnimatePresence } from "framer-motion";
import { Crown, Pencil, CheckCircle2, Circle } from "lucide-react";
import type { LobbyPlayer } from "../utils/types";
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
      animate={{
        opacity: 1,
        x: 0,
        boxShadow: player.isReady
          ? "0 0 40px rgba(139, 92, 246, 0.3)"
          : "0 0 20px rgba(0, 0, 0, 0.4)",
      }}
      transition={{ duration: 0.5 }}
      className={`relative h-full rounded-[2.5rem] p-10 bg-[#121018] border transition-all duration-700 flex flex-col items-center justify-between overflow-hidden ${
        player.isReady ? "border-purple-500/50" : "border-purple-500/20"
      }`}
    >
      {/* 1. BACKGROUND: Animated gradient glow inside the card */}
      <div
        className={`absolute inset-0 transition-opacity duration-1000 -z-10 ${
          player.isReady ? "opacity-100" : "opacity-0"
        }`}
        style={{
          background:
            "radial-gradient(circle at center, rgba(139, 92, 246, 0.15) 0%, transparent 70%)",
        }}
      />

      <div
        className={`absolute top-0 left-1/2 -translate-x-1/2 w-1/2 h-[1px] bg-gradient-to-r from-transparent via-purple-400/50 to-transparent transition-opacity duration-700 ${
          player.isReady ? "opacity-100" : "opacity-0"
        }`}
      />

      <div className="text-center w-full relative z-10">
        <button
          onClick={onAvatarClick}
          className="group relative mx-auto w-44 h-44 rounded-full border-4 border-purple-500 p-1.5 transition-transform hover:scale-105 active:scale-95"
        >
          <img
            src={player.avatar}
            className="w-full h-full object-cover rounded-full"
            alt="avatar"
          />
          <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 rounded-full transition-opacity flex items-center justify-center">
            <Pencil size={24} className="text-white" />
          </div>

          <AnimatePresence>
            {player.isReady && (
              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.8 }}
                className="absolute inset-0 rounded-full shadow-[0_0_35px_rgba(139,92,246,0.6)] border-4 border-purple-400"
              />
            )}
          </AnimatePresence>
        </button>

        <div className="mt-6 space-y-2">
          <div className="flex flex-col justify-center items-center gap-1">
            {/* Host Crown Indicator */}
            <div className="h-6 flex items-center justify-center">
              {player.isHost && (
                <Crown
                  className="text-yellow-400 drop-shadow-[0_0_10px_rgba(250,204,21,0.5)] animate-in fade-in zoom-in duration-300"
                  size={24}
                />
              )}
            </div>
            
            <span className="text-2xl font-black tracking-tight text-white drop-shadow-md truncate max-w-[250px]">
              {player.username}
            </span>
          </div>

          <div
            className={`flex items-center justify-center gap-2 text-xs font-black uppercase tracking-[0.3em] transition-all duration-500 ${
              player.isReady
                ? "text-green-400 drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]"
                : "text-gray-500"
            }`}
          >
            {player.isReady ? <CheckCircle2 size={14} /> : <Circle size={14} />}
            {player.isReady ? "Status: Ready" : "Status: Not Ready"}
          </div>
        </div>
      </div>

      <motion.button
        onClick={onToggleReady}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
        className={`relative w-full py-5 rounded-2xl font-black uppercase tracking-widest text-sm transition-all duration-300 shadow-xl overflow-hidden ${
          player.isReady
            ? hover
              ? "bg-red-500 shadow-red-500/40 text-white"
              : "bg-green-600 shadow-green-500/40 text-white"
            : "bg-purple-600 shadow-purple-500/40 text-white hover:bg-purple-500"
        }`}
      >
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:animate-shine" />

        {player.isReady && hover
          ? "Cancel Ready"
          : player.isReady
          ? "Ready to Play"
          : "I am Ready"}
      </motion.button>
    </motion.div>
  );
}
