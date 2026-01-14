import React from "react";
import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const Title: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const handlePlayNow = (e?: React.MouseEvent) => {
    e?.preventDefault();
    console.log("Play Now button clicked", isAuthenticated);
    if (isAuthenticated) {
      navigate("/home");
    } else {
      navigate("/login");
    }
  };

  const handleViewGames = (e?: React.MouseEvent) => {
    e?.preventDefault();
    navigate("/games");
  };

  return (
    <section className="relative min-h-screen flex items-center justify-center text-center overflow-hidden bg-bg px-4 py-20 sm:py-0">
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] animate-gradient-bg" />

      <motion.div
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: { transition: { staggerChildren: 0.2 } },
        }}
        className="w-full max-w-4xl px-2 sm:px-6 relative z-10 pt-16 sm:pt-0"
      >
        <motion.h1
          variants={{
            hidden: { y: 40, opacity: 0 },
            visible: { y: 0, opacity: 1, transition: { duration: 0.8 } },
          }}
          className="text-3xl xs:text-4xl sm:text-5xl md:text-6xl lg:text-8xl font-extrabold leading-tight flex flex-col"
        >
          <div className="flex flex-row justify-center items-center flex-wrap gap-2 sm:gap-3">
            <motion.div
              whileHover={{ scale: 1.1, rotate: 5 }}
              className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl rounded-md bg-gradient-to-br from-purple-500 via-purple-600 to-purple-700 shadow-neon flex items-center justify-center p-1.5 sm:p-2 text-white"
            >
              OG
            </motion.div>
            <span className="text-2xl xs:text-3xl sm:text-4xl md:text-5xl lg:text-7xl">OnlineGames</span>
          </div>
          <motion.span
            variants={{
              hidden: { opacity: 0, y: 20 },
              visible: {
                opacity: 1,
                y: 0,
                transition: { duration: 0.6, delay: 0.4 },
              },
            }}
            className="pb-2 mt-4 sm:mt-7 text-xl sm:text-2xl md:text-3xl lg:text-4xl text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-purple-700"
          >
            gaming starts here
          </motion.span>
        </motion.h1>

        <motion.p
          variants={{
            hidden: { opacity: 0, y: 20 },
            visible: {
              opacity: 1,
              y: 0,
              transition: { duration: 0.6, delay: 0.6 },
            },
          }}
          className="mt-6 sm:mt-12 text-gray-300 text-base sm:text-lg md:text-xl px-2"
        >
          Play, chat, and add friends – all in one place. Log in or sign up and
          start your adventure today!
        </motion.p>

        <motion.div
          variants={{
            hidden: { opacity: 0, y: 20 },
            visible: {
              opacity: 1,
              y: 0,
              transition: { duration: 0.6, delay: 0.8 },
            },
          }}
          className="mt-6 sm:mt-8 flex flex-col sm:flex-row gap-3 sm:gap-4 justify-center px-4 sm:px-0"
        >
          <motion.button
            whileHover={{
              scale: 1.05,
              boxShadow: "0 0 20px rgba(168,85,247,0.6)",
            }}
            onClick={handlePlayNow}
            className="inline-flex items-center justify-center gap-3 px-5 sm:px-6 py-3 rounded-2xl border border-purple-600 shadow-neon transition-transform cursor-pointer min-h-[48px]"
          >
            <span className="font-semibold">Play now</span>
            <span className="inline-block w-3 h-3 rounded-full bg-gradient-to-br from-purple-500 to-purple-700 animate-pulse" />
          </motion.button>
          <motion.a
            whileHover={{ scale: 1.05 }}
            href="#features"
            onClick={handleViewGames}
            className="inline-flex items-center justify-center gap-3 px-5 sm:px-6 py-3 rounded-2xl bg-white/5 hover:bg-white/10 transition min-h-[48px]"
          >
            View games
          </motion.a>
        </motion.div>
      </motion.div>
    </section>
  );
};

export default Title;
