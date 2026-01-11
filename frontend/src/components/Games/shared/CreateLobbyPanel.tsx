import { PlusCircle, Settings, X, Lock, Globe } from "lucide-react";
import { GameStepper } from "./GameStepper";
import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";

interface CreateProps {
  roomName: string;
  setRoomName: (val: string) => void;
  playerCount: number;
  setPlayerCount: (val: number) => void;
  isPrivate: boolean;
  setIsPrivate: (val: boolean) => void;
  onCreate: () => void;
  minPlayers?: number;
  maxPlayers?: number;
}

export function CreateLobbyPanel({
  roomName,
  setRoomName,
  playerCount,
  setPlayerCount,
  isPrivate,
  setIsPrivate,
  onCreate,
  minPlayers,
  maxPlayers,
}: CreateProps) {
  const [showSettings, setShowSettings] = useState(false);

  return (
    <div className="flex-1 bg-[#121018] rounded-[2.5rem] border border-white/5 p-8 flex flex-col shadow-2xl relative overflow-hidden">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
            <PlusCircle size={26} />
          </div>
          <h2 className="text-2xl font-bold">New Lobby</h2>
        </div>
        <button
          onClick={() => setShowSettings(true)}
          className="p-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
        >
          <Settings size={20} />
        </button>
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
          min={minPlayers || 1}
          max={maxPlayers || 8}
        />
      </div>

      {/* Settings Overlay */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="absolute inset-0 bg-[#121018]/95 backdrop-blur-md z-10 flex flex-col p-8"
          >
            <div className="flex items-center justify-between mb-8">
              <h3 className="text-xl font-bold flex items-center gap-3">
                <Settings className="text-purple-500" size={20} />
                Lobby Settings
              </h3>
              <button
                onClick={() => setShowSettings(false)}
                className="p-2 rounded-lg hover:bg-white/10 transition-colors"
              >
                <X size={20} />
              </button>
            </div>

            <div className="space-y-4">
              <label className="text-[10px] font-black text-gray-500 uppercase tracking-[0.2em] ml-1">
                Visibility
              </label>
              
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => setIsPrivate(false)}
                  className={`p-4 rounded-2xl border flex flex-col items-center gap-3 transition-all ${
                    !isPrivate
                      ? "bg-purple-600/20 border-purple-500/50 text-white"
                      : "bg-white/5 border-white/10 text-gray-500 hover:bg-white/10"
                  }`}
                >
                  <Globe size={24} />
                  <span className="font-bold text-sm">Public</span>
                </button>

                <button
                  onClick={() => setIsPrivate(true)}
                  className={`p-4 rounded-2xl border flex flex-col items-center gap-3 transition-all ${
                    isPrivate
                      ? "bg-purple-600/20 border-purple-500/50 text-white"
                      : "bg-white/5 border-white/10 text-gray-500 hover:bg-white/10"
                  }`}
                >
                  <Lock size={24} />
                  <span className="font-bold text-sm">Private</span>
                </button>
              </div>
              
              <p className="text-xs text-gray-500 text-center leading-relaxed px-4">
                {isPrivate 
                  ? "Private rooms require an access code to join."
                  : "Public rooms can be joined by quick join."
                }
              </p>
            </div>

            <div className="mt-auto">
              <button
                onClick={() => setShowSettings(false)}
                className="w-full py-4 bg-white/10 hover:bg-white/20 rounded-xl font-bold transition-all text-sm"
              >
                Save Changes
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <button
        onClick={onCreate}
        className="mt-5 w-full py-6 bg-purple-600/60 hover:bg-purple-500 rounded-[1.5rem] font-black transition-all uppercase tracking-widest text-m"
      >
        Create Lobby
      </button>
    </div>
  );
}
