import { useEffect, useState, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Users } from "lucide-react";

import { GameLobbyLayout } from "../shared/GameLobbyLayout";
import { LobbyHeader } from "../shared/LobbyHeader";
import { LobbyActions } from "../shared/LobbyActions";
import { LobbyPlayersSection } from "../shared/LobbyPlayersSection";
import { AvatarPicker } from "../shared/AvatarPicker";
import { FriendsSidebar } from "../../Shared/FriendsSidebar";

import { lobbyService } from "../../../services/lobbyService";
import { useAuth } from "../../../context/AuthContext";
import { LobbyInfo, LobbyInfoRaw, LobbyPlayer } from "../shared/types";

import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

export function MakaoLobby() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [lobby, setLobby] = useState<LobbyInfo | null>(null);
  const [avatarSelectFor, setAvatarSelectFor] = useState<string | null>(null);
  const [friendsOpen, setFriendsOpen] = useState(false);

  const [stompClient, setStompClient] = useState<StompJs.Client | null>(null);
  const [connected, setConnected] = useState(false);

  const lobbyRef = useRef<LobbyInfo | null>(null);

  useEffect(() => {
    lobbyService
      .getRoomInfo()
      .then((raw: LobbyInfoRaw) => {
        const newLobby = mapLobbyRawToLobby(raw, user?.id);
        setLobby(newLobby);
        lobbyRef.current = newLobby;
        console.log("current lobby:", newLobby);
        console.log("Lobby loaded raw:", raw);
      })
      .catch((err) => {
        console.error("Failed to load lobby:", err);
        navigate("/home");
      });
  }, [user?.id, navigate]);

  const connectStomp = useCallback((roomId: string) => {
    console.log("Connecting to room:", roomId);

    const socket = new SockJS("http://localhost/api/menu/ws");
    const client = StompJs.over(socket);

    client.debug = () => {};

    client.connect(
      {} as any,
      (frame?: StompJs.Frame) => {
        console.log("Connected:", frame);
        setConnected(true);

        if (roomId) {
          const topic = `/topic/room/${roomId}`;
          console.log("Subscribing to topic:", topic);

          client.subscribe(topic, (message: StompJs.Message) => {
            console.log("Message received:", message.body);

            try {
              const data = JSON.parse(message.body);
              console.log("Parsed data:", data);
              handleStompMessage(data);
            } catch (err) {
              console.error("Failed to parse message:", err);
            }
          });
        } else {
          console.warn("No room ID provided, connected but not subscribed.");
        }
      },
      (error: string | StompJs.Frame) => {
        console.error("STOMP connection error:", error);
        setConnected(false);
      }
    );

    setStompClient(client);
  }, []);

  const handleStompMessage = (data: any) => {
    console.log(
      "Handling STOMP message with current lobby ref:",
      lobbyRef.current
    );

    if (!data) return;

    console.log("Received message:", data);

    const updatedLobby = mapLobbyRawToLobby(data, user?.id);
    setLobby(updatedLobby);
    lobbyRef.current = updatedLobby;

    console.log("Lobby updated after message:", updatedLobby);
  };

  useEffect(() => {
    console.log("Lobby state inside useEffect:", lobby);

    if (lobby?.roomId) {
      console.log("Room ID available, attempting to connect...");
      if (!stompClient) {
        console.log("No stompClient found, connecting...");
        connectStomp(lobby.roomId);
      } else {
        console.log("Already connected, skipping connect.");
      }
    } else {
      console.warn("Lobby roomId not yet available.");
    }

    return () => {
      if (stompClient) {
        stompClient.disconnect(() => {
          setConnected(false);
          console.log("Disconnected from STOMP");
        });
      }
    };
  }, [lobby, connectStomp]);

  if (!lobby) return <div>Loading lobby...</div>;

  const players: LobbyPlayer[] = lobby.players;
  const you = players.find((p) => p.isYou);
  const isHost = !!you?.isHost;

  const handleToggleReady = async () => {
    if (!you || !stompClient) return;
    try {
      const updatedRawLobby = await lobbyService.toggleReady();
      const updatedLobby = mapLobbyRawToLobby(updatedRawLobby, user?.id);
      setLobby(updatedLobby);
      lobbyRef.current = updatedLobby;
    } catch (err) {
      console.error("Failed to toggle ready:", err);
    }
  };

  const handleAvatarSelect = async (avatar: string) => {
    if (!avatarSelectFor || !stompClient) return;
    try {
      //const updatedRawLobby = await lobbyService.changeAvatar(avatarSelectFor, avatar);
      //const updatedLobby = mapLobbyRawToLobby(updatedRawLobby, user?.id);
      //setLobby(updatedLobby);
      //lobbyRef.current = updatedLobby; // Update the ref with the new lobby data
      //setAvatarSelectFor(null);
      console.log("Avatar change (not implemented):", avatarSelectFor, avatar);
    } catch (err) {
      console.error("Failed to change avatar:", err);
    }
  };

  const handleStartGame = async () => {
    if (!isHost || !stompClient) return;
    try {
      const updatedRawLobby = await lobbyService.startGame();
      const updatedLobby = mapLobbyRawToLobby(updatedRawLobby, user?.id);
      setLobby(updatedLobby);
      lobbyRef.current = updatedLobby;
    } catch (err) {
      console.error("Failed to start game:", err);
    }
  };

  const handleLeave = async () => {
    try {
      await lobbyService.leaveRoom();
      stompClient?.disconnect(() => setConnected(false));
      navigate("/home");
    } catch (err) {
      console.error("Failed to leave room:", err);
    }
  };

  return (
    <GameLobbyLayout>
      <LobbyHeader
        title={`Makao - Access Code: ${lobby.accessCode}`}
        subtitle={connected ? "Connected" : "Disconnected"}
      />

      <LobbyPlayersSection
        players={players}
        maxPlayers={lobby.maxPlayers}
        onAvatarSelect={setAvatarSelectFor}
        onToggleReady={handleToggleReady}
      />

      <LobbyActions
        isHost={isHost}
        canStart={players.length >= 2 && players.every((p) => p.isReady)}
        onStart={handleStartGame}
        onLeave={handleLeave}
      />

      {avatarSelectFor && (
        <AvatarPicker
          onSelect={handleAvatarSelect}
          onClose={() => setAvatarSelectFor(null)}
        />
      )}

      <motion.button
        onClick={() => setFriendsOpen(true)}
        className="fixed bottom-10 right-10 z-30 flex items-center gap-3 rounded-2xl bg-purple-600 px-5 py-3 font-semibold shadow-lg shadow-purple-600/40 hover:bg-purple-500 transition"
        initial={{ opacity: 0, y: 40 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.4 }}
      >
        <Users /> Friends
      </motion.button>

      <FriendsSidebar
        isOpen={friendsOpen}
        onClose={() => setFriendsOpen(false)}
      />
    </GameLobbyLayout>
  );
}

function mapLobbyRawToLobby(
  raw: LobbyInfoRaw,
  currentUserId?: string
): LobbyInfo {
  return {
    roomId: raw.id,
    gameType: raw.gameType,
    maxPlayers: raw.maxPlayers,
    players: Object.entries(raw.players).map(([userId, username]) => ({
      userId,
      username,
      avatar: `/avatars/avatar_1.png`,
      isHost: userId === raw.hostUserId,
      isYou: userId === currentUserId,
      isReady: false,
    })),
    gameStarted: raw.status === "STARTED",
    name: raw.name,
    accessCode: raw.accessCode,
  };
}
