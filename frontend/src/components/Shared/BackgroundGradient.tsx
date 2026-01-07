export function BackgroundGradient() {
  return (
    <div className="absolute inset-0">
      <div className="absolute inset-0 bg-gradient-to-b from-[#0a0b1f] via-[#05060f] to-black" />
      <div
        className="absolute top-[-20%] left-1/2 -translate-x-1/2
                        w-[900px] h-[900px]
                        bg-purple-900/30 blur-[140px]"
      />
      <div className="absolute inset-0 bg-black/40" />
    </div>
  );
}
