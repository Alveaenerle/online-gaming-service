import { ChevronLeft, Users } from "lucide-react";
import { useAuth } from "../../../context/AuthContext";

interface GameHeaderProps {
  title: string;
  subtitle: string;
  onBack: () => void;
  onSocialClick: () => void;
}

export function GameHeader({ title, subtitle, onBack, onSocialClick }: GameHeaderProps) {
  const { user } = useAuth();
  const isGuest = user?.isGuest ?? false;

  return (
    <div className="flex items-center justify-between mb-8">
      <div className="flex items-center gap-6">
        <button
          onClick={onBack}
          className="p-3 bg-white/5 hover:bg-white/10 rounded-2xl border border-white/10 transition-all group"
        >
          <ChevronLeft
            size={20}
            className="group-hover:-translate-x-1 transition-transform"
          />
        </button>
        <h1 className="text-4xl font-black tracking-tighter flex items-center gap-4">
          <span className="bg-purple-600 bg-clip-text text-transparent uppercase">
            {title}
          </span>
          <span className="text-white/10 font-light text-3xl">/</span>
          <span className="text-xl text-gray-500 font-medium tracking-widest uppercase">
            {subtitle}
          </span>
        </h1>
        {!isGuest && (
          <button
            onClick={onSocialClick}
            className="flex items-center gap-3 px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl transition-all group"
          >
            <div className="relative">
              <Users size={18} className="text-purple-400" />
              <div className="absolute -top-1 -right-1 w-2 h-2 bg-green-500 rounded-full border-2 border-[#07060b] animate-pulse" />
            </div>
            <span className="text-xs font-black uppercase tracking-widest">
              Social
            </span>
          </button>
        )}
      </div>
    </div>
  );
}
