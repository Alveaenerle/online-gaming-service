import { PlayerCard } from "./PlayerCard";
import { BigPlayerCard } from "./BigPlayerCard";
import { LobbyPlayer } from "./types";

type Props = {
  players: LobbyPlayer[];
  maxPlayers: number;
  onAvatarSelect: (id: string) => void;
  onToggleReady: () => void;
};

export function LobbyPlayersSection({
  players,
  maxPlayers,
  onAvatarSelect,
  onToggleReady,
}: Props) {
  const you = players.find((p) => p.isYou);
  const others = players.filter((p) => !p.isYou);

  return (
    <div className="grid grid-cols-[1fr_320px] gap-8">
      <div
        className="grid grid-cols-2 gap-6
                   max-h-[550px] overflow-y-auto pr-2"
      >
        {Array.from({ length: maxPlayers - 1 }).map((_, i) => {
          const player = others[i];
          return <PlayerCard key={player?.id ?? i} player={player} />;
        })}
      </div>

      {you && (
        <BigPlayerCard
          player={you}
          onAvatarClick={() => onAvatarSelect(you.id)}
          onToggleReady={onToggleReady}
        />
      )}
    </div>
  );
}
