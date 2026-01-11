import { useEffect, useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";

import { GameLobbyLayout } from "../shared/GameLobbyLayout";
import { LobbyHeader } from "../shared/LobbyHeader";
import { LobbyActions } from "../shared/LobbyActions";
import { LobbyPlayersSection } from "../shared/LobbyPlayersSection";
import { AvatarPicker } from "../shared/AvatarPicker";
import { SocialCenter } from "../../Shared/SocialCenter";

import { lobbyService } from "../../../services/lobbyService";
import { useAuth } from "../../../context/AuthContext";
import { useLobby } from "../../../context/LobbyContext";
import { mapLobbyRawToLobby } from "../utils/lobbyMapper";
import { useSocial } from "../../../context/SocialContext";

export function MakaoLobby() {
  const { user } = useAuth();
  const { currentLobby, clearLobby, refreshLobbyStatus, setCurrentLobby } = useLobby();
  const { friends, sentRequests, pendingRequests, sendFriendRequest } = useSocial();
  const navigate = useNavigate();

  // Helper to check friend status
  const isFriend = (userId: string) => friends.some(f => f.id === userId); 
  const isInvited = (userId: string) => sentRequests.some(r => r.addresseeId === userId);
  // Also check if there's a pending request FROM this user TO me
  const hasReceivedRequest = (userId: string) => pendingRequests.some(r => r.requesterId === userId);
  const canSendRequests = !user?.isGuest;

  const [avatarSelectFor, setAvatarSelectFor] = useState<string | null>(null);

  // Map the raw lobby from context
  const lobby = useMemo(() => {
    if (!currentLobby) return null;
    return mapLobbyRawToLobby(currentLobby, user?.id);
  }, [currentLobby, user?.id]);

  // Initial check
  useEffect(() => {
    if (!currentLobby) {
      refreshLobbyStatus().catch(() => navigate("/home"));
    }
  }, [currentLobby, refreshLobbyStatus, navigate]);

  // Navigation effects based on lobby state
  useEffect(() => {
    if (currentLobby?.status === "PLAYING") {
      navigate("/makao/game");
    }
  }, [currentLobby?.status, navigate]);

  if (!lobby) return <div className="text-white p-10">Loading lobby...</div>;

  const you = lobby.players.find((p) => p.isYou);
  const isHost = !!you?.isHost;
  const canStart =
    lobby.players.length >= 2 && lobby.players.every((p) => p.isReady);

  const handleAvatarChange = async (avatar: string) => {
    try {
      // Extract avatar filename from path (e.g., "/avatars/avatar_1.png" -> "avatar_1.png")
      const avatarId = avatar.split("/").pop() || avatar;
      const updated = await lobbyService.updateAvatar(avatarId);
      setCurrentLobby(updated);
      setAvatarSelectFor(null);
    } catch (err) {
      console.error("Failed to change avatar:", err);
      alert("Failed to change avatar. Please try again.");
    }
  };

  const handleToggleReady = async () => {
    try {
      const updated = await lobbyService.toggleReady();
      setCurrentLobby(updated);
    } catch (err) {
      console.error("Failed to toggle ready:", err);
    }
  };

  const handleLeave = async () => {
    try {
      await lobbyService.leaveRoom();
      // Context will handle socket unsubscribe via clearLobby
      clearLobby();
      navigate("/home");
    } catch (err) {
      console.error("Failed to leave room:", err);
      // Even if API fails (e.g. network), we should clear local state
      clearLobby();
      navigate("/home");
    }
  };

  return (
    <GameLobbyLayout>
      <LobbyHeader
        title={"Makao Lobby"}
        accessCode={lobby.accessCode}
        isPrivate={lobby.isPrivate}
      />

      <LobbyPlayersSection
        players={lobby.players}
        maxPlayers={lobby.maxPlayers}
        onAvatarSelect={setAvatarSelectFor}
        onToggleReady={handleToggleReady}
        isHost={isHost}
        onAddFriend={sendFriendRequest}
        isFriend={isFriend}
        isInvited={isInvited}
        hasReceivedRequest={hasReceivedRequest}
        canSendFriendRequest={canSendRequests}
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
