// hooks/useLudoGame.ts
export function useLudoGame(gameId: string) {
  const [gameState, setGameState] = useState<LudoGameState>(null);

  // Subskrypcja WebSocketa
  useEffect(() => {
    const sub = socketService.subscribe(`/topic/game/${gameId}`, (msg) => {
      setGameState(msg); // Otrzymujesz LudoGameStateMessage z kontrolera
    });
    return () => sub.unsubscribe();
  }, [gameId]);

  const roll = async () => {
    await ludoService.rollDice(gameId);
  };

  const move = async (pawnIndex: number) => {
    await ludoService.movePawn(gameId, pawnIndex);
  };

  return { gameState, roll, move };
}
