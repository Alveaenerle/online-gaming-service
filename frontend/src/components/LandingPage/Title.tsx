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

  return (
    <section className="relative h-screen flex items-center justify-center text-center overflow-hidden bg-bg">
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] animate-gradient-bg" />

      <motion.div
        initial="hidden"
        animate="visible"
        variants={{
          hidden: {},
          visible: { transition: { staggerChildren: 0.2 } },
        }}
        className="max-w-4xl px-6 relative z-10"
      >
        <motion.h1
          variants={{
            hidden: { y: 40, opacity: 0 },
            visible: { y: 0, opacity: 1, transition: { duration: 0.8 } },
          }}
          className="text-5xl md:text-8xl font-extrabold leading-tight flex flex-col"
        >
          <div className="flex flex-row justify-center items-center">
            <motion.div
              whileHover={{ scale: 1.1, rotate: 5 }}
              className="text-7xl mr-3 rounded-md bg-gradient-to-br from-purple-500 via-purple-600 to-purple-700 shadow-neon flex items-center justify-center p-2 text-white"
            >
              OG
            </motion.div>
            OnlineGames
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
            className="pb-2 mt-7 md:text-4xl text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-purple-700"
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
          className="mt-12 text-gray-300 text-lg md:text-xl"
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
          className="mt-8 flex flex-col sm:flex-row gap-4 justify-center"
        >
          <motion.button
            whileHover={{
              scale: 1.05,
              boxShadow: "0 0 20px rgba(168,85,247,0.6)",
            }}
            onClick={handlePlayNow}
            className="inline-flex items-center gap-3 px-6 py-3 rounded-2xl border border-purple-600 shadow-neon transition-transform cursor-pointer"
          >
            <span className="font-semibold">Play now</span>
            <span className="inline-block w-3 h-3 rounded-full bg-gradient-to-br from-purple-500 to-purple-700 animate-pulse" />
          </motion.button>
          <motion.a
            whileHover={{ scale: 1.05 }}
            href="#features"
            className="inline-flex items-center gap-3 px-6 py-3 rounded-2xl bg-white/5 hover:bg-white/10 transition"
          >
            View games
          </motion.a>
        </motion.div>
      </motion.div>
    </section>
  );
};

export default Title;
