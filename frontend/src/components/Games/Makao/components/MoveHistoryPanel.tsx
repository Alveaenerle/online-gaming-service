import React, { useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

interface MoveHistoryPanelProps {
  moveHistory: string[];
  lastMoveLog?: string | null;
}

/**
 * Move History Panel - displays chronological list of game moves
 * with animated entry for new moves
 */
const MoveHistoryPanel: React.FC<MoveHistoryPanelProps> = ({
  moveHistory,
  lastMoveLog,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new moves are added
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [moveHistory.length]);

  // Format move text for display - parse card names and player names
  const formatMove = (move: string): React.ReactNode => {
    // Highlight card names (e.g., "Ace of Hearts", "2 of Clubs")
    const cardPattern = /(\d+|Ace|King|Queen|Jack|J|Q|K|A) of (Hearts|Diamonds|Clubs|Spades)/gi;
    const parts = move.split(cardPattern);

    if (parts.length === 1) {
      return <>{move}</>;
    }

    return (
      <>
        {move.split(cardPattern).map((part, i) => {
          // Check if this part is a suit
          if (/^(Hearts|Diamonds|Clubs|Spades)$/i.test(part)) {
            const isRed = /hearts|diamonds/i.test(part);
            return (
              <span key={i} className={isRed ? "text-red-400" : "text-gray-200"}>
                {part}
              </span>
            );
          }
          // Check if this part is a rank
          if (/^(\d+|Ace|King|Queen|Jack|J|Q|K|A)$/i.test(part)) {
            return (
              <span key={i} className="font-semibold text-purpleEnd">
                {part}
              </span>
            );
          }
          return <span key={i}>{part}</span>;
        })}
      </>
    );
  };

  // Get icon for move type
  const getMoveIcon = (move: string): string => {
    if (move.includes("played")) return "🎴";
    if (move.includes("drew") || move.includes("draw")) return "📥";
    if (move.includes("skip")) return "⏭️";
    if (move.includes("started")) return "🎮";
    if (move.includes("ended")) return "🏁";
    if (move.includes("replaced") || move.includes("timed out")) return "🤖";
    return "•";
  };

  return (
    <div className="bg-[#121018]/80 backdrop-blur rounded-xl border border-white/10 flex flex-col h-full min-h-0">
      {/* Header */}
      <div className="px-3 py-2 border-b border-white/10 flex-shrink-0">
        <div className="flex items-center gap-2">
          <svg
            className="w-4 h-4 text-purpleEnd"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
          <h3 className="text-xs font-medium text-gray-400">Move History</h3>
          <span className="ml-auto text-[10px] text-gray-600">
            {moveHistory.length} moves
          </span>
        </div>
      </div>

      {/* Move List */}
      <div
        ref={scrollRef}
        className="flex-1 overflow-y-auto px-2 py-2 space-y-1 min-h-0 scrollbar-thin scrollbar-thumb-purpleEnd/20 scrollbar-track-transparent"
      >
        {moveHistory.length === 0 ? (
          <div className="text-center py-4">
            <p className="text-xs text-gray-500">No moves yet</p>
            <p className="text-[10px] text-gray-600 mt-1">
              Moves will appear here as the game progresses
            </p>
          </div>
        ) : (
          <AnimatePresence initial={false}>
            {moveHistory.map((move, index) => {
              const isLatest = move === lastMoveLog && index === moveHistory.length - 1;

              return (
                <motion.div
                  key={`${index}-${move}`}
                  initial={{ opacity: 0, x: -20, height: 0 }}
                  animate={{ opacity: 1, x: 0, height: "auto" }}
                  exit={{ opacity: 0, x: 20, height: 0 }}
                  transition={{
                    type: "spring",
                    stiffness: 500,
                    damping: 30,
                    opacity: { duration: 0.2 },
                  }}
                  className={`
                    text-xs rounded-lg px-2 py-1.5
                    ${isLatest
                      ? "bg-purpleEnd/20 border border-purpleEnd/40"
                      : "bg-white/5 hover:bg-white/10"
                    }
                    transition-colors
                  `}
                >
                  <div className="flex items-start gap-2">
                    <span className="text-[10px] flex-shrink-0 mt-0.5">
                      {getMoveIcon(move)}
                    </span>
                    <span className="text-gray-300 leading-relaxed">
                      {formatMove(move)}
                    </span>
                  </div>
                  {/* Timestamp - could be added if backend sends it */}
                  <div className="text-[9px] text-gray-600 mt-0.5 text-right">
                    Move #{index + 1}
                  </div>
                </motion.div>
              );
            })}
          </AnimatePresence>
        )}
      </div>

      {/* Latest Move Highlight */}
      {lastMoveLog && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="px-3 py-2 border-t border-white/10 bg-purpleEnd/10 flex-shrink-0"
        >
          <p className="text-[10px] text-gray-500 mb-1">Latest:</p>
          <p className="text-xs text-white font-medium truncate">
            {lastMoveLog}
          </p>
        </motion.div>
      )}
    </div>
  );
};

export default MoveHistoryPanel;
