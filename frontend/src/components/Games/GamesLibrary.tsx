import React from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import {
  Spade,
  Dice5,
  BookOpen,
  Users,
  Trophy,
  Zap,
  Target,
  Gamepad2,
} from "lucide-react";
import Navbar from "../Shared/Navbar";
import { BackgroundGradient } from "../Shared/BackgroundGradient";
import { SocialCenter } from "../Shared/SocialCenter";
import { GameBadge } from "./shared/GameBadge";
import { useAuth } from "../../context/AuthContext";

interface GameRule {
  title: string;
  description: string;
}

interface GameInfo {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  icon: React.ElementType;
  color: string;
  glowColor: string;
  badgeColor: "purple" | "fuchsia";
  rules: GameRule[];
  features: { icon: React.ElementType; label: string }[];
}

const gamesData: GameInfo[] = [
  {
    id: "makao",
    title: "Makao",
    subtitle: "Strategic Card War",
    description:
      "A dynamic strategic card game. The objective is to be the first to get rid of all cards.",
    icon: Spade,
    color: "from-purple-600 to-fuchsia-500",
    glowColor: "purple",
    badgeColor: "purple",
    rules: [
      {
        title: "Attack Cards (2 & 3)",
        description:
          "When a 2 or 3 is played, the next player must draw the corresponding number of cards. These cards are cumulative. You can stack a 2 on a 3, or a 3 on a 2 to increase the penalty for the next player.",
      },
      {
        title: "Jack (Demands)",
        description:
          "Playing a Jack allows you to demand a specific card rank (non-special). The demand can be overridden by the next player if they play another Jack.",
      },
      {
        title: "Ace (Suit Change)",
        description:
          "Playing an Ace allows you to change the current suit. This can be overridden by the next player playing another Ace.",
      },
      {
        title: "Kings (Direction Change)",
        description:
          "The King of Hearts and the King of Spades change the direction of the turn order.",
      },
      {
        title: "Stop Card (4)",
        description:
          "Playing a 4 causes the next player to skip their turn (block).",
      },
      {
        title: "Queen Rule",
        description:
          "\"Queen on everything, everything on Queen.\" A Queen can be played on any card regardless of suit or rank, and any card can be played on top of a Queen (except for active special cards like 2, 3, or 4).",
      },
    ],
    features: [
      { icon: Users, label: "2-8 Players" },
      { icon: Zap, label: "Fast-paced" },
      { icon: Trophy, label: "Ranked" },
    ],
  },
  {
    id: "ludo",
    title: "Ludo",
    subtitle: "Classic Board Battle",
    description:
      "A classic race game for 2-4 players. Be the first player to move all four of your pawns from the starting area to the center of the board (Home).",
    icon: Dice5,
    color: "from-blue-600 to-cyan-500",
    glowColor: "blue",
    badgeColor: "fuchsia",
    rules: [
      {
        title: "Objective",
        description:
          "Be the first player to move all four of your pawns from the starting area to the center of the board (Home).",
      },
      {
        title: "Starting",
        description:
          "A player must roll a 6 to move a pawn out of the base and onto the starting square.",
      },
      {
        title: "Movement",
        description:
          "Players move their pawns clockwise according to the number rolled on the die. Rolling a 6 grants an additional throw.",
      },
      {
        title: "Capturing (Bumping)",
        description:
          "If a player's pawn lands on a square occupied by an opponent's pawn, the opponent's pawn is captured and returned to its starting base. The player must then roll a 6 again to bring it back into play.",
      },
    ],
    features: [
      { icon: Users, label: "2-4 Players" },
      { icon: Target, label: "Strategic" },
      { icon: Trophy, label: "Ranked" },
    ],
  },
];

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.15,
    },
  },
};

const cardVariants = {
  hidden: { opacity: 0, y: 30 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.16, 1, 0.3, 1] as const,
    },
  },
};

interface GameCardProps {
  game: GameInfo;
}

const GameCard: React.FC<GameCardProps> = ({ game }) => {
  const Icon = game.icon;

  return (
    <motion.div
      variants={cardVariants}
      whileHover={{ y: -8, transition: { duration: 0.3 } }}
      className="group relative bg-[#121018] rounded-[2rem] border border-white/5 overflow-hidden hover:border-white/10 transition-all duration-300"
    >
      {/* Glow effect */}
      <div
        className={`absolute inset-0 bg-gradient-to-br ${game.color} opacity-0 group-hover:opacity-5 transition-opacity duration-500`}
      />

      {/* Header with icon */}
      <div className={`relative p-8 pb-0`}>
        <div className="flex items-start justify-between mb-6">
          <div
            className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${game.color} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-300`}
          >
            <Icon size={32} className="text-white" />
          </div>
          <GameBadge color={game.badgeColor}>{game.subtitle}</GameBadge>
        </div>

        <h3 className="text-3xl font-black text-white mb-2 tracking-tight">
          {game.title}
        </h3>
        <p className="text-gray-400 text-sm leading-relaxed font-medium">
          {game.description}
        </p>
      </div>

      {/* Features */}
      <div className="px-8 py-6">
        <div className="flex gap-4 flex-wrap">
          {game.features.map((feature, idx) => {
            const FeatureIcon = feature.icon;
            return (
              <div
                key={idx}
                className="flex items-center gap-2 text-xs text-gray-500 font-semibold uppercase tracking-wider"
              >
                <FeatureIcon size={14} className="text-gray-600" />
                {feature.label}
              </div>
            );
          })}
        </div>
      </div>

      {/* Rules section */}
      <div className="px-8 pb-8">
        <div className="bg-white/[0.02] rounded-xl p-5 border border-white/5">
          <div className="flex items-center gap-2 mb-4">
            <BookOpen size={14} className="text-purple-400" />
            <span className="text-xs font-bold uppercase tracking-widest text-purple-400">
              Game Rules
            </span>
          </div>
          <ul className="space-y-4">
            {game.rules.map((rule, idx) => (
              <li key={idx} className="text-sm">
                <span className="font-semibold text-white block mb-1">
                  {rule.title}
                </span>
                <span className="text-gray-500 leading-relaxed">
                  {rule.description}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </motion.div>
  );
};

const GamesLibrary: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-[#050508] text-white overflow-hidden font-sans">
      <Navbar />

      <BackgroundGradient />

      <main className="relative z-10 pt-32 pb-20 px-6 max-w-7xl mx-auto">
        {/* Header */}
        <motion.header
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          className="text-center mb-16"
        >
          <div className="inline-flex items-center gap-2 mb-4">
            <div className="h-px w-8 bg-purple-500/50" />
            <span className="text-[10px] font-black uppercase tracking-[0.4em] text-purple-500">
              Games Library
            </span>
            <div className="h-px w-8 bg-purple-500/50" />
          </div>
          <h1 className="text-5xl md:text-6xl font-black tracking-tighter mb-4">
            Choose Your{" "}
            <span className="bg-gradient-to-r from-purple-400 via-fuchsia-500 to-purple-600 bg-clip-text text-transparent">
              Arena
            </span>
          </h1>
          <p className="text-gray-500 text-lg font-medium max-w-2xl mx-auto mb-8">
            Explore our collection of competitive multiplayer games. Each game
            offers unique mechanics and endless fun with players worldwide.
          </p>

          {/* Primary Action Button */}
          <Link to="/home">
            <motion.button
              whileHover={{ scale: 1.05, boxShadow: "0 0 30px rgba(168,85,247,0.5)" }}
              whileTap={{ scale: 0.98 }}
              className="inline-flex items-center gap-3 px-8 py-4 rounded-2xl bg-gradient-to-r from-purple-600 to-fuchsia-500 text-white font-bold text-sm uppercase tracking-wider shadow-lg hover:shadow-xl transition-all"
            >
              <Gamepad2 size={20} />
              Choose Your Arena
            </motion.button>
          </Link>
        </motion.header>

        {/* Games Grid */}
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="grid md:grid-cols-2 gap-8"
        >
          {gamesData.map((game) => (
            <GameCard key={game.id} game={game} />
          ))}
        </motion.div>

        {/* Footer text */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.8 }}
          className="mt-16 text-center"
        >
          <p className="text-gray-600 text-sm font-medium">
            More games coming soon! Stay tuned for exciting new additions.
          </p>
        </motion.div>
      </main>

      {isAuthenticated && <SocialCenter />}
    </div>
  );
};

export default GamesLibrary;
