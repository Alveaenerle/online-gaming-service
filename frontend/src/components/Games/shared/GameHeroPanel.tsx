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
      className="lg:col-span-8 flex flex-col gap-6 max-h-[calc(100vh-200px)]"
    >
      <div className="flex-1 bg-[#121018] rounded-[2.5rem] border border-white/5 relative overflow-hidden group shadow-2xl">
        <div
          className="absolute inset-0 bg-cover bg-center opacity-30 group-hover:scale-105 transition-transform duration-700"
          style={{ backgroundImage: `url(${image})` }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#121018] via-[#121018]/20 to-transparent" />
        <div className="absolute bottom-12 left-12 z-10 space-y-4">
          <div className="flex gap-3">
            <GameBadge color="purple">Strategic</GameBadge>
            <GameBadge color="fuchsia">Multiplayer</GameBadge>
          </div>
          <h2 className="text-5xl font-black tracking-tight">{title}</h2>
          <p className="text-gray-400 text-xl max-w-xl leading-relaxed">
            {description}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        {rules.map((rule, i) => (
          <GameRuleCard key={i} {...rule} />
        ))}
      </div>
    </motion.div>
  );
}
