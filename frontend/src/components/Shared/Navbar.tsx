import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Link } from "react-router-dom";

const Navbar: React.FC = () => {
  return (
    <header className="w-full py-4 px-6 md:px-12 flex items-center justify-between z-50 fixed top-0 left-0 bg-[#121018]/80 backdrop-blur-md">
      <motion.div
        initial={{ x: -20, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <Link to="/" className="text-2xl font-bold tracking-wide flex items-center gap-3">
          <div className="w-10 h-10 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-neon flex items-center justify-center p-2 text-white font-bold">
            OG
          </div>
          <span>OnlineGames</span>
        </Link>
      </motion.div>

      <nav className="hidden md:flex gap-6 items-center opacity-90">
        <a className="hover:text-purple-300 transition-colors duration-300">
          Games
        </a>
        <a className="hover:text-purple-300 transition-colors duration-300">
          Rankings
        </a>

        <Link to="/login">
          <motion.button
            whileHover={{ scale: 1.1 }}
            className="px-4 py-2 rounded-md border border-purpleEnd text-white transition-transform"
          >
            Login
          </motion.button>
        </Link>

        <Link to="/register">
          <motion.button
            whileHover={{ scale: 1.1 }}
            className="px-4 py-2 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon transition-transform"
          >
            Register
          </motion.button>
        </Link>
      </nav>
    </header>
  );
};

export default Navbar;
