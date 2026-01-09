import React from "react";
import { motion } from "framer-motion";
import { X, Loader2 } from "lucide-react";
import Card from "./Card";
import { Suit, Rank, DemandType } from "./types";
import { SUITS, DEMANDABLE_RANKS, SUIT_SYMBOLS, SUIT_COLORS, convertRank, convertSuit } from "./constants";

// ============================================
// Demand Modal - for Jack (rank) and Ace (suit)
// ============================================

interface DemandModalProps {
  type: DemandType;
  onSelect: (value: string) => void;
  onClose: () => void;
}

export const DemandModal: React.FC<DemandModalProps> = ({ type, onSelect, onClose }) => {
  const items = type === "suit" ? [...SUITS] : [...DEMANDABLE_RANKS];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#1a1a27] p-6 rounded-2xl border border-purple-500/30 shadow-2xl max-w-sm w-full mx-4"
      >
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xl font-bold text-white">
            {type === "suit" ? "Wybierz kolor (As)" : "Wybierz figurę (Walet)"}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-white transition-colors"
            aria-label="Zamknij"
          >
            <X size={20} />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-3">
          {items.map((item) => (
            <motion.button
              key={item}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => onSelect(item)}
              className="py-4 px-4 bg-purple-600/20 hover:bg-purple-600/40 border border-purple-500/30 rounded-xl text-white font-medium transition-colors"
            >
              {type === "suit" ? (
                <span className={`text-2xl ${SUIT_COLORS[item]}`}>
                  {SUIT_SYMBOLS[item]}
                </span>
              ) : (
                <span className="text-lg">{convertRank(item)}</span>
              )}
            </motion.button>
          ))}
        </div>
      </motion.div>
    </motion.div>
  );
};

// ============================================
// Drawn Card Modal - shown after drawing a card
// ============================================

interface DrawnCardModalProps {
  card: { suit: string; rank: string };
  isPlayable: boolean;
  onPlay: () => void;
  onSkip: () => void;
  loading: boolean;
}

export const DrawnCardModal: React.FC<DrawnCardModalProps> = ({
  card,
  isPlayable,
  onPlay,
  onSkip,
  loading
}) => {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#1a1a27] p-6 rounded-2xl border border-purple-500/30 shadow-2xl text-center"
      >
        <p className="text-gray-400 mb-4">Dobrana karta:</p>
        <div className="flex justify-center mb-4">
          <Card
            card={{
              suit: convertSuit(card.suit) as Suit,
              rank: convertRank(card.rank) as Rank,
              id: "drawn",
            }}
            size="lg"
          />
        </div>
        <div className="flex gap-3 justify-center">
          {isPlayable && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={onPlay}
              disabled={loading}
              className="px-6 py-3 bg-purple-600 hover:bg-purple-500 rounded-xl font-bold text-white disabled:opacity-50 flex items-center gap-2"
            >
              {loading ? <Loader2 className="animate-spin" size={20} /> : "Zagraj kartę"}
            </motion.button>
          )}
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={onSkip}
            disabled={loading}
            className="px-6 py-3 bg-gray-600 hover:bg-gray-500 rounded-xl font-bold text-white disabled:opacity-50 flex items-center gap-2"
          >
            {loading ? <Loader2 className="animate-spin" size={20} /> : "Pomiń turę"}
          </motion.button>
        </div>
      </motion.div>
    </motion.div>
  );
};

// ============================================
// Game Finished Modal
// ============================================

interface GameFinishedModalProps {
  ranking: Record<string, number>;
  getPlayerName: (playerId: string) => string;
  onLeave: () => void;
}

export const GameFinishedModal: React.FC<GameFinishedModalProps> = ({
  ranking,
  getPlayerName,
  onLeave,
}) => {
  const sortedRanking = Object.entries(ranking).sort(([, a], [, b]) => a - b);

  const getPlaceEmoji = (place: number): string => {
    switch (place) {
      case 1: return "🥇";
      case 2: return "🥈";
      case 3: return "🥉";
      default: return `#${place}`;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
    >
      <motion.div
        initial={{ scale: 0.9, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        className="bg-[#121018] p-10 rounded-2xl border border-purple-500/50 shadow-2xl text-center max-w-md"
      >
        <h2 className="text-4xl font-bold mb-4 bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">
          🎉 Gra zakończona!
        </h2>

        <div className="mb-6">
          {sortedRanking.map(([id, place]) => (
            <div
              key={id}
              className={`flex justify-between items-center py-2 px-4 rounded-lg mb-2 ${
                place === 1 ? "bg-yellow-500/20" : "bg-white/5"
              }`}
            >
              <span className="text-white font-medium">{getPlayerName(id)}</span>
              <span className={`font-bold ${place === 1 ? "text-yellow-400" : "text-gray-400"}`}>
                {getPlaceEmoji(place)}
              </span>
            </div>
          ))}
        </div>

        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          onClick={onLeave}
          className="px-8 py-3 bg-purple-600 hover:bg-purple-500 rounded-xl font-bold text-white"
        >
          Wróć do strony głównej
        </motion.button>
      </motion.div>
    </motion.div>
  );
};
