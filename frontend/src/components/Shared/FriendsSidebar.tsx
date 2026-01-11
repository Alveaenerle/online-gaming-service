import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Users, X, Check, Clock } from "lucide-react";
import { useSocial } from "../../context/SocialContext";

interface FriendsSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const FriendsSidebar: React.FC<FriendsSidebarProps> = ({
  isOpen,
  onClose,
}) => {
  const { friends, pendingRequests, acceptFriendRequest, rejectFriendRequest } = useSocial();

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          <motion.aside
            initial={{ x: 400, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: 400, opacity: 0 }}
            transition={{ type: "spring", stiffness: 120, damping: 18 }}
            className="fixed right-6 top-6 bottom-6 z-50 w-[320px]
                       rounded-3xl border border-purple-500/20
                       bg-white/5 backdrop-blur-xl shadow-2xl shadow-purple-700/30
                       flex flex-col overflow-hidden"
          >
            <div className="flex items-center justify-between px-6 py-4 border-b border-white/10 shrink-0">
              <div className="flex items-center gap-3 text-purple-400">
                <Users />
                <h3 className="font-bold text-lg">Social Center</h3>
              </div>
              <button
                onClick={onClose}
                className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition"
              >
                <X />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-6">
              
              {/* Pending Requests Section */}
              {pendingRequests.length > 0 && (
                <div className="space-y-3">
                    <div className="flex items-center justify-between text-xs font-bold uppercase tracking-wider text-orange-400">
                        <span>Pending Requests</span>
                        <span className="bg-orange-500/20 px-2 py-0.5 rounded text-orange-300">
                            {pendingRequests.length}
                        </span>
                    </div>
                    
                    {pendingRequests.map((req) => (
                        <motion.div 
                            key={req.id}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="bg-white/5 p-3 rounded-xl border border-orange-500/20 flex items-center justify-between gap-2"
                        >
                            <div className="flex flex-col min-w-0">
                                <span className="font-semibold text-sm truncate text-white">{req.requesterUsername}</span>
                                <span className="text-[10px] text-gray-400 flex items-center gap-1">
                                    <Clock size={10} /> {new Date(req.createdAt).toLocaleDateString()}
                                </span>
                            </div>
                            <div className="flex items-center gap-1 shrink-0">
                                <button 
                                    onClick={() => acceptFriendRequest(req.id)}
                                    className="p-1.5 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500 hover:text-white transition-colors"
                                >
                                    <Check size={16} />
                                </button>
                                <button 
                                    onClick={() => rejectFriendRequest(req.id)}
                                    className="p-1.5 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white transition-colors"
                                >
                                    <X size={16} />
                                </button>
                            </div>
                        </motion.div>
                    ))}
                </div>
              )}

              {/* Friends List Section */}
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs font-bold uppercase tracking-wider text-gray-500">
                    <span>Friends</span>
                    <span>{friends.length}</span>
                </div>

                {friends.length === 0 ? (
                    <div className="text-gray-500 text-center text-sm py-8 italic opacity-50">
                    No friends yet. 
                    <br />
                    Invite players from the lobby!
                    </div>
                ) : (
                    friends.map((friend) => (
                    <motion.div
                        key={friend.id}
                        whileHover={{ scale: 1.02 }}
                        className="flex items-center justify-between rounded-xl
                                bg-white/5 px-4 py-3
                                border border-white/10
                                hover:border-purple-500/40 transition group"
                    >
                        <div className="flex items-center gap-3">
                            <div className="relative">
                                {/* Initials avatar since we might not have URL */}
                                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-600 to-blue-600 flex items-center justify-center text-xs font-bold">
                                    {friend.username.substring(0,2).toUpperCase()}
                                </div>
                                <div className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-[#121018]
                                    ${friend.status === 'ONLINE' ? 'bg-green-500' : 
                                      friend.status === 'PLAYING' ? 'bg-yellow-500' : 'bg-gray-500'}`} 
                                />
                            </div>
                            <div>
                                <p className="font-semibold text-sm group-hover:text-purple-300 transition-colors">{friend.username}</p>
                                <p className={`text-[10px] font-medium uppercase tracking-wide
                                    ${friend.status === 'ONLINE' ? 'text-green-400' : 
                                      friend.status === 'PLAYING' ? 'text-yellow-400' : 'text-gray-500'}`}
                                >
                                    {friend.status}
                                </p>
                            </div>
                        </div>
                    </motion.div>
                    ))
                )}
              </div>
            </div>

            <div className="px-6 py-4 border-t border-white/10 text-center text-xs text-gray-400 shrink-0">
              Online:{" "}
              <span className="text-purple-400 font-semibold">
                {friends.filter((f) => f.status === "ONLINE" || f.status === 'PLAYING').length}
              </span>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
};
