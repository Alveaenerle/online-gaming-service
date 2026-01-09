import { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";

import { GameLobbyLayout } from "../shared/GameLobbyLayout";
import { LobbyHeader } from "../shared/LobbyHeader";
import { LobbyActions } from "../shared/LobbyActions";
import { LobbyPlayersSection } from "../shared/LobbyPlayersSection";
import { AvatarPicker } from "../shared/AvatarPicker";
import { SocialCenter } from "../../Shared/SocialCenter";

import { lobbyService } from "../../../services/lobbyService";
import { socketService } from "../../../services/socketService";
import { useAuth } from "../../../context/AuthContext";
import { mapLobbyRawToLobby } from "../utils/lobbyMapper";
import { LobbyInfo } from "../utils/types";

export function LudoLobby() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isLeavingRef = useRef(false);

  const [lobby, setLobby] = useState<LobbyInfo | null>(null);
  const [avatarSelectFor, setAvatarSelectFor] = useState<string | null>(null);

  const handleLobbyUpdate = useCallback(
    (data: any) => {
      if (!data || isLeavingRef.current) return;

      if (data.players && !data.players[user?.id || ""]) {
        alert("You have been removed from the lobby.");
        navigate("/home");
        return;
      }

      if (data.status === "PLAYING") {
        navigate("/game/ludo");
        return;
      }

      setLobby(mapLobbyRawToLobby(data, user?.id));
    },
    [user?.id, navigate]
  );

  useEffect(() => {
    lobbyService
      .getRoomInfo()
      .then((raw) => {
        setLobby(mapLobbyRawToLobby(raw, user?.id));
      })
      .catch((err) => {
        console.error("Failed to load Ludo lobby info:", err);
        navigate("/home");
      });
  }, [user?.id, navigate]);

  useEffect(() => {
    if (!lobby?.roomId) return;

    const initSocket = async () => {
      try {
        await socketService.connect();
        socketService.subscribe(
          `/topic/room/${lobby.roomId}`,
          handleLobbyUpdate
        );
      } catch (err) {
        console.error("Socket connection failed:", err);
      }
    };

    initSocket();

    return () => {
      socketService.unsubscribe(`/topic/room/${lobby.roomId}`);
    };
  }, [lobby?.roomId, handleLobbyUpdate]);

  if (!lobby)
    return (
      <div className="min-h-screen bg-[#07060b] flex items-center justify-center text-white font-sans">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
          <p className="text-xl font-medium">Entering Ludo Arena...</p>
        </div>
      </div>
    );

  const you = lobby.players.find((p) => p.isYou);
  const isHost = !!you?.isHost;

  const canStart =
    lobby.players.length >= 2 &&
    lobby.players.length <= 4 &&
    lobby.players.every((p) => p.isReady);

  const handleAvatarChange = async (avatar: string) => {
    try {
      console.log("New Ludo avatar selected:", avatar);
      setAvatarSelectFor(null);
    } catch (err) {
      console.error("Failed to change avatar:", err);
    }
  };

  const handleLeave = async () => {
    try {
      isLeavingRef.current = true;
      await lobbyService.leaveRoom();
      socketService.disconnect();
      navigate("/home");
    } catch (err) {
      console.error("Failed to leave Ludo lobby:", err);
    }
  };

  return (
    <GameLobbyLayout>
      <LobbyHeader title={"Ludo Lobby"} accessCode={lobby.accessCode} />

      <LobbyPlayersSection
        players={lobby.players}
        maxPlayers={4}
        onAvatarSelect={setAvatarSelectFor}
        onToggleReady={() => lobbyService.toggleReady()}
        isHost={isHost}
      />

      <LobbyActions
        isHost={isHost}
        canStart={canStart}
        onStart={() => lobbyService.startGame()}
        onLeave={handleLeave}
      />

      <SocialCenter />

      {avatarSelectFor && (
        <AvatarPicker
          onSelect={handleAvatarChange}
          onClose={() => setAvatarSelectFor(null)}
        />
      )}
    </GameLobbyLayout>
  );
}
