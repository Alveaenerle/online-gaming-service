import { motion } from "framer-motion";
import { Crown } from "lucide-react";

type Props = {
  player?: {
    username: string;
    avatar: string;
    isHost?: boolean;
    isReady?: boolean;
  };
};

export function PlayerCard({ player }: Props) {
  return (
    <motion.div
      layout
      whileHover={player ? { scale: 1.03 } : undefined}
      className={`relative w-[80%] h-60 rounded-2xl p-4
        flex flex-col items-center justify-center
        ${
          player
            ? `
              bg-gradient-to-b from-[#14162b] to-[#0b0d1a]
              border border-white/10
              ${
                player.isReady
                  ? "shadow-[0_0_30px_rgba(139,92,246,0.4)]"
                  : "opacity-90"
              }
            `
            : "bg-transparent border border-dashed border-white/20"
        }`}
    >
      {player ? (
        <>
          <div
            className="relative w-20 h-20 rounded-full
                       border-2 border-purple-500
                       overflow-hidden p-1"
          >
            <img
              src={player.avatar}
              alt=""
              className="w-full h-full object-cover"
            />

            {player.isReady && (
              <div
                className="absolute inset-0
                           shadow-[inset_0_0_25px_rgba(139,92,246,0.8)]"
              />
            )}
          </div>

          <div className="mt-3 text-center">
            <div className="flex justify-center items-center gap-1">
              {player.isHost && <Crown size={14} className="text-yellow-400" />}
              <span className="font-semibold tracking-wide">
                {player.username}
              </span>
            </div>

            <span
              className={`mt-2 flex items-center justify-center gap-2
                        text-xs tracking-widest
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
        </>
      ) : (
        <span className="text-gray-500 text-sm tracking-wide">
          Waiting for player
        </span>
      )}
    </motion.div>
  );
}
