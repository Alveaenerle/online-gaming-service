import React, { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CardSuit, CardRank } from "../types";
import { SUIT_INFO, RANK_DISPLAY } from "../utils/cardHelpers";

interface SidebarNotificationsProps {
  effectNotification: string | null | undefined;
  specialEffectActive: boolean;
  demandedSuit?: CardSuit | null;
  demandedRank?: CardRank | null;
  isMyTurn: boolean;
  lastMoveLog?: string | null;
}

interface ToastMessage {
  id: string;
  message: string;
  type: "effect" | "demand" | "warning" | "info" | "move";
  timestamp: number;
}

const SidebarNotifications: React.FC<SidebarNotificationsProps> = ({
  effectNotification,
  specialEffectActive,
  demandedSuit,
  demandedRank,
  isMyTurn,
  lastMoveLog,
}) => {
  const [notifications, setNotifications] = useState<ToastMessage[]>([]);

  // Helper to add notification
  const addNotification = (message: string, type: ToastMessage["type"]) => {
    // Avoid duplicates if the message is identical to the very last one
    setNotifications((prev) => {
      if (prev.length > 0 && prev[prev.length - 1].message === message) {
        return prev;
      }
      const newToast: ToastMessage = {
        id: `${Date.now()}-${Math.random()}`,
        message,
        type,
        timestamp: Date.now(),
      };
      // Keep only last 5 notifications
      return [...prev, newToast].slice(-5);
    });
  };

  // Watch for move logs (Game Action) - Priority 1
  useEffect(() => {
    if (lastMoveLog) {
      addNotification(lastMoveLog, "move");
    }
  }, [lastMoveLog]);

  // Watch for effect notifications (Consequence) - Priority 2
  useEffect(() => {
    if (effectNotification) {
      addNotification(effectNotification, "effect");
    }
  }, [effectNotification]);

  // Watch for active special effect state
  useEffect(() => {
    if (specialEffectActive) {
       // Optional: We could add a persistent marker or just relying on the visual state in the notification list
       // For now, let's just ensure the user knows.
    }
  }, [specialEffectActive]);

  // Render demanded suit/rank as a persistent info card if active
  const renderDemandCard = () => {
    if (!demandedSuit && !demandedRank) return null;

    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.9 }}
        className="w-full bg-gradient-to-br from-purple-900/80 to-indigo-900/80 p-3 rounded-xl border border-purple-500/30 mb-2 shadow-lg"
      >
        <div className="flex items-center gap-2 mb-1">
          <span className="text-xl">🎯</span>
          <span className="text-xs font-bold text-gray-300 uppercase tracking-wider">Current Demand</span>
        </div>
        <div className="flex items-center justify-between">
            {demandedSuit && (
              <div className="flex items-center gap-2">
                <span className={`text-2xl ${SUIT_INFO[demandedSuit].color}`}>
                  {SUIT_INFO[demandedSuit].symbol}
                </span>
                <span className="font-bold text-white text-sm">
                  {SUIT_INFO[demandedSuit].name}
                </span>
              </div>
            )}
            {demandedRank && (
              <div className="flex items-center gap-2">
                <span className="font-bold text-lg text-purpleEnd">
                  {RANK_DISPLAY[demandedRank]}
                </span>
              </div>
            )}
        </div>
      </motion.div>
    );
  };

  const getTypeStyles = (type: ToastMessage["type"]) => {
    switch (type) {
      case "effect":
        return "bg-purple-600/20 border-purple-500/30 text-purple-100";
      case "warning":
        return "bg-orange-600/20 border-orange-500/30 text-orange-100";
      case "demand":
        return "bg-blue-600/20 border-blue-500/30 text-blue-100";
      case "move":
        return "bg-gray-700/40 border-gray-600/30 text-gray-300";
      default:
        return "bg-gray-800/40 border-gray-700/30 text-gray-300";
    }
  };

  return (
    <div className="flex flex-col gap-2 w-full mt-auto">
      <AnimatePresence>
        {renderDemandCard()}
      </AnimatePresence>

      {/* Constraints added here */}
      <div className="flex flex-col-reverse gap-2 overflow-y-auto max-h-[300px] pr-1 scrollbar-thin scrollbar-thumb-white/20 scrollbar-track-transparent">
        <AnimatePresence initial={false} mode="popLayout">
          {notifications.map((toast) => (
            <motion.div
              key={toast.id}
              layout
              initial={{ opacity: 0, x: 100 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 100, transition: { duration: 0.2 } }}
              transition={{ type: "spring", stiffness: 400, damping: 25 }}
              className={`p-3 rounded-l-xl border-l-4 text-sm backdrop-blur-md shadow-lg mb-2 relative overflow-hidden ${getTypeStyles(toast.type)}`}
            >
              <div className="relative z-10 flex justify-between items-start gap-2">
                 <span className="font-medium">{toast.message}</span>
                 <span className="text-[10px] opacity-60 font-mono whitespace-nowrap mt-0.5">
                   {new Date(toast.timestamp).toLocaleTimeString([], { hour12: false, hour: '2-digit', minute:'2-digit' })}
                 </span>
              </div>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default SidebarNotifications;
