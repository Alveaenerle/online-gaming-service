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
    <div className="bg-[#121018] p-8 rounded-[2rem] border border-white/5 space-y-4 hover:border-white/10 transition-colors group">
      <div
        className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-transform group-hover:scale-110 ${colors[color]}`}
      >
        {icon}
      </div>
      <h3 className="text-lg font-bold text-white">{title}</h3>
      <p className="text-sm text-gray-500 leading-relaxed font-medium">
        {desc}
      </p>
    </div>
  );
}
