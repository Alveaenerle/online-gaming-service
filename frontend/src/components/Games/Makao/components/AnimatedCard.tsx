import React, { useEffect, useState } from "react";
import { motion, AnimatePresence, Variants } from "framer-motion";
import { Card as CardType } from "../types";
import { getCardImagePath, getCardBackPath } from "../utils/cardHelpers";

// ============================================
// Types
// ============================================

interface Position {
  x: number;
  y: number;
}

export interface CardAnimationConfig {
  id: string;
  card: CardType;
  type: "play" | "draw";
  fromPosition?: Position;
  toPosition?: Position;
  playerId?: string;
  onComplete?: () => void;
}

interface AnimatedCardProps {
  animation: CardAnimationConfig;
  onAnimationComplete: (id: string) => void;
}

interface CardAnimationManagerProps {
  animations: CardAnimationConfig[];
  onAnimationComplete: (id: string) => void;
  centerPosition: Position;
  deckPosition: Position;
  handPosition: Position;
}

// ============================================
// Animation Variants
// ============================================

const playCardVariants: Variants = {
  initial: (custom: { from: Position }) => ({
    x: custom.from.x,
    y: custom.from.y,
    scale: 1,
    rotate: 0,
    opacity: 1,
  }),
  animate: {
    x: 0,
    y: 0,
    scale: 1.1,
    rotate: [0, -5, 5, 0],
    opacity: 1,
    transition: {
      type: "spring",
      stiffness: 300,
      damping: 25,
      rotate: {
        duration: 0.3,
        times: [0, 0.3, 0.6, 1],
      },
    },
  },
  exit: {
    scale: 0.8,
    opacity: 0,
    transition: { duration: 0.2 },
  },
};

const drawCardVariants: Variants = {
  initial: (custom: { from: Position }) => ({
    x: custom.from.x,
    y: custom.from.y,
    scale: 0.8,
    rotateY: 180,
    opacity: 1,
  }),
  animate: (custom: { to: Position }) => ({
    x: custom.to.x,
    y: custom.to.y,
    scale: 1,
    rotateY: 0,
    opacity: 1,
    transition: {
      type: "spring",
      stiffness: 200,
      damping: 20,
      rotateY: {
        duration: 0.4,
        ease: "easeOut",
      },
    },
  }),
  exit: {
    scale: 0.5,
    opacity: 0,
    transition: { duration: 0.15 },
  },
};

// ============================================
// Animated Card Component
// ============================================

const AnimatedCard: React.FC<AnimatedCardProps> = ({
  animation,
  onAnimationComplete,
}) => {
  const { id, card, type, fromPosition, toPosition } = animation;

  const cardImage = getCardImagePath(card);
  const cardBack = getCardBackPath();

  const [showFront, setShowFront] = useState(type === "play");

  // For draw animation, flip card after initial movement
  useEffect(() => {
    if (type === "draw") {
      const timer = setTimeout(() => setShowFront(true), 200);
      return () => clearTimeout(timer);
    }
  }, [type]);

  const handleAnimationComplete = () => {
    onAnimationComplete(id);
    animation.onComplete?.();
  };

  const from = fromPosition || { x: 0, y: 0 };
  const to = toPosition || { x: 0, y: 0 };

  if (type === "play") {
    return (
      <motion.div
        key={id}
        custom={{ from }}
        variants={playCardVariants}
        initial="initial"
        animate="animate"
        exit="exit"
        onAnimationComplete={handleAnimationComplete}
        className="absolute z-50 pointer-events-none"
        style={{
          width: 72,
          height: 101,
          transformOrigin: "center center",
        }}
      >
        <div className="w-full h-full rounded-lg overflow-hidden shadow-2xl ring-2 ring-purpleEnd">
          <img
            src={cardImage}
            alt={`${card.rank} of ${card.suit}`}
            className="w-full h-full object-contain bg-white"
            draggable={false}
          />
        </div>
        {/* Glow effect */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: [0, 0.8, 0] }}
          transition={{ duration: 0.6 }}
          className="absolute inset-0 rounded-lg bg-purpleEnd/40 blur-xl -z-10"
        />
      </motion.div>
    );
  }

  // Draw animation
  return (
    <motion.div
      key={id}
      custom={{ from, to }}
      variants={drawCardVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      onAnimationComplete={handleAnimationComplete}
      className="absolute z-50 pointer-events-none"
      style={{
        width: 72,
        height: 101,
        transformStyle: "preserve-3d",
      }}
    >
      <div
        className="w-full h-full rounded-lg overflow-hidden shadow-2xl"
        style={{
          backfaceVisibility: "hidden",
          transform: showFront ? "rotateY(0deg)" : "rotateY(180deg)",
          transition: "transform 0.4s ease-out",
        }}
      >
        <img
          src={showFront ? cardImage : cardBack}
          alt={showFront ? `${card.rank} of ${card.suit}` : "Card back"}
          className="w-full h-full object-contain bg-white"
          draggable={false}
        />
      </div>
    </motion.div>
  );
};

// ============================================
// Card Animation Manager Component
// ============================================

/**
 * Manages multiple card animations on the game board
 * Use this component to orchestrate play/draw animations
 */
export const CardAnimationManager: React.FC<CardAnimationManagerProps> = ({
  animations,
  onAnimationComplete,
  centerPosition,
  deckPosition,
  handPosition,
}) => {
  // Calculate positions for each animation
  const getAnimationPositions = (
    animation: CardAnimationConfig
  ): { from: Position; to: Position } => {
    if (animation.type === "play") {
      return {
        from: animation.fromPosition || handPosition,
        to: centerPosition,
      };
    }
    // draw
    return {
      from: animation.fromPosition || deckPosition,
      to: animation.toPosition || handPosition,
    };
  };

  return (
    <AnimatePresence>
      {animations.map((animation) => {
        const positions = getAnimationPositions(animation);
        return (
          <AnimatedCard
            key={animation.id}
            animation={{
              ...animation,
              fromPosition: positions.from,
              toPosition: positions.to,
            }}
            onAnimationComplete={onAnimationComplete}
          />
        );
      })}
    </AnimatePresence>
  );
};

// ============================================
// Hook for managing card animations
// ============================================

export interface UseCardAnimationsReturn {
  animations: CardAnimationConfig[];
  addPlayAnimation: (card: CardType, fromPos?: Position, playerId?: string) => void;
  addDrawAnimation: (card: CardType, toPos?: Position, playerId?: string) => void;
  removeAnimation: (id: string) => void;
  clearAnimations: () => void;
}

export const useCardAnimations = (): UseCardAnimationsReturn => {
  const [animations, setAnimations] = useState<CardAnimationConfig[]>([]);

  const addPlayAnimation = (
    card: CardType,
    fromPos?: Position,
    playerId?: string
  ) => {
    const newAnimation: CardAnimationConfig = {
      id: `play-${Date.now()}-${Math.random()}`,
      card,
      type: "play",
      fromPosition: fromPos,
      playerId,
    };
    setAnimations((prev) => [...prev, newAnimation]);
  };

  const addDrawAnimation = (
    card: CardType,
    toPos?: Position,
    playerId?: string
  ) => {
    const newAnimation: CardAnimationConfig = {
      id: `draw-${Date.now()}-${Math.random()}`,
      card,
      type: "draw",
      toPosition: toPos,
      playerId,
    };
    setAnimations((prev) => [...prev, newAnimation]);
  };

  const removeAnimation = (id: string) => {
    setAnimations((prev) => prev.filter((a) => a.id !== id));
  };

  const clearAnimations = () => {
    setAnimations([]);
  };

  return {
    animations,
    addPlayAnimation,
    addDrawAnimation,
    removeAnimation,
    clearAnimations,
  };
};

// ============================================
// Card Pile Animation (for deck/discard)
// ============================================

interface CardPileProps {
  count: number;
  type: "draw" | "discard";
  topCard?: CardType;
  onClick?: () => void;
  isClickable?: boolean;
  showGlow?: boolean;
}

export const AnimatedCardPile: React.FC<CardPileProps> = ({
  count,
  type,
  topCard,
  onClick,
  isClickable = false,
  showGlow = false,
}) => {
  // Calculate stack offset based on count
  const stackLayers = Math.min(count, 5);

  return (
    <div
      className={`relative ${isClickable ? "cursor-pointer group" : ""}`}
      onClick={isClickable ? onClick : undefined}
    >
      {/* Stack shadow layers */}
      {Array.from({ length: stackLayers }).map((_, i) => (
        <motion.div
          key={i}
          initial={false}
          animate={{
            x: -(stackLayers - i - 1) * 1,
            y: -(stackLayers - i - 1) * 1,
          }}
          className="absolute w-[72px] h-[101px] rounded-lg"
          style={{
            backgroundColor: `rgba(108, 42, 255, ${0.1 + i * 0.05})`,
            zIndex: i,
          }}
        />
      ))}

      {/* Top card or back */}
      <motion.div
        whileHover={isClickable ? { scale: 1.05, y: -4 } : undefined}
        className={`
          relative w-[72px] h-[101px] rounded-lg overflow-hidden shadow-lg
          ${showGlow ? "ring-2 ring-purpleEnd animate-pulse" : ""}
          ${isClickable ? "group-hover:shadow-2xl" : ""}
          transition-shadow
        `}
        style={{ zIndex: stackLayers }}
      >
        {type === "discard" && topCard ? (
          <img
            src={getCardImagePath(topCard)}
            alt={`${topCard.rank} of ${topCard.suit}`}
            className="w-full h-full object-contain bg-white"
            draggable={false}
          />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-purple-600 to-purple-900 flex items-center justify-center">
            <span className="text-white/40 font-bold text-lg">OG</span>
          </div>
        )}
      </motion.div>

      {/* Count badge */}
      <motion.div
        initial={false}
        animate={{ scale: [1, 1.1, 1] }}
        transition={{ duration: 0.3 }}
        className="absolute -bottom-2 -right-2 bg-gray-900 border border-white/20 rounded-full px-2 py-0.5 z-20"
      >
        <span className="text-xs text-gray-300 font-mono">{count}</span>
      </motion.div>

      {/* Glow effect */}
      {showGlow && (
        <motion.div
          animate={{ opacity: [0.3, 0.6, 0.3] }}
          transition={{ duration: 1.5, repeat: Infinity }}
          className="absolute inset-0 rounded-lg bg-purpleEnd/30 blur-xl -z-10"
        />
      )}
    </div>
  );
};

export default AnimatedCard;
