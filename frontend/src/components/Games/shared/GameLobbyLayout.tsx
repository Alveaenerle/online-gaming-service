import { motion } from "framer-motion";
import Navbar from "../../Shared/Navbar";
import { BackgroundGradient } from "../../Shared/BackgroundGradient";

type Props = {
  children: React.ReactNode;
};

export function GameLobbyLayout({ children }: Props) {
  return (
    <div className="relative min-h-screen bg-[#05060f] text-white overflow-hidden ">
      <Navbar />
      <BackgroundGradient />
      <motion.div
        className="relative z-10 max-w-6xl mx-auto px-3 sm:px-4 md:px-6 py-16 sm:py-20"
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.7, ease: "easeOut" }}
      >
        <div className="relative rounded-2xl sm:rounded-[2rem] lg:rounded-[32px] p-4 sm:p-6 lg:p-8 space-y-6 sm:space-y-8 lg:space-y-10">{children}</div>
      </motion.div>
    </div>
  );
}
