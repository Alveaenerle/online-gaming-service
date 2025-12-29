import { useState } from "react";
import { GameLobbyLayout } from "../shared/GameLobbyLayout";
import { LobbyHeader } from "../shared/LobbyHeader";
import { LobbyActions } from "../shared/LobbyActions";
import { LobbyPlayersSection } from "../shared/LobbyPlayersSection";
import { AvatarPicker } from "../shared/AvatarPicker";
import { useAuth } from "../../../context/AuthContext";
import { FriendsSidebar } from "../../Shared/FriendsSidebar";
import { motion } from "framer-motion";
import { Users } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function MakaoLobby() {
  const { user } = useAuth();
  const [friendsOpen, setFriendsOpen] = useState(false);
  const navigate = useNavigate();

  const [players, setPlayers] = useState([
    {
      id: "1",
      username: user?.username ?? "You",
      avatar: "/avatars/avatar_1.png",
      isHost: true,
      isReady: false,
      isYou: true,
    },
    {
      id: "2",
      username: "Bob",
      avatar: "/avatars/avatar_2.png",
      isReady: true,
      isYou: false,
    },
  ]);

  const [avatarSelectFor, setAvatarSelectFor] = useState<string | null>(null);

  const youPlayer = players.find((p) => p.isYou === true);

  const isHost = !!youPlayer?.isHost;

  function handleAvatarSelect(avatar: string) {
    setPlayers((prev) =>
      prev.map((p) => (p.id === avatarSelectFor ? { ...p, avatar } : p))
    );
  }

  function toggleReady() {
    setPlayers((prev) =>
      prev.map((p) =>
        p.id === youPlayer?.id ? { ...p, isReady: !p.isReady } : p
      )
    );
  }

  return (
    <GameLobbyLayout>
      <LobbyHeader
        title="Makao"
        subtitle="Connect with other players and start playing"
      />

      <LobbyPlayersSection
        players={players}
        maxPlayers={8}
        onAvatarSelect={setAvatarSelectFor}
        onToggleReady={toggleReady}
      />

      <LobbyActions
        isHost={isHost}
        canStart={players.length >= 2 && players.every((p) => p.isReady)}
        onStart={() => console.log("START GAME")}
        onLeave={() => navigate("/makao")}
      />

      {avatarSelectFor && (
        <AvatarPicker
          onSelect={handleAvatarSelect}
          onClose={() => setAvatarSelectFor(null)}
        />
      )}
      <motion.button
        onClick={() => setFriendsOpen(true)}
        className="fixed bottom-10 right-10 z-30
                   flex items-center gap-3
                   rounded-2xl bg-purple-600 px-5 py-3
                   font-semibold shadow-lg shadow-purple-600/40
                   hover:bg-purple-500 transition"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
      >
        <Users />
        Friends
      </motion.button>

      <FriendsSidebar
        isOpen={friendsOpen}
        onClose={() => setFriendsOpen(false)}
      />
    </GameLobbyLayout>
  );
}
