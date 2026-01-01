import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Spade,
  ChessPawn,
  ChevronLeft,
  ChevronRight,
  Play,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

const games = [
  {
    id: 1,
    title: "Ludo Arena",
    subtitle: "Classic Board Battle",
    description:
      "A modern take on the classic board game. Compete with other players, block opponents, and race to the finish line.",
    icon: ChessPawn,
    route: "/ludo",
    color: "from-blue-600 to-cyan-500",
    glow: "shadow-blue-500/20",
  },
  {
    id: 2,
    title: "Makao Clash",
    subtitle: "Strategic Card War",
    description:
      "A dynamic card game full of twists. Force opponents to draw or skip turns and be the first to clear your hand.",
    icon: Spade,
    route: "/makao",
    color: "from-purple-600 to-fuchsia-500",
    glow: "shadow-purple-500/20",
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
    return [
      (centerIndex - 1 + count) % count,
      centerIndex,
      (centerIndex + 1) % count,
    ];
  };

  const indices = getRenderIndices();

  return (
    <div className="relative w-full max-w-7xl mx-auto px-4 py-10 flex flex-col items-center">
      <div className="text-center mb-12">
        <h2 className="text-[10px] font-black uppercase tracking-[0.4em] text-purple-500 mb-2">
          Select Arena
        </h2>
        <div className="h-[1px] w-12 bg-purple-500 mx-auto opacity-50" />
      </div>

      <div className="relative flex items-center justify-center w-full overflow-visible">
        {count > 1 && (
          <div className="absolute top-1/2 -translate-y-1/2 w-full flex justify-between  z-30 pointer-events-none">
            <button
              onClick={prev}
              className="p-4 rounded-2xl bg-white/5 border border-white/10 text-white hover:bg-purple-600 hover:border-purple-500 transition-all pointer-events-auto active:scale-90"
            >
              <ChevronLeft size={24} />
            </button>
            <button
              onClick={next}
              className="p-4 rounded-2xl bg-white/5 border border-white/10 text-white hover:bg-purple-600 hover:border-purple-500 transition-all pointer-events-auto active:scale-90"
            >
              <ChevronRight size={24} />
            </button>
          </div>
        )}

        <div className="flex items-center justify-center gap-0 perspective-1000 h-500px]">
          <AnimatePresence mode="popLayout">
            {indices.map((gameIndex, idx) => {
              const game = games[gameIndex];
              const Icon = game.icon;
              const isCenter = idx === 1;
              const isLeft = idx === 0;

              return (
                <motion.div
                  key={`${game.id}-${idx}`}
                  layout
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{
                    opacity: isCenter ? 1 : 0.4,
                    scale: isCenter ? 1.05 : 0.8,
                    rotateY: isCenter ? 0 : isLeft ? 25 : -25,
                    x: isCenter ? 0 : isLeft ? -50 : 50,
                    zIndex: isCenter ? 20 : 10,
                  }}
                  transition={{ type: "spring", stiffness: 200, damping: 25 }}
                  className={`relative flex-shrink-0 rounded-[2.5rem] p-8 flex flex-col items-center justify-between
                    bg-[#121018] border border-white/5 shadow-2xl backdrop-blur-xl overflow-hidden group`}
                  style={{
                    width: isCenter ? 420 : 300,
                    height: isCenter ? 520 : 400,
                  }}
                >
                  {isCenter && (
                    <div
                      className={`absolute -top-24 -left-24 w-64 h-64 bg-gradient-to-br ${game.color} blur-[100px] opacity-20`}
                    />
                  )}

                  <div className="relative flex flex-col items-center gap-6 mt-4">
                    <div
                      className={`p-8 rounded-3xl shadow-2xl bg-gradient-to-br ${game.color} ${game.glow} transition-transform duration-500 group-hover:scale-110`}
                    >
                      <Icon
                        className={`${
                          isCenter ? "w-16 h-16" : "w-10 h-10"
                        } text-white drop-shadow-lg`}
                      />
                    </div>

                    <div className="text-center">
                      <h3
                        className={`font-black uppercase tracking-tight ${
                          isCenter ? "text-3xl" : "text-xl text-gray-500"
                        }`}
                      >
                        {game.title}
                      </h3>
                      {isCenter && (
                        <p className="text-[10px] font-bold text-purple-400 uppercase tracking-widest mt-1">
                          {game.subtitle}
                        </p>
                      )}
                    </div>
                  </div>

                  <div
                    className={`w-full transition-all duration-500 ${
                      isCenter
                        ? "opacity-100 translate-y-0"
                        : "opacity-0 translate-y-10"
                    }`}
                  >
                    <p className="text-center text-gray-400 text-sm leading-relaxed mb-8 px-4">
                      {game.description}
                    </p>

                    <button
                      onClick={() => navigate(game.route)}
                      className={`w-full py-4 rounded-2xl bg-gradient-to-r ${game.color} text-white font-black uppercase text-xs tracking-[0.2em] 
                        shadow-xl flex items-center justify-center gap-3 hover:scale-[1.02] active:scale-95 transition-all`}
                    >
                      <Play size={16} fill="currentColor" />
                      Enter Arena
                    </button>
                  </div>

                  <div className="absolute top-0 left-0 w-full h-1/2 bg-gradient-to-b from-white/5 to-transparent pointer-events-none" />
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>
      </div>

      <div className="flex gap-2 mt-8">
        {games.map((_, i) => (
          <div
            key={i}
            className={`h-1 rounded-full transition-all duration-500 ${
              centerIndex === i ? "w-8 bg-purple-500" : "w-2 bg-white/10"
            }`}
          />
        ))}
      </div>
    </div>
  );
};
