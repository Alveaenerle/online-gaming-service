import React, { useRef, useEffect, useState, forwardRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { PlayerView } from "../types";

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
  size = 44,
}) => {
  const progress = Math.max(0, Math.min(1, remainingSeconds / totalSeconds));
  const circumference = 2 * Math.PI * ((size - 4) / 2);
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
  );
};

// ============================================
// Makao Badge Component
// ============================================

interface MakaoBadgeProps {
  isVisible: boolean;
}

const MakaoBadge: React.FC<MakaoBadgeProps> = ({ isVisible }) => {
  return (
    <AnimatePresence>
      {isVisible && (
        <motion.div
          initial={{ scale: 0, rotate: -20, opacity: 0 }}
          animate={{
            scale: 1,
            rotate: 0,
            opacity: 1,
          }}
          exit={{ scale: 0, rotate: 20, opacity: 0 }}
          transition={{
            type: "spring",
            stiffness: 500,
            damping: 15,
          }}
          className="absolute -top-2 -right-2 z-20"
        >
          <motion.div
            animate={{
              scale: [1, 1.1, 1],
              rotate: [0, -5, 5, 0],
            }}
            transition={{
              duration: 1.5,
              repeat: Infinity,
              repeatType: "reverse",
            }}
            className="bg-gradient-to-r from-yellow-400 to-orange-500 text-black text-[9px] font-black px-1.5 py-0.5 rounded-full shadow-lg shadow-yellow-500/50 border border-yellow-300"
          >
            MAKAO!
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// ============================================
// Bot Thinking Bubble Component
// ============================================

interface BotThinkingBubbleProps {
  isThinking: boolean;
}

const BotThinkingBubble: React.FC<BotThinkingBubbleProps> = ({ isThinking }) => {
  return (
    <AnimatePresence>
      {isThinking && (
        <motion.div
          initial={{ opacity: 0, scale: 0.5, x: -10 }}
          animate={{ opacity: 1, scale: 1, x: 0 }}
          exit={{ opacity: 0, scale: 0.5, x: -10 }}
          transition={{ type: "spring", stiffness: 400, damping: 20 }}
          className="absolute -right-16 top-1/2 -translate-y-1/2 z-10"
        >
          <div className="bg-gray-800/95 backdrop-blur-sm border border-cyan-500/30 rounded-lg px-2 py-1 shadow-lg relative">
            {/* Speech bubble arrow */}
            <div className="absolute left-0 top-1/2 -translate-x-full -translate-y-1/2">
              <div className="w-0 h-0 border-t-[6px] border-t-transparent border-b-[6px] border-b-transparent border-r-[6px] border-r-gray-800" />
            </div>
            <div className="flex items-center gap-1">
              <span className="text-[10px] text-cyan-400">Thinking</span>
              <div className="flex gap-0.5">
                {[0, 1, 2].map((i) => (
                  <motion.span
                    key={i}
                    animate={{
                      y: [0, -3, 0],
                      opacity: [0.4, 1, 0.4],
                    }}
                    transition={{
                      duration: 0.5,
                      repeat: Infinity,
                      delay: i * 0.12,
                    }}
                    className="w-1 h-1 rounded-full bg-cyan-400"
                  />
                ))}
              </div>
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

// ============================================
// Main Player Component
// ============================================

import { Position } from "../utils/cardHelpers";

interface PositionedPlayerView extends PlayerView {
  position: Position;
}

export interface PlayerProps {
  player: PositionedPlayerView;
  isBot?: boolean;
  isBotThinking?: boolean;
  hasMakao?: boolean;
  turnRemainingSeconds?: number | null;
}

const Player = forwardRef<HTMLDivElement, PlayerProps>(({
  player,
  isBot: playerIsBot = false,
  isBotThinking = false,
  hasMakao = false,
  turnRemainingSeconds,
}, ref) => {
  const showTimer = player.isActive && !playerIsBot && turnRemainingSeconds != null && turnRemainingSeconds > 0;
  const isLowTime = turnRemainingSeconds != null && turnRemainingSeconds <= 10;

  return (
    <motion.div
      ref={ref}
      layout
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`relative bg-[#1a1a27] p-3 rounded-xl border transition-all ${
        player.isActive
          ? isLowTime
            ? "border-red-500 shadow-lg shadow-red-500/20"
            : "border-purpleEnd shadow-lg shadow-purpleEnd/20"
          : "border-white/10"
      }`}
    >
      <div className="flex items-center gap-2">
        {/* Avatar with Timer Ring */}
        <div className="relative">
          {/* Timer Ring */}
          {showTimer && (
            <TurnTimerRing
              remainingSeconds={turnRemainingSeconds}
              totalSeconds={60}
              size={44}
            />
          )}

          {/* Avatar Image */}
          <div
            className={`w-10 h-10 rounded-full overflow-hidden border-2 ${
              player.isActive
                ? isLowTime
                  ? "border-red-500"
                  : "border-purpleEnd"
                : playerIsBot
                ? "border-cyan-500/50"
                : "border-white/20"
            }`}
          >
            <img
              src={player.avatarUrl}
              alt={`${player.username}'s avatar`}
              className="w-full h-full object-cover"
              onError={(e) => {
                // Fallback to default avatar if image fails to load
                const target = e.target as HTMLImageElement;
                target.src = playerIsBot ? "/avatars/bot_avatar.svg" : "/avatars/avatar_1.png";
              }}
            />
          </div>

          {/* MAKAO Badge */}
          <MakaoBadge isVisible={hasMakao} />
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1">
            <p className="text-sm font-medium text-white truncate">
              {player.username}
            </p>
            {playerIsBot && (
              <span className="text-[9px] text-cyan-400 bg-cyan-500/20 px-1 rounded">
                BOT
              </span>
            )}
          </div>
          <div className="flex items-center gap-2">
            <p className="text-xs text-gray-400">{player.cardCount} cards</p>
            {/* Timer text */}
            {showTimer && (
              <motion.span
                animate={isLowTime ? { opacity: [1, 0.5, 1] } : {}}
                transition={{ duration: 0.5, repeat: Infinity }}
                className={`text-[10px] font-mono ${
                  isLowTime ? "text-red-400" : "text-gray-500"
                }`}
              >
                {turnRemainingSeconds}s
              </motion.span>
            )}
          </div>
        </div>

        {/* Active indicator */}
        {player.isActive && !showTimer && (
          <motion.div
            animate={{
              scale: [1, 1.2, 1],
              opacity: [0.5, 1, 0.5]
            }}
            transition={{ duration: 1.5, repeat: Infinity }}
            className={`w-2.5 h-2.5 rounded-full ${
              playerIsBot ? "bg-cyan-400" : "bg-purpleEnd"
            }`}
          />
        )}
      </div>

      {/* Bot Thinking Bubble - positioned outside */}
      {playerIsBot && <BotThinkingBubble isThinking={isBotThinking} />}

      {/* Status indicators */}
      <div className="flex gap-2 mt-1">
        {player.skipTurns > 0 && (
          <motion.span
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            className="text-[10px] text-orange-400 bg-orange-500/20 px-1.5 py-0.5 rounded"
          >
            ⏭️ Skips: {player.skipTurns}
          </motion.span>
        )}
        {player.placement && (
          <motion.span
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            className="text-[10px] text-green-400 bg-green-500/20 px-1.5 py-0.5 rounded"
          >
            🏆 #{player.placement}
          </motion.span>
        )}
      </div>
    </motion.div>
  );
});

Player.displayName = "Player";

export default Player;
