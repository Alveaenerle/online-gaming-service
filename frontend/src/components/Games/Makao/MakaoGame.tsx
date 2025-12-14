import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";
import Navbar from "../../Shared/Navbar";
import Footer from "../../Shared/Footer";
import Player from "./Player";
import Card from "./Card";
import { Card as CardType, Player as PlayerType, GameState, Suit, Rank } from "./types";

const MakaoGame: React.FC = () => {
  const [playerCount, setPlayerCount] = useState<number>(4);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [message, setMessage] = useState<string>("");

  // Initialize deck
  const createDeck = (): CardType[] => {
    const suits: Suit[] = ["hearts", "diamonds", "clubs", "spades"];
    const ranks: Rank[] = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"];
    const deck: CardType[] = [];

    suits.forEach((suit) => {
      ranks.forEach((rank) => {
        deck.push({
          suit,
          rank,
          id: `${suit}-${rank}-${Math.random()}`,
        });
      });
    });

    return shuffleDeck(deck);
  };

  const shuffleDeck = (deck: CardType[]): CardType[] => {
    const shuffled = [...deck];
    for (let i = shuffled.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
  };

  const startGame = () => {
    const deck = createDeck();
    const players: PlayerType[] = [];

    // Create players
    for (let i = 0; i < playerCount; i++) {
      players.push({
        id: i,
        name: i === 0 ? "You" : `Player ${i + 1}`,
        cards: [],
        isHuman: i === 0,
      });
    }

    // Deal 5 cards to each player
    let cardIndex = 0;
    for (let i = 0; i < 5; i++) {
      players.forEach((player) => {
        player.cards.push(deck[cardIndex++]);
      });
    }

    const remainingDeck = deck.slice(cardIndex);
    const firstCard = remainingDeck.pop()!;

    setGameState({
      players,
      currentPlayerIndex: 0,
      deck: remainingDeck,
      discardPile: [firstCard],
      direction: 1,
      drawCount: 0,
      gameStarted: true,
      winner: null,
    });

    setMessage("Game started! Play a card or draw from the deck.");
  };

  const canPlayCard = (card: CardType, topCard: CardType, state: GameState): boolean => {
    // If there's a requested suit or rank from special cards
    if (state.requestedSuit) {
      return card.suit === state.requestedSuit || card.rank === "A";
    }
    if (state.requestedRank) {
      return card.rank === state.requestedRank || card.rank === "J";
    }

    // If player must draw cards (from 2 or 3)
    if (state.drawCount > 0) {
      return card.rank === "2" || card.rank === "3";
    }

    // Normal play: same suit or same rank
    return card.suit === topCard.suit || card.rank === topCard.rank;
  };

  const playCard = (card: CardType) => {
    if (!gameState || gameState.currentPlayerIndex !== 0) return;

    const topCard = gameState.discardPile[gameState.discardPile.length - 1];

    if (!canPlayCard(card, topCard, gameState)) {
      setMessage("You can't play that card!");
      return;
    }

    const newGameState = { ...gameState };
    const playerCards = newGameState.players[0].cards.filter((c) => c.id !== card.id);
    newGameState.players[0].cards = playerCards;
    newGameState.discardPile.push(card);

    // Check for winner
    if (playerCards.length === 0) {
      newGameState.winner = 0;
      setGameState(newGameState);
      setMessage("🎉 You won! Congratulations!");
      return;
    }

    // Handle special cards
    let skipNext = false;

    if (card.rank === "4") {
      // Skip next player
      skipNext = true;
      setMessage("You played a 4! Next player is skipped.");
    } else if (card.rank === "K" && (card.suit === "hearts" || card.suit === "spades")) {
      // Change direction
      newGameState.direction *= -1 as 1 | -1;
      setMessage(`You played King of ${card.suit}! Direction reversed.`);
    } else if (card.rank === "2" || card.rank === "3") {
      // Force next player to draw
      const drawAmount = card.rank === "2" ? 2 : 3;
      newGameState.drawCount += drawAmount;
      setMessage(`You played a ${card.rank}! Next player must draw ${newGameState.drawCount} cards or play another 2/3.`);
    } else if (card.rank === "A") {
      // Request suit - for now, auto-select randomly
      const suits: Suit[] = ["hearts", "diamonds", "clubs", "spades"];
      newGameState.requestedSuit = suits[Math.floor(Math.random() * suits.length)];
      setMessage(`You played an Ace! Requested suit: ${newGameState.requestedSuit}`);
    } else if (card.rank === "J") {
      // Request rank - for now, auto-select randomly
      const ranks: Rank[] = ["5", "6", "7", "8", "9", "10"];
      newGameState.requestedRank = ranks[Math.floor(Math.random() * ranks.length)];
      setMessage(`You played a Jack! Requested rank: ${newGameState.requestedRank}`);
    } else {
      setMessage("Card played!");
    }

    // Move to next player
    let nextPlayerIndex = (newGameState.currentPlayerIndex + newGameState.direction + newGameState.players.length) % newGameState.players.length;

    if (skipNext) {
      nextPlayerIndex = (nextPlayerIndex + newGameState.direction + newGameState.players.length) % newGameState.players.length;
    }

    newGameState.currentPlayerIndex = nextPlayerIndex;
    setGameState(newGameState);

    // AI players play after a delay
    setTimeout(() => {
      playAITurns(newGameState);
    }, 1000);
  };

  const drawCard = () => {
    if (!gameState || gameState.currentPlayerIndex !== 0) return;

    const newGameState = { ...gameState };

    if (newGameState.deck.length === 0) {
      setMessage("No more cards in the deck!");
      return;
    }

    const drawAmount = newGameState.drawCount > 0 ? newGameState.drawCount : 1;
    const drawnCards: CardType[] = [];

    for (let i = 0; i < drawAmount && newGameState.deck.length > 0; i++) {
      const card = newGameState.deck.pop()!;
      drawnCards.push(card);
    }

    newGameState.players[0].cards.push(...drawnCards);
    newGameState.drawCount = 0;
    newGameState.requestedSuit = undefined;
    newGameState.requestedRank = undefined;

    setMessage(`You drew ${drawAmount} card(s).`);

    // Move to next player
    const nextPlayerIndex = (newGameState.currentPlayerIndex + newGameState.direction + newGameState.players.length) % newGameState.players.length;
    newGameState.currentPlayerIndex = nextPlayerIndex;
    setGameState(newGameState);

    setTimeout(() => {
      playAITurns(newGameState);
    }, 1000);
  };

  const playAITurns = (currentState: GameState) => {
    let state = { ...currentState };

    while (state.currentPlayerIndex !== 0 && !state.winner) {
      const currentPlayer = state.players[state.currentPlayerIndex];
      const topCard = state.discardPile[state.discardPile.length - 1];

      // Find playable cards
      const playableCards = currentPlayer.cards.filter((card) =>
        canPlayCard(card, topCard, state)
      );

      if (playableCards.length > 0) {
        // Play first playable card
        const cardToPlay = playableCards[0];
        const playerCards = currentPlayer.cards.filter((c) => c.id !== cardToPlay.id);
        currentPlayer.cards = playerCards;
        state.discardPile.push(cardToPlay);

        // Check for winner
        if (playerCards.length === 0) {
          state.winner = state.currentPlayerIndex;
          setGameState(state);
          setMessage(`${currentPlayer.name} won the game!`);
          return;
        }

        // Handle special cards (simplified for AI)
        let skipNext = false;

        if (cardToPlay.rank === "4") {
          skipNext = true;
        } else if (cardToPlay.rank === "K" && (cardToPlay.suit === "hearts" || cardToPlay.suit === "spades")) {
          state.direction *= -1 as 1 | -1;
        } else if (cardToPlay.rank === "2" || cardToPlay.rank === "3") {
          const drawAmount = cardToPlay.rank === "2" ? 2 : 3;
          state.drawCount += drawAmount;
        } else if (cardToPlay.rank === "A") {
          const suits: Suit[] = ["hearts", "diamonds", "clubs", "spades"];
          state.requestedSuit = suits[Math.floor(Math.random() * suits.length)];
        } else if (cardToPlay.rank === "J") {
          const ranks: Rank[] = ["5", "6", "7", "8", "9", "10"];
          state.requestedRank = ranks[Math.floor(Math.random() * ranks.length)];
        }

        setMessage(`${currentPlayer.name} played ${cardToPlay.rank} of ${cardToPlay.suit}`);

        let nextPlayerIndex = (state.currentPlayerIndex + state.direction + state.players.length) % state.players.length;

        if (skipNext) {
          nextPlayerIndex = (nextPlayerIndex + state.direction + state.players.length) % state.players.length;
        }

        state.currentPlayerIndex = nextPlayerIndex;
      } else {
        // Draw cards
        const drawAmount = state.drawCount > 0 ? state.drawCount : 1;
        const drawnCards: CardType[] = [];

        for (let i = 0; i < drawAmount && state.deck.length > 0; i++) {
          const card = state.deck.pop()!;
          drawnCards.push(card);
        }

        currentPlayer.cards.push(...drawnCards);
        state.drawCount = 0;
        state.requestedSuit = undefined;
        state.requestedRank = undefined;

        setMessage(`${currentPlayer.name} drew ${drawAmount} card(s).`);

        const nextPlayerIndex = (state.currentPlayerIndex + state.direction + state.players.length) % state.players.length;
        state.currentPlayerIndex = nextPlayerIndex;
      }
    }

    setGameState(state);
  };

  const resetGame = () => {
    setGameState(null);
    setMessage("");
  };

  if (!gameState) {
    return (
      <div className="min-h-screen bg-bg text-white antialiased">
        <Navbar />
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] animate-gradient-bg" />

        <main className="pt-24 pb-10 px-6 md:px-12 lg:px-24">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="max-w-2xl mx-auto"
          >
            <div className="mb-6">
              <Link
                to="/"
                className="text-purpleEnd hover:text-purple-400 flex items-center gap-2"
              >
                ← Back to Home
              </Link>
            </div>

            <div className="bg-[#121018] p-8 rounded-3xl border border-white/10 shadow-neon">
              <h1 className="text-4xl font-extrabold mb-2 bg-gradient-to-r from-purpleStart to-purpleEnd bg-clip-text text-transparent">
                Makao Card Game
              </h1>
              <p className="text-gray-400 mb-8">
                Classic card game - Play against AI opponents
              </p>

              <div className="space-y-6">
                <div>
                  <label className="block text-sm font-medium mb-3">
                    Number of Players (1-8)
                  </label>
                  <div className="flex gap-2 flex-wrap">
                    {[2, 3, 4, 5, 6, 7, 8].map((count) => (
                      <motion.button
                        key={count}
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => setPlayerCount(count)}
                        className={`px-6 py-3 rounded-xl font-semibold transition-all ${
                          playerCount === count
                            ? "bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon"
                            : "bg-[#1a1a27] border border-white/10 text-gray-300"
                        }`}
                      >
                        {count}
                      </motion.button>
                    ))}
                  </div>
                </div>

                <div className="bg-[#1a1a27] p-6 rounded-xl border border-white/10">
                  <h3 className="font-bold text-lg mb-3">Game Rules:</h3>
                  <ul className="text-sm text-gray-300 space-y-2">
                    <li>• Match the suit or rank of the top card</li>
                    <li>• <strong>2</strong> - Forces next player to draw 2 cards</li>
                    <li>• <strong>3</strong> - Forces next player to draw 3 cards</li>
                    <li>• <strong>4</strong> - Skips the next player</li>
                    <li>• <strong>J</strong> - Request any rank</li>
                    <li>• <strong>K♥/K♠</strong> - Reverses play direction</li>
                    <li>• <strong>A</strong> - Request any suit</li>
                    <li>• First player to run out of cards wins!</li>
                  </ul>
                </div>

                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={startGame}
                  className="w-full py-4 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-bold text-lg shadow-neon"
                >
                  Start Game
                </motion.button>
              </div>
            </div>
          </motion.div>
        </main>
        <Footer />
      </div>
    );
  }

  const topCard = gameState.discardPile[gameState.discardPile.length - 1];
  const humanPlayer = gameState.players[0];
  const otherPlayers = gameState.players.slice(1);

  // Distribute players around table (clockwise: right -> top -> left)
  const distributePlayersAroundTable = () => {
    const total = otherPlayers.length;
    const top: PlayerType[] = [];
    const left: PlayerType[] = [];
    const right: PlayerType[] = [];

    // Clockwise from human player (bottom): left -> top -> right
    if (total === 1) {
      top.push(otherPlayers[0]);
    } else if (total === 2) {
        left.push(otherPlayers[0]);
        right.push(otherPlayers[1]);
    } else if (total === 3) {
      left.push(otherPlayers[0]);
      top.push(otherPlayers[1]);
      right.push(otherPlayers[2]);
    } else if (total === 4) {
      left.push(otherPlayers[0]);
      top.push(otherPlayers[1], otherPlayers[2]);
      right.push(otherPlayers[3]);
    } else if (total === 5) {
      left.push(otherPlayers[1], otherPlayers[0]);
      top.push(otherPlayers[2]);
      right.push(otherPlayers[3], otherPlayers[4]);
    } else if (total === 6) {
      left.push(otherPlayers[1], otherPlayers[0]);
      top.push(otherPlayers[2], otherPlayers[3]);
      right.push(otherPlayers[4], otherPlayers[5]);
    } else {
      // 7 players
      left.push(otherPlayers[1], otherPlayers[0]);
      top.push(otherPlayers[2], otherPlayers[3], otherPlayers[4]);
      right.push(otherPlayers[5], otherPlayers[6]);
    }

    return { top, left, right };
  };

  const { top: topPlayers, left: leftPlayers, right: rightPlayers } = distributePlayersAroundTable();

  return (
    <div className="min-h-screen bg-bg text-white antialiased overflow-hidden">
      <Navbar />
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)]" />

      <main className="pt-36 pb-4 px-4 h-[calc(100vh-144px)]">
        <div className="h-full max-w-[2000px] mx-auto flex gap-2 justify-center">
          {/* Game Table Area */}
          <div className="flex-1 flex items-center justify-center">
            <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purpleEnd/20 shadow-2xl shadow-black/50 p-8">
              {/* Table glow */}
              <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.1),_transparent_60%)]" />

              {/* Top Players */}
              {topPlayers.length > 0 && (
                <div className="absolute top-3 left-1/2 -translate-x-1/2 flex gap-3">
                  {topPlayers.map((player) => (
                    <Player
                      key={player.id}
                      player={player}
                      isCurrentPlayer={gameState.currentPlayerIndex === player.id}
                      compact
                      position="top"
                    />
                  ))}
                </div>
              )}

              {/* Left Players */}
              {leftPlayers.length > 0 && (
                <div className="absolute left-3 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {leftPlayers.map((player) => (
                    <Player
                      key={player.id}
                      player={player}
                      isCurrentPlayer={gameState.currentPlayerIndex === player.id}
                      compact
                      position="left"
                    />
                  ))}
                </div>
              )}

              {/* Right Players */}
              {rightPlayers.length > 0 && (
                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex flex-col gap-3">
                  {rightPlayers.map((player) => (
                    <Player
                      key={player.id}
                      player={player}
                      isCurrentPlayer={gameState.currentPlayerIndex === player.id}
                      compact
                      position="right"
                    />
                  ))}
                </div>
              )}

              {/* Center - Deck & Discard */}
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center gap-12">
                {/* Draw Pile */}
                <div
                  className={`text-center ${gameState.currentPlayerIndex === 0 && !gameState.winner ? "cursor-pointer group" : ""}`}
                  onClick={gameState.currentPlayerIndex === 0 && !gameState.winner ? drawCard : undefined}
                >
                  <div className="relative group-hover:scale-105 transition-transform">
                    <div className="absolute -top-0.5 -left-0.5 w-[58px] h-[82px] rounded-lg bg-purpleStart/30" />
                    <div className="absolute -top-1 -left-1 w-[58px] h-[82px] rounded-lg bg-purpleStart/20" />
                    <Card card={topCard} isFaceDown size="md" />
                  </div>
                  <p className="text-xs text-white/60 mt-2">Draw ({gameState.deck.length})</p>
                </div>

                {/* Discard Pile */}
                <div className="text-center">
                  <Card card={topCard} size="md" />
                  <p className="text-xs text-white/60 mt-2">Discard</p>
                </div>
              </div>

              {/* Bottom - Human Player */}
              <div className="absolute bottom-3 left-1/2 -translate-x-1/2 max-w-[90%]">
                <Player
                  player={humanPlayer}
                  isCurrentPlayer={gameState.currentPlayerIndex === 0}
                  onCardClick={playCard}
                  canPlayCard={(card) => canPlayCard(card, topCard, gameState)}
                  position="bottom"
                />
              </div>
            </div>
          </div>

          {/* Side Panel */}
          <div className="w-64 flex flex-col gap-3 flex-shrink-0">
            {/* Navigation */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <Link
                to="/"
                className="flex items-center gap-2 text-gray-400 hover:text-purpleEnd transition-colors text-sm mb-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                Back to Home
              </Link>
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={resetGame}
                className="w-full py-2 rounded-lg bg-red-500/20 hover:bg-red-500/30 text-red-400 text-sm font-medium transition-colors flex items-center justify-center gap-2 border border-red-500/30"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                New Game
              </motion.button>
            </div>

            {/* Activity */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <h3 className="text-xs font-medium text-gray-500 mb-2">Activity</h3>
              <div className="min-h-[48px] flex items-center">
                {message ? (
                  <motion.p
                    key={message}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    className="text-white text-sm"
                  >
                    {message}
                  </motion.p>
                ) : (
                  <p className="text-gray-600 text-sm">Waiting...</p>
                )}
              </div>
            </div>

            {/* Game Status */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10 flex-1">
              <h3 className="text-xs font-medium text-gray-500 mb-3">Game Status</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">Direction</span>
                  <span className="text-white text-lg">{gameState.direction === 1 ? "→" : "←"}</span>
                </div>
                {gameState.drawCount > 0 && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Must Draw</span>
                    <span className="text-red-400 font-bold">{gameState.drawCount}</span>
                  </div>
                )}
                {gameState.requestedSuit && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Suit</span>
                    <span className="text-purpleEnd capitalize">{gameState.requestedSuit}</span>
                  </div>
                )}
                {gameState.requestedRank && (
                  <div className="flex justify-between">
                    <span className="text-gray-400">Rank</span>
                    <span className="text-purpleEnd">{gameState.requestedRank}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Current Turn */}
            <div className="bg-[#121018]/80 backdrop-blur p-3 rounded-xl border border-white/10">
              <h3 className="text-xs font-medium text-gray-500 mb-2">Current Turn</h3>
              <div className="flex items-center gap-2">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm ${
                  gameState.currentPlayerIndex === 0
                    ? "bg-gradient-to-br from-purpleStart to-purpleEnd text-white"
                    : "bg-gray-700 text-gray-300"
                }`}>
                  {gameState.players[gameState.currentPlayerIndex].name.charAt(0)}
                </div>
                <div>
                  <p className="text-white text-sm font-medium">
                    {gameState.players[gameState.currentPlayerIndex].name}
                  </p>
                  <p className="text-[10px] text-gray-500">
                    {gameState.currentPlayerIndex === 0 ? "Your turn!" : "Thinking..."}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Winner Modal */}
      {gameState.winner !== null && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 bg-black/80 flex items-center justify-center z-50"
          onClick={resetGame}
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="bg-[#121018] p-10 rounded-2xl border border-purpleEnd/50 shadow-2xl text-center"
          >
            <h2 className="text-4xl font-bold mb-3 bg-gradient-to-r from-purpleStart to-purpleEnd bg-clip-text text-transparent">
              {gameState.winner === 0 ? "🎉 You Won!" : `${gameState.players[gameState.winner].name} Won!`}
            </h2>
            <p className="text-gray-400 text-sm">Click anywhere to start a new game</p>
          </motion.div>
        </motion.div>
      )}
    </div>
  );
};

export default MakaoGame;
