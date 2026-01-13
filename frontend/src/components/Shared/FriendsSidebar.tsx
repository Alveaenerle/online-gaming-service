import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Users, X, Check, Clock, UserMinus, Gamepad2 } from "lucide-react";
import { useSocial } from "../../context/SocialContext";
import { useNavigate } from "react-router-dom";
import { ConfirmationModal } from "./ConfirmationModal";
import { lobbyService } from "../../services/lobbyService";
import { useToast } from "../../context/ToastContext";

interface FriendsSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const FriendsSidebar: React.FC<FriendsSidebarProps> = ({
  isOpen,
  onClose,
}) => {
  const { friends, pendingRequests, gameInvites, acceptFriendRequest, rejectFriendRequest, removeFriend, acceptGameInvite, declineGameInvite } = useSocial();
  const navigate = useNavigate();
  const { showToast } = useToast();

  // State for remove friend confirmation
  const [friendToRemove, setFriendToRemove] = useState<{ id: string; username: string } | null>(null);

  const handleAcceptGameInvite = async (inviteId: string) => {
    try {
      const invite = await acceptGameInvite(inviteId);
      
      // Join the lobby using the accessCode
      if (invite.accessCode) {
        await lobbyService.joinRoom(invite.accessCode, invite.gameType, false);
      }
      
      // Navigate to the lobby
      onClose();
      navigate(`/lobby/${invite.gameType.toLowerCase()}`);
    } catch (err: any) {
      showToast(err.message || "Failed to join lobby", "error");
    }
  };

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
              
              {/* Game Invites Section */}
              {gameInvites.length > 0 && (
                <div className="space-y-3">
                    <div className="flex items-center justify-between text-xs font-bold uppercase tracking-wider text-cyan-400">
                        <span className="flex items-center gap-2">
                            <Gamepad2 size={14} />
                            Game Invites
                        </span>
                        <span className="bg-cyan-500/20 px-2 py-0.5 rounded text-cyan-300 animate-pulse">
                            {gameInvites.length}
                        </span>
                    </div>
                    
                    {gameInvites.map((invite) => (
                        <motion.div 
                            key={invite.id}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="bg-gradient-to-br from-cyan-500/10 to-purple-500/10 p-3 rounded-xl border border-cyan-500/30 flex flex-col gap-2"
                        >
                            <div className="flex items-center justify-between">
                                <div className="flex flex-col min-w-0">
                                    <span className="font-semibold text-sm truncate text-white">
                                        {invite.senderUsername}
                                    </span>
                                    <span className="text-xs text-gray-400">
                                        invites you to <span className="text-cyan-400 font-medium">{invite.lobbyName}</span>
                                    </span>
                                </div>
                                <span className="text-xs px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 font-medium">
                                    {invite.gameType}
                                </span>
                            </div>
                            <div className="flex items-center gap-2">
                                <button 
                                    onClick={() => handleAcceptGameInvite(invite.id)}
                                    className="flex-1 py-1.5 rounded-lg bg-green-500/20 text-green-400 hover:bg-green-500 hover:text-white transition-colors text-xs font-medium flex items-center justify-center gap-1"
                                >
                                    <Check size={14} /> Join
                                </button>
                                <button 
                                    onClick={() => declineGameInvite(invite.id)}
                                    className="flex-1 py-1.5 rounded-lg bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white transition-colors text-xs font-medium flex items-center justify-center gap-1"
                                >
                                    <X size={14} /> Decline
                                </button>
                            </div>
                        </motion.div>
                    ))}
                </div>
              )}
              
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
                        <div className="flex flex-col">
                            <p className="font-semibold text-sm text-white group-hover:text-purple-300 transition-colors">{friend.username}</p>
                            <p className={`text-xs font-medium
                                ${friend.status === 'ONLINE' ? 'text-green-400' : 
                                  friend.status === 'PLAYING' ? 'text-yellow-400' : 'text-gray-500'}`}
                            >
                                {friend.status === 'ONLINE' ? 'online' : 
                                 friend.status === 'PLAYING' ? 'playing' : 'offline'}
                            </p>
                        </div>
                        <div className="flex items-center gap-2">
                            <button 
                                onClick={() => setFriendToRemove({ id: friend.id, username: friend.username })}
                                className="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 
                                         bg-red-500/20 text-red-400 hover:bg-red-500 hover:text-white 
                                         transition-all"
                                title="Remove friend"
                            >
                                <UserMinus size={14} />
                            </button>
                            {/* Status dot on the right */}
                            <div className={`w-3 h-3 rounded-full flex-shrink-0
                                ${friend.status === 'ONLINE' ? 'bg-green-500 shadow-lg shadow-green-500/50' : 
                                  friend.status === 'PLAYING' ? 'bg-yellow-500 shadow-lg shadow-yellow-500/50' : 'bg-gray-500'}`} 
                            />
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

          <ConfirmationModal
            isOpen={!!friendToRemove}
            onClose={() => setFriendToRemove(null)}
            onConfirm={() => {
              if (friendToRemove) {
                removeFriend(friendToRemove.id);
                setFriendToRemove(null);
              }
            }}
            title="Remove Friend?"
            message={`Are you sure you want to remove ${friendToRemove?.username} from your friends list? You'll need to send a new friend request to add them again.`}
            confirmText="Remove"
            variant="danger"
          />
        </>
      )}
    </AnimatePresence>
  );
};
