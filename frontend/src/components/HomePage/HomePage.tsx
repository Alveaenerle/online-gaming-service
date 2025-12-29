import React, { useState } from "react";
import { motion } from "framer-motion";
import { Users } from "lucide-react";
import { FriendsSidebar } from "../Shared/FriendsSidebar";
import Navbar from "../Shared/Navbar";
import { GameCarousel } from "./GamesCarousel";
import { useAuth } from "../../context/AuthContext";

const Home: React.FC = () => {
  const [friendsOpen, setFriendsOpen] = useState(false);
  const { user, isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen bg-bg text-white overflow-hidden">
      <Navbar />
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0b1f] via-[#05060f] to-black" />
        <div
          className="absolute top-[-20%] left-1/2 -translate-x-1/2
                        w-[900px] h-[900px]
                        bg-purple-900/30 blur-[140px]"
        />
        <div className="absolute inset-0 bg-black/40" />
      </div>
      <section className="relative py-32 px-6 max-w-7xl mx-auto">
        <motion.div
          className="text-center mb-3"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-4xl font-bold mb-4">
            Welcome Back{isAuthenticated && user ? `, ${user.username}` : ""}!
          </h2>
          <p className="text-gray-400">Choose your game below:</p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <GameCarousel />
        </motion.div>
      </section>

      <motion.button
        onClick={() => setFriendsOpen(true)}
        className="fixed bottom-10 right-10 z-30
                   flex items-center gap-3
                   rounded-2xl bg-purple-600 px-5 py-3
                   font-semibold shadow-lg shadow-purple-600/40
                   hover:bg-purple-500 transition"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
      >
        <Users />
        Friends
      </motion.button>

      <FriendsSidebar
        isOpen={friendsOpen}
        onClose={() => setFriendsOpen(false)}
      />
    </div>
  );
};

export default Home;
