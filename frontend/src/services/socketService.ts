import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

// Build WebSocket URL - use env var or current origin to ensure correct protocol (http/https)
const getWsUrl = () => {
  if (import.meta.env.VITE_MENU_WS_URL) {
    return import.meta.env.VITE_MENU_WS_URL;
  }
  return `${window.location.origin}/api/menu/ws`;
};
const WS_URL = getWsUrl();

interface StompSubscription {
  topic: string;
  callback: (payload: any) => void;
  subscription: any | null;
}

class SocketService {
  private client: any | null = null;
  private connected: boolean = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private isConnecting: boolean = false;
  private connectPromise: Promise<void> | null = null;
  private pendingDisconnect: boolean = false;

  constructor() {
    this.connect = this.connect.bind(this);
    this.onConnect = this.onConnect.bind(this);
    this.onError = this.onError.bind(this);
  }

  /**
   * Connect to the WebSocket server
   * Returns a promise that resolves when connected
   */
  connect(): Promise<void> {
    this.pendingDisconnect = false;

    if (this.connected) {
      return Promise.resolve();
    }

    if (this.connectPromise) {
      return this.connectPromise;
    }

    this.isConnecting = true;

    this.connectPromise = new Promise((resolve, reject) => {
      const attemptConnect = () => {
        try {
          const socket = new SockJS(WS_URL);
          this.client = StompJs.over(socket);

          // Configure heartbeat: 20s outgoing, 20s incoming
          this.client.heartbeat.outgoing = 20000;
          this.client.heartbeat.incoming = 20000;

          // Disable debug logs
          this.client.debug = () => {};

          this.client.connect(
            {},
            () => {
              this.onConnect();
              resolve();
            },
            (error: any) => {
              this.onError(error);
              // Do not reject, let the retry logic handle it
              // The promise remains pending until eventual success
            }
          );
        } catch (err) {
          this.onError(err);
          // Retry on sync errors too
        }
      };

      attemptConnect();
    });

    return this.connectPromise;
  }

  private onConnect() {
    console.log("[SocketService] Connected");
    this.connected = true;
    this.isConnecting = false;

    // Keep connectPromise 'resolved' effectively by not clearing it?
    // Actually we should clear it so subsequent calls return immediate resolve
    this.connectPromise = null;

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    this.resubscribeAll();
  }

  private onError(error: any) {
    console.error("[SocketService] Connection error:", error);
    this.connected = false;
    this.isConnecting = false;
    // this.connectPromise = null; // Don't clear promise, so awaiters still wait for the retry

    if (!this.pendingDisconnect) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer || this.pendingDisconnect) return;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      console.log("[SocketService] Retrying connection...");
      this.connect();
    }, 5000);
  }

  private resubscribeAll() {
    if (!this.client || !this.connected) return;

    this.subscriptions.forEach((sub) => {
      if (sub.subscription) {
        try {
          sub.subscription.unsubscribe();
        } catch (e) {}
      }

      try {
        const stompSub = this.client.subscribe(sub.topic, (msg: any) => {
          if (msg.body) {
            try {
              sub.callback(JSON.parse(msg.body));
            } catch (e) {
              console.error("JSON Error", e);
            }
          }
        });
        sub.subscription = stompSub;
      } catch (err) {
        console.error("Resubscribe error", err);
      }
    });
  }

  subscribe(topic: string, callback: (payload: any) => void): () => void {
    const subEntry: StompSubscription = {
      topic,
      callback,
      subscription: null,
    };
    this.subscriptions.set(topic, subEntry);

    if (this.connected && this.client) {
      const stompSub = this.client.subscribe(topic, (msg: any) => {
        if (msg.body) {
          try {
            callback(JSON.parse(msg.body));
          } catch (e) {
            console.error(e);
          }
        }
      });
      subEntry.subscription = stompSub;
    } else {
      if (!this.isConnecting && !this.connected) {
        this.connect();
      }
    }

    return () => {
      this.unsubscribe(topic);
    };
  }

  unsubscribe(topic: string) {
    const entry = this.subscriptions.get(topic);
    if (entry?.subscription) {
      try {
        entry.subscription.unsubscribe();
      } catch (e) {}
    }
    this.subscriptions.delete(topic);
  }

  send(destination: string, body: object) {
    if (!this.client?.connected) {
      console.warn("[SocketService] Not connected. Message lost:", destination);
      return;
    }
    this.client.send(destination, {}, JSON.stringify(body));
  }

  disconnect() {
    this.pendingDisconnect = true;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);

    this.client?.disconnect(() => {
      this.connected = false;
      this.client = null;
    });
    this.subscriptions.clear();
  }
}

export const socketService = new SocketService();
