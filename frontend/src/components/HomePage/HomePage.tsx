import React, { useState } from "react";
import { motion } from "framer-motion";
import { Users } from "lucide-react";
import { FriendsSidebar } from "../Shared/FriendsSidebar";
import Navbar from "../Shared/Navbar";
import { GameCarousel } from "./GamesCarousel";

const Home: React.FC = () => {
  const [friendsOpen, setFriendsOpen] = useState(false);

  return (
    <div className="min-h-screen bg-bg text-white overflow-hidden">
      <Navbar />

      <section className="relative py-32 px-6 max-w-7xl mx-auto">
        <motion.div
          className="text-center mb-3"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
        >
          <h2 className="text-4xl font-bold mb-4">Welcome Back</h2>
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
        className="fixed bottom-6 right-6 z-30
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
