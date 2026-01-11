import { LogOut } from "lucide-react";

export function SidebarFooter({ onDiceRoll }: { onDiceRoll: () => void }) {
  return (
    <div className="p-8 border-t border-white/5 bg-black/20">
      <button
        onClick={onDiceRoll}
        className="w-full py-5 bg-white text-black font-black uppercase text-[11px] tracking-[0.2em] rounded-[24px] hover:scale-[1.02] active:scale-95 transition-all shadow-[0_15px_40px_rgba(255,255,255,0.1)] mb-4"
      >
        Initiate Dice Roll
      </button>
      <button className="flex items-center justify-center gap-3 w-full py-4 text-red-500/60 hover:text-red-500 transition-all text-[9px] font-black uppercase tracking-[0.3em] group">
        <LogOut
          size={14}
          className="group-hover:-translate-x-1 transition-transform"
        />
        Leave Game
      </button>
    </div>
  );
}
