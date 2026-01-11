import { motion, AnimatePresence } from "framer-motion";
import { Bell, X } from "lucide-react";

interface GameNotificationProps {
  message: string;
  isVisible: boolean;
  onClose: () => void;
}

export function GameNotification({
  message,
  isVisible,
  onClose,
}: GameNotificationProps) {
  return (
    <div className="absolute top-12 left-0 right-0 z-20 flex justify-center px-8">
      <AnimatePresence>
        {isVisible && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20, scale: 0.95 }}
            className="relative w-full max-w-xl bg-purple-600/10 border border-purple-500/30 backdrop-blur-xl rounded-2xl p-4 flex items-center justify-between shadow-[0_0_30px_rgba(168,85,247,0.1)] mb-3"
          >
            <div className="flex items-center gap-4">
              <div className="bg-purple-500/20 p-2 rounded-lg">
                <Bell size={18} className="text-purple-400 animate-pulse" />
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] font-black uppercase tracking-[0.2em] text-purple-400/60">
                  System Notification
                </span>
                <span className="text-white text-sm font-medium italic">
                  "{message}"
                </span>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-white/5 rounded-full text-white/20 hover:text-white transition-all"
            >
              <X size={18} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
