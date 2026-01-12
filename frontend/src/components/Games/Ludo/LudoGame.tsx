import { useState } from "react";
import { motion } from "framer-motion";
import { GameNotification, NotificationType } from "./GameNotification";
import { SidebarHeader } from "./SidebarHeader";
import { ChatSection } from "./ChatSection";
import { LudoBoard } from "./Board/LudoBoard";
import { PlayerCard } from "./PlayerCard";
import { DicePopup } from "./DicePopUp";
import { SidebarFooter } from "./SidebarFooter";

// Importujemy hooki i typy
import { useLudo } from "../../../context/LudoGameContext";
import { useAuth } from "../../../context/AuthContext";

export function LudoArenaPage() {
  // 1. Wyciągamy dane i akcje z LudoContext
  const { gameState, rollDice, movePawn, isMyTurn, isLoading } = useLudo();

  // 2. Wyciągamy zalogowanego użytkownika
  const { user } = useAuth();

  const [diceOpen, setDiceOpen] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [showMessage, setShowMessage] = useState(true);

  const [notification, setNotification] = useState(
    "System online. Neural link established."
  );
  const [notificationType, setNotificationType] =
    useState<NotificationType>("INFO");

  // Jeśli gra się jeszcze nie załadowała
  if (!gameState) {
    return (
      <div className="min-h-screen bg-[#050508] flex items-center justify-center text-purple-500 font-black tracking-tighter">
        INITIALIZING NEURAL LINK...
      </div>
    );
  }

  // --- LOGIKA RZUTU ---
  const initiateRoll = async () => {
    setNotification("Calculating trajectory... Requesting server seed.");
    setNotificationType("ROLLING");
    setShowMessage(true);

    // Otwieramy popup - on sam wywoła rollDice() w środku lub zrobimy to tutaj:
    setDiceOpen(true);
  };

  const handleRollComplete = async (value: number) => {
    // Uwaga: W wersji z contextem, rollDice() wyśle request,
    // a serwer zwróci nowy gameState przez WebSocket, co zamknie flow.
    setNotification(`Dice Protocol: Value ${value} confirmed.`);
    setNotificationType("ROLLED");
  };

  // --- LOGIKA KLIKNIĘCIA W PIONEK ---
  const handlePawnClick = async (pawnId: number) => {
    if (!isMyTurn || !gameState.waitingForMove) return;

    setNotification(`Command sent: Unit moving to sector...`);
    setNotificationType("MOVING");

    // Wywołujemy akcję z kontekstu (wysyła request do API)
    await movePawn(pawnId);
  };

  const handlePawnMoveFinished = (pawnId: number) => {
    // Animacja na froncie się skończyła.
    // Stan na serwerze prawdopodobnie już się zaktualizował przez WebSocket.
    console.log(`Unit ${pawnId} position synchronized.`);
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
              {/* Karty graczy z rzeczywistymi danymi */}
              {gameState.players.map((player, idx) => {
                const sides = [
                  "top-left",
                  "top-right",
                  "bottom-right",
                  "bottom-left",
                ] as const;
                return (
                  <PlayerCard
                    key={player.userId}
                    player={{
                      ...player,
                      username:
                        gameState.usernames[player.userId] || "Unknown Unit",
                      isTurn: player.userId === gameState.currentPlayerId,
                      avatar: "/avatars/avatar_1.png",
                    }}
                    side={sides[idx]}
                  />
                );
              })}

              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="relative z-10"
              >
                <LudoBoard
                  players={gameState.players}
                  currentPlayerId={gameState.currentPlayerId}
                  diceValue={gameState.lastDiceRoll}
                  waitingForMove={gameState.waitingForMove}
                  onPawnMoveComplete={handlePawnMoveFinished}
                  onPawnClick={handlePawnClick}
                />
              </motion.div>
            </div>
          </div>
        </main>

        <aside className="w-[420px] border-l border-white/5 bg-black/40 flex flex-col relative z-20 backdrop-blur-xl">
          <SidebarHeader />
          <ChatSection message={chatMessage} onMessageChange={setChatMessage} />

          {/* Stopka reaguje na to, czy jest Twoja tura */}
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
