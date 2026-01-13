import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

// Build WebSocket URL - use env var or current origin to ensure correct protocol (http/https)
const getWsUrl = () => {
  // Bezpieczne sprawdzenie czy import.meta oraz import.meta.env istnieją
  const envUrl =
    typeof import.meta !== "undefined" && import.meta.env
      ? import.meta.env.VITE_API_SOCIAL_WS_URL
      : null;

  if (envUrl) {
    return envUrl;
  }

  // Fallback do window.location
  return `${window.location.origin}/api/social/ws/presence`;
};
const WS_URL = getWsUrl();
const HEARTBEAT_INTERVAL_MS = 25000; // 25 seconds (TTL is 35s)

class SocialSocketService {
  private client: StompJs.Client | null = null;
  private subscriptions: Map<string, StompJs.Subscription> = new Map();
  private connectionPromise: Promise<void> | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;

  connect(): Promise<void> {
    if (this.client?.connected) return Promise.resolve();
    if (this.connectionPromise) return this.connectionPromise;

    this.connectionPromise = new Promise((resolve, reject) => {
      const socket = new SockJS(WS_URL);
      this.client = StompJs.over(socket);
      this.client.debug = () => {};

      this.client.connect(
        {},
        () => {
          console.log("[SocialSocket] Connected");
          this.connectionPromise = null;
          this.startHeartbeat();
          resolve();
        },
        (err) => {
          console.error("[SocialSocket] Connection failed", err);
          this.connectionPromise = null;
          reject(err);
        }
      );
    });

    return this.connectionPromise;
  }

  private startHeartbeat() {
    this.stopHeartbeat(); // Clear any existing interval
    console.log("[SocialSocket] Starting heartbeat");
    // Send immediate ping to set initial presence
    this.sendPing();
    // Then continue sending pings at regular intervals
    this.heartbeatInterval = setInterval(() => {
      this.sendPing();
    }, HEARTBEAT_INTERVAL_MS);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private sendPing() {
    if (this.client?.connected) {
      console.log("[SocialSocket] Sending presence ping");
      this.client.send("/app/presence.ping", {}, "");
    }
  }

  send(destination: string, body: any = {}) {
    if (this.client?.connected) {
      this.client.send(destination, {}, JSON.stringify(body));
    }
  }

  subscribe(topic: string, callback: (payload: any) => void) {
    if (!this.client?.connected) {
      console.warn(
        "[SocialSocket] Cannot subscribe - not connected. Topic:",
        topic
      );
      return;
    }

    if (this.subscriptions.has(topic)) {
      console.log("[SocialSocket] Already subscribed to", topic);
      return;
    }

    console.log("[SocialSocket] Subscribing to", topic);
    const sub = this.client.subscribe(topic, (msg) => {
      console.log("[SocialSocket] Received message on", topic, msg.body);
      callback(JSON.parse(msg.body));
    });
    this.subscriptions.set(topic, sub);
  }

  unsubscribe(topic: string) {
    const sub = this.subscriptions.get(topic);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(topic);
    }
  }

  disconnect() {
    this.stopHeartbeat();
    this.client?.disconnect(() => {
      this.subscriptions.clear();
      this.client = null;
    });
  }
}

export const socialSocketService = new SocialSocketService();
