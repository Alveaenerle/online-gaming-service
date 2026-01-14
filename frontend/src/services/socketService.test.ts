import { socketService } from "./socketService";
import * as StompJs from "stompjs";
import SockJS from "sockjs-client";

jest.mock("sockjs-client");
jest.mock("stompjs");

describe("SocketService", () => {
  let mockStompClient: any;

  beforeEach(() => {
    jest.clearAllMocks();

    // Reset singleton state
    (socketService as any).connected = false;
    (socketService as any).isConnecting = false;
    (socketService as any).connectPromise = null;
    (socketService as any).client = null;
    (socketService as any).subscriptions = new Map();

    mockStompClient = {
      connect: jest.fn((headers, onConnect) => {
        mockStompClient.connected = true;
        onConnect();
      }),
      disconnect: jest.fn((callback) => {
        mockStompClient.connected = false;
        if (callback) callback();
      }),
      send: jest.fn(),
      subscribe: jest.fn((topic, callback) => ({
        unsubscribe: jest.fn(),
      })),
      heartbeat: { outgoing: 0, incoming: 0 },
      debug: jest.fn(),
    };

    (StompJs.over as jest.Mock).mockReturnValue(mockStompClient);
  });

  describe("Basic Socket Actions", () => {
    it("should connect successfully", async () => {
      await socketService.connect();
      expect(mockStompClient.connect).toHaveBeenCalled();
      expect((socketService as any).connected).toBe(true);
    });

    it("should send message through the socket", async () => {
      await socketService.connect();
      const payload = { test: "data" };
      socketService.send("/app/test", payload);
      expect(mockStompClient.send).toHaveBeenCalledWith(
        "/app/test",
        {},
        JSON.stringify(payload)
      );
    });

    it("should handle subscriptions", async () => {
      await socketService.connect();
      const callback = jest.fn();
      socketService.subscribe("/topic/test", callback);
      expect(mockStompClient.subscribe).toHaveBeenCalledWith(
        "/topic/test",
        expect.any(Function)
      );
    });

    it("should unsubscribe when calling the returned function", async () => {
      await socketService.connect();
      const unsub = socketService.subscribe("/topic/unsub", () => {});
      unsub();
      expect((socketService as any).subscriptions.has("/topic/unsub")).toBe(
        false
      );
    });

    it("should disconnect and clear state", async () => {
      await socketService.connect();
      socketService.disconnect();
      expect(mockStompClient.disconnect).toHaveBeenCalled();
      expect((socketService as any).client).toBeNull();
    });
  });
});
