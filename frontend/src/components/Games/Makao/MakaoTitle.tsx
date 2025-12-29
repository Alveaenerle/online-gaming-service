import { motion } from "framer-motion";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "../../Shared/Navbar";
import { useState } from "react";

export function MakaoTitle() {
  const navigate = useNavigate();
  const [playerCount, setPlayerCount] = useState(4);

  const handleCreateLobby = () => navigate("/lobby/makao");
  const handleJoinLobby = () => navigate("/lobby/makao");

  return (
    <div className="min-h-screen bg-bg text-white antialiased">
      <Navbar />
      <div
        className="absolute inset-0 -z-10 
        bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),
             radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] 
        animate-gradient-bg"
      />

      <main className="pt-24 pb-10 px-6 md:px-12 lg:px-24">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="max-w-2xl mx-auto space-y-6"
        >
          <div className="mb-6">
            <Link
              to="/home"
              className="text-purpleEnd hover:text-purple-400 flex items-center gap-2 font-medium"
            >
              ← Back to Home
            </Link>
          </div>

          <div className="bg-[#121018] p-8 rounded-3xl border border-white/10 shadow-neon space-y-6">
            <div className="text-center">
              <h1
                className="text-4xl font-extrabold mb-2 
                bg-gradient-to-r from-purpleStart to-purpleEnd bg-clip-text text-transparent"
              >
                Makao Card Game
              </h1>
              <p className="text-gray-400">
                Classic card game - Play against AI opponents
              </p>
            </div>

            <div className="bg-[#1a1a27] p-6 rounded-2xl border border-white/10 shadow-inner-neon">
              <label className="block text-sm font-medium mb-3">
                Number of Players (2-8)
              </label>
              <div className="flex gap-2 flex-wrap justify-center">
                {[2, 3, 4, 5, 6, 7, 8].map((count) => (
                  <motion.button
                    key={count}
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setPlayerCount(count)}
                    className={`px-6 py-3 rounded-xl font-semibold transition-all ${
                      playerCount === count
                        ? "bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon"
                        : "bg-[#1a1a27] border border-white/10 text-gray-300 hover:bg-white/5"
                    }`}
                  >
                    {count}
                  </motion.button>
                ))}
              </div>
            </div>

            <div className="bg-[#1a1a27] p-6 rounded-2xl border border-white/10 shadow-inner-neon">
              <h3 className="font-bold text-lg mb-3">Game Rules:</h3>
              <ul className="text-sm text-gray-300 space-y-2 list-disc list-inside">
                <li>Match the suit or rank of the top card</li>
                <li>
                  <strong>2</strong> - Forces next player to draw 2 cards
                </li>
                <li>
                  <strong>3</strong> - Forces next player to draw 3 cards
                </li>
                <li>
                  <strong>4</strong> - Skips the next player
                </li>
                <li>
                  <strong>J</strong> - Request any rank
                </li>
                <li>
                  <strong>K♥/K♠</strong> - Reverses play direction
                </li>
                <li>
                  <strong>A</strong> - Request any suit
                </li>
                <li>First player to run out of cards wins!</li>
              </ul>
            </div>

            <div className="mt-6 space-y-4">
              <p className="text-center text-gray-400 text-sm">
                Create a new lobby or join an existing one
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleCreateLobby}
                  className="flex-1 py-4 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold text-lg shadow-neon text-center"
                >
                  Create Lobby
                </motion.button>

                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleJoinLobby}
                  className="flex-1 py-4 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold text-lg shadow-neon text-center"
                >
                  Join Lobby
                </motion.button>
              </div>
            </div>
          </div>
        </motion.div>
      </main>
    </div>
  );
}
