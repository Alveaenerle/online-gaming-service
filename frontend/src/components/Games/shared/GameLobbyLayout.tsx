import { motion } from "framer-motion";
import Navbar from "../../Shared/Navbar";

type Props = {
  children: React.ReactNode;
};

export function GameLobbyLayout({ children }: Props) {
  return (
    <div className="relative min-h-screen bg-[#05060f] text-white overflow-hidden">
      <Navbar />
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-gradient-to-b from-[#0a0b1f] via-[#05060f] to-black" />
        <div
          className="absolute top-[-20%] left-1/2 -translate-x-1/2
                        w-[900px] h-[900px]
                        bg-purple-900/30 blur-[140px]"
        />
        <div className="absolute inset-0 bg-black/40" />
      </div>

      <motion.div
        className="relative z-10 max-w-6xl mx-auto px-6 py-24"
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: "easeOut" }}
      >
        <div className="relative rounded-[32px] p-8 space-y-10">{children}</div>
      </motion.div>
    </div>
  );
}
