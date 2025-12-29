import { motion } from "framer-motion";
import { LogOut } from "lucide-react";

type Props = {
  isHost: boolean;
  canStart: boolean;
  onStart: () => void;
  onLeave: () => void;
};

export function LobbyActions({ isHost, canStart, onStart, onLeave }: Props) {
  return (
    <div className="flex justify-between items-center pt-6">
      <button
        onClick={onLeave}
        className="flex items-center gap-2 px-4 py-2
                   rounded-xl border border-white/20
                   text-gray-400 hover:text-red-400 hover:border-red-400
                   bg-transparent transition font-semibold"
      >
        <LogOut size={16} />
        Leave lobby
      </button>

      {isHost && (
        <motion.button
          whileHover={canStart ? { scale: 1.05 } : undefined}
          whileTap={canStart ? { scale: 0.95 } : undefined}
          disabled={!canStart}
          onClick={onStart}
          className={`px-8 py-4 rounded-3xl font-bold tracking-wide w-[30%]
            ${
              canStart
                ? "bg-purple-600 hover:bg-purple-500 shadow-lg shadow-purple-600/40"
                : "bg-gray-700 cursor-not-allowed"
            }`}
        >
          START GAME
        </motion.button>
      )}
    </div>
  );
}
