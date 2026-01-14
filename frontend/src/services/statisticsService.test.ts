import { statisticsService } from "./statisticsService";

// Mockowanie fetch
global.fetch = jest.fn();

describe("statisticsService", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const mockStats = {
    playerId: "user-1",
    username: "TestUser",
    gameType: "MAKAO",
    gamesPlayed: 10,
    gamesWon: 5,
    winRatio: 0.5,
  };

  describe("getMyStatistics", () => {
    it("should fetch current user statistics for a game type", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockStats),
      });

      const result = await statisticsService.getMyStatistics("MAKAO");

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/statistics/me/MAKAO"),
        expect.objectContaining({
          method: "GET",
          credentials: "include",
        })
      );
      expect(result).toEqual(mockStats);
    });

    it("should throw error with message from JSON response on failure", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: false,
        headers: { get: () => "application/json" },
        json: jest.fn().mockResolvedValue({ message: "Unauthorized access" }),
      });

      await expect(statisticsService.getMyStatistics("MAKAO")).rejects.toThrow(
        "Unauthorized access"
      );
    });
  });

  describe("getAllMyStatistics", () => {
    it("should fetch all statistics for the current user", async () => {
      const mockAllStats = {
        playerId: "user-1",
        statistics: [mockStats],
      };

      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockAllStats),
      });

      const result = await statisticsService.getAllMyStatistics();

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/statistics/me"),
        expect.objectContaining({ credentials: "include" })
      );
      expect(result).toEqual(mockAllStats);
    });
  });

  describe("getPlayerStatistics", () => {
    it("should fetch statistics for a specific player", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockStats),
      });

      const result = await statisticsService.getPlayerStatistics(
        "player-123",
        "LUDO"
      );

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/statistics/player/player-123/LUDO"),
        expect.objectContaining({ method: "GET" })
      );
      expect(result).toEqual(mockStats);
    });
  });

  describe("getRankings", () => {
    it("should fetch rankings with default limit", async () => {
      const mockRankings = {
        gameType: "MAKAO",
        topByGamesPlayed: [mockStats],
        topByGamesWon: [mockStats],
      };

      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue(mockRankings),
      });

      const result = await statisticsService.getRankings("MAKAO");

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/statistics/rankings/MAKAO?limit=30"),
        expect.any(Object)
      );
      expect(result).toEqual(mockRankings);
    });

    it("should fetch rankings with custom limit", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: jest.fn().mockResolvedValue({}),
      });

      await statisticsService.getRankings("MAKAO", 10);

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("limit=10"),
        expect.any(Object)
      );
    });
  });

  describe("parseErrorResponse (Internal logic check)", () => {
    it("should handle plain text error responses", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: false,
        headers: { get: () => "text/plain" },
        text: jest.fn().mockResolvedValue("Internal Server Error"),
      });

      await expect(statisticsService.getRankings("MAKAO")).rejects.toThrow(
        "Internal Server Error"
      );
    });

    it("should fallback to default message if body is unparseable", async () => {
      (fetch as jest.Mock).mockResolvedValue({
        ok: false,
        headers: { get: () => "application/json" },
        json: jest.fn().mockRejectedValue(new Error("Parse fail")),
      });

      await expect(statisticsService.getMyStatistics("MAKAO")).rejects.toThrow(
        "Failed to fetch statistics"
      );
    });
  });
});
