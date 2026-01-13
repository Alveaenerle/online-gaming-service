import { motion, AnimatePresence } from "framer-motion";
import { BOARD_CELLS } from "./constants";
import { BoardCell } from "./BoardCell";
import { Pawn } from "./Pawn";
import { LudoPlayer } from "../types";

interface LudoBoardProps {
  players: LudoPlayer[];
  usernames: Record<string, string>; // Dodajemy mapę nazw użytkowników
  currentPlayerId: string;
  diceValue: number | null;
  waitingForMove: boolean;
  onPawnMoveComplete: (pawnId: number) => void;
  onPawnClick: (pawnId: number) => void;
  loggedPlayerId: string;
  winnerId: string | null;
}

export function LudoBoard({
  players,
  usernames,
  currentPlayerId,
  diceValue,
  waitingForMove,
  onPawnMoveComplete,
  onPawnClick,
  loggedPlayerId,
  winnerId,
}: LudoBoardProps) {
  const isGameOver = !!winnerId;

  // Pobieramy nazwę zwycięzcy z przekazanej mapy usernames
  const winnerUsername = winnerId
    ? usernames[winnerId] || "Unknown Unit"
    : null;

  return (
    <div className="relative p-4 bg-[#0a0a0f] rounded-[45px] border-[3px] border-purple-500/50 shadow-[0_0_40px_rgba(168,85,247,0.2)]">
      {/* Plansza z efektem wyszarzenia po wygranej */}
      <div
        className={`grid grid-cols-13 grid-rows-13 gap-0.5 w-[600px] h-[600px] relative transition-all duration-700 ${
          isGameOver
            ? "grayscale opacity-30 scale-[0.98] pointer-events-none"
            : ""
        }`}
      >
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

            const isInteractable =
              !isGameOver &&
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

      {/* Nakładka Victory Screen */}
      <AnimatePresence>
        {isGameOver && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-md rounded-[45px]"
          >
            <motion.div
              initial={{ scale: 0.5, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              className="bg-[#121018] border border-yellow-500/30 p-12 rounded-[40px] shadow-[0_0_100px_rgba(234,179,8,0.2)] text-center relative overflow-hidden"
            >
              {/* Efekt poświaty za tekstem */}
              <div className="absolute -top-20 -left-20 w-40 h-40 bg-yellow-500/10 blur-[80px] rounded-full" />

              <div className="relative z-10">
                <motion.div
                  animate={{ y: [0, -10, 0] }}
                  transition={{ repeat: Infinity, duration: 2 }}
                  className="text-6xl mb-6"
                >
                  🏆
                </motion.div>

                <h2 className="text-white text-4xl font-black uppercase tracking-tighter mb-2 italic">
                  Neural Link Secured
                </h2>

                <p className="text-yellow-500 font-bold text-xl uppercase tracking-[0.2em] mb-8">
                  {winnerId === loggedPlayerId
                    ? "Protocol Complete: You Won"
                    : `Dominance Established by ${winnerUsername}`}
                </p>

                <button
                  onClick={() => (window.location.href = "/")}
                  className="px-10 py-4 bg-white text-black font-black rounded-2xl hover:bg-yellow-500 transition-all duration-300 uppercase text-sm tracking-widest shadow-[0_10px_20px_rgba(255,255,255,0.1)] active:scale-95"
                >
                  Terminate Session
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
