import React, { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface EffectToastProps {
  effectNotification: string | null | undefined;
  specialEffectActive: boolean;
  demandedSuit?: string | null;
  demandedRank?: string | null;
  isMyTurn: boolean;
}

interface ToastMessage {
  id: string;
  message: string;
  type: "effect" | "demand" | "warning" | "info";
}

/**
 * Effect Toast - displays card effect notifications as animated toasts
 * with auto-dismiss functionality
 */
const EffectToast: React.FC<EffectToastProps> = ({
  effectNotification,
  specialEffectActive,
  demandedSuit,
  demandedRank,
  isMyTurn,
}) => {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const [lastNotification, setLastNotification] = useState<string | null>(null);

  // Add new toast when effectNotification changes
  useEffect(() => {
    if (effectNotification && effectNotification !== lastNotification) {
      const newToast: ToastMessage = {
        id: `${Date.now()}-${Math.random()}`,
        message: effectNotification,
        type: determineToastType(effectNotification),
      };
      setToasts((prev) => [...prev.slice(-2), newToast]); // Keep max 3 toasts
      setLastNotification(effectNotification);

      // Auto-dismiss after 5 seconds
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== newToast.id));
      }, 5000);
    }
  }, [effectNotification, lastNotification]);

  // Helper to determine toast type based on message content
  const determineToastType = (message: string): ToastMessage["type"] => {
    if (message.includes("skip") || message.includes("draw")) return "warning";
    if (message.includes("demands") || message.includes("Demands")) return "demand";
    if (message.includes("reversed")) return "info";
    return "effect";
  };

  // Get toast styling based on type
  const getToastStyles = (type: ToastMessage["type"]) => {
    switch (type) {
      case "warning":
        return {
          bg: "bg-gradient-to-r from-orange-500/90 to-red-500/90",
          border: "border-orange-400/50",
          icon: "⚠️",
          textColor: "text-white",
        };
      case "demand":
        return {
          bg: "bg-gradient-to-r from-purple-600/90 to-indigo-600/90",
          border: "border-purple-400/50",
          icon: "🎯",
          textColor: "text-white",
        };
      case "info":
        return {
          bg: "bg-gradient-to-r from-blue-500/90 to-cyan-500/90",
          border: "border-blue-400/50",
          icon: "🔄",
          textColor: "text-white",
        };
      default:
        return {
          bg: "bg-gradient-to-r from-purpleStart/90 to-purpleEnd/90",
          border: "border-purpleEnd/50",
          icon: "✨",
          textColor: "text-white",
        };
    }
  };

  // Get suit symbol and color
  const getSuitDisplay = (suit: string) => {
    const suits: Record<string, { symbol: string; color: string }> = {
      HEARTS: { symbol: "♥", color: "text-red-400" },
      DIAMONDS: { symbol: "♦", color: "text-red-400" },
      CLUBS: { symbol: "♣", color: "text-gray-200" },
      SPADES: { symbol: "♠", color: "text-gray-200" },
    };
    return suits[suit] || { symbol: suit, color: "text-white" };
  };

  return (
    <>
      {/* Toast Container - positioned at top center */}
      <div className="fixed top-24 left-1/2 -translate-x-1/2 z-50 flex flex-col gap-2 pointer-events-none">
        <AnimatePresence mode="popLayout">
          {toasts.map((toast) => {
            const styles = getToastStyles(toast.type);

            return (
              <motion.div
                key={toast.id}
                initial={{ opacity: 0, y: -50, scale: 0.8 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -20, scale: 0.8 }}
                transition={{
                  type: "spring",
                  stiffness: 400,
                  damping: 25,
                }}
                className={`
                  ${styles.bg} ${styles.border}
                  border rounded-xl px-5 py-3 shadow-2xl
                  backdrop-blur-sm min-w-[280px] max-w-md
                  pointer-events-auto
                `}
              >
                <div className="flex items-center gap-3">
                  <span className="text-2xl">{styles.icon}</span>
                  <div className="flex-1">
                    <p className={`font-medium ${styles.textColor}`}>
                      {toast.message}
                    </p>
                    {isMyTurn && toast.type === "warning" && (
                      <p className="text-xs text-white/70 mt-1">
                        Accept the effect or play a counter card!
                      </p>
                    )}
                  </div>
                </div>

                {/* Progress bar for auto-dismiss */}
                <motion.div
                  initial={{ width: "100%" }}
                  animate={{ width: "0%" }}
                  transition={{ duration: 5, ease: "linear" }}
                  className="absolute bottom-0 left-0 h-1 bg-white/30 rounded-b-xl"
                />
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>

      {/* Persistent Effect Banner (when special effect is active) */}
      <AnimatePresence>
        {specialEffectActive && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="fixed top-20 right-4 z-40"
          >
            <div className="bg-orange-500/20 border border-orange-500/40 rounded-xl px-4 py-2 backdrop-blur-sm">
              <div className="flex items-center gap-2">
                <motion.div
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 1, repeat: Infinity }}
                  className="w-3 h-3 rounded-full bg-orange-500"
                />
                <span className="text-sm font-medium text-orange-400">
                  Special Effect Active!
                </span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Demand Display (when a suit or rank is demanded) */}
      <AnimatePresence>
        {(demandedSuit || demandedRank) && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.8 }}
            className="fixed top-20 left-4 z-40"
          >
            <div className="bg-purple-500/20 border border-purple-500/40 rounded-xl px-4 py-3 backdrop-blur-sm">
              <p className="text-xs text-gray-400 mb-1">Current Demand</p>
              <div className="flex items-center gap-2">
                {demandedSuit && (
                  <div className="flex items-center gap-1">
                    <span className="text-2xl">
                      {getSuitDisplay(demandedSuit).symbol}
                    </span>
                    <span className={`font-bold ${getSuitDisplay(demandedSuit).color}`}>
                      {demandedSuit.charAt(0) + demandedSuit.slice(1).toLowerCase()}
                    </span>
                  </div>
                )}
                {demandedRank && (
                  <div className="flex items-center gap-1">
                    <span className="text-xl font-bold text-purpleEnd">
                      {demandedRank}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default EffectToast;
