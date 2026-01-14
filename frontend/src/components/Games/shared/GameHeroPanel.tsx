import { motion } from "framer-motion";
import { GameBadge } from "./GameBadge";
import { GameRuleCard } from "./GameRuleCard";

interface HeroProps {
  title: string;
  description: string;
  image: string;
  rules: { title: string; desc: string; icon: any; color: any }[];
}

export function GameHeroPanel({ title, description, image, rules }: HeroProps) {
  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      className="lg:col-span-8 flex flex-col gap-4 sm:gap-6 lg:max-h-[calc(100vh-200px)]"
    >
      <div className="flex-1 min-h-[200px] sm:min-h-[280px] lg:min-h-[300px] bg-[#121018] rounded-2xl sm:rounded-[2rem] lg:rounded-[2.5rem] border border-white/5 relative overflow-hidden group shadow-2xl flex flex-col justify-end">
        <div
          className="absolute inset-0 bg-cover bg-center opacity-30 group-hover:scale-105 transition-transform duration-700"
          style={{ backgroundImage: `url(${image})` }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#121018] via-[#121018]/40 to-[#121018]/10" />
        <div className="relative p-4 sm:p-8 lg:p-12 z-10 space-y-2 sm:space-y-3 lg:space-y-4">
          <div className="flex gap-2 sm:gap-3 flex-wrap">
            <GameBadge color="purple">Strategic</GameBadge>
            <GameBadge color="fuchsia">Multiplayer</GameBadge>
          </div>
          <h2 className="text-2xl sm:text-3xl lg:text-5xl font-black tracking-tight">{title}</h2>
          <p className="text-gray-400 text-sm sm:text-base lg:text-xl max-w-xl leading-relaxed line-clamp-3 sm:line-clamp-none">
            {description}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 xs:grid-cols-3 gap-3 sm:gap-4 lg:gap-6">
        {rules.map((rule, i) => (
          <GameRuleCard key={i} {...rule} />
        ))}
      </div>
    </motion.div>
  );
}
