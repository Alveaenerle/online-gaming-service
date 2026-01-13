import { makaoGameService, PlayCardPayload } from "./makaoGameService";
import { CardSuit, CardRank } from "../components/Games/Makao/types/index";

describe("makaoGameService", () => {
  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn();
  });

  describe("API Base Logic & Error Handling", () => {
    it("should throw an error with a message from the server when response is not ok", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Invalid card play" }),
      });

      await expect(makaoGameService.drawCard()).rejects.toThrow(
        "Invalid card play"
      );
    });

    it("should throw a default error message if JSON error parsing fails", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: () => Promise.reject("Not JSON"),
      });

      await expect(makaoGameService.drawCard()).rejects.toThrow(
        "Makao API error"
      );
    });

    it("should return an empty object if the response body is empty", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => "",
      });

      const result = await makaoGameService.acceptEffect();
      expect(result).toEqual({});
    });
  });

  describe("Game Actions", () => {
    it("should call playCard with correct payload and mapped nulls", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify({ message: "Card played" }),
      });

      const payload: PlayCardPayload = {
        cardSuit: "CLUBS" as CardSuit,
        cardRank: "ACE" as CardRank,
        // requestSuit and requestRank left undefined to test mapping to null
      };

      await makaoGameService.playCard(payload);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/play-card"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            cardSuit: "CLUBS",
            cardRank: "ACE",
            requestSuit: null,
            requestRank: null,
          }),
        })
      );
    });

    it("should call drawCard successfully", async () => {
      const mockResponse = { cards: [{ suit: "HEARTS", rank: "10" }] };
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await makaoGameService.drawCard();
      expect(result).toEqual(mockResponse);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/draw-card"),
        expect.any(Object)
      );
    });

    it("should call playDrawnCard with optional request parameters", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify({ message: "Drawn card played" }),
      });

      await makaoGameService.playDrawnCard({
        requestSuit: "SPADES" as CardSuit,
      });

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/play-drawn-card"),
        expect.objectContaining({
          body: JSON.stringify({
            requestSuit: "SPADES",
            requestRank: null,
          }),
        })
      );
    });

    it("should call skipDrawnCard via POST", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify({ message: "Skipped" }),
      });

      await makaoGameService.skipDrawnCard();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/skip-drawn-card"),
        expect.objectContaining({ method: "POST" })
      );
    });

    it("should call requestState via POST", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify({ message: "State requested" }),
      });

      await makaoGameService.requestState();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/request-state"),
        expect.any(Object)
      );
    });

    it("should call leaveGame via POST", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => JSON.stringify({ message: "Left game" }),
      });

      await makaoGameService.leaveGame();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/leave-game"),
        expect.any(Object)
      );
    });
  });

  describe("checkActiveGame()", () => {
    it("should return true if requestState succeeds", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => "{}",
      });
      const result = await makaoGameService.checkActiveGame();
      expect(result).toBe(true);
    });

    it("should return false if requestState fails", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: false });
      const result = await makaoGameService.checkActiveGame();
      expect(result).toBe(false);
    });
  });
});
