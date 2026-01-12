import React, { useEffect, useState, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Card as CardType } from "../types";
import { getCardImagePath } from "../utils/cardHelpers";

interface DiscardPileProps {
  topCard?: CardType;
  count?: number;
}

const DiscardPile: React.FC<DiscardPileProps> = ({
  topCard,
  count = 0,
}) => {
  const [displayedTopCard, setDisplayedTopCard] = useState<CardType | undefined>(topCard);
  const [secondCard, setSecondCard] = useState<CardType | null>(null);
  const prevTopCardRef = useRef<CardType | undefined>(topCard);

  // Update memory of the previous card whenever topCard changes, but with a delay
  // to allow the flying card animation to land first.
  useEffect(() => {
    // Check if card effectively changed
    const isSameCard =
      topCard?.rank === prevTopCardRef.current?.rank &&
      topCard?.suit === prevTopCardRef.current?.suit;

    if (isSameCard) return;

    // Delay the update to match the flight animation duration (approx 400-500ms)
    // Spring stiffness 300/damping 25 takes roughly 0.4s-0.5s to settle visually
    const timer = setTimeout(() => {
        if (prevTopCardRef.current) {
            setSecondCard(prevTopCardRef.current);
        }
        prevTopCardRef.current = topCard;
        setDisplayedTopCard(topCard);
    }, 450);

    return () => clearTimeout(timer);
  }, [topCard]);

  // If initial load (displayedTopCard is undefined but topCard exists), sync immediately?
  // No, useState(topCard) handles the initial sync.

  return (
    <div className="relative w-[72px] h-[101px]">
      {/* Visual clutter for pile depth */}
      {count > 2 && (
        <div className="absolute inset-0 bg-gray-800/50 rounded-lg transform translate-x-1 translate-y-1 -z-20 border border-white/5" />
      )}
      {count > 5 && (
        <div className="absolute inset-0 bg-gray-800/50 rounded-lg transform -translate-x-1 translate-y-2 -z-30 border border-white/5" />
      )}

      {/* Previous card (Second from top) */}
      <AnimatePresence>
        {secondCard && (
          <motion.div
            key={`prev-${secondCard.suit}-${secondCard.rank}`}
            initial={{ opacity: 0 }}
            animate={{
              opacity: 1,
              scale: 1,
              x: -4,
              y: -4,
              rotate: 0,
            }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 rounded-lg overflow-hidden shadow-md -z-10"
          >
            <img
              src={getCardImagePath(secondCard)}
              alt="Previous card"
              className="w-full h-full object-contain bg-white opacity-90 filter grayscale-[0.1]"
              draggable={false}
            />
          </motion.div>
        )}
      </AnimatePresence>
      {/* Top Card */}
      <AnimatePresence mode="popLayout">
        {displayedTopCard ? (
          <motion.div
            key={`top-${displayedTopCard.suit}-${displayedTopCard.rank}`}
            initial={{ opacity: 1, scale: 1 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ scale: 0.9, opacity: 0 }} // Don't animate out too excessively
            transition={{ type: "spring", stiffness: 300, damping: 25 }}
            className="absolute inset-0 rounded-lg overflow-hidden shadow-xl ring-1 ring-white/10"
            style={{ zIndex: 10 }}
          >
            <img
              src={getCardImagePath(displayedTopCard)}
              alt={`${displayedTopCard.rank} of ${displayedTopCard.suit}`}
              className="w-full h-full object-contain bg-white"
              draggable={false}
            />
          </motion.div>
        ) : (
          <div className="absolute inset-0 bg-white/5 rounded-lg border-2 border-dashed border-white/10 flex items-center justify-center">
            <span className="text-xs text-white/20">Empty</span>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default DiscardPile;
