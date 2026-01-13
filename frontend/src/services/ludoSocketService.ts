import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

class LudoSocketService {
  private client: StompJs.Client | null = null;
  private subscriptions: Map<string, StompJs.Subscription> = new Map();

  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) return resolve();

      const socket = new SockJS("http://localhost/api/ludo/ws");
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
      console.log(`Received message on topic ${topic}:`, msg.body);
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

export const ludoSocketService = new LudoSocketService();
