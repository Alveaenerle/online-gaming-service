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
    <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3 sm:gap-4">
      <button
        onClick={onLeave}
        className="group flex items-center justify-center gap-2 sm:gap-3 px-4 sm:px-6 py-2.5 sm:py-3
                   rounded-xl sm:rounded-2xl border border-white/5
                   text-gray-500 hover:text-red-400 hover:border-red-500/50
                   bg-[#121018] transition-all duration-300 font-bold uppercase text-[10px] sm:text-xs tracking-widest min-h-[44px]"
      >
        <LogOut
          size={16}
          className="group-hover:-translate-x-1 transition-transform sm:hidden"
        />
        <LogOut
          size={18}
          className="group-hover:-translate-x-1 transition-transform hidden sm:block"
        />
        Leave Arena
      </button>

      {isHost && (
        <div className="relative w-full sm:w-[280px] md:w-[320px]">
          <motion.button
            whileHover={canStart ? { scale: 1.02, y: -2 } : {}}
            whileTap={canStart ? { scale: 0.98 } : {}}
            disabled={!canStart}
            onClick={onStart}
            className={`relative overflow-hidden w-full py-3 sm:py-4 lg:py-5 rounded-xl sm:rounded-[1.25rem] lg:rounded-[1.5rem] font-black tracking-[0.15em] sm:tracking-[0.2em] uppercase text-xs sm:text-sm
              transition-all duration-500 shadow-2xl flex items-center justify-center gap-2 sm:gap-3 min-h-[44px]
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
                  className="flex items-center gap-2 sm:gap-3"
                >
                  <Play size={16} fill="currentColor" className="sm:hidden" />
                  <Play size={18} fill="currentColor" className="hidden sm:block" />
                  START GAME
                </motion.div>
              ) : (
                <motion.div
                  key="locked"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-2 sm:gap-3"
                >
                  <Lock size={16} className="sm:hidden" />
                  <Lock size={18} className="hidden sm:block" />
                  <span className="hidden xs:inline">Waiting for players</span>
                  <span className="xs:hidden">Waiting...</span>
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
