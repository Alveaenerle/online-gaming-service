import { motion, AnimatePresence } from "framer-motion";
import { useEffect } from "react";
import {
  X,
  Dices,
  Move,
  Swords,
  AlertTriangle,
  Info,
  CheckCircle,
} from "lucide-react";

export type NotificationType =
  | "INFO"
  | "ROLLING"
  | "ROLLED"
  | "MOVING"
  | "COMBAT"
  | "ERROR";

interface GameNotificationProps {
  message: string;
  type: NotificationType;
  isVisible: boolean;
  onClose: () => void;
  autoCloseMs?: number;
}

export function GameNotification({
  message,
  type = "INFO",
  isVisible,
  onClose,
  autoCloseMs = 3000,
}: GameNotificationProps) {
  // Reset timer when message changes (even if isVisible stays true)
  useEffect(() => {
    if (isVisible && autoCloseMs > 0) {
      const timer = setTimeout(onClose, autoCloseMs);
      return () => clearTimeout(timer);
    }
  }, [isVisible, autoCloseMs, onClose, message]);

  const config = {
    INFO: {
      icon: Info,
      label: "System Message",
      theme: "bg-purple-600/10 border-purple-500/30 text-purple-400",
      glow: "shadow-purple-500/10",
    },
    ROLLING: {
      icon: Dices,
      label: "Rolling a Dice",
      theme: "bg-blue-600/10 border-blue-500/30 text-blue-400",
      glow: "shadow-blue-500/10",
      animate: "animate-spin",
    },
    ROLLED: {
      icon: CheckCircle,
      label: "Roll Result",
      theme: "bg-emerald-600/10 border-emerald-500/30 text-emerald-400",
      glow: "shadow-emerald-500/10",
    },
    MOVING: {
      icon: Move,
      label: "Pawn Movement",
      theme: "bg-amber-600/10 border-amber-500/30 text-amber-400",
      glow: "shadow-amber-500/10",
    },
    COMBAT: {
      icon: Swords,
      label: "Combat Encounter",
      theme: "bg-red-600/10 border-red-500/30 text-red-400",
      glow: "shadow-red-500/10",
    },
    ERROR: {
      icon: AlertTriangle,
      label: "System Error",
      theme: "bg-red-900/20 border-red-600/40 text-red-500",
      glow: "shadow-red-600/20",
    },
  }[type];

  const Icon = config.icon;

  return (
    <div className="absolute top-12 left-0 right-0 z-[100] flex justify-center px-8 pointer-events-none">
      <AnimatePresence>
        {isVisible && (
          <motion.div
            initial={{ opacity: 0, y: -40, filter: "blur(10px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className={`
              relative w-full max-w-xl p-4 flex items-center justify-between 
              backdrop-blur-xl rounded-2xl border shadow-2xl pointer-events-auto
              transition-colors duration-500
              ${config.theme} ${config.glow}
            `}
          >
            <div className="absolute inset-0 bg-gradient-to-r from-white/5 to-transparent rounded-2xl pointer-events-none" />

            <div className="flex items-center gap-4 relative z-10">
              <div className="bg-black/20 p-2.5 rounded-xl border border-white/5 flex items-center justify-center">
                <Icon
                  size={20}
                  className={`${
                    config.animate || "animate-pulse"
                  } stroke-[2.5px]`}
                />
              </div>

              <div className="flex flex-col">
                <span className="text-[9px] font-black uppercase tracking-[0.3em] opacity-60">
                  {config.label}
                </span>
                <span className="text-white text-sm font-bold tracking-tight italic">
                  {message}
                </span>
              </div>
            </div>

            <button
              onClick={onClose}
              className="p-2 hover:bg-white/10 rounded-full text-white/20 hover:text-white transition-all relative z-10"
            >
              <X size={18} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
