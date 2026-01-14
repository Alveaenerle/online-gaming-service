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
  hideHeader?: boolean;
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
  hideHeader = false,
}: CreateProps) {
  const [showSettings, setShowSettings] = useState(false);

  return (
    <div className="w-full bg-[#121018] rounded-2xl sm:rounded-[2rem] lg:rounded-[2.5rem] border border-white/5 p-4 sm:p-6 lg:p-8 flex flex-col shadow-2xl relative overflow-hidden">
      {!hideHeader && (
        <div className="flex items-center justify-between mb-3 sm:mb-5">
          <div className="flex items-center gap-2 sm:gap-4">
            <div className="w-10 h-10 sm:w-12 sm:h-12 rounded-xl sm:rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center flex-shrink-0">
              <PlusCircle size={22} className="sm:hidden" />
              <PlusCircle size={26} className="hidden sm:block" />
            </div>
            <h2 className="text-lg sm:text-xl lg:text-2xl font-bold">New Lobby</h2>
          </div>
          <button
            onClick={() => setShowSettings(true)}
            className="p-2 sm:p-3 rounded-lg sm:rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            <Settings size={18} className="sm:hidden" />
            <Settings size={20} className="hidden sm:block" />
          </button>
        </div>
      )}
      {hideHeader && (
        <div className="flex items-center justify-end mb-3">
          <button
            onClick={() => setShowSettings(true)}
            className="p-2 rounded-lg bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors min-h-[40px] min-w-[40px] flex items-center justify-center"
          >
            <Settings size={18} />
          </button>
        </div>
      )}

      <div className="space-y-2 sm:space-y-3 flex-1">
        <div className="space-y-2 sm:space-y-3">
          <label className="text-[10px] font-black text-gray-500 uppercase tracking-[0.2em] ml-1">
            Room Identity
          </label>
          <input
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
            className="w-full bg-white/5 border border-white/10 rounded-xl sm:rounded-2xl px-4 sm:px-6 py-3 sm:py-4 lg:py-5 focus:ring-2 focus:ring-purple-500 outline-none transition-all placeholder:text-gray-700 text-sm sm:text-base min-h-[44px]"
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
            className="absolute inset-0 bg-[#121018]/95 backdrop-blur-md z-10 flex flex-col p-4 sm:p-6 lg:p-8"
          >
            <div className="flex items-center justify-between mb-4 sm:mb-8">
              <h3 className="text-base sm:text-xl font-bold flex items-center gap-2 sm:gap-3">
                <Settings className="text-purple-500" size={18} />
                Lobby Settings
              </h3>
              <button
                onClick={() => setShowSettings(false)}
                className="p-2 rounded-lg hover:bg-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <X size={20} />
              </button>
            </div>

            <div className="space-y-3 sm:space-y-4">
              <label className="text-[10px] font-black text-gray-500 uppercase tracking-[0.2em] ml-1">
                Visibility
              </label>
              
              <div className="grid grid-cols-2 gap-2 sm:gap-3">
                <button
                  onClick={() => setIsPrivate(false)}
                  className={`p-3 sm:p-4 rounded-xl sm:rounded-2xl border flex flex-col items-center gap-2 sm:gap-3 transition-all min-h-[80px] sm:min-h-[100px] ${
                    !isPrivate
                      ? "bg-purple-600/20 border-purple-500/50 text-white"
                      : "bg-white/5 border-white/10 text-gray-500 hover:bg-white/10"
                  }`}
                >
                  <Globe size={20} className="sm:hidden" />
                  <Globe size={24} className="hidden sm:block" />
                  <span className="font-bold text-xs sm:text-sm">Public</span>
                </button>

                <button
                  onClick={() => setIsPrivate(true)}
                  className={`p-3 sm:p-4 rounded-xl sm:rounded-2xl border flex flex-col items-center gap-2 sm:gap-3 transition-all min-h-[80px] sm:min-h-[100px] ${
                    isPrivate
                      ? "bg-purple-600/20 border-purple-500/50 text-white"
                      : "bg-white/5 border-white/10 text-gray-500 hover:bg-white/10"
                  }`}
                >
                  <Lock size={20} className="sm:hidden" />
                  <Lock size={24} className="hidden sm:block" />
                  <span className="font-bold text-xs sm:text-sm">Private</span>
                </button>
              </div>
              
              <p className="text-[10px] sm:text-xs text-gray-500 text-center leading-relaxed px-2 sm:px-4">
                {isPrivate 
                  ? "Private rooms require an access code to join."
                  : "Public rooms can be joined by quick join."
                }
              </p>
            </div>

            <div className="mt-auto pt-4">
              <button
                onClick={() => setShowSettings(false)}
                className="w-full py-3 sm:py-4 bg-white/10 hover:bg-white/20 rounded-lg sm:rounded-xl font-bold transition-all text-xs sm:text-sm min-h-[44px]"
              >
                Save Changes
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <button
        onClick={onCreate}
        className="mt-4 sm:mt-5 w-full py-4 sm:py-5 lg:py-6 bg-purple-600/60 hover:bg-purple-500 rounded-xl sm:rounded-[1.25rem] lg:rounded-[1.5rem] font-black transition-all uppercase tracking-widest text-xs sm:text-sm min-h-[44px]"
      >
        Create Lobby
      </button>
    </div>
  );
}
