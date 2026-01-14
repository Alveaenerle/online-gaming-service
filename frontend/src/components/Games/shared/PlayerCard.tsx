import { motion } from "framer-motion";
import { Crown, UserMinus, UserPlus, UserCheck, Mail, Plus } from "lucide-react";

type Props = {
  player?: {
    userId: string;
    username: string;
    avatar: string;
    isHost?: boolean;
    isReady?: boolean;
    isYou?: boolean;
  };
  onKick?: (userId: string) => void;
  onAddFriend?: (userId: string) => void;
  onInviteToLobby?: () => void;
  showKickButton?: boolean;
  canAddFriend?: boolean;
  canInviteToLobby?: boolean;
  isInvited?: boolean;
  hasReceivedRequest?: boolean; // This user sent us a friend request
};

export function PlayerCard({ player, onKick, onAddFriend, onInviteToLobby, showKickButton, canAddFriend, canInviteToLobby, isInvited, hasReceivedRequest }: Props) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`relative w-full h-60 rounded-3xl p-5 flex flex-col items-center justify-center transition-all duration-500 ${
        player
          ? `bg-[#121018] border border-white/5 shadow-2xl ${
              player.isReady ? "border-purple-500/40" : ""
            }`
          : "bg-white/[0.02] border border-dashed border-white/10 group cursor-pointer hover:border-purple-500/40 hover:bg-purple-500/5"
      }`}
      onClick={() => {
        if (!player && canInviteToLobby && onInviteToLobby) {
          onInviteToLobby();
        }
      }}
    >
      {player ? (
        <>
          <div
            className={`relative w-24 h-24 rounded-full p-1 border-2 transition-colors duration-500 ${
              player.isReady
                ? "border-green-500 shadow-[0_0_15px_rgba(34,197,94,0.3)]"
                : "border-purple-500/50"
            }`}
          >
            <img
              src={player.avatar}
              alt=""
              className="w-full h-full object-cover rounded-full"
            />
            {player.isReady && (
              <motion.div
                animate={{ opacity: [0.5, 1, 0.5] }}
                transition={{ repeat: Infinity, duration: 2 }}
                className="absolute inset-0 rounded-full shadow-[inset_0_0_15px_rgba(34,197,94,0.6)]"
              />
            )}
          </div>

          <div className="mt-4 text-center">
            <div className="flex justify-center items-center gap-2">
              {player.isHost && (
                <Crown
                  size={14}
                  className="text-yellow-400 drop-shadow-[0_0_5px_rgba(250,204,21,0.4)]"
                />
              )}
              <span className="text-lg font-bold text-gray-200 tracking-tight truncate max-w-[180px]">
                {player.username}
              </span>
            </div>

            <div
              className={`mt-2 flex items-center justify-center gap-2 text-[10px] font-black uppercase tracking-[0.2em] ${
                player.isReady ? "text-green-400" : "text-gray-500"
              }`}
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${
                  player.isReady ? "bg-green-500 animate-pulse" : "bg-gray-600"
                }`}
              />
              {player.isReady ? "Ready" : "Waiting"}
            </div>
          </div>

          {showKickButton && onKick && (
            <button
              onClick={() => onKick(player.userId)}
              title="Kick Player"
              className="absolute top-3 right-3 p-2 bg-red-500/10 hover:bg-red-500 text-red-500 hover:text-white rounded-xl transition-all duration-300 border border-red-500/20"
            >
              <UserMinus size={14} />
            </button>
          )}

          {canAddFriend && onAddFriend && !player.isYou && (
             <button
                onClick={() => onAddFriend(player.userId)}
                title="Add Friend"
                className="absolute top-3 left-3 p-2 bg-purple-500/10 hover:bg-purple-500 text-purple-500 hover:text-white rounded-xl transition-all duration-300 border border-purple-500/20"
             >
                <UserPlus size={14} />
             </button>
          )}
          
          {isInvited && !player.isYou && (
            <div
                title="Request Sent"
                className="absolute top-3 left-3 p-2 bg-white/10 text-white/50 rounded-xl transition-all duration-300 border border-white/20 cursor-default"
            >
                <UserCheck size={14} />
            </div>
          )}
          
          {hasReceivedRequest && !player.isYou && (
            <div
                title="Has Pending Request - Check Social Center"
                className="absolute top-2 left-2 sm:top-3 sm:left-3 p-1.5 sm:p-2 bg-orange-500/20 text-orange-400 rounded-lg sm:rounded-xl transition-all duration-300 border border-orange-500/30 cursor-default"
            >
                <Mail size={12} className="sm:hidden" />
                <Mail size={14} className="hidden sm:block" />
            </div>
          )}
        </>
      ) : (
        <div className="flex flex-col items-center gap-2 sm:gap-3">
          {canInviteToLobby ? (
            <>
              <div className="w-12 h-12 sm:w-14 sm:h-14 md:w-16 md:h-16 rounded-full border-2 border-dashed border-white/20 flex items-center justify-center group-hover:border-purple-500/50 group-hover:bg-purple-500/10 transition-all duration-300">
                <Plus size={20} className="text-white/30 group-hover:text-purple-400 transition-colors sm:hidden" />
                <Plus size={24} className="text-white/30 group-hover:text-purple-400 transition-colors hidden sm:block" />
              </div>
              <span className="text-[10px] sm:text-xs font-bold uppercase tracking-widest text-white/30 group-hover:text-purple-400 transition-colors">
                Invite Friend
              </span>
            </>
          ) : (
            <>
              <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-full border-2 border-dashed border-white/20 opacity-30" />
              <span className="text-[8px] sm:text-[10px] font-bold uppercase tracking-widest opacity-30">
                Empty Slot
              </span>
            </>
          )}
        </div>
      )}
    </motion.div>
  );
}
