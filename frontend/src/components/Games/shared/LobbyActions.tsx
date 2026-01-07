import { motion, AnimatePresence } from "framer-motion";
import { LogOut, Play, Lock } from "lucide-react";

type Props = {
  isHost: boolean;
  canStart: boolean;
  onStart: () => void;
  onLeave: () => void;
};

export function LobbyActions({ isHost, canStart, onStart, onLeave }: Props) {
  return (
    <div className="flex justify-between items-center ">
      <button
        onClick={onLeave}
        className="group flex items-center gap-3 px-6 py-3
                   rounded-2xl border border-white/5
                   text-gray-500 hover:text-red-400 hover:border-red-500/50
                   bg-[#121018] transition-all duration-300 font-bold uppercase text-xs tracking-widest"
      >
        <LogOut
          size={18}
          className="group-hover:-translate-x-1 transition-transform"
        />
        Leave Arena
      </button>

      {isHost && (
        <div className="relative w-[320px]">
          <motion.button
            whileHover={canStart ? { scale: 1.02, y: -2 } : {}}
            whileTap={canStart ? { scale: 0.98 } : {}}
            disabled={!canStart}
            onClick={onStart}
            className={`relative overflow-hidden w-full py-5 rounded-[1.5rem] font-black tracking-[0.2em] uppercase text-sm
              transition-all duration-500 shadow-2xl flex items-center justify-center gap-3
              ${
                canStart
                  ? "bg-purple-600 text-white shadow-purple-500/40 cursor-pointer"
                  : "bg-white/5 text-white/20 border border-white/5 cursor-not-allowed"
              }`}
          >
            {canStart && (
              <motion.div
                animate={{ x: ["-100%", "200%"] }}
                transition={{
                  repeat: Infinity,
                  duration: 3,
                  ease: "easeInOut",
                }}
                className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -skew-x-12"
              />
            )}

            <AnimatePresence mode="wait">
              {canStart ? (
                <motion.div
                  key="ready"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-3"
                >
                  <Play size={18} fill="currentColor" />
                  START GAME
                </motion.div>
              ) : (
                <motion.div
                  key="locked"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-3"
                >
                  <Lock size={18} />
                  Waiting for players
                </motion.div>
              )}
            </AnimatePresence>
          </motion.button>

          {canStart && (
            <div className="absolute inset-0 bg-purple-500/20 blur-2xl -z-10 animate-pulse" />
          )}
        </div>
      )}
    </div>
  );
}
