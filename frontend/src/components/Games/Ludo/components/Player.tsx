import React, { forwardRef } from "react";
import { motion } from "framer-motion";
import type { Color } from "../Board/constants";

// ============================================
// Turn Timer Ring Component
// ============================================

interface TurnTimerRingProps {
  remainingSeconds: number;
  totalSeconds?: number;
  size?: number;
}

const TurnTimerRing: React.FC<TurnTimerRingProps> = ({
  remainingSeconds,
  totalSeconds = 60,
  size = 52,
}) => {
  const progress = Math.max(0, Math.min(1, remainingSeconds / totalSeconds));
  const circumference = 2 * Math.PI * ((size - 4) / 2);
  const strokeDashoffset = circumference * (1 - progress);
  const isLow = remainingSeconds <= 10;
  const isCritical = remainingSeconds <= 5;

  return (
    <div className="absolute inset-0 flex items-center justify-center">
      <svg
        className="-rotate-90"
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
      >
        {/* Background ring */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={(size - 4) / 2}
          fill="none"
          stroke="rgba(255,255,255,0.1)"
          strokeWidth={3}
        />
        {/* Progress ring */}
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={(size - 4) / 2}
          fill="none"
          stroke={isCritical ? "#ef4444" : isLow ? "#f97316" : "#8b5cf6"}
          strokeWidth={3}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          animate={{
            strokeDashoffset,
            stroke: isCritical ? "#ef4444" : isLow ? "#f97316" : "#8b5cf6",
          }}
          transition={{ duration: 0.3, ease: "linear" }}
        />
      </svg>
    </div>
  );
};

// ============================================
// Color Styles
// ============================================

const colorStyles: Record<Color, { text: string; border: string; bg: string; glow: string }> = {
  RED: {
    text: "text-red-400",
    border: "border-red-500",
    bg: "bg-red-500/20",
    glow: "shadow-[0_0_20px_rgba(239,68,68,0.4)]",
  },
  BLUE: {
    text: "text-blue-400",
    border: "border-blue-500",
    bg: "bg-blue-500/20",
    glow: "shadow-[0_0_20px_rgba(59,130,246,0.4)]",
  },
  YELLOW: {
    text: "text-yellow-400",
    border: "border-yellow-500",
    bg: "bg-yellow-500/20",
    glow: "shadow-[0_0_20px_rgba(234,179,8,0.4)]",
  },
  GREEN: {
    text: "text-green-400",
    border: "border-green-500",
    bg: "bg-green-500/20",
    glow: "shadow-[0_0_20px_rgba(34,197,94,0.4)]",
  },
};

// ============================================
// Main Player Component (Makao-style)
// ============================================

export type PlayerPosition = "top" | "top-left" | "top-right" | "left" | "right" | "bottom-left" | "bottom-right";

export interface LudoPlayerViewProps {
  id: string;
  username: string;
  color: Color;
  isActive: boolean;
  isBot: boolean;
  pawnsInBase: number;
  pawnsOnBoard: number;
  pawnsInHome: number;
  avatarUrl?: string;
}

interface PlayerProps {
  player: LudoPlayerViewProps;
  position: PlayerPosition;
  turnRemainingSeconds?: number | null;
  onPlayerClick?: (event: React.MouseEvent, playerId: string, username: string) => void;
}

const Player = forwardRef<HTMLDivElement, PlayerProps>(
  ({ player, position: _position, turnRemainingSeconds, onPlayerClick }, ref) => {
    const showTimer = player.isActive && !player.isBot && turnRemainingSeconds != null && turnRemainingSeconds > 0;
    const isLowTime = turnRemainingSeconds != null && turnRemainingSeconds <= 10;
    const styles = colorStyles[player.color];

    const handleAvatarClick = (e: React.MouseEvent) => {
      if (player.isBot || !onPlayerClick) return;
      onPlayerClick(e, player.id, player.username);
    };

    // Compact horizontal layout for players around the board
    return (
      <motion.div
        ref={ref}
        layout
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className={`
          relative flex items-center gap-3 px-3 py-2 rounded-xl
          backdrop-blur-sm transition-all duration-300
          ${player.isActive
            ? `${styles.bg} border ${styles.border} ${styles.glow}`
            : "bg-black/40 border border-white/10"
          }
        `}
      >
        {/* Avatar with timer ring */}
        <div
          className="relative w-[52px] h-[52px] cursor-pointer flex-shrink-0"
          onClick={handleAvatarClick}
        >
          {showTimer && <TurnTimerRing remainingSeconds={turnRemainingSeconds!} size={52} />}
          <div className="absolute inset-0 flex items-center justify-center">
            <div
              className={`w-10 h-10 rounded-full overflow-hidden border-2 ${
                player.isActive ? styles.border : "border-white/20"
              } ${isLowTime ? "animate-pulse" : ""}`}
            >
              <img
                src={player.avatarUrl || `/avatars/avatar_${Math.floor(Math.random() * 4) + 1}.png`}
                alt={player.username}
                className="w-full h-full object-cover"
                onError={(e) => {
                  const target = e.target as HTMLImageElement;
                  target.src = player.isBot ? "/avatars/bot_avatar.svg" : "/avatars/avatar_1.png";
                }}
              />
            </div>
          </div>

          {/* Active indicator */}
          {player.isActive && (
            <motion.div
              animate={{ scale: [1, 1.2, 1] }}
              transition={{ repeat: Infinity, duration: 1.5 }}
              className={`absolute -bottom-1 -right-1 w-4 h-4 rounded-full ${styles.border.replace('border-', 'bg-')} border-2 border-[#0d0c12]`}
            />
          )}

          {/* Bot indicator */}
          {player.isBot && (
            <div className="absolute -top-1 -right-1 px-1 py-0.5 bg-cyan-500/80 text-[8px] font-bold text-white rounded-full">
              BOT
            </div>
          )}
        </div>

        {/* Player info */}
        <div className="flex-1 min-w-0">
          <p className={`text-sm font-bold truncate ${player.isActive ? styles.text : "text-white"}`}>
            {player.username}
          </p>
          <div className="flex items-center gap-2 text-[10px] text-gray-500">
            <span className={styles.text}>{player.color}</span>
            <span>•</span>
            <span>{player.pawnsInHome}/4 home</span>
          </div>
        </div>
      </motion.div>
    );
  }
);

Player.displayName = "LudoPlayer";

export default Player;
