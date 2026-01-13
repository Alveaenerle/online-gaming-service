import { lobbyService } from "./lobbyService";

describe("lobbyService", () => {
  const mockLobbyInfo = {
    id: "room-123",
    gameType: "MAKAO",
    players: [],
    status: "LOBBY",
  };

  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn();
  });

  describe("API Request Logic & Error Handling", () => {
    it("should throw an error with the message from server when response is not ok", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Specific API Error" }),
      });

      await expect(lobbyService.getRoomInfo()).rejects.toThrow(
        "Specific API Error"
      );
    });

    it("should throw default error message if JSON parsing fails on error response", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: () => Promise.reject("Invalid JSON"),
      });

      await expect(lobbyService.getRoomInfo()).rejects.toThrow(
        "Lobby API error"
      );
    });
  });

  describe("getRoomInfo()", () => {
    it("should fetch room information successfully", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockLobbyInfo,
      });

      const result = await lobbyService.getRoomInfo();
      expect(result).toEqual(mockLobbyInfo);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/room-info"),
        expect.any(Object)
      );
    });
  });

  describe("createRoom()", () => {
    it("should send a POST request to create a room with default name if none provided", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockLobbyInfo,
      });

      await lobbyService.createRoom("MAKAO", 4, undefined, true);

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/create"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            gameType: "MAKAO",
            maxPlayers: 4,
            name: "Default Room Name",
            isPrivate: true,
          }),
        })
      );
    });
  });

  describe("joinRoom()", () => {
    it("should send a POST request to join a room", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockLobbyInfo,
      });

      await lobbyService.joinRoom("CODE123");

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/join"),
        expect.objectContaining({
          method: "POST",
          body: expect.stringContaining("CODE123"),
        })
      );
    });
  });

  describe("Room Actions (Start, Ready, Avatar, Leave, Kick)", () => {
    beforeEach(() => {
      (global.fetch as jest.Mock).mockResolvedValue({
        ok: true,
        json: async () => ({ message: "Success" }),
      });
    });

    it("should call startGame via POST", async () => {
      await lobbyService.startGame();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/start"),
        expect.objectContaining({ method: "POST" })
      );
    });

    it("should call toggleReady via POST", async () => {
      await lobbyService.toggleReady();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/ready"),
        expect.objectContaining({ method: "POST" })
      );
    });

    it("should call updateAvatar with correct payload", async () => {
      await lobbyService.updateAvatar("avatar_5");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/update-avatar"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ avatarId: "avatar_5" }),
        })
      );
    });

    it("should call leaveRoom via POST", async () => {
      const result = await lobbyService.leaveRoom();
      expect(result.message).toBe("Success");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/leave"),
        expect.any(Object)
      );
    });

    it("should call kickPlayer with the target userId", async () => {
      await lobbyService.kickPlayer("user-999");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/kick-player"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ userId: "user-999" }),
        })
      );
    });
  });
});
