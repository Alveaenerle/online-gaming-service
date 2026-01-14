import {
  socialService,
  Friend,
  FriendRequest,
  GameInvite,
} from "./socialService";

describe("socialService", () => {
  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn();
  });

  const mockFriend: Friend = {
    id: "user-1",
    username: "Friend1",
    status: "ONLINE",
  };

  const mockFriendRequest: FriendRequest = {
    id: "req-1",
    requesterId: "user-1",
    requesterUsername: "Friend1",
    addresseeId: "me",
    status: "PENDING",
    createdAt: "2023-01-01",
  };

  const mockGameInvite: GameInvite = {
    id: "inv-1",
    senderId: "user-1",
    senderUsername: "Friend1",
    targetId: "me",
    lobbyId: "lobby-123",
    lobbyName: "Fun Room",
    gameType: "MAKAO",
    createdAt: Date.now(),
  };

  describe("Friend Management", () => {
    it("should send a friend request successfully", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });

      await expect(
        socialService.sendFriendRequest("target-id")
      ).resolves.not.toThrow();
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/friends/invite"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ targetUserId: "target-id" }),
        })
      );
    });

    it("should throw error if sending friend request fails", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: false });
      await expect(socialService.sendFriendRequest("id")).rejects.toThrow(
        "Failed to send friend request"
      );
    });

    it("should accept a friend request", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
      await socialService.acceptFriendRequest("req-1");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/friends/accept"),
        expect.any(Object)
      );
    });

    it("should reject a friend request", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
      await socialService.rejectFriendRequest("req-1");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/friends/reject"),
        expect.any(Object)
      );
    });

    it("should fetch friends list", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockFriend],
      });
      const result = await socialService.getFriends();
      expect(result).toEqual([mockFriend]);
    });

    it("should fetch pending requests", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockFriendRequest],
      });
      const result = await socialService.getPendingRequests();
      expect(result).toEqual([mockFriendRequest]);
    });

    it("should fetch sent requests", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockFriendRequest],
      });
      const result = await socialService.getSentRequests();
      expect(result).toEqual([mockFriendRequest]);
    });

    it("should remove a friend", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
      await socialService.removeFriend("friend-id");
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/friends/friend-id"),
        expect.objectContaining({ method: "DELETE" })
      );
    });
  });

  describe("Game Invites", () => {
    it("should send a game invite and return invite data", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockGameInvite,
      });

      const result = await socialService.sendGameInvite(
        "u1",
        "l1",
        "Room",
        "MAKAO"
      );
      expect(result).toEqual(mockGameInvite);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/invites/send"),
        expect.objectContaining({ method: "POST" })
      );
    });

    it("should throw server message on failed game invite", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "User is busy" }),
      });

      await expect(
        socialService.sendGameInvite("u1", "l1", "Room", "MAKAO")
      ).rejects.toThrow("User is busy");
    });

    it("should fetch pending game invites", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockGameInvite],
      });
      const result = await socialService.getPendingGameInvites();
      expect(result).toHaveLength(1);
    });

    it("should fetch sent game invites", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockGameInvite],
      });
      const result = await socialService.getSentGameInvites();
      expect(result).toHaveLength(1);
    });

    it("should accept a game invite", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockGameInvite,
      });
      const result = await socialService.acceptGameInvite("inv-1");
      expect(result).toEqual(mockGameInvite);
    });

    it("should decline a game invite", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => mockGameInvite,
      });
      const result = await socialService.declineGameInvite("inv-1");
      expect(result).toEqual(mockGameInvite);
    });

    it("should throw default message if invite rejection fails without error body", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        json: () => Promise.reject(),
      });
      await expect(socialService.declineGameInvite("id")).rejects.toThrow(
        "Failed to decline game invite"
      );
    });
  });
});
