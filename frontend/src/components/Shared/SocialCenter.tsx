import { useState } from "react";
import { motion } from "framer-motion";
import { Users } from "lucide-react";
import { FriendsSidebar } from "./FriendsSidebar";
import { useAuth } from "../../context/AuthContext";
import { useSocial } from "../../context/SocialContext";

export function SocialCenter() {
  const [isOpen, setIsOpen] = useState(false);
  const { user } = useAuth();
  const { pendingRequests } = useSocial();

  // Don't render for guest users
  if (user?.isGuest) {
    return null;
  }

  const hasNotifications = pendingRequests.length > 0;

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
          {/* Online status indicator dot (or notification badge) */}
          {hasNotifications ? (
             <div className="absolute -top-2 -right-2 min-w-[18px] h-[18px] bg-red-500 rounded-full border-2 border-[#121018] flex items-center justify-center">
                <span className="text-[9px] font-bold text-white">{pendingRequests.length}</span>
             </div>
          ) : (
             <div className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-[#121018] animate-pulse" />
          )}
        </div>

        <span className="hidden md:block">Social Center</span>
      </motion.button>

      {/* Sidebar controlled internally */}
      <FriendsSidebar isOpen={isOpen} onClose={() => setIsOpen(false)} />
    </>
  );
}
