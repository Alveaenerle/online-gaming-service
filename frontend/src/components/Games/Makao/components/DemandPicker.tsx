import React from "react";
import { motion } from "framer-motion";
import { type CardSuit, type CardRank, ALL_SUITS, DEMANDABLE_RANKS } from "../types";
import { SUIT_INFO, RANK_DISPLAY } from "../utils/cardHelpers";

interface DemandPickerProps {
  type: "suit" | "rank";
  onSelect: (value: CardSuit | CardRank) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

/**
 * Modal for selecting demanded suit (Ace) or rank (Jack)
 */
const DemandPicker: React.FC<DemandPickerProps> = ({
  type,
  onSelect,
  onCancel,
  isLoading = false,
}) => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/70 flex items-center justify-center z-50"
      onClick={onCancel}
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        exit={{ scale: 0.9, opacity: 0 }}
        className="bg-[#1a1a27] rounded-2xl p-6 max-w-md w-full mx-4 border border-purpleEnd/30 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="text-xl font-bold text-white mb-2 text-center">
          {type === "suit" ? "Choose a Suit" : "Choose a Rank"}
        </h3>
        <p className="text-gray-400 text-sm text-center mb-6">
          {type === "suit"
            ? "Select the suit you want to demand (Ace effect)"
            : "Select the rank you want to demand (Jack effect)"}
        </p>

        <div className="grid grid-cols-2 gap-3">
          {type === "suit"
            ? ALL_SUITS.map((suit) => (
                <motion.button
                  key={suit}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => onSelect(suit)}
                  disabled={isLoading}
                  className={`
                    p-4 rounded-xl text-center font-bold transition-all
                    bg-[#121018] hover:bg-[#252532] border border-white/10
                    disabled:opacity-50 disabled:cursor-not-allowed
                  `}
                >
                  <span className={`text-4xl ${SUIT_INFO[suit].color}`}>
                    {SUIT_INFO[suit].symbol}
                  </span>
                  <p className="text-white text-sm mt-2 capitalize">
                    {SUIT_INFO[suit].name}
                  </p>
                </motion.button>
              ))
            : DEMANDABLE_RANKS.map((rank) => (
                <motion.button
                  key={rank}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => onSelect(rank)}
                  disabled={isLoading}
                  className={`
                    p-4 rounded-xl text-center font-bold transition-all
                    bg-[#121018] hover:bg-[#252532] border border-white/10
                    disabled:opacity-50 disabled:cursor-not-allowed
                  `}
                >
                  <span className="text-3xl text-white">
                    {RANK_DISPLAY[rank]}
                  </span>
                </motion.button>
              ))}
        </div>

        <motion.button
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          onClick={onCancel}
          disabled={isLoading}
          className="mt-6 w-full py-3 text-gray-400 hover:text-white transition-colors text-sm disabled:opacity-50"
        >
          Cancel
        </motion.button>
      </motion.div>
    </motion.div>
  );
};

export default DemandPicker;
