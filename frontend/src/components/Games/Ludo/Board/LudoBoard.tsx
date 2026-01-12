import { BOARD_CELLS, Color } from "./constants";
import { BoardCell } from "./BoardCell";
import { Pawn } from "./Pawn";
import { LudoPlayer } from "../types"; // Importujemy Twoje typy

interface LudoBoardProps {
  players: LudoPlayer[]; // Używamy konkretnego typu zamiast any[]
  currentPlayerId: string; // ID gracza, który teraz wykonuje ruch
  diceValue: number | null;
  waitingForMove: boolean; // Flaga z LudoGameStateMessage
  onPawnMoveComplete: (pawnId: number) => void;
  onPawnClick: (pawnId: number) => void;
}

export function LudoBoard({
  players,
  currentPlayerId,
  diceValue,
  waitingForMove,
  onPawnMoveComplete,
  onPawnClick,
}: LudoBoardProps) {
  return (
    <div className="relative p-4 bg-[#0a0a0f] rounded-[45px] border-[3px] border-purple-500/50 shadow-[0_0_40px_rgba(168,85,247,0.2)]">
      <div className="grid grid-cols-13 grid-rows-13 gap-0.5 w-[600px] h-[600px] relative">
        {/* Renderowanie komórek planszy */}
        {BOARD_CELLS.map((cell) => (
          <div
            key={cell.id}
            className="w-full h-full"
            style={{
              gridRow: `span ${cell.span || 1} / span ${cell.span || 1}`,
              gridColumn: `span ${cell.span || 1} / span ${cell.span || 1}`,
              gridRowStart: cell.row,
              gridColumnStart: cell.col,
            }}
          >
            <BoardCell type={cell.type} color={cell.color} />
          </div>
        ))}

        {/* Renderowanie pionków wszystkich graczy */}
        {players.map((player) =>
          player.pawns.map((pawn) => {
            // Logika interaktywności:
            // 1. To musi być tura tego gracza (currentPlayerId)
            // 2. Serwer musi czekać na ruch (waitingForMove)
            // 3. Kostka musi mieć wartość > 0
            const isPlayerTurn = player.userId === currentPlayerId;
            const isInteractable =
              isPlayerTurn && waitingForMove && (diceValue ?? 0) > 0;

            return (
              <Pawn
                key={`${player.color}-${pawn.id}`}
                pawn={pawn} // Przekazujemy cały obiekt LudoPawn
                isInteractable={isInteractable}
                diceValue={diceValue ?? 0}
                onMoveComplete={onPawnMoveComplete}
                onClick={onPawnClick}
              />
            );
          })
        )}
      </div>
    </div>
  );
}
