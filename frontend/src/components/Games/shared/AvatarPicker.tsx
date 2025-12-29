import { motion } from "framer-motion";

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
      className="fixed inset-0 bg-black/70 z-50
                 flex items-center justify-center"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
    >
      <motion.div
        initial={{ scale: 0.9 }}
        animate={{ scale: 1 }}
        className="bg-[#0e1022] rounded-3xl p-6
                   border border-purple-500/30
                   shadow-2xl shadow-purple-900/40"
      >
        <h3 className="text-center font-bold mb-4">Choose your icon</h3>

        <div className="grid grid-cols-4 gap-4">
          {avatars.map((a) => (
            <button
              key={a}
              onClick={() => {
                onSelect(a);
                onClose();
              }}
              className="w-16 h-16 rounded-full
                         border-2 border-transparent
                         hover:border-purple-500
                         overflow-hidden"
            >
              <img src={a} className="w-full h-full" />
            </button>
          ))}
        </div>
      </motion.div>
    </motion.div>
  );
}
