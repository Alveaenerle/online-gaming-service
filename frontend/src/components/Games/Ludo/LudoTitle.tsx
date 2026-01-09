import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { PlayCircle, Trophy, Dices, Swords } from "lucide-react";

import Navbar from "../../Shared/Navbar";
import { GameHeader } from "../shared/GameHeader";
import { GameHeroPanel } from "../shared/GameHeroPanel";
import { CreateLobbyPanel } from "../shared/CreateLobbyPanel";
import { JoinLobbyPanel } from "../shared/JoinLobbyPanel";
import { lobbyService } from "../../../services/lobbyService";
import { BackgroundGradient } from "../../Shared/BackgroundGradient";
import { FriendsSidebar } from "../../Shared/FriendsSidebar";

export function LudoTitle() {
  const navigate = useNavigate();
  const [playerCount, setPlayerCount] = useState(4);
  const [roomName, setRoomName] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [friendsOpen, setFriendsOpen] = useState(false);

  const handleCreateLobby = async () => {
    if (!roomName.trim()) return alert("Please enter a room name");
    try {
      await lobbyService.createRoom("LUDO", playerCount, roomName.trim());
      navigate("/lobby/ludo");
    } catch (err: any) {
      alert(err.message || "Failed to create room");
    }
  };

  const handleJoinLobby = async (isRandom = false) => {
    if (!isRandom && !roomCode.trim())
      return alert("Please enter an access code");
    try {
      await lobbyService.joinRoom(
        isRandom ? "" : roomCode,
        isRandom ? playerCount : undefined,
        "LUDO",
        isRandom
      );
      navigate("/lobby/ludo");
    } catch (err: any) {
      alert(err.message || "Room not found");
    }
  };

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased overflow-hidden font-sans">
      <Navbar />

      <BackgroundGradient />

      <main className="relative pt-24 pb-12 px-6 max-w-[1600px] mx-auto h-[calc(100vh-100px)] flex flex-col">
        <GameHeader
          title="Ludo"
          subtitle="Arena"
          onBack={() => navigate("/home")}
          onSocialClick={() => setFriendsOpen(true)}
        />

        <div className="flex-1 grid lg:grid-cols-12 gap-8 min-h-0">
          <GameHeroPanel
            title="Roll the dice."
            description="The classic board game reimagined. Race your tokens from start to finish, capture your opponents, and secure your place in the home triangle."
            image="/game-previews/ludo-bg.jpg"
            rules={[
              {
                icon: <Dices size={22} />,
                title: "Enter the Race",
                desc: "Roll a six to move a token out of the base and onto the starting square.",
                color: "blue",
              },
              {
                icon: <Swords size={22} />,
                title: "Capture",
                desc: "Land on an opponent's token to send them back to their base!",
                color: "purple",
              },
              {
                icon: <Trophy size={22} />,
                title: "Victory",
                desc: "Be the first to get all four of your tokens into the home pocket to win.",
                color: "yellow",
              },
            ]}
          />

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="lg:col-span-4 flex flex-col gap-6 max-h-[calc(100vh-200px)]"
          >
            <CreateLobbyPanel
              roomName={roomName}
              setRoomName={setRoomName}
              playerCount={playerCount}
              setPlayerCount={setPlayerCount}
              onCreate={handleCreateLobby}
            />

            <JoinLobbyPanel
              roomCode={roomCode}
              setRoomCode={setRoomCode}
              onJoin={handleJoinLobby}
            />
          </motion.div>
          <FriendsSidebar
            isOpen={friendsOpen}
            onClose={() => setFriendsOpen(false)}
          />
        </div>
      </main>
    </div>
  );
}
