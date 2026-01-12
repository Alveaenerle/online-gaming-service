import { BOARD_CELLS, Color } from "./constants";
import { BoardCell } from "./BoardCell";
import { Pawn } from "./Pawn";
import { LudoPlayer } from "../types";

interface LudoBoardProps {
  players: LudoPlayer[];
  currentPlayerId: string;
  diceValue: number | null;
  waitingForMove: boolean;
  onPawnMoveComplete: (pawnId: number) => void;
  onPawnClick: (pawnId: number) => void;
  loggedPlayerId: string;
}

export function LudoBoard({
  players,
  currentPlayerId,
  diceValue,
  waitingForMove,
  onPawnMoveComplete,
  onPawnClick,
  loggedPlayerId,
}: LudoBoardProps) {
  return (
    <div className="relative p-4 bg-[#0a0a0f] rounded-[45px] border-[3px] border-purple-500/50 shadow-[0_0_40px_rgba(168,85,247,0.2)]">
      <div className="grid grid-cols-13 grid-rows-13 gap-0.5 w-[600px] h-[600px] relative">
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

        {players.map((player) =>
          player.pawns.map((pawn) => {
            const isPlayerTurn = player.userId === currentPlayerId;
            const isOwnedByLoggedUser = player.userId === loggedPlayerId;
            console.log;
            const isInteractable =
              isPlayerTurn &&
              isOwnedByLoggedUser &&
              waitingForMove &&
              (diceValue ?? 0) > 0;

            return (
              <Pawn
                key={`${player.color}-${pawn.id}`}
                pawn={pawn}
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
