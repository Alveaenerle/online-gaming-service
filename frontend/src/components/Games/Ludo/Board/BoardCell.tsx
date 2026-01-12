import { CellType, Color } from "./constants";

interface Props {
  type: CellType;
  color?: Color;
}

export function BoardCell({ type, color }: Props) {
  if (type === "EMPTY") {
    return <div className="w-full h-full bg-transparent" />;
  }

  const playerNeonColors = {
    RED: "from-red-600/20 to-purple-900/40 border-red-500/50 shadow-[0_0_15px_rgba(239,68,68,0.3)]",
    BLUE: "from-blue-600/20 to-purple-900/40 border-blue-500/50 shadow-[0_0_15px_rgba(59,130,246,0.3)]",
    YELLOW:
      "from-yellow-500/20 to-purple-900/40 border-yellow-400/50 shadow-[0_0_15px_rgba(234,179,8,0.3)]",
    GREEN:
      "from-green-600/20 to-purple-900/40 border-green-500/50 shadow-[0_0_15px_rgba(34,197,94,0.3)]",
  };

  if (type === "BASE") {
    const style = playerNeonColors[color!] || "";
    return (
      <div
        className={`w-full h-full rounded-3xl border-2 bg-gradient-to-br p-1 ${style} relative group overflow-hidden`}
      >
        <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" />
        <div className="relative w-full h-full border border-white/10 rounded-2xl flex items-center justify-center">
          <div className="absolute inset-0 opacity-20 bg-[linear-gradient(45deg,transparent_25%,rgba(255,255,255,0.1)_50%,transparent_75%)] bg-[length:250%_250%] animate-shimmer" />
          <div className="w-8 h-8 rounded-full border border-white/20 flex items-center justify-center animate-pulse">
            <div className="w-2 h-2 bg-white rounded-full shadow-[0_0_10px_white]" />
          </div>
        </div>
      </div>
    );
  }

  if (type === "CENTER") {
    return (
      <div className="w-full h-full p-1">
        <div className="w-full h-full bg-slate-950 rounded-2xl border-2 border-purple-500/50 shadow-[0_0_40px_rgba(168,85,247,0.4)] relative overflow-hidden flex items-center justify-center">
          <div className="absolute w-[150%] h-[150%] bg-[conic-gradient(from_0deg,transparent,rgb(168,85,247),transparent,rgb(192,132,252),transparent)] animate-[spin_4s_linear_infinite] opacity-30" />

          <div className="relative z-10 w-3/4 h-3/4 bg-purple-950/80 rounded-xl border border-purple-400/50 backdrop-blur-md flex items-center justify-center">
            <div className="w-4 h-4 bg-white rounded-sm rotate-45 shadow-[0_0_20px_white] animate-bounce" />
          </div>

          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay" />
        </div>
      </div>
    );
  }

  const isStart = type === "START";
  const baseColor = color
    ? playerNeonColors[color]
    : "from-purple-900/20 to-slate-900 border-purple-500/20 shadow-none";

  return (
    <div className="w-full h-full p-[2px]">
      <div
        className={`
          w-full h-full border transition-all duration-300 relative
          ${isStart ? "rounded-xl border-2 z-10 scale-110" : "rounded-lg"}
          bg-gradient-to-tr ${baseColor}
          hover:brightness-125 hover:scale-[1.02]
        `}
      >
        <div className="absolute inset-x-0 top-0 h-[1px] bg-gradient-to-r from-transparent via-white/20 to-transparent" />
        {isStart && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="w-1/2 h-[1px] bg-white/40 rotate-45" />
            <div className="w-1/2 h-[1px] bg-white/40 -rotate-45" />
          </div>
        )}
        .
      </div>
    </div>
  );
}
