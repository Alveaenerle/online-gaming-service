import React from "react";
import { motion } from "framer-motion";

const FeatureCard: React.FC<{
  icon: React.ReactNode;
  title: string;
  desc: string;
}> = ({ icon, title, desc }) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    whileInView={{ opacity: 1, y: 0 }}
    viewport={{ once: true }}
    whileHover={{
      y: -8,
      scale: 1.05,
      boxShadow: "0 0 20px rgba(139, 92, 246, 0.6)",
    }}
    className="feature-card bg-[#121018] p-4 sm:p-6 rounded-2xl border border-white/10 shadow-md hover:shadow-neon transition-all duration-300"
  >
    <div className="text-3xl sm:text-4xl mb-3 sm:mb-4 flex items-center justify-center w-12 h-12 sm:w-16 sm:h-16 rounded-full bg-gradient-to-br from-purple-500 to-purple-800 shadow-lg">
      {icon}
    </div>
    <h3 className="font-semibold text-lg sm:text-xl mb-1 sm:mb-2">{title}</h3>
    <p className="text-gray-300 text-sm">{desc}</p>
  </motion.div>
);

const Features: React.FC = () => (
  <section className="bg-[#1a1a27] p-4 sm:p-8 md:p-12 rounded-2xl sm:rounded-3xl shadow-lg mx-auto">
    <h2 className="text-2xl sm:text-3xl md:text-4xl font-extrabold text-white mb-6 sm:mb-8 ml-1 sm:ml-3">
      Features
    </h2>

    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6 md:gap-8">
      <FeatureCard
        icon={<span>🎮</span>}
        title="Play your favorite games"
        desc="Enjoy the classic games you love from childhood, all online and ready to play!"
      />
      <FeatureCard
        icon={<span>⚡</span>}
        title="Chatting and matchmaking"
        desc="Talk, play, and match with gamers in one place."
      />
      <FeatureCard
        icon={<span>👥</span>}
        title="Community"
        desc="Connect with friends and enjoy gaming together!"
      />
    </div>
  </section>
);

export default Features;
