import { PlayerCard } from "./PlayerCard";
import { BigPlayerCard } from "./BigPlayerCard";
import { LobbyPlayer } from "../utils/types";
import { lobbyService } from "../../../services/lobbyService";

type Props = {
  players: LobbyPlayer[];
  maxPlayers: number;
  onAvatarSelect: (id: string) => void;
  onToggleReady: () => void;
  isHost: boolean;
};

export function LobbyPlayersSection({
  players,
  maxPlayers,
  onAvatarSelect,
  onToggleReady,
  isHost,
}: Props) {
  const you = players.find((p) => p.isYou);
  const others = players.filter((p) => !p.isYou);

  return (
    <div className="grid lg:grid-cols-12 gap-8 h-full">
      <div className="lg:col-span-8">
        <div
          className="grid grid-cols-2 gap-4 overflow-y-auto pr-2 custom-scrollbar content-start"
          style={{ maxHeight: "calc(100vh - 450px)" }}
        >
          {Array.from({ length: maxPlayers - 1 }).map((_, i) => {
            const player = others[i];
            return (
              <PlayerCard
                key={player?.userId ?? `empty-${i}`}
                player={player}
                onKick={
                  isHost ? (id) => lobbyService.kickPlayer(id) : undefined
                }
                showKickButton={isHost && !!player}
              />
            );
          })}
        </div>
      </div>

      <div className="lg:col-span-4">
        <div className="h-full" style={{ maxHeight: "calc(100vh - 450px)" }}>
          {you && (
            <BigPlayerCard
              player={you}
              onAvatarClick={() => onAvatarSelect(you.userId)}
              onToggleReady={onToggleReady}
            />
          )}
        </div>
      </div>
    </div>
  );
}
