import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Crown } from "lucide-react";
import { Color } from "../Board/constants";

// ============================================
// Turn Timer Ring Component (Enhanced)
// ============================================

interface TurnTimerRingProps {
  remainingSeconds: number;
  totalSeconds?: number;
  size?: number;
}

const TurnTimerRing: React.FC<TurnTimerRingProps> = ({
  remainingSeconds,
  totalSeconds = 60,
  size = 100,
}) => {
  const progress = Math.max(0, Math.min(1, remainingSeconds / totalSeconds));
  const radius = (size - 8) / 2;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference * (1 - progress);
  const isLow = remainingSeconds <= 10;
  const isCritical = remainingSeconds <= 5;

  return (
    <svg
      className="absolute inset-0 -rotate-90"
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
    >
      {/* Background ring */}
      <circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke="rgba(255,255,255,0.1)"
        strokeWidth={4}
      />
      {/* Progress ring */}
      <motion.circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        fill="none"
        stroke={isCritical ? "#ef4444" : isLow ? "#f97316" : "#8b5cf6"}
        strokeWidth={4}
        strokeLinecap="round"
        strokeDasharray={circumference}
        initial={{ strokeDashoffset: 0 }}
        animate={{
          strokeDashoffset,
          stroke: isCritical ? "#ef4444" : isLow ? "#f97316" : "#8b5cf6",
        }}
        transition={{ duration: 0.5, ease: "linear" }}
      />
    </svg>
  );
};

// ============================================
// Color Styles for Each Player Color
// ============================================

const colorStyles: Record<
  Color,
  {
    text: string;
    border: string;
    bg: string;
    glow: string;
    gradient: string;
    ring: string;
  }
> = {
  RED: {
    text: "text-red-400",
    border: "border-red-500",
    bg: "bg-red-500/20",
    glow: "shadow-[0_0_30px_rgba(239,68,68,0.5)]",
    gradient: "from-red-600/30 to-red-900/20",
    ring: "ring-red-500/50",
  },
  BLUE: {
    text: "text-blue-400",
    border: "border-blue-500",
    bg: "bg-blue-500/20",
    glow: "shadow-[0_0_30px_rgba(59,130,246,0.5)]",
    gradient: "from-blue-600/30 to-blue-900/20",
    ring: "ring-blue-500/50",
  },
  YELLOW: {
    text: "text-yellow-400",
    border: "border-yellow-500",
    bg: "bg-yellow-500/20",
    glow: "shadow-[0_0_30px_rgba(234,179,8,0.5)]",
    gradient: "from-yellow-600/30 to-yellow-900/20",
    ring: "ring-yellow-500/50",
  },
  GREEN: {
    text: "text-green-400",
    border: "border-green-500",
    bg: "bg-green-500/20",
    glow: "shadow-[0_0_30px_rgba(34,197,94,0.5)]",
    gradient: "from-green-600/30 to-green-900/20",
    ring: "ring-green-500/50",
  },
};

// ============================================
// Corner Position Type
// ============================================

export type CornerPosition = "top-left" | "top-right" | "bottom-left" | "bottom-right";

// Map color to corner position
export const colorToCorner: Record<Color, CornerPosition> = {
  RED: "top-left",
  BLUE: "top-right",
  YELLOW: "bottom-right",
  GREEN: "bottom-left",
};

// ============================================
// Corner Player Card Props
// ============================================

export interface CornerPlayerCardProps {
  id: string;
  username: string;
  color: Color;
  isActive: boolean;
  isBot: boolean;
  isHost?: boolean;
  isMe?: boolean;
  pawnsInBase: number;
  pawnsOnBoard: number;
  pawnsInHome: number;
  avatarUrl?: string;
  turnRemainingSeconds?: number | null;
  onPlayerClick?: (
    event: React.MouseEvent,
    playerId: string,
    username: string
  ) => void;
}

// ============================================
// Corner Player Card Component
// ============================================

export const CornerPlayerCard: React.FC<CornerPlayerCardProps> = ({
  id,
  username,
  color,
  isActive,
  isBot,
  isHost = false,
  isMe = false,
  pawnsInBase,
  pawnsOnBoard,
  pawnsInHome,
  avatarUrl,
  turnRemainingSeconds,
  onPlayerClick,
}) => {
  const styles = colorStyles[color];
  const corner = colorToCorner[color];
  const showTimer =
    isActive && !isBot && turnRemainingSeconds != null && turnRemainingSeconds > 0;
  const isLowTime = turnRemainingSeconds != null && turnRemainingSeconds <= 10;
  const isCriticalTime = turnRemainingSeconds != null && turnRemainingSeconds <= 5;

  // Position styles for each corner
  const cornerPositionStyles: Record<CornerPosition, string> = {
    "top-left": "top-2 left-2",
    "top-right": "top-2 right-2",
    "bottom-left": "bottom-2 left-2",
    "bottom-right": "bottom-2 right-2",
  };

  const handleClick = (e: React.MouseEvent) => {
    if (isBot || !onPlayerClick) return;
    onPlayerClick(e, id, username);
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.8 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: "spring", damping: 20, stiffness: 300 }}
      className={`absolute ${cornerPositionStyles[corner]} z-30`}
    >
      <motion.div
        layout
        className={`
          relative w-36 h-44 rounded-2xl overflow-hidden
          backdrop-blur-xl transition-all duration-500
          border-2 ${isActive ? styles.border : "border-white/10"}
          ${isActive ? styles.glow : "shadow-lg shadow-black/30"}
          bg-gradient-to-br ${isActive ? styles.gradient : "from-[#1a1825]/90 to-[#0d0c12]/90"}
        `}
        animate={
          isActive && isCriticalTime
            ? { scale: [1, 1.02, 1] }
            : {}
        }
        transition={
          isActive && isCriticalTime
            ? { duration: 0.5, repeat: Infinity }
            : {}
        }
      >
        {/* Glassmorphism overlay */}
        <div className="absolute inset-0 bg-gradient-to-b from-white/5 to-transparent pointer-events-none" />

        {/* Active glow effect */}
        <AnimatePresence>
          {isActive && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className={`absolute inset-0 ${styles.bg} animate-pulse pointer-events-none`}
            />
          )}
        </AnimatePresence>

        {/* Content */}
        <div className="relative h-full flex flex-col items-center justify-between p-3">
          {/* Host Crown */}
          <div className="h-5 flex items-center justify-center">
            {isHost && (
              <Crown
                size={16}
                className="text-yellow-400 drop-shadow-[0_0_8px_rgba(250,204,21,0.6)]"
              />
            )}
          </div>

          {/* Avatar with Timer Ring */}
          <div
            className="relative cursor-pointer"
            onClick={handleClick}
          >
            {/* Timer Ring */}
            {showTimer && (
              <TurnTimerRing
                remainingSeconds={turnRemainingSeconds!}
                size={76}
              />
            )}

            {/* Avatar Container */}
            <div
              className={`
                w-[68px] h-[68px] rounded-full overflow-hidden
                border-3 ${isActive ? styles.border : "border-white/20"}
                ${isLowTime ? "animate-pulse" : ""}
                transition-all duration-300
              `}
            >
              <img
                src={avatarUrl || `/avatars/avatar_1.png`}
                alt={username}
                className="w-full h-full object-cover"
                onError={(e) => {
                  const target = e.target as HTMLImageElement;
                  target.src = isBot
                    ? "/avatars/bot_avatar.svg"
                    : "/avatars/avatar_1.png";
                }}
              />
            </div>

            {/* Active Pulse Indicator */}
            {isActive && !showTimer && (
              <motion.div
                animate={{ scale: [1, 1.3, 1], opacity: [0.8, 0.4, 0.8] }}
                transition={{ repeat: Infinity, duration: 1.5 }}
                className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full ${styles.bg} ${styles.border} border-2`}
              />
            )}

            {/* Bot Badge */}
            {isBot && (
              <div className="absolute -top-1 -right-1 px-1.5 py-0.5 bg-cyan-500 text-[8px] font-bold text-white rounded-full shadow-lg">
                BOT
              </div>
            )}
          </div>

          {/* Player Info */}
          <div className="text-center w-full">
            <p
              className={`text-xs font-bold truncate ${
                isActive ? styles.text : "text-white"
              }`}
            >
              {username}
              {isMe && (
                <span className="text-[10px] text-gray-400 ml-1">(You)</span>
              )}
            </p>

            {/* Color Badge */}
            <div
              className={`
                mt-1 inline-flex items-center gap-1 px-2 py-0.5 rounded-full
                ${styles.bg} ${styles.border} border text-[9px] font-bold uppercase tracking-wider
                ${styles.text}
              `}
            >
              <span
                className={`w-2 h-2 rounded-full ${styles.border.replace(
                  "border-",
                  "bg-"
                )}`}
              />
              {color}
            </div>
          </div>

          {/* Stats Row */}
          <div className="flex items-center justify-center gap-2 text-[9px] text-gray-400">
            <span>{pawnsInHome}/4 🏠</span>
            <span>•</span>
            <span>{pawnsOnBoard} ⚔️</span>
          </div>

          {/* Timer Display (when active) */}
          {showTimer && (
            <motion.div
              animate={
                isCriticalTime
                  ? { opacity: [1, 0.3, 1], scale: [1, 1.1, 1] }
                  : isLowTime
                  ? { opacity: [1, 0.5, 1] }
                  : {}
              }
              transition={{ duration: 0.5, repeat: Infinity }}
              className={`
                absolute bottom-1 right-1 px-2 py-0.5 rounded-lg
                text-xs font-mono font-black
                ${
                  isCriticalTime
                    ? "bg-red-500/30 text-red-400 border border-red-500/50"
                    : isLowTime
                    ? "bg-orange-500/30 text-orange-400 border border-orange-500/50"
                    : "bg-purple-500/30 text-purple-400 border border-purple-500/50"
                }
              `}
            >
              {turnRemainingSeconds}s
            </motion.div>
          )}
        </div>
      </motion.div>
    </motion.div>
  );
};

export default CornerPlayerCard;
