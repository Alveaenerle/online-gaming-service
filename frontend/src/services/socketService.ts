import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

const WS_URL = import.meta.env.VITE_MENU_WS_URL || "/api/menu/ws";

class SocketService {
  private client: StompJs.Client | null = null;
  private subscriptions: Map<string, StompJs.Subscription> = new Map();

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) return resolve();

      const socket = new SockJS(WS_URL);
      this.client = StompJs.over(socket);
      this.client.debug = () => {};

      this.client.connect(
        {},
        () => resolve(),
        (err) => reject(err)
      );
    });
  }

  subscribe(topic: string, callback: (payload: any) => void) {
    if (!this.client?.connected) return;

    if (this.subscriptions.has(topic)) return;

    const sub = this.client.subscribe(topic, (msg) => {
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

export const socketService = new SocketService();
