export function GameBadge({
  children,
  color,
}: {
  children: React.ReactNode;
  color: "purple" | "fuchsia";
}) {
  const colors = {
    purple: "bg-purple-600/20 border-purple-500/30 text-purple-400",
    fuchsia: "bg-fuchsia-600/20 border-fuchsia-500/30 text-fuchsia-400",
  };
  return (
    <span
      className={`px-2 sm:px-3 lg:px-4 py-0.5 sm:py-1 border rounded-full text-[8px] sm:text-[10px] font-black uppercase tracking-wider sm:tracking-widest ${colors[color]}`}
    >
      {children}
    </span>
  );
}
