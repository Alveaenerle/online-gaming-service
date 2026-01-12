import { LogOut } from "lucide-react";

export function SidebarFooter() {
  return (
    <div className="p-8 border-t border-white/5 bg-black/20">
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
