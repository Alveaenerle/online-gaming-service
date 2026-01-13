import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, UserPlus, Search, Users, Gamepad2, Loader2 } from "lucide-react";
import { useSocial } from "../../../context/SocialContext";
import { useLobby } from "../../../context/LobbyContext";
import { Friend } from "../../../services/socialService";

interface InviteFriendModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const InviteFriendModal: React.FC<InviteFriendModalProps> = ({
  isOpen,
  onClose,
}) => {
  const { friends, sendGameInvite } = useSocial();
  const { currentLobby } = useLobby();
  const [searchQuery, setSearchQuery] = useState("");
  const [invitingId, setInvitingId] = useState<string | null>(null);
  const [invitedIds, setInvitedIds] = useState<Set<string>>(new Set());

  // Filter online friends (exclude those already in the lobby)
  const playersInLobby = currentLobby ? Object.keys(currentLobby.players) : [];
  
  const availableFriends = friends.filter((friend) => {
    // Only show online/playing friends not already in lobby
    const isOnline = friend.status === "ONLINE" || friend.status === "PLAYING";
    const isInLobby = playersInLobby.includes(friend.id);
    const matchesSearch = friend.username.toLowerCase().includes(searchQuery.toLowerCase());
    return isOnline && !isInLobby && matchesSearch;
  });

  const offlineFriends = friends.filter((friend) => {
    const isOffline = friend.status === "OFFLINE";
    const isInLobby = playersInLobby.includes(friend.id);
    const matchesSearch = friend.username.toLowerCase().includes(searchQuery.toLowerCase());
    return isOffline && !isInLobby && matchesSearch;
  });

  const handleInvite = async (friend: Friend) => {
    if (!currentLobby || invitingId) return;

    setInvitingId(friend.id);
    try {
      await sendGameInvite(
        friend.id,
        currentLobby.id,
        currentLobby.name,
        currentLobby.gameType as "MAKAO" | "LUDO"
      );
      setInvitedIds((prev) => new Set(prev).add(friend.id));
    } finally {
      setInvitingId(null);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          {/* Backdrop */}
          <motion.div
            className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          {/* Modal */}
          <motion.div
            className="fixed inset-0 z-50 flex items-center justify-center p-4"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              transition={{ type: "spring", stiffness: 300, damping: 25 }}
              className="relative w-full max-w-md rounded-2xl border border-purple-500/30
                         bg-gradient-to-br from-purple-500/10 to-cyan-500/10
                         backdrop-blur-xl shadow-2xl shadow-purple-900/30"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-white/10">
                <div className="flex items-center gap-3 text-purple-400">
                  <Gamepad2 size={22} />
                  <h3 className="font-bold text-lg text-white">Invite Friends</h3>
                </div>
                <button
                  onClick={onClose}
                  className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition"
                >
                  <X size={18} />
                </button>
              </div>

              <div className="p-6 space-y-4">
                {/* Search */}
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
                  <input
                    type="text"
                    placeholder="Search friends..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/10 rounded-xl
                             text-white placeholder-gray-500 focus:outline-none focus:border-purple-500/50
                             transition"
                  />
                </div>

                {/* Lobby Info */}
                {currentLobby && (
                  <div className="flex items-center justify-between px-3 py-2 bg-white/5 rounded-lg border border-white/10">
                    <span className="text-sm text-gray-400">Inviting to:</span>
                    <span className="text-sm font-medium text-purple-400">{currentLobby.name}</span>
                  </div>
                )}

                {/* Friends List */}
                <div className="max-h-[300px] overflow-y-auto space-y-2 custom-scrollbar">
                  {/* Online Friends */}
                  {availableFriends.length > 0 && (
                    <>
                      <div className="text-xs font-bold uppercase tracking-wider text-green-400 px-1 pt-2">
                        Online ({availableFriends.length})
                      </div>
                      {availableFriends.map((friend) => (
                        <motion.div
                          key={friend.id}
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          className="flex items-center justify-between p-3 bg-white/5 rounded-xl border border-white/10 hover:border-purple-500/40 transition group"
                        >
                          <div className="flex items-center gap-3">
                            <div className="relative">
                              <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-cyan-500 flex items-center justify-center text-white font-bold">
                                {friend.username.charAt(0).toUpperCase()}
                              </div>
                              <div className={`absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-[#121018]
                                ${friend.status === 'ONLINE' ? 'bg-green-500' : 'bg-yellow-500'}`}
                              />
                            </div>
                            <div>
                              <p className="font-semibold text-sm text-white">{friend.username}</p>
                              <p className={`text-xs ${friend.status === 'ONLINE' ? 'text-green-400' : 'text-yellow-400'}`}>
                                {friend.status === 'ONLINE' ? 'Online' : 'Playing'}
                              </p>
                            </div>
                          </div>
                          
                          {invitedIds.has(friend.id) ? (
                            <span className="px-3 py-1.5 text-xs font-medium text-green-400 bg-green-500/20 rounded-lg">
                              Invited ✓
                            </span>
                          ) : (
                            <button
                              onClick={() => handleInvite(friend)}
                              disabled={invitingId === friend.id}
                              className="px-3 py-1.5 rounded-lg bg-purple-500/20 text-purple-400 
                                       hover:bg-purple-500 hover:text-white transition-all
                                       flex items-center gap-1.5 text-xs font-medium
                                       disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                              {invitingId === friend.id ? (
                                <Loader2 size={14} className="animate-spin" />
                              ) : (
                                <UserPlus size={14} />
                              )}
                              Invite
                            </button>
                          )}
                        </motion.div>
                      ))}
                    </>
                  )}

                  {/* Offline Friends */}
                  {offlineFriends.length > 0 && (
                    <>
                      <div className="text-xs font-bold uppercase tracking-wider text-gray-500 px-1 pt-4">
                        Offline ({offlineFriends.length})
                      </div>
                      {offlineFriends.map((friend) => (
                        <div
                          key={friend.id}
                          className="flex items-center justify-between p-3 bg-white/5 rounded-xl border border-white/5 opacity-50"
                        >
                          <div className="flex items-center gap-3">
                            <div className="w-10 h-10 rounded-full bg-gray-700 flex items-center justify-center text-gray-400 font-bold">
                              {friend.username.charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <p className="font-semibold text-sm text-gray-400">{friend.username}</p>
                              <p className="text-xs text-gray-600">Offline</p>
                            </div>
                          </div>
                          <span className="text-xs text-gray-600">Not available</span>
                        </div>
                      ))}
                    </>
                  )}

                  {/* Empty State */}
                  {availableFriends.length === 0 && offlineFriends.length === 0 && (
                    <div className="py-12 text-center">
                      <Users className="w-12 h-12 mx-auto text-gray-600 mb-3" />
                      <p className="text-gray-500 text-sm">
                        {searchQuery ? "No friends match your search" : "No friends to invite"}
                      </p>
                      <p className="text-gray-600 text-xs mt-1">
                        Add friends from the Social Center
                      </p>
                    </div>
                  )}
                </div>
              </div>

              {/* Footer */}
              <div className="px-6 py-4 border-t border-white/10">
                <button
                  onClick={onClose}
                  className="w-full py-3 rounded-xl bg-white/5 border border-white/10
                           text-gray-300 font-medium hover:bg-white/10 transition"
                >
                  Done
                </button>
              </div>
            </motion.div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

export default InviteFriendModal;
