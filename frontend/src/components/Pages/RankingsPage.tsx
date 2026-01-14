import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Gamepad2, Spade, Dice5, Medal, Crown } from "lucide-react";
import Navbar from "../Shared/Navbar";
import { BackgroundGradient } from "../Shared/BackgroundGradient";
import { SocialCenter } from "../Shared/SocialCenter";
import { useAuth } from "../../context/AuthContext";
import statisticsService, { type Rankings, type PlayerStatistics } from "../../services/statisticsService";

type GameType = "MAKAO" | "LUDO";

interface TabButtonProps {
  active: boolean;
  onClick: () => void;
  icon: React.ElementType;
  label: string;
  color: string;
}

const TabButton: React.FC<TabButtonProps> = ({ active, onClick, icon: Icon, label, color }) => (
  <button
    onClick={onClick}
    className={`flex items-center gap-1.5 sm:gap-2 px-3 sm:px-4 md:px-6 py-2.5 sm:py-3 rounded-lg sm:rounded-xl font-bold text-sm sm:text-base transition-all duration-300 min-h-[44px] ${
      active
        ? `bg-gradient-to-r ${color} text-white shadow-lg`
        : "bg-white/5 text-gray-400 hover:bg-white/10 hover:text-white"
    }`}
  >
    <Icon size={18} className="sm:hidden" />
    <Icon size={20} className="hidden sm:block" />
    {label}
  </button>
);

interface RankingTableProps {
  title: string;
  icon: React.ElementType;
  players: PlayerStatistics[];
  valueKey: "gamesPlayed" | "gamesWon";
  color: string;
}

const RankingTable: React.FC<RankingTableProps> = ({ title, icon: Icon, players, valueKey, color }) => {
  const getMedalColor = (rank: number) => {
    switch (rank) {
      case 1:
        return "text-yellow-400";
      case 2:
        return "text-gray-300";
      case 3:
        return "text-amber-600";
      default:
        return "text-gray-500";
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="bg-[#121018] rounded-xl sm:rounded-2xl border border-white/5 overflow-hidden"
    >
      <div className={`bg-gradient-to-r ${color} px-4 sm:px-6 py-3 sm:py-4 flex items-center gap-2 sm:gap-3`}>
        <Icon size={20} className="text-white sm:hidden" />
        <Icon size={24} className="text-white hidden sm:block" />
        <h3 className="text-base sm:text-lg font-bold text-white">{title}</h3>
      </div>
      <div className="divide-y divide-white/5">
        {players.length === 0 ? (
          <div className="px-4 sm:px-6 py-6 sm:py-8 text-center text-gray-500 text-sm">
            No players found yet. Be the first!
          </div>
        ) : (
          players.map((player, index) => (
            <div
              key={`${player.playerId}-${index}`}
              className="px-3 sm:px-6 py-3 sm:py-4 flex items-center justify-between hover:bg-white/5 transition-colors"
            >
              <div className="flex items-center gap-2 sm:gap-4 min-w-0">
                <div className="w-6 sm:w-8 flex justify-center flex-shrink-0">
                  {index < 3 ? (
                    <Medal size={18} className={`sm:hidden ${getMedalColor(index + 1)}`} />
                  ) : (
                    <span className="text-gray-500 font-mono text-xs sm:text-sm">{index + 1}</span>
                  )}
                  {index < 3 && (
                    <Medal size={20} className={`hidden sm:block ${getMedalColor(index + 1)}`} />
                  )}
                </div>
                <div className="min-w-0">
                  <p className="font-semibold text-white text-sm sm:text-base truncate">
                    {player.username || "Unknown Player"}
                  </p>
                  <p className="text-[10px] sm:text-xs text-gray-500">
                    Win Rate: {player.winRatio.toFixed(1)}%
                  </p>
                </div>
              </div>
              <div className="text-right flex-shrink-0 ml-2">
                <p className="text-lg sm:text-xl font-black text-white">{player[valueKey]}</p>
                <p className="text-[10px] sm:text-xs text-gray-500 uppercase">
                  {valueKey === "gamesPlayed" ? "Games" : "Wins"}
                </p>
              </div>
            </div>
          ))
        )}
      </div>
    </motion.div>
  );
};

const RankingsPage: React.FC = () => {
  const { isAuthenticated } = useAuth();
  const [selectedGame, setSelectedGame] = useState<GameType>("MAKAO");
  const [rankings, setRankings] = useState<Rankings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchRankings = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await statisticsService.getRankings(selectedGame, 30);
        setRankings(data);
      } catch (err: any) {
        console.error("Failed to fetch rankings:", err);
        setError(err.message || "Failed to load rankings");
      } finally {
        setLoading(false);
      }
    };

    fetchRankings();
  }, [selectedGame]);

  const gameColors: Record<GameType, string> = {
    MAKAO: "from-purple-600 to-fuchsia-500",
    LUDO: "from-blue-600 to-cyan-500",
  };

  return (
    <div className="min-h-screen bg-[#050508] text-white overflow-x-hidden font-sans">
      <Navbar />
      <BackgroundGradient />

      <main className="relative z-10 pt-24 sm:pt-28 lg:pt-32 pb-12 sm:pb-16 lg:pb-20 px-3 sm:px-4 md:px-6 max-w-6xl mx-auto">
        {/* Header */}
        <motion.header
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          className="text-center mb-8 sm:mb-12"
        >
          <div className="inline-flex items-center gap-2 mb-3 sm:mb-4">
            <div className="h-px w-6 sm:w-8 bg-purple-500/50" />
            <span className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.3em] sm:tracking-[0.4em] text-purple-500">
              Leaderboards
            </span>
            <div className="h-px w-6 sm:w-8 bg-purple-500/50" />
          </div>
          <h1 className="text-3xl sm:text-4xl md:text-5xl font-black tracking-tighter mb-3 sm:mb-4">
            <span className="bg-gradient-to-r from-purple-400 via-fuchsia-500 to-purple-600 bg-clip-text text-transparent">
              Rankings
            </span>
          </h1>
          <p className="text-gray-400 text-sm sm:text-base max-w-xl mx-auto px-4">
            See who's dominating the leaderboards! Top 30 players by games played and games won.
          </p>
        </motion.header>

        {/* Game Tabs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="flex justify-center gap-2 sm:gap-4 mb-6 sm:mb-8"
        >
          <TabButton
            active={selectedGame === "MAKAO"}
            onClick={() => setSelectedGame("MAKAO")}
            icon={Spade}
            label="Makao"
            color="from-purple-600 to-fuchsia-500"
          />
          <TabButton
            active={selectedGame === "LUDO"}
            onClick={() => setSelectedGame("LUDO")}
            icon={Dice5}
            label="Ludo"
            color="from-blue-600 to-cyan-500"
          />
        </motion.div>

        {/* Content */}
        {loading ? (
          <div className="flex justify-center py-12 sm:py-20">
            <div className="animate-spin rounded-full h-10 w-10 sm:h-12 sm:w-12 border-t-2 border-b-2 border-purple-500"></div>
          </div>
        ) : error ? (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center py-8 sm:py-12"
          >
            <p className="text-red-400 mb-4 text-sm sm:text-base">{error}</p>
            <button
              onClick={() => setSelectedGame(selectedGame)}
              className="px-4 py-2 bg-purple-600 hover:bg-purple-700 rounded-lg transition-colors text-sm min-h-[44px]"
            >
              Retry
            </button>
          </motion.div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
            <RankingTable
              title="Most Games Played"
              icon={Gamepad2}
              players={rankings?.topByGamesPlayed || []}
              valueKey="gamesPlayed"
              color={gameColors[selectedGame]}
            />
            <RankingTable
              title="Most Games Won"
              icon={Crown}
              players={rankings?.topByGamesWon || []}
              valueKey="gamesWon"
              color={gameColors[selectedGame]}
            />
          </div>
        )}
      </main>

      {isAuthenticated && <SocialCenter />}
    </div>
  );
};

export default RankingsPage;
