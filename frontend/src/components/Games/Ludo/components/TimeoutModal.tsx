import React from "react";
import { motion, AnimatePresence } from "framer-motion";

interface TimeoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  onReturnToLobby: () => void;
}

/**
 * Modal shown to players who have been replaced by a bot due to inactivity timeout.
 * Styled to match the Ludo game's futuristic/glassmorphism aesthetic.
 */
const TimeoutModal: React.FC<TimeoutModalProps> = ({
  isOpen,
  onClose,
  onReturnToLobby,
}) => {
  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[200] flex items-center justify-center bg-black/80 backdrop-blur-sm"
        >
          <motion.div
            initial={{ scale: 0.8, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.8, opacity: 0, y: 20 }}
            transition={{ type: "spring", damping: 20, stiffness: 300 }}
            className="bg-gradient-to-br from-[#1a1a27] to-[#0d0c12] rounded-3xl p-8 max-w-md mx-4 border border-red-500/30 shadow-2xl shadow-red-500/20"
          >
            {/* Warning Icon */}
            <div className="flex justify-center mb-6">
              <motion.div
                animate={{
                  scale: [1, 1.1, 1],
                  rotate: [0, -5, 5, 0],
                }}
                transition={{
                  duration: 2,
                  repeat: Infinity,
                  repeatType: "reverse",
                }}
                className="w-20 h-20 rounded-full bg-gradient-to-br from-red-500 to-orange-600 flex items-center justify-center shadow-lg shadow-red-500/40"
              >
                <svg
                  className="w-10 h-10 text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
                </svg>
              </motion.div>
            </div>

            {/* Title */}
            <h2 className="text-2xl font-black text-white text-center mb-3 uppercase tracking-wider">
              Turn Timeout
            </h2>

            {/* Message */}
            <p className="text-gray-300 text-center mb-6 leading-relaxed">
              You have been{" "}
              <span className="text-red-400 font-semibold">
                replaced by a bot
              </span>{" "}
              due to inactivity. The game continues with the bot playing in your
              place.
            </p>

            {/* Info box */}
            <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-4 mb-6">
              <div className="flex items-start gap-3">
                <svg
                  className="w-5 h-5 text-red-400 mt-0.5 flex-shrink-0"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <div className="text-sm text-gray-400">
                  <p className="font-medium text-red-400 mb-1">Why did this happen?</p>
                  <p>
                    Players have 60 seconds to make their move. If no action is
                    taken, the game automatically replaces inactive players with
                    bots to keep the game flowing.
                  </p>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="flex flex-col gap-3">
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={onReturnToLobby}
                className="w-full py-4 rounded-2xl bg-gradient-to-r from-purple-600 to-purple-500 text-white font-black uppercase tracking-widest shadow-lg shadow-purple-500/30 hover:shadow-purple-500/50 transition-shadow"
              >
                Exit
              </motion.button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default TimeoutModal;
