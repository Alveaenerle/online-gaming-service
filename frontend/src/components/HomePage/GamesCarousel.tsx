import React, { useState } from "react";
import { motion } from "framer-motion";
import { Spade, ChessPawn } from "lucide-react";
import { useNavigate } from "react-router-dom";

const games = [
  {
    id: 1,
    title: "Ludo Arena",
    description:
      "A modern take on the classic board game. Compete with other players, block your opponents, and race your tokens to the finish before anyone else. Simple rules, fast-paced matches, and plenty of competitive fun.",
    icon: ChessPawn,
    route: "/ludo",
  },
  {
    id: 2,
    title: "Makao Card Clash",
    description:
      "A dynamic card game full of twists and strategy. Play your cards wisely, force opponents to draw or skip turns, and be the first to get rid of all your cards. Easy to learn, unpredictable, and highly competitive.",
    icon: Spade,
    route: "/makao",
  },
];

export const GameCarousel: React.FC = () => {
  const [centerIndex, setCenterIndex] = useState(0);
  const count = games.length;
  const navigate = useNavigate();

  const prev = () => setCenterIndex((i) => (i === 0 ? count - 1 : i - 1));
  const next = () => setCenterIndex((i) => (i === count - 1 ? 0 : i + 1));

  const getRenderIndices = () => {
    if (count === 1) return [0];
    if (count === 2) {
      const other = centerIndex === 0 ? 1 : 0;
      return [other, centerIndex, other];
    }
    return [
      (centerIndex - 1 + count) % count,
      centerIndex,
      (centerIndex + 1) % count,
    ];
  };

  const indices = getRenderIndices();

  return (
    <div className="relative flex items-center justify-center">
      {count > 1 && (
        <>
          <button
            onClick={prev}
            className="absolute left-4 z-20 px-4 py-2 rounded bg-purple-600/60 hover:bg-purple-500 transition"
          >
            {"<"}
          </button>
          <button
            onClick={next}
            className="absolute right-4 z-20 px-4 py-2 rounded bg-purple-600/60 hover:bg-purple-500 transition"
          >
            {">"}
          </button>
        </>
      )}

      <div className="w-full max-w-6xl overflow-hidden py-16 flex justify-center gap-8">
        {indices.map((gameIndex, idx) => {
          const game = games[gameIndex];
          const Icon = game.icon;
          const isCenter = idx === 1;

          return (
            <motion.div
              key={`${game.id}-${idx}`}
              className={`relative flex-shrink-0 rounded-3xl flex flex-col items-center justify-center p-6
                ${isCenter ? "z-20" : "z-10"}`}
              style={{
                width: isCenter ? 400 : 250,
                height: isCenter ? 480 : 350,
                backgroundColor: isCenter
                  ? "rgba(255,255,255,0.1)"
                  : "rgba(255,255,255,0.05)",
                border: isCenter
                  ? "2px solid rgba(168,85,247,0.6)"
                  : "1px solid rgba(255,255,255,0.1)",
                backdropFilter: isCenter ? "none" : "blur(10px)",
                opacity: isCenter ? 1 : 0.5,
              }}
              animate={{ scale: isCenter ? 1 : 0.85 }}
              transition={{ type: "spring", stiffness: 120, damping: 20 }}
            >
              <div className="flex items-center justify-center mb-4">
                <div
                  className="p-6 rounded-2xl shadow-neon flex items-center justify-center"
                  style={{
                    background: "linear-gradient(135deg, #a855f7, #6c2aff)",
                  }}
                >
                  <Icon
                    className={
                      isCenter ? "w-12 h-12 text-white" : "w-8 h-8 text-white"
                    }
                  />
                </div>
              </div>

              {isCenter && (
                <>
                  <h3 className="text-center text-2xl font-bold">
                    {game.title}
                  </h3>
                  <p className="mt-7 text-center text-gray-200 text-base">
                    {game.description}
                  </p>
                  <div className="mt-7 flex justify-center gap-4">
                    <button
                      onClick={() => navigate(game.route)}
                      className="px-6 py-3 rounded-xl text-sm font-semibold bg-purple-600 hover:bg-purple-500 shadow shadow-purple-600/40 transition"
                    >
                      Play
                    </button>
                  </div>
                </>
              )}
            </motion.div>
          );
        })}
      </div>
    </div>
  );
};
