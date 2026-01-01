import { motion } from "framer-motion";
import Navbar from "../../Shared/Navbar";
import { BackgroundGradient } from "../../Shared/BackgroundGradient";

type Props = {
  children: React.ReactNode;
};

export function GameLobbyLayout({ children }: Props) {
  return (
    <div className="relative bg-[#05060f] text-white overflow-hidden ">
      <Navbar />
      <BackgroundGradient />
      <motion.div
        className="relative z-10 max-w-6xl mx-auto px-6 py-20"
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: "easeOut" }}
      >
        <div className="relative rounded-[32px] p-8 space-y-10">{children}</div>
      </motion.div>
    </div>
  );
}
