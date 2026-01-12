import { useState } from "react";
import { motion } from "framer-motion";
import { GameNotification, NotificationType } from "./GameNotification";
import { SidebarHeader } from "./SidebarHeader";
import { ChatSection } from "./ChatSection";
import { LudoBoard } from "./Board/LudoBoard";
import { Color } from "./Board/constants";
import { PlayerCard } from "./PlayerCard";
import { DicePopup } from "./DicePopUp";
import { SidebarFooter } from "./SidebarFooter";

export function LudoArenaPage() {
  const [redPos, setRedPos] = useState(-1);
  const [diceOpen, setDiceOpen] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [showMessage, setShowMessage] = useState(true);

  const [notification, setNotification] = useState(
    "System online. Neural link established."
  );
  const [notificationType, setNotificationType] =
    useState<NotificationType>("INFO");
  const [lastDiceValue, setLastDiceValue] = useState<number | null>(null);

  const mockPlayers = [
    {
      userId: "1",
      username: "Commander RED",
      color: "RED" as Color,
      isTurn: true,
      pawns: [
        { position: redPos },
        { position: -1 },
        { position: -1 },
        { position: -1 },
      ],
      avatar: "/avatars/avatar_1.png",
      isHost: true,
    },
    {
      userId: "2",
      username: "Unit BLUE",
      color: "BLUE" as Color,
      isTurn: false,
      pawns: [
        { position: -1 },
        { position: -1 },
        { position: -1 },
        { position: -1 },
      ],
      avatar: "/avatars/avatar_1.png",
    },
    {
      userId: "3",
      username: "Unit YELLOW",
      color: "YELLOW" as Color,
      isTurn: false,
      pawns: [
        { position: -1 },
        { position: -1 },
        { position: -1 },
        { position: -1 },
      ],
      avatar: "/avatars/avatar_1.png",
    },
    {
      userId: "4",
      username: "Unit GREEN",
      color: "GREEN" as Color,
      isTurn: false,
      pawns: [
        { position: -1 },
        { position: -1 },
        { position: -1 },
        { position: -1 },
      ],
      avatar: "/avatars/avatar_1.png",
    },
  ];

  const initiateRoll = () => {
    setNotification("Calculating trajectory... Rolling dice.");
    setNotificationType("ROLLING");
    setShowMessage(true);
    setDiceOpen(true);
  };

  const handleRollComplete = (value: number) => {
    setLastDiceValue(value);
    setNotification(
      `Dice Protocol: Value ${value} confirmed. Proceed to move.`
    );
    setNotificationType("ROLLED");
    setShowMessage(true);
  };

  const handlePawnClick = (color: Color, index: number) => {
    if (color === "RED" && index === 0) {
      setRedPos((prev) => (prev === -1 ? 0 : (prev + 1) % 51));
      setNotification(
        `Tactical Alert: Unit RED advanced to sector ${redPos + 1}.`
      );
      setNotificationType("MOVING");
      setShowMessage(true);
    }
  };

  return (
    <div className="min-h-screen bg-[#050508] flex items-center justify-center p-4 md:p-8 font-sans overflow-hidden">
      <div className="relative flex w-full max-w-[98%] h-[900px] bg-[#0d0c14] border border-white/10 rounded-[60px] overflow-hidden shadow-[0_0_100px_rgba(0,0,0,1)]">
        <main className="flex-1 relative flex flex-col bg-[radial-gradient(circle_at_center,_#121018_0%,_transparent_150%)]">
          <GameNotification
            message={notification}
            type={notificationType}
            isVisible={showMessage}
            onClose={() => setShowMessage(false)}
          />

          <div className="flex-1 relative flex items-center justify-center p-8">
            <div className="absolute inset-20 border border-white/[0.02] rounded-[40px] pointer-events-none" />

            <div className="relative pl-64 pr-64">
              <PlayerCard player={mockPlayers[0]} side="top-left" />
              <PlayerCard player={mockPlayers[1]} side="top-right" />
              <PlayerCard player={mockPlayers[2]} side="bottom-right" />
              <PlayerCard player={mockPlayers[3]} side="bottom-left" />

              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="relative z-10"
              >
                <LudoBoard
                  players={mockPlayers}
                  onPawnClick={handlePawnClick}
                />
              </motion.div>
            </div>
          </div>
        </main>

        <aside className="w-[420px] border-l border-white/5 bg-black/40 flex flex-col relative z-20 backdrop-blur-xl">
          <SidebarHeader />
          <ChatSection message={chatMessage} onMessageChange={setChatMessage} />

          <SidebarFooter onDiceRoll={initiateRoll} />
        </aside>
      </div>

      <DicePopup
        isOpen={diceOpen}
        onClose={() => setDiceOpen(false)}
        onRollComplete={handleRollComplete}
      />
    </div>
  );
}
