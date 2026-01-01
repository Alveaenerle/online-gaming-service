import { motion } from "framer-motion";
import { X, User } from "lucide-react";

const avatars = [
  "/avatars/avatar_1.png",
  "/avatars/avatar_2.png",
  "/avatars/avatar_3.png",
  "/avatars/avatar_4.png",
  "/avatars/avatar_5.png",
  "/avatars/avatar_6.png",
];

type Props = {
  onSelect: (avatar: string) => void;
  onClose: () => void;
};

export function AvatarPicker({ onSelect, onClose }: Props) {
  return (
    <motion.div
      className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[100]
                 flex items-center justify-center p-6"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        initial={{ scale: 0.9, y: 20 }}
        animate={{ scale: 1, y: 0 }}
        exit={{ scale: 0.9, y: 20 }}
        onClick={(e) => e.stopPropagation()}
        className="bg-[#121018] rounded-[2.5rem] p-8 max-w-md w-full
                   border border-white/10 relative shadow-[0_0_50px_rgba(139,92,246,0.2)]"
      >
        <button
          onClick={onClose}
          className="absolute top-6 right-6 p-2 rounded-xl bg-white/5 hover:bg-white/10 
                     text-gray-400 hover:text-white transition-all"
        >
          <X size={20} />
        </button>

        <div className="flex items-center gap-4 mb-8">
          <div className="w-12 h-12 rounded-2xl bg-purple-600/20 flex items-center justify-center text-purple-400">
            <User size={24} />
          </div>
          <div>
            <h3 className="text-xl font-black uppercase tracking-tight">
              Avatar
            </h3>
            <p className="text-gray-500 text-xs font-bold uppercase tracking-widest">
              Choose your icon
            </p>
          </div>
        </div>

        <div className="grid grid-cols-4 gap-4">
          {avatars.map((a, index) => (
            <motion.button
              key={a}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={() => {
                onSelect(a);
                onClose();
              }}
              className="relative aspect-square rounded-full border-2 border-white/10
                         hover:border-purple-500 overflow-hidden bg-white/5 transition-colors group"
            >
              <img
                src={a}
                className="w-full h-full object-cover p-2"
                alt="avatar"
              />

              <div className="absolute inset-0 bg-purple-600/20 opacity-0 group-hover:opacity-100 transition-opacity" />
            </motion.button>
          ))}
        </div>

        <div className="mt-8 pt-6 border-t border-white/5 text-center">
          <p className="text-[10px] text-gray-600 uppercase font-black tracking-[0.2em]">
            Select an avatar for the game
          </p>
        </div>
      </motion.div>
    </motion.div>
  );
}
