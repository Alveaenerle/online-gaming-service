import { socialSocketService } from "./socialSocketService";
import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

jest.mock("sockjs-client");
jest.mock("stompjs");

describe("SocialSocketService", () => {
  let mockStompClient: any;

  beforeEach(() => {
    jest.useFakeTimers();
    jest.clearAllMocks();
    // Reset singleton
    (socialSocketService as any).client = null;
    (socialSocketService as any).connectionPromise = null;

    mockStompClient = {
      connect: jest.fn((headers, onConnect, onError) => {
        // success by default
        mockStompClient.connected = true;
        onConnect();
      }),
      disconnect: jest.fn((callback) => {
        mockStompClient.connected = false;
        callback?.();
      }),
      send: jest.fn(),
      subscribe: jest.fn((topic, callback) => {
        return { unsubscribe: jest.fn() };
      }),
      connected: false,
      debug: jest.fn(),
    };

    (StompJs.over as jest.Mock).mockReturnValue(mockStompClient);
  });

  afterEach(() => {
    socialSocketService.disconnect();
    jest.useRealTimers();
  });

  describe("connect()", () => {
    it("should establish a connection and start heartbeat", async () => {
      await socialSocketService.connect();
      expect(SockJS).toHaveBeenCalled();
      expect(mockStompClient.send).toHaveBeenCalledWith(
        "/app/presence.ping",
        {},
        ""
      );
    });

    it("should handle connection failure", async () => {
      const connectionError = "Socket error";
      mockStompClient.connect = jest.fn((headers, onConnect, onError) => {
        onError(connectionError);
      });

      await expect(socialSocketService.connect()).rejects.toBe(connectionError);
    });
  });

  describe("Messaging", () => {
    it("should subscribe to a topic and handle incoming messages", async () => {
      await socialSocketService.connect();
      const callback = jest.fn();

      socialSocketService.subscribe("/topic/test", callback);

      const messageHandler = mockStompClient.subscribe.mock.calls[0][1];
      messageHandler({ body: JSON.stringify({ data: "ok" }) });

      expect(callback).toHaveBeenCalledWith({ data: "ok" });
    });

    it("should unsubscribe from topic", async () => {
      await socialSocketService.connect();
      const unsubSpy = jest.fn();
      mockStompClient.subscribe.mockReturnValue({ unsubscribe: unsubSpy });

      socialSocketService.subscribe("/topic/unsub", () => {});
      socialSocketService.unsubscribe("/topic/unsub");

      expect(unsubSpy).toHaveBeenCalled();
    });
  });
});
