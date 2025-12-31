import { motion } from "framer-motion";

type Props = {
  title: string;
  subtitle?: string;
  accessCode?: string; // nowa opcja
};

export function LobbyHeader({ title, subtitle, accessCode }: Props) {
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

      {subtitle && <p className="text-gray-400 mt-2">{subtitle}</p>}

      {accessCode && (
        <p className="text-purple-500 mt-1 font-semibold">
          Access Code: {accessCode}
        </p>
      )}
    </motion.div>
  );
}
