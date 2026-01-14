import React from "react";
import { motion } from "framer-motion";
import type { Card as CardType } from "../types";
import Card from "../Card";
import { requiresDemand, getCardEffectDescription } from "../utils/cardHelpers";

interface DrawnCardModalProps {
  card: CardType;
  canPlay: boolean;
  onPlay: () => void;
  onSkip: () => void;
  isLoading?: boolean;
}

/**
 * Modal shown when player draws a card - allows playing or keeping it
 */
const DrawnCardModal: React.FC<DrawnCardModalProps> = ({
  card,
  canPlay,
  onPlay,
  onSkip,
  isLoading = false,
}) => {
  const needsDemand = requiresDemand(card);
  const effectDescription = getCardEffectDescription(card);

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/70 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0, y: 20 }}
        animate={{ scale: 1, opacity: 1, y: 0 }}
        exit={{ scale: 0.9, opacity: 0, y: 20 }}
        className="bg-[#1a1a27] rounded-2xl p-6 max-w-sm w-full mx-4 border border-purpleEnd/30 shadow-2xl text-center"
      >
        <h3 className="text-xl font-bold text-white mb-4">You drew a card!</h3>

        <div className="flex justify-center mb-4">
          <Card card={card} size="lg" isPlayable={canPlay} showEffect />
        </div>

        {effectDescription && (
          <p className="text-yellow-400 text-sm mb-4 bg-yellow-400/10 py-2 px-3 rounded-lg">
            ⚡ {effectDescription}
          </p>
        )}

        {canPlay ? (
          <div className="space-y-3">
            <p className="text-green-400 text-sm mb-3">
              ✓ This card can be played!
            </p>

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={onPlay}
              disabled={isLoading}
              className="w-full py-3 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold shadow-neon disabled:opacity-50"
            >
              {needsDemand ? "Play & Choose Demand" : "Play Card"}
            </motion.button>

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={onSkip}
              disabled={isLoading}
              className="w-full py-3 rounded-xl bg-gray-700 hover:bg-gray-600 text-white font-medium transition-colors disabled:opacity-50"
            >
              Keep Card & End Turn
            </motion.button>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-gray-400 text-sm mb-3">
              This card cannot be played right now.
            </p>

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={onSkip}
              disabled={isLoading}
              className="w-full py-3 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold shadow-neon disabled:opacity-50"
            >
              Keep Card & End Turn
            </motion.button>
          </div>
        )}

        {isLoading && (
          <div className="mt-4 flex justify-center">
            <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-purpleEnd" />
          </div>
        )}
      </motion.div>
    </motion.div>
  );
};

export default DrawnCardModal;
