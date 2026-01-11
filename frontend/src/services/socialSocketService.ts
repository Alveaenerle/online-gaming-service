import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

const WS_URL = import.meta.env.VITE_API_SOCIAL_WS_URL || "/api/social/ws/presence";

class SocialSocketService {
  private client: StompJs.Client | null = null;
  private subscriptions: Map<string, StompJs.Subscription> = new Map();
  private connectionPromise: Promise<void> | null = null;

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
          console.log('[SocialSocket] Connected');
          this.connectionPromise = null;
          resolve();
        },
        (err) => {
          console.error('[SocialSocket] Connection failed', err);
          this.connectionPromise = null;
          reject(err);
        }
      );
    });

    return this.connectionPromise;
  }

  subscribe(topic: string, callback: (payload: any) => void) {
    if (!this.client?.connected) {
      console.warn('[SocialSocket] Cannot subscribe - not connected. Topic:', topic);
      return;
    }

    if (this.subscriptions.has(topic)) {
      console.log('[SocialSocket] Already subscribed to', topic);
      return;
    }

    console.log('[SocialSocket] Subscribing to', topic);
    const sub = this.client.subscribe(topic, (msg) => {
      console.log('[SocialSocket] Received message on', topic, msg.body);
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
    this.client?.disconnect(() => {
      this.subscriptions.clear();
      this.client = null;
    });
  }
}

export const socialSocketService = new SocialSocketService();
