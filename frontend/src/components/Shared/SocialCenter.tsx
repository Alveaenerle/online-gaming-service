import { useState } from "react";
import { motion } from "framer-motion";
import { Users } from "lucide-react";
import { FriendsSidebar } from "./FriendsSidebar"; // Upewnij się, że ścieżka jest poprawna

export function SocialCenter() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <motion.button
        onClick={() => setIsOpen(true)}
        whileHover={{ scale: 1.05, x: -5 }}
        whileTap={{ scale: 0.95 }}
        initial={{ opacity: 0, x: 50 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.5, delay: 0.6 }}
        className="fixed bottom-10 right-10 z-30
                   flex items-center gap-4
                   rounded-[1.5rem] bg-[#121018] border border-purple-500/30 px-6 py-4
                   font-black uppercase text-xs tracking-widest text-white shadow-2xl 
                   shadow-purple-900/20 backdrop-blur-xl transition-all group"
      >
        <div className="relative">
          <Users
            size={20}
            className="group-hover:text-purple-400 transition-colors"
          />
          {/* Status Online - kropka */}
          <div className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-[#121018] animate-pulse" />
        </div>

        <span className="hidden md:block">Social Center</span>
      </motion.button>

      {/* Sidebar kontrolowany wewnętrznie */}
      <FriendsSidebar isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </>
  );
}
