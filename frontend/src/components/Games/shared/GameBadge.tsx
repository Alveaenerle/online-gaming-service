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
      className={`px-4 py-1 border rounded-full text-[10px] font-black uppercase tracking-widest ${colors[color]}`}
    >
      {children}
    </span>
  );
}
