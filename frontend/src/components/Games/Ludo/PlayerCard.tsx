import { motion } from "framer-motion";
import type { Color } from "./Board/constants";
import { Crown } from "lucide-react";

interface PlayerPawn {
  position: number;
}

interface Player {
  userId: string;
  username: string;
  avatar: string;
  color: Color;
  isTurn: boolean;
  isHost?: boolean;
  pawns: PlayerPawn[];
}

interface PlayerCardProps {
  player: Player;
  side: "top-left" | "top-right" | "bottom-left" | "bottom-right";
}

export function PlayerCard({ player, side }: PlayerCardProps) {
  const cornerStyles = {
    "top-left": "top-0 left-0",
    "top-right": "top-0 right-0",
    "bottom-left": "bottom-0 left-0",
    "bottom-right": "bottom-0 right-0",
  };

  const glowStyles = {
    RED: "shadow-[0_0_30px_rgba(239,68,68,0.4),0_0_60px_rgba(239,68,68,0.2)] border-red-500/60",
    BLUE: "shadow-[0_0_30px_rgba(59,130,246,0.4),0_0_60px_rgba(59,130,246,0.2)] border-blue-500/60",
    YELLOW:
      "shadow-[0_0_30px_rgba(234,179,8,0.4),0_0_60px_rgba(234,179,8,0.2)] border-yellow-500/60",
    GREEN:
      "shadow-[0_0_30px_rgba(34,197,94,0.4),0_0_60px_rgba(34,197,94,0.2)] border-green-500/60",
  };

  const activePawns = player.pawns.filter((p) => p.position >= 0).length;

  return (
    <motion.div
      layout
      className={`absolute ${
        cornerStyles[side]
      } w-60 p-4 rounded-3xl transition-all duration-500 ${
        player.isTurn
          ? `bg-[#1a1825]/80 border-2 ${
              glowStyles[player.color]
            } z-30 ring-1 ring-white/20`
          : "bg-[#1a1825]/80 border border-white/10 z-10"
      }`}
    >
      {player.isTurn && (
        <div className="absolute inset-0 rounded-3xl bg-gradient-to-br from-white/[0.05] to-transparent pointer-events-none" />
      )}

      <div className="flex flex-col items-center relative z-10">
        <div
          className={`relative w-20 h-20 rounded-full p-1 border-2 transition-all duration-500 ${
            player.isTurn
              ? "border-white shadow-[0_0_20px_white/30]"
              : "border-white/10"
          }`}
        >
          <img
            src={player.avatar}
            alt={player.username}
            className="w-full h-full object-cover rounded-full bg-black/40"
          />

          {player.isTurn && (
            <motion.div
              animate={{ scale: [1, 1.2, 1], opacity: [0.8, 1, 0.8] }}
              transition={{ repeat: Infinity, duration: 2 }}
              className="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 border-4 border-[#1d1a29] rounded-full shadow-[0_0_15px_#22c55e]"
            />
          )}
        </div>

        <div className="mt-4 text-center w-full">
          <div className="flex justify-center items-center gap-2">
            {player.isHost && (
              <Crown
                size={14}
                className="text-yellow-400 drop-shadow-[0_0_5px_rgba(250,204,21,0.5)]"
              />
            )}
            <span className="text-base font-black italic text-white tracking-tight uppercase">
              {player.username}
            </span>
          </div>

          <div
            className={`mt-1 flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] ${
              player.isTurn ? "text-green-400" : "text-gray-500"
            }`}
          >
            <span
              className={`w-2 h-2 rounded-full ${
                player.isTurn
                  ? "bg-green-500 shadow-[0_0_8px_#22c55e] animate-pulse"
                  : "bg-gray-600"
              }`}
            />
            {player.isTurn ? "Your turn" : "Waiting"}
          </div>
        </div>

        <div className="mt-3 w-full grid grid-cols-2 gap-2 text-center">
          <div className="bg-black/60 rounded-xl p-2 border border-white/5">
            <p className="text-[7px] uppercase font-black text-white/40 tracking-widest mb-0.5">
              Pawns
            </p>
            <p className="text-xs font-bold text-white font-mono">
              {activePawns} / 4
            </p>
          </div>
          <div className="bg-black/60 rounded-xl p-2 border border-white/5">
            <p className="text-[7px] uppercase font-black text-white/40 tracking-widest mb-0.5">
              Sector
            </p>
            <p
              className={`text-[10px] font-black italic ${
                player.color === "RED"
                  ? "text-red-400"
                  : player.color === "BLUE"
                  ? "text-blue-400"
                  : player.color === "YELLOW"
                  ? "text-yellow-400"
                  : "text-green-400"
              }`}
            >
              {player.color}
            </p>
          </div>
        </div>
      </div>

      <div
        className={`absolute top-0 right-0 p-2 transition-opacity duration-500 ${
          player.isTurn ? "opacity-40" : "opacity-10"
        }`}
      >
        <div className="w-6 h-6 border-t-2 border-r-2 border-white rounded-tr-lg" />
      </div>
    </motion.div>
  );
}
