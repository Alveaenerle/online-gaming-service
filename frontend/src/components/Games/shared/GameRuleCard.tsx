interface RuleCardProps {
  icon: React.ReactNode;
  title: string;
  desc: string;
  color: "blue" | "purple" | "yellow";
}

export function GameRuleCard({ icon, title, desc, color }: RuleCardProps) {
  const colors = {
    blue: "bg-blue-500/10 text-blue-400",
    purple: "bg-purple-500/10 text-purple-400",
    yellow: "bg-yellow-500/10 text-yellow-400",
  };

  return (
    <div className="bg-[#121018] p-4 sm:p-6 lg:p-8 rounded-xl sm:rounded-2xl lg:rounded-[2rem] border border-white/5 space-y-2 sm:space-y-3 lg:space-y-4 hover:border-white/10 transition-colors group">
      <div
        className={`w-10 h-10 sm:w-12 sm:h-12 rounded-xl sm:rounded-2xl flex items-center justify-center transition-transform group-hover:scale-110 ${colors[color]}`}
      >
        {icon}
      </div>
      <h3 className="text-sm sm:text-base lg:text-lg font-bold text-white">{title}</h3>
      <p className="text-xs sm:text-sm text-gray-500 leading-relaxed font-medium line-clamp-2 sm:line-clamp-none">
        {desc}
      </p>
    </div>
  );
}
