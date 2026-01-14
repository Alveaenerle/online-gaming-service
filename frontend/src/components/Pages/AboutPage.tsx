import React from "react";
import { motion } from "framer-motion";
import { Users, Gamepad2, Globe, Heart } from "lucide-react";
import Navbar from "../Shared/Navbar";
import Footer from "../Shared/Footer";
import { BackgroundGradient } from "../Shared/BackgroundGradient";

const AboutPage: React.FC = () => {
  const values = [
    {
      icon: <Gamepad2 size={28} />,
      title: "Gaming Excellence",
      description: "We craft premium multiplayer experiences that bring players together through classic games reimagined for the modern era."
    },
    {
      icon: <Users size={28} />,
      title: "Community First",
      description: "Our platform is built around fostering genuine connections between players from around the world."
    },
    {
      icon: <Globe size={28} />,
      title: "Accessible Gaming",
      description: "Play instantly in your browser with no downloads required. Jump into games with friends or meet new opponents."
    },
    {
      icon: <Heart size={28} />,
      title: "Passion Driven",
      description: "Created by gamers, for gamers. Every feature is designed with the player experience in mind."
    }
  ];

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased font-sans overflow-x-hidden">
      <Navbar />
      <BackgroundGradient />
      
      <main className="relative pt-28 pb-20 px-6 md:px-12 lg:px-24 max-w-6xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-16"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-6">
            About <span className="bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">OnlineGames</span>
          </h1>
          <p className="text-lg text-gray-400 max-w-2xl mx-auto">
            We're on a mission to bring the joy of classic board and card games to the digital world, 
            creating memorable moments between friends and players worldwide.
          </p>
        </motion.div>

        <motion.section
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mb-20"
        >
          <div className="rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm p-8 md:p-12">
            <h2 className="text-2xl font-bold mb-6 text-purple-400">Our Story</h2>
            <div className="space-y-4 text-gray-300 leading-relaxed">
              <p>
                OnlineGames was born from a simple idea: what if we could capture the magic of gathering 
                around a table for game night, but make it accessible to anyone, anywhere, at any time?
              </p>
              <p>
                Founded in 2024, our team of passionate developers and designers set out to create a 
                platform that combines the nostalgia of classic games like Makao and Ludo with modern 
                multiplayer technology and stunning visuals.
              </p>
              <p>
                Today, we're proud to host thousands of daily matches, bringing together players from 
                across the globe for friendly competition and unforgettable gaming moments.
              </p>
            </div>
          </div>
        </motion.section>

        <motion.section
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
        >
          <h2 className="text-2xl font-bold mb-8 text-center">Our Values</h2>
          <div className="grid md:grid-cols-2 gap-6">
            {values.map((value, index) => (
              <motion.div
                key={value.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.5 + index * 0.1 }}
                className="rounded-xl bg-white/5 border border-white/10 p-6 hover:border-purple-500/40 transition-colors"
              >
                <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-purple-500/20 to-pink-500/20 flex items-center justify-center text-purple-400 mb-4">
                  {value.icon}
                </div>
                <h3 className="text-lg font-semibold mb-2">{value.title}</h3>
                <p className="text-gray-400 text-sm">{value.description}</p>
              </motion.div>
            ))}
          </div>
        </motion.section>
      </main>

      <Footer />
    </div>
  );
};

export default AboutPage;
