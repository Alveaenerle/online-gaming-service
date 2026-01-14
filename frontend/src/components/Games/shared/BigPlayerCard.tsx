import { motion, AnimatePresence } from "framer-motion";
import { Crown, Pencil, CheckCircle2, Circle } from "lucide-react";
import { LobbyPlayer } from "../utils/types";
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
      className={`relative h-full rounded-2xl sm:rounded-[2rem] lg:rounded-[2.5rem] p-4 sm:p-6 lg:p-10 bg-[#121018] border transition-all duration-700 flex flex-col items-center justify-between overflow-hidden ${
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
          className="group relative mx-auto w-24 h-24 sm:w-32 sm:h-32 lg:w-44 lg:h-44 rounded-full border-4 border-purple-500 p-1 sm:p-1.5 transition-transform hover:scale-105 active:scale-95"
        >
          <img
            src={player.avatar}
            className="w-full h-full object-cover rounded-full"
            alt="avatar"
          />
          <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 rounded-full transition-opacity flex items-center justify-center">
            <Pencil size={20} className="text-white sm:hidden" />
            <Pencil size={24} className="text-white hidden sm:block" />
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

        <div className="mt-4 sm:mt-6 space-y-1 sm:space-y-2">
          <div className="flex flex-col justify-center items-center gap-1">
            {/* Host Crown Indicator */}
            <div className="h-5 sm:h-6 flex items-center justify-center">
              {player.isHost && (
                <>
                  <Crown
                    className="text-yellow-400 drop-shadow-[0_0_10px_rgba(250,204,21,0.5)] animate-in fade-in zoom-in duration-300 sm:hidden"
                    size={20}
                  />
                  <Crown
                    className="text-yellow-400 drop-shadow-[0_0_10px_rgba(250,204,21,0.5)] animate-in fade-in zoom-in duration-300 hidden sm:block"
                    size={24}
                  />
                </>
              )}
            </div>
            
            <span className="text-lg sm:text-xl lg:text-2xl font-black tracking-tight text-white drop-shadow-md truncate max-w-[180px] sm:max-w-[220px] lg:max-w-[250px]">
              {player.username}
            </span>
          </div>

          <div
            className={`flex items-center justify-center gap-1.5 sm:gap-2 text-[10px] sm:text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.3em] transition-all duration-500 ${
              player.isReady
                ? "text-green-400 drop-shadow-[0_0_8px_rgba(74,222,128,0.5)]"
                : "text-gray-500"
            }`}
          >
            {player.isReady ? <CheckCircle2 size={12} className="sm:hidden" /> : <Circle size={12} className="sm:hidden" />}
            {player.isReady ? <CheckCircle2 size={14} className="hidden sm:block" /> : <Circle size={14} className="hidden sm:block" />}
            <span className="hidden xs:inline">{player.isReady ? "Status: Ready" : "Status: Not Ready"}</span>
            <span className="xs:hidden">{player.isReady ? "Ready" : "Not Ready"}</span>
          </div>
        </div>
      </div>

      <motion.button
        onClick={onToggleReady}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        onMouseEnter={() => setHover(true)}
        onMouseLeave={() => setHover(false)}
        className={`relative w-full py-3 sm:py-4 lg:py-5 rounded-xl sm:rounded-2xl font-black uppercase tracking-wider sm:tracking-widest text-xs sm:text-sm transition-all duration-300 shadow-xl overflow-hidden min-h-[44px] ${
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
