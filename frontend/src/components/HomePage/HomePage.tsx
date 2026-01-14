import React, { useState } from "react";
import { motion } from "framer-motion";
import { SocialCenter } from "../Shared/SocialCenter";
import Navbar from "../Shared/Navbar";
import { GameCarousel } from "./GamesCarousel";
import { useAuth } from "../../context/AuthContext";

const Home: React.FC = () => {
  const [friendsOpen, setFriendsOpen] = useState(false);
  const { user, isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-[#050508] text-white overflow-x-hidden relative font-sans">
      <Navbar />

      <div className="absolute inset-0 z-0 pointer-events-none">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_-20%,_rgba(108,42,255,0.15),_transparent_80%)]" />

        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            opacity: [0.3, 0.5, 0.3],
          }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
          className="absolute top-[-10%] left-1/2 -translate-x-1/2 w-[400px] sm:w-[600px] lg:w-[800px] h-[300px] sm:h-[450px] lg:h-[600px] bg-purple-600/20 blur-[80px] sm:blur-[100px] lg:blur-[120px] rounded-full"
        />
      </div>

      <main className="relative z-10 pt-20 sm:pt-24 lg:pt-32 pb-10 sm:pb-16 lg:pb-20 px-3 sm:px-4 md:px-6 max-w-[1600px] mx-auto min-h-screen flex flex-col justify-center">
        <header className="text-center mb-4 space-y-2 sm:space-y-4">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          >
            <h2 className="text-2xl xs:text-3xl sm:text-4xl lg:text-5xl font-black tracking-tighter mb-2 sm:mb-4 leading-none">
              Welcome Back,{" "}
              <span className="bg-gradient-to-r from-purple-400 via-fuchsia-500 to-purple-600 bg-clip-text text-transparent">
                {isAuthenticated && user ? user.username : "Player"}
              </span>
            </h2>
            <p className="text-gray-500 text-xs sm:text-sm md:text-base font-medium max-w-2xl mx-auto tracking-wide px-2">
              Your arena is ready. Select a game and start competing with
              players from around the world.
            </p>
          </motion.div>
        </header>

        <motion.section
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 1, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
          className="relative"
        >
          <GameCarousel />
        </motion.section>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1 }}
          className="mt-6 sm:mt-8 lg:mt-12 flex flex-wrap justify-center items-center gap-3 sm:gap-4 lg:gap-8 text-white/20 font-black text-[8px] sm:text-[10px] uppercase tracking-[0.2em] sm:tracking-[0.4em]"
        >
          <span>Global Ranking</span>
          <div className="w-1 h-1 rounded-full bg-white/20" />
          <span>Tournaments</span>
          <div className="w-1 h-1 rounded-full bg-white/20" />
          <span>Community</span>
        </motion.div>
      </main>
      <SocialCenter />
    </div>
  );
};

export default Home;
