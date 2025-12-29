import { motion } from "framer-motion";

type Props = {
  title: string;
  subtitle?: string;
};

export function LobbyHeader({ title, subtitle }: Props) {
  return (
    <motion.div
      className="text-center"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ delay: 0.1 }}
    >
      <h1
        className="text-4xl font-extrabold tracking-wide
                     bg-white
                     bg-clip-text text-transparent"
      >
        {title}
      </h1>

      {subtitle && <p className="text-gray-400 mt-3">{subtitle}</p>}
    </motion.div>
  );
}
