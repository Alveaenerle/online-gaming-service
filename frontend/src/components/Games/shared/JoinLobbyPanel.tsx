import { Users, Hash, Shuffle } from "lucide-react";

interface JoinProps {
  roomCode: string;
  setRoomCode: (val: string) => void;
  onJoin: (isRandom: boolean) => void;
  hideHeader?: boolean;
}

export function JoinLobbyPanel({ roomCode, setRoomCode, onJoin, hideHeader = false }: JoinProps) {
  return (
    <div className="bg-[#121018] rounded-2xl sm:rounded-[2rem] lg:rounded-[2.5rem] border border-white/5 p-4 sm:p-6 lg:p-8 shadow-2xl space-y-4 sm:space-y-6">
      {!hideHeader && (
        <div className="flex items-center gap-2 sm:gap-4 mb-1 sm:mb-2">
          <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl sm:rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center flex-shrink-0">
            <Users size={20} className="sm:hidden" />
            <Users size={24} className="hidden sm:block" />
          </div>
          <h2 className="text-lg sm:text-xl lg:text-2xl font-bold">Quick Join</h2>
        </div>
      )}
      <div className="relative group">
        <Hash
          className="absolute left-3 sm:left-5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-purple-500 transition-colors"
          size={18}
        />
        <input
          value={roomCode}
          onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
          className="w-full bg-white/5 border border-white/10 rounded-xl sm:rounded-2xl pl-10 sm:pl-14 pr-4 sm:pr-6 py-3 sm:py-4 lg:py-5 focus:ring-2 focus:ring-purple-500 outline-none uppercase font-mono text-base sm:text-xl tracking-[0.2em] sm:tracking-[0.3em] transition-all min-h-[44px]"
          placeholder="CODE"
        />
      </div>
      <div className="grid grid-cols-2 gap-2 sm:gap-4">
        <button
          onClick={() => onJoin(false)}
          className="py-3 sm:py-4 lg:py-5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl sm:rounded-2xl font-black uppercase text-[10px] sm:text-xs tracking-widest transition-all min-h-[44px]"
        >
          Join Code
        </button>
        <button
          onClick={() => onJoin(true)}
          className="py-3 sm:py-4 lg:py-5 flex items-center justify-center gap-1.5 sm:gap-3 bg-purple-600/10 hover:bg-purple-600/20 text-purple-400 rounded-xl sm:rounded-2xl font-black border border-fuchsia-500/20 uppercase text-[10px] sm:text-xs tracking-widest transition-all min-h-[44px]"
        >
          <Shuffle size={16} className="sm:hidden" />
          <Shuffle size={18} className="hidden sm:block" />
          Random
        </button>
      </div>
    </div>
  );
}
