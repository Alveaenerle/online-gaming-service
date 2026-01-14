import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

// Build WebSocket URL - use env var or current origin to ensure correct protocol (http/https)
const getWsUrl = () => {
  if (import.meta.env.VITE_LUDO_WS_URL) {
    return import.meta.env.VITE_LUDO_WS_URL;
  }
  return `${window.location.origin}/api/ludo/ws`;
};

interface StompSubscription {
  topic: string;
  callback: (payload: any) => void;
  subscription: StompJs.Subscription | null;
}

class LudoSocketService {
  private client: StompJs.Client | null = null;
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
   * Connect to the Ludo WebSocket server
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

    this.connectPromise = new Promise((resolve) => {
      const attemptConnect = () => {
        try {
          const wsUrl = getWsUrl();
          console.log(`[LudoSocketService] Connecting to ${wsUrl}...`);
          const socket = new SockJS(wsUrl);
          this.client = StompJs.over(socket);

          // Configure heartbeat: 20s outgoing, 20s incoming
          this.client.heartbeat = { outgoing: 20000, incoming: 20000 };

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
              // Don't reject - let retry logic handle it
            }
          );
        } catch (err) {
          this.onError(err);
        }
      };

      attemptConnect();
    });

    return this.connectPromise;
  }

  private onConnect() {
    console.log("[LudoSocketService] Connected");
    this.connected = true;
    this.isConnecting = false;
    this.connectPromise = null;

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    this.resubscribeAll();
  }

  private onError(error: any) {
    console.error("[LudoSocketService] Connection error:", error);
    this.connected = false;
    this.isConnecting = false;

    if (!this.pendingDisconnect) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer || this.pendingDisconnect) return;

    console.log("[LudoSocketService] Scheduling reconnect in 3s...");
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connectPromise = null; // Clear to allow new connection attempt
      console.log("[LudoSocketService] Retrying connection...");
      this.connect();
    }, 3000);
  }

  private resubscribeAll() {
    if (!this.client || !this.connected) return;

    console.log(`[LudoSocketService] Resubscribing to ${this.subscriptions.size} topics...`);
    this.subscriptions.forEach((sub) => {
      if (sub.subscription) {
        try {
          sub.subscription.unsubscribe();
        } catch (e) {
          // Ignore
        }
      }

      try {
        const stompSub = this.client!.subscribe(sub.topic, (msg: StompJs.Message) => {
          if (msg.body) {
            try {
              sub.callback(JSON.parse(msg.body));
            } catch (e) {
              console.error("[LudoSocketService] JSON parse error", e);
            }
          }
        });
        sub.subscription = stompSub;
        console.log(`[LudoSocketService] Resubscribed to ${sub.topic}`);
      } catch (err) {
        console.error("[LudoSocketService] Resubscribe error", err);
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
      try {
        const stompSub = this.client.subscribe(topic, (msg: StompJs.Message) => {
          if (msg.body) {
            try {
              callback(JSON.parse(msg.body));
            } catch (e) {
              console.error("[LudoSocketService] JSON parse error", e);
            }
          }
        });
        subEntry.subscription = stompSub;
        console.log(`[LudoSocketService] Subscribed to ${topic}`);
      } catch (err) {
        console.error("[LudoSocketService] Subscribe error", err);
      }
    } else {
      // Trigger connection if not connected
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
      } catch (e) {
        // Ignore
      }
    }
    this.subscriptions.delete(topic);
  }

  isConnected(): boolean {
    return this.connected;
  }

  disconnect() {
    this.pendingDisconnect = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.client) {
      try {
        this.client.disconnect(() => {
          console.log("[LudoSocketService] Disconnected");
          this.connected = false;
          this.client = null;
        });
      } catch (e) {
        // Ignore
      }
    }
    this.subscriptions.clear();
    this.connectPromise = null;
  }
}

export const ludoSocketService = new LudoSocketService();
