import React from "react";
import { AnimatePresence } from "framer-motion";
import { Loader2 } from "lucide-react";
import Navbar from "../../Shared/Navbar";
import { GameTable } from "./GameTable";
import { SidePanel } from "./SidePanel";
import { DemandModal, DrawnCardModal, GameFinishedModal } from "./Modals";
import { useMakaoGame } from "./useMakaoGame";

/**
 * MakaoGame - Main component for the Makao card game
 *
 * This component handles the complete Makao game experience for human players only.
 * It uses WebSocket for real-time game state updates and REST API for game actions.
 *
 * Game flow:
 * 1. Connect to WebSocket and subscribe to game updates
 * 2. Receive game state updates from backend
 * 3. Display game table with cards, players, and actions
 * 4. Handle card plays, draws, and special effects
 */
const MakaoGame: React.FC = () => {
  const {
    // State
    gameState,
    message,
    loading,
    isConnecting,
    drawnCard,
    drawnCardPlayable,
    demandModal,

    // Computed
    isMyTurn,
    user,

    // Actions
    handlePlayCard,
    handleDrawCard,
    handlePlayDrawnCard,
    handleSkipDrawnCard,
    handleAcceptEffect,
    handleLeaveGame,
    handleDemandSelect,
    closeDemandModal,
    getPlayerName,
  } = useMakaoGame();

  // Loading state
  if (isConnecting || !gameState) {
    return (
      <div className="min-h-screen bg-[#0a0a0f] text-white flex flex-col">
        <Navbar />
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <Loader2 className="animate-spin w-12 h-12 text-purple-500 mx-auto mb-4" />
            <p className="text-gray-400">Łączenie z grą...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0a0a0f] text-white antialiased overflow-hidden">
      <Navbar />

      {/* Background gradient */}
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)]" />

      <main className="pt-24 pb-4 px-4 h-[calc(100vh-96px)]">
        <div className="h-full max-w-[1800px] mx-auto flex gap-4">
          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center">
            <GameTable
              gameState={gameState}
              userId={user?.id}
              isMyTurn={isMyTurn}
              getPlayerName={getPlayerName}
              onPlayCard={handlePlayCard}
              onDrawCard={handleDrawCard}
              drawnCard={drawnCard}
            />
          </div>

          {/* Side Panel */}
          <SidePanel
            gameState={gameState}
            message={message}
            loading={loading}
            isMyTurn={isMyTurn}
            getPlayerName={getPlayerName}
            onLeaveGame={handleLeaveGame}
            onAcceptEffect={handleAcceptEffect}
          />
        </div>
      </main>

      {/* Modals */}
      <AnimatePresence>
        {drawnCard && (
          <DrawnCardModal
            card={drawnCard}
            isPlayable={drawnCardPlayable}
            onPlay={handlePlayDrawnCard}
            onSkip={handleSkipDrawnCard}
            loading={loading}
          />
        )}

        {demandModal && (
          <DemandModal
            type={demandModal.type}
            onSelect={handleDemandSelect}
            onClose={closeDemandModal}
          />
        )}

        {gameState.status === "FINISHED" && (
          <GameFinishedModal
            ranking={gameState.ranking}
            getPlayerName={getPlayerName}
            onLeave={handleLeaveGame}
          />
        )}
      </AnimatePresence>
    </div>
  );
};

export default MakaoGame;
