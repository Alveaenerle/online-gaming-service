import { LogOut } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { lobbyService } from "../../../services/lobbyService";
import { useLobby } from "../../../context/LobbyContext";

export function SidebarFooter() {
  const { clearLobby, currentLobby } = useLobby();
  const navigate = useNavigate();
  const [isLeaving, setIsLeaving] = useState(false);

  const isPlaying = currentLobby?.status === "PLAYING";

  const handleLeave = async () => {
    if (isLeaving) return;

    const confirmMessage = isPlaying
      ? "Are you sure you want to leave the game? You may lose your progress."
      : "Are you sure you want to leave the lobby?";

    if (!window.confirm(confirmMessage)) return;

    setIsLeaving(true);
    try {
      await lobbyService.leaveRoom();
      clearLobby();
      navigate("/home");
    } catch (err) {
      console.error("Failed to leave:", err);
      clearLobby();
      navigate("/home");
    } finally {
      setIsLeaving(false);
    }
  };

  return (
    <div className="p-8 border-t border-white/5 bg-black/20">
      <button
        onClick={handleLeave}
        disabled={isLeaving}
        className="flex items-center justify-center gap-3 w-full py-4 text-red-500/60 hover:text-red-500 transition-all text-[9px] font-black uppercase tracking-[0.3em] group disabled:opacity-50"
      >
        <LogOut
          size={14}
          className="group-hover:-translate-x-1 transition-transform"
        />
        {isLeaving ? "..." : "Leave Game"}
      </button>
    </div>
  );
}
