import {
  chatService,
  ChatMessage,
  ChatError,
  ChatHistoryResponse,
  TypingIndicator,
} from "./chatService";
import { socketService } from "./socketService";

// Mock the socketService
jest.mock("./socketService", () => ({
  socketService: {
    connect: jest.fn(),
    subscribe: jest.fn(),
    unsubscribe: jest.fn(),
    send: jest.fn(),
  },
}));

describe("ChatService", () => {
  const lobbyId = "test-lobby-123";

  beforeEach(() => {
    jest.clearAllMocks();
    chatService.disconnect(); // Ensure clean state
  });

  describe("connectToLobby()", () => {
    it("should connect to the socket and subscribe to all required topics", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);

      await chatService.connectToLobby(lobbyId);

      expect(socketService.connect).toHaveBeenCalled();
      expect(socketService.subscribe).toHaveBeenCalledWith(
        `/topic/room/${lobbyId}/chat`,
        expect.any(Function)
      );
      expect(socketService.subscribe).toHaveBeenCalledWith(
        `/topic/room/${lobbyId}/typing`,
        expect.any(Function)
      );
      expect(socketService.subscribe).toHaveBeenCalledWith(
        "/user/queue/chat/error",
        expect.any(Function)
      );
      expect(socketService.subscribe).toHaveBeenCalledWith(
        "/user/queue/chat/history",
        expect.any(Function)
      );
      expect(chatService.isConnected()).toBe(true);
    });

    it("should not reconnect if already connected to the same lobby", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);

      await chatService.connectToLobby(lobbyId);
      await chatService.connectToLobby(lobbyId);

      expect(socketService.connect).toHaveBeenCalledTimes(1);
    });

    it("should disconnect from previous lobby when connecting to a new one", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);

      await chatService.connectToLobby("old-lobby");
      await chatService.connectToLobby("new-lobby");

      expect(socketService.unsubscribe).toHaveBeenCalledWith(
        "/topic/room/old-lobby/chat"
      );
      expect(socketService.connect).toHaveBeenCalledTimes(2);
    });

    it("should throw an error if socket connection fails", async () => {
      const error = new Error("Connection failed");
      (socketService.connect as jest.Mock).mockRejectedValue(error);

      await expect(chatService.connectToLobby(lobbyId)).rejects.toThrow(
        "Connection failed"
      );
    });
  });

  describe("Messaging and Event Handling", () => {
    beforeEach(async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);
      await chatService.connectToLobby(lobbyId);
    });

    it("should trigger message handlers when a new message arrives", () => {
      const handler = jest.fn();
      chatService.onMessage(handler);

      // Simulate receiving a message from socket
      const mockMsg: ChatMessage = {
        id: "1",
        content: "Hello",
        senderId: "u1",
        senderUsername: "User",
        senderAvatar: "",
        timestamp: "now",
        isBlurred: false,
        type: "USER_MESSAGE",
      };

      const subscribeCallback = (
        socketService.subscribe as jest.Mock
      ).mock.calls.find((call) => call[0] === `/topic/room/${lobbyId}/chat`)[1];

      subscribeCallback(mockMsg);
      expect(handler).toHaveBeenCalledWith(mockMsg);
    });

    it("should trigger error handlers when a chat error occurs", () => {
      const handler = jest.fn();
      chatService.onError(handler);

      const mockError: ChatError = { code: "RATE_LIMIT", message: "Too fast" };
      const errorCallback = (
        socketService.subscribe as jest.Mock
      ).mock.calls.find((call) => call[0] === "/user/queue/chat/error")[1];

      errorCallback(mockError);
      expect(handler).toHaveBeenCalledWith(mockError);
    });

    it("should trigger history handlers when history is received", () => {
      const handler = jest.fn();
      chatService.onHistory(handler);

      const mockHistory: ChatHistoryResponse = {
        messages: [],
        offset: 0,
        limit: 10,
        hasMore: false,
        totalMessages: 0,
      };
      const historyCallback = (
        socketService.subscribe as jest.Mock
      ).mock.calls.find((call) => call[0] === "/user/queue/chat/history")[1];

      historyCallback(mockHistory);
      expect(handler).toHaveBeenCalledWith(mockHistory);
    });

    it("should trigger typing handlers when a user starts typing", () => {
      const handler = jest.fn();
      chatService.onTyping(handler);

      const mockIndicator: TypingIndicator = {
        userId: "1",
        username: "User",
        isTyping: true,
      };
      const typingCallback = (
        socketService.subscribe as jest.Mock
      ).mock.calls.find(
        (call) => call[0] === `/topic/room/${lobbyId}/typing`
      )[1];

      typingCallback(mockIndicator);
      expect(handler).toHaveBeenCalledWith(mockIndicator);
    });
  });

  describe("Send Actions", () => {
    it("should send a message via socket if connected", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);
      await chatService.connectToLobby(lobbyId);

      chatService.sendMessage("Hello world");
      expect(socketService.send).toHaveBeenCalledWith(
        `/app/chat/${lobbyId}/send`,
        { content: "Hello world" }
      );
    });

    it("should warn and not send if not connected to a lobby", () => {
      const consoleSpy = jest.spyOn(console, "warn").mockImplementation();
      chatService.sendMessage("Hidden");
      expect(socketService.send).not.toHaveBeenCalled();
      expect(consoleSpy).toHaveBeenCalled();
      consoleSpy.mockRestore();
    });

    it("should send typing indicator", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);
      await chatService.connectToLobby(lobbyId);

      chatService.sendTypingIndicator(true);
      expect(socketService.send).toHaveBeenCalledWith(
        `/app/chat/${lobbyId}/typing`,
        { isTyping: true }
      );
    });

    it("should request chat history with default params", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);
      await chatService.connectToLobby(lobbyId);

      chatService.requestHistory();
      expect(socketService.send).toHaveBeenCalledWith(
        `/app/chat/${lobbyId}/history`,
        { offset: 0, limit: 50 }
      );
    });
  });

  describe("Cleanup and Unsubscription", () => {
    it("should properly remove event handlers using the returned cleanup function", () => {
      const handler = jest.fn();
      const unsubscribe = chatService.onMessage(handler);

      unsubscribe(); // Remove handler

      // Manually trigger internal call (mocking state)
      chatService["messageHandlers"].forEach((h) => h({} as any));
      expect(handler).not.toHaveBeenCalled();
    });

    it("should unsubscribe from all topics on disconnect", async () => {
      (socketService.connect as jest.Mock).mockResolvedValue(undefined);
      await chatService.connectToLobby(lobbyId);

      chatService.disconnect();

      expect(socketService.unsubscribe).toHaveBeenCalledWith(
        `/topic/room/${lobbyId}/chat`
      );
      expect(socketService.unsubscribe).toHaveBeenCalledWith(
        "/user/queue/chat/error"
      );
      expect(chatService.isConnected()).toBe(false);
    });
  });
});
