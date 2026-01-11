import { Cpu } from "lucide-react";

export function SidebarHeader() {
  return (
    <div className="p-8 border-b border-white/5">
      <div className="flex items-center gap-3 mb-4 opacity-40">
        <Cpu size={14} className="text-purple-500" />
        <span className="text-[9px] font-black uppercase tracking-[0.4em] text-white">
          Battle Management System
        </span>
      </div>
      <h1 className="text-white text-3xl font-black italic uppercase tracking-tighter leading-none">
        Ludo{" "}
        <span className="text-purple-500 drop-shadow-[0_0_15px_rgba(168,85,247,0.4)]">
          Arena
        </span>
      </h1>
    </div>
  );
}
