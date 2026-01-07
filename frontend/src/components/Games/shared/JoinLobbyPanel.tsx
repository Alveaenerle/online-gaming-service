import { Users, Hash, Shuffle } from "lucide-react";

interface JoinProps {
  roomCode: string;
  setRoomCode: (val: string) => void;
  onJoin: (isRandom: boolean) => void;
}

export function JoinLobbyPanel({ roomCode, setRoomCode, onJoin }: JoinProps) {
  return (
    <div className="bg-[#121018] rounded-[2.5rem] border border-white/5 p-8 shadow-2xl space-y-6">
      <div className="flex items-center gap-4 mb-2">
        <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
          <Users size={24} />
        </div>
        <h2 className="text-2xl font-bold">Quick Join</h2>
      </div>
      <div className="relative group">
        <Hash
          className="absolute left-5 top-1/2 -translate-y-1/2 text-gray-500 group-focus-within:text-purple-500 transition-colors"
          size={20}
        />
        <input
          value={roomCode}
          onChange={(e) => setRoomCode(e.target.value.toUpperCase())}
          className="w-full bg-white/5 border border-white/10 rounded-2xl pl-14 pr-6 py-5 focus:ring-2 focus:ring-purple-500 outline-none uppercase font-mono text-xl tracking-[0.3em] transition-all"
          placeholder="CODE"
        />
      </div>
      <div className="grid grid-cols-2 gap-4">
        <button
          onClick={() => onJoin(false)}
          className="py-5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-2xl font-black uppercase text-xs tracking-widest transition-all"
        >
          Join Code
        </button>
        <button
          onClick={() => onJoin(true)}
          className="py-5 flex items-center justify-center gap-3 bg-purple-600/10 hover:bg-purple-600/20 text-purple-400 rounded-2xl font-black border border-fuchsia-500/20 uppercase text-xs tracking-widest transition-all"
        >
          <Shuffle size={18} /> Random
        </button>
      </div>
    </div>
  );
}
