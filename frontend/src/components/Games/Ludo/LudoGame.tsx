import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { GameNotification } from "./GameNotification";
import { SidebarHeader } from "./SidebarHeader";
import { ChatSection } from "./ChatSection";
import { LudoBoard } from "./Board/LudoBoard";
import { PlayerCard } from "./PlayerCard";
import { DicePopup } from "./DicePopUp";
import { SidebarFooter } from "./SidebarFooter";

import { useLudo } from "../../../context/LudoGameContext";
import { useAuth } from "../../../context/AuthContext";

export function LudoArenaPage() {
  const {
    gameState,
    rollDice,
    movePawn,
    isMyTurn,
    isRolling,
    notification,
    notificationType,
  } = useLudo();

  const { user } = useAuth();

  const [diceOpen, setDiceOpen] = useState(false);
  const [chatMessage, setChatMessage] = useState("");
  const [showMessage, setShowMessage] = useState(true);

  useEffect(() => {
    if (!gameState || !user) return;

    const shouldShowDice =
      isMyTurn && gameState.rollsLeft > 0 && !gameState.waitingForMove;

    if (shouldShowDice && !diceOpen) {
      console.log("[Arena] Auto-opening dice protocol...");
      setDiceOpen(true);
    }

    if (!shouldShowDice && diceOpen && !isRolling) {
      const timer = setTimeout(() => {
        setDiceOpen(false);
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [
    isMyTurn,
    gameState?.rollsLeft,
    gameState?.waitingForMove,
    diceOpen,
    isRolling,
    user,
  ]);

  useEffect(() => {
    if (notification) {
      setShowMessage(true);
    }
  }, [notification]);

  if (!gameState) {
    return (
      <div className="min-h-screen bg-[#050508] flex items-center justify-center text-purple-500 font-black tracking-tighter">
        INITIALIZING NEURAL LINK...
      </div>
    );
  }

  const handlePawnClick = async (pawnId: number) => {
    await movePawn(pawnId);
  };

  const handlePawnMoveFinished = (pawnId: number) => {
    console.log(`Unit ${pawnId} synchronization complete.`);
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
                      avatar: `/avatars/avatar_${(idx % 4) + 1}.png`,
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
                  loggedPlayerId={user ? user.id : ""}
                />
              </motion.div>
            </div>
          </div>
        </main>

        <aside className="w-[420px] border-l border-white/5 bg-black/40 flex flex-col relative z-20 backdrop-blur-xl">
          <SidebarHeader />
          <ChatSection message={chatMessage} onMessageChange={setChatMessage} />

          <SidebarFooter />
        </aside>
      </div>

      <DicePopup isOpen={diceOpen} onClose={() => setDiceOpen(false)} />
    </div>
  );
}
