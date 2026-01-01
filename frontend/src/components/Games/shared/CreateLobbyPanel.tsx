import { PlusCircle } from "lucide-react";
import { GameStepper } from "./GameStepper";

interface CreateProps {
  roomName: string;
  setRoomName: (val: string) => void;
  playerCount: number;
  setPlayerCount: (val: number) => void;
  onCreate: () => void;
}

export function CreateLobbyPanel({
  roomName,
  setRoomName,
  playerCount,
  setPlayerCount,
  onCreate,
}: CreateProps) {
  return (
    <div className="flex-1 bg-[#121018] rounded-[2.5rem] border border-white/5 p-8 flex flex-col shadow-2xl relative overflow-hidden">
      <div className="flex items-center gap-4 mb-5">
        <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
          <PlusCircle size={26} />
        </div>
        <h2 className="text-2xl font-bold">New Lobby</h2>
      </div>

      <div className="space-y-3 flex-1">
        <div className="space-y-3">
          <label className="text-[10px] font-black text-gray-500 uppercase tracking-[0.2em] ml-1">
            Room Identity
          </label>
          <input
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            className="w-full bg-white/5 border border-white/10 rounded-2xl px-6 py-5 focus:ring-2 focus:ring-purple-500 outline-none transition-all placeholder:text-gray-700"
            placeholder="Enter room name..."
          />
        </div>
        <GameStepper
          value={playerCount}
          onChange={setPlayerCount}
          label="Player Capacity"
        />
      </div>

      <button
        onClick={onCreate}
        className="mt-5 w-full py-6 bg-purple-600/60 hover:bg-purple-500 rounded-[1.5rem] font-black transition-all uppercase tracking-widest text-m"
      >
        Create Lobby
      </button>
    </div>
  );
}
