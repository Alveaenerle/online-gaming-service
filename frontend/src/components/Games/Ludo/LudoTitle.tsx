import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { Trophy, Dices, Swords } from "lucide-react";

import Navbar from "../../Shared/Navbar";
import { GameHeader } from "../shared/GameHeader";
import { GameHeroPanel } from "../shared/GameHeroPanel";
import { CreateLobbyPanel } from "../shared/CreateLobbyPanel";
import { JoinLobbyPanel } from "../shared/JoinLobbyPanel";
import { LobbyAccordion } from "../shared/LobbyAccordion";
import { lobbyService } from "../../../services/lobbyService";
import { BackgroundGradient } from "../../Shared/BackgroundGradient";
import { FriendsSidebar } from "../../Shared/FriendsSidebar";
import { useAuth } from "../../../context/AuthContext";
import { useToast } from "../../../context/ToastContext";

export function LudoTitle() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showToast } = useToast();
  const isGuest = user?.isGuest ?? false;
  const [playerCount, setPlayerCount] = useState(4);
  const [roomName, setRoomName] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
  const [friendsOpen, setFriendsOpen] = useState(false);

  const handleCreateLobby = async () => {
    if (!roomName.trim()) return showToast("Please enter a room name", "error");
    try {
      await lobbyService.createRoom("LUDO", playerCount, roomName.trim(), isPrivate);
      navigate("/lobby/ludo");
    } catch (err: any) {
      showToast(err.message || "Failed to create room", "error");
    }
  };

  const handleJoinLobby = async (isRandom = false) => {
    if (!isRandom && !roomCode.trim())
      return showToast("Please enter an access code", "error");
    try {
      await lobbyService.joinRoom(
        isRandom ? "" : roomCode,
        "LUDO",
        isRandom
      );
      navigate("/lobby/ludo");
    } catch (err: any) {
      showToast(err.message || "Room not found", "error");
    }
  };

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased overflow-x-hidden overflow-y-auto font-sans">
      <Navbar />

      <BackgroundGradient />

      <main className="relative pt-20 sm:pt-24 pb-8 sm:pb-12 px-3 sm:px-4 md:px-6 max-w-[1600px] mx-auto min-h-[calc(100vh-80px)] lg:h-[calc(100vh-100px)] flex flex-col overflow-x-hidden">
        <GameHeader
          title="Ludo"
          subtitle="Arena"
          onBack={() => navigate("/home")}
          onSocialClick={() => setFriendsOpen(true)}
        />

        <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-6 lg:gap-8 min-h-0 overflow-y-auto lg:overflow-visible">
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
            className="lg:col-span-4 flex flex-col h-full max-h-[calc(100vh-220px)] sm:max-h-[calc(100vh-200px)]"
          >
            {/* Mobile Layout - Show both panels stacked */}
            <div className="lg:hidden flex-1 flex flex-col">
              <div className="flex-none mb-4 sm:mb-6">
                <CreateLobbyPanel
                  roomName={roomName}
                  setRoomName={setRoomName}
                  playerCount={playerCount}
                  setPlayerCount={setPlayerCount}
                  isPrivate={isPrivate}
                  setIsPrivate={setIsPrivate}
                  onCreate={handleCreateLobby}
                  minPlayers={2}
                  maxPlayers={4}
                />
              </div>
              <div className="flex-1 overflow-y-auto custom-scrollbar min-h-0 pr-1 pb-4">
                <JoinLobbyPanel
                  roomCode={roomCode}
                  setRoomCode={setRoomCode}
                  onJoin={handleJoinLobby}
                />
              </div>
            </div>

            {/* Desktop Layout - Accordion style */}
            <div className="hidden lg:block overflow-y-auto custom-scrollbar pr-1 pb-4">
              <LobbyAccordion
                createContent={
                  <CreateLobbyPanel
                    roomName={roomName}
                    setRoomName={setRoomName}
                    playerCount={playerCount}
                    setPlayerCount={setPlayerCount}
                    isPrivate={isPrivate}
                    setIsPrivate={setIsPrivate}
                    onCreate={handleCreateLobby}
                    minPlayers={2}
                    maxPlayers={4}
                    hideHeader
                  />
                }
                joinContent={
                  <JoinLobbyPanel
                    roomCode={roomCode}
                    setRoomCode={setRoomCode}
                    onJoin={handleJoinLobby}
                    hideHeader
                  />
                }
              />
            </div>
          </motion.div>
          {!isGuest && (
            <FriendsSidebar
              isOpen={friendsOpen}
              onClose={() => setFriendsOpen(false)}
            />
          )}
        </div>
      </main>
    </div>
  );
}
