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
  onAddFriend?: (userId: string) => void;
  onInviteToLobby?: () => void;
  isFriend?: (userId: string) => boolean;
  isInvited?: (userId: string) => boolean;
  hasReceivedRequest?: (userId: string) => boolean; // If this user has sent US a request
  canSendFriendRequest?: boolean; // If current user is allowed to send requests (e.g. not guest)
  canInviteToLobby?: boolean; // If current user can invite friends to lobby
};

export function LobbyPlayersSection({
  players,
  maxPlayers,
  onAvatarSelect,
  onToggleReady,
  isHost,
  onAddFriend,
  onInviteToLobby,
  isFriend,
  isInvited,
  hasReceivedRequest,
  canSendFriendRequest = false,
  canInviteToLobby = false
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
            const alreadyFriend = player && isFriend ? isFriend(player.userId) : false;
            const alreadyInvited = player && isInvited ? isInvited(player.userId) : false;
            const receivedFromThem = player && hasReceivedRequest ? hasReceivedRequest(player.userId) : false;
            // Don't show add button for guests (usernames starting with "Guest_")
            const isTargetGuest = player?.username?.startsWith("Guest_") ?? false;
            const showAdd = canSendFriendRequest && !alreadyFriend && !alreadyInvited && !receivedFromThem && !isTargetGuest;

            return (
              <PlayerCard
                key={player?.userId ?? `empty-${i}`}
                player={player}
                onKick={
                  isHost ? (id) => lobbyService.kickPlayer(id) : undefined
                }
                showKickButton={isHost && !!player}
                onAddFriend={onAddFriend}
                canAddFriend={showAdd}
                isInvited={alreadyInvited}
                hasReceivedRequest={receivedFromThem}
                onInviteToLobby={onInviteToLobby}
                canInviteToLobby={canInviteToLobby && !player}
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
