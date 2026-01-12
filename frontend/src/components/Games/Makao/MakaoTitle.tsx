import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { useState, useEffect } from "react";
import { PlayCircle, Trophy, Sparkles, RefreshCw, LogOut } from "lucide-react";

import Navbar from "../../Shared/Navbar";
import { GameHeader } from "../shared/GameHeader";
import { GameHeroPanel } from "../shared/GameHeroPanel";
import { CreateLobbyPanel } from "../shared/CreateLobbyPanel";
import { JoinLobbyPanel } from "../shared/JoinLobbyPanel";
import { lobbyService } from "../../../services/lobbyService";
import makaoGameService from "../../../services/makaoGameService";
import { BackgroundGradient } from "../../Shared/BackgroundGradient";
import { FriendsSidebar } from "../../Shared/FriendsSidebar";
import { useAuth } from "../../../context/AuthContext";
import { useToast } from "../../../context/ToastContext";

export function MakaoTitle() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showToast } = useToast();
  const isGuest = user?.isGuest ?? false;
  const [playerCount, setPlayerCount] = useState(4);
  const [roomName, setRoomName] = useState("");
  const [roomCode, setRoomCode] = useState("");
  const [isPrivate, setIsPrivate] = useState(false);
  const [friendsOpen, setFriendsOpen] = useState(false);
  const [hasActiveGame, setHasActiveGame] = useState(false);
  const [isCheckingGame, setIsCheckingGame] = useState(true);

  // Check if user has an active game on mount
  useEffect(() => {
    const checkForActiveGame = async () => {
      setIsCheckingGame(true);
      try {
        const inGame = await makaoGameService.checkActiveGame();
        setHasActiveGame(inGame);
      } catch {
        setHasActiveGame(false);
      } finally {
        setIsCheckingGame(false);
      }
    };

    if (user?.id) {
      checkForActiveGame();
    } else {
      setIsCheckingGame(false);
    }
  }, [user?.id]);

  const handleReconnect = () => {
    navigate("/makao/game");
  };

  const handleLeaveActiveGame = async () => {
    try {
      await makaoGameService.leaveGame();
      setHasActiveGame(false);
      showToast("Left the active game. You can now create or join a new one.", "success");
    } catch (err: any) {
      showToast(err.message || "Failed to leave game", "error");
    }
  };

  const handleCreateLobby = async () => {
    if (hasActiveGame) {
      showToast("You must leave your active game first", "error");
      return;
    }
    if (!roomName.trim()) return showToast("Please enter a room name", "error");
    try {
      await lobbyService.createRoom("MAKAO", playerCount, roomName.trim(), isPrivate);
      navigate("/lobby/makao");
    } catch (err: any) {
      showToast(err.message || "Failed to create room", "error");
    }
  };

  const handleJoinLobby = async (isRandom = false) => {
    if (hasActiveGame) {
      showToast("You must leave your active game first", "error");
      return;
    }
    if (!isRandom && !roomCode.trim())
      return showToast("Please enter an access code", "error");
    try {
      await lobbyService.joinRoom(
        isRandom ? "" : roomCode,
        "MAKAO",
        isRandom
      );
      navigate("/lobby/makao");
    } catch (err: any) {
      showToast(err.message || "Room not found", "error");
    }
  };

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased overflow-hidden font-sans">
      <Navbar />

      <BackgroundGradient />

      <main className="relative pt-24 pb-12 px-6 max-w-[1600px] mx-auto h-[calc(100vh-100px)] flex flex-col">
        <GameHeader
          title="Makao"
          subtitle="Arena"
          onBack={() => navigate("/home")}
          onSocialClick={() => setFriendsOpen(true)}
        />

        <div className="flex-1 grid lg:grid-cols-12 gap-8 min-h-0">
          <GameHeroPanel
            title="Master the deck."
            description="The ultimate Makao experience. Use special cards to sabotage opponents and be the first to clear your hand."
            image="/game-previews/makao-bg.jpg"
            rules={[
              {
                icon: <PlayCircle size={22} />,
                title: "How to play",
                desc: "Match cards by rank or suit. Draw if you can't move.",
                color: "blue",
              },
              {
                icon: <Sparkles size={22} />,
                title: "Special Cards",
                desc: "Jacks demand ranks, Aces change colors, 2-3 force draws.",
                color: "purple",
              },
              {
                icon: <Trophy size={22} />,
                title: "The Goal",
                desc: "Clear your hand and shout MAKAO before your last card!",
                color: "yellow",
              },
            ]}
          />

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="lg:col-span-4 flex flex-col gap-6 max-h-[calc(100vh-200px)]"
          >
            {isCheckingGame ? (
              <div className="bg-[#121018]/80 backdrop-blur rounded-2xl p-6 border border-white/10 flex items-center justify-center">
                <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purpleEnd" />
              </div>
            ) : hasActiveGame ? (
              /* Active Game Panel - shown when user is in an ongoing game */
              <div className="bg-[#121018]/80 backdrop-blur rounded-2xl p-6 border border-amber-500/30 shadow-lg shadow-amber-500/10">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-full bg-amber-500/20 flex items-center justify-center">
                    <RefreshCw className="w-5 h-5 text-amber-400" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">Active Game Found</h3>
                    <p className="text-sm text-gray-400">You have an ongoing game</p>
                  </div>
                </div>

                <p className="text-gray-300 text-sm mb-6">
                  You're currently in an active Makao game. You can rejoin the game or leave it to start a new one.
                </p>

                <div className="flex flex-col gap-3">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleReconnect}
                    className="w-full py-3 rounded-xl bg-gradient-to-r from-purpleStart to-purpleEnd text-white font-bold shadow-lg shadow-purpleEnd/30 hover:shadow-purpleEnd/50 transition-shadow flex items-center justify-center gap-2"
                  >
                    <RefreshCw className="w-5 h-5" />
                    Rejoin Game
                  </motion.button>

                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleLeaveActiveGame}
                    className="w-full py-3 rounded-xl bg-red-500/20 hover:bg-red-500/30 text-red-400 font-medium border border-red-500/30 transition-colors flex items-center justify-center gap-2"
                  >
                    <LogOut className="w-5 h-5" />
                    Leave Game & Start New
                  </motion.button>
                </div>
              </div>
            ) : (
              /* Normal Create/Join panels */
              <>
                <CreateLobbyPanel
                  roomName={roomName}
                  setRoomName={setRoomName}
                  playerCount={playerCount}
                  setPlayerCount={setPlayerCount}
                  isPrivate={isPrivate}
                  setIsPrivate={setIsPrivate}
                  onCreate={handleCreateLobby}
                />

                <JoinLobbyPanel
                  roomCode={roomCode}
                  setRoomCode={setRoomCode}
                  onJoin={handleJoinLobby}
                />
              </>
            )}
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
