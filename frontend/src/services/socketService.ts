import SockJS from "sockjs-client";
import * as StompJs from "stompjs";

class SocketService {
  private client: StompJs.Client | null = null;
  private subscriptions: Map<string, StompJs.Subscription> = new Map();
  private currentEndpoint: string | null = null;

  connect(): Promise<void> {
    return this.connectTo("http://localhost/api/menu/ws");
  }

  connectMakao(): Promise<void> {
    return this.connectTo("http://localhost/api/makao/ws");
  }

  private connectTo(endpoint: string): Promise<void> {
    return new Promise((resolve, reject) => {
      // If already connected to the same endpoint, resolve immediately
      if (this.client?.connected && this.currentEndpoint === endpoint) {
        return resolve();
      }

      // If connected to different endpoint, disconnect first
      if (this.client?.connected && this.currentEndpoint !== endpoint) {
        this.client.disconnect(() => {
          this.subscriptions.clear();
          this.client = null;
          this.currentEndpoint = null;
          this.doConnect(endpoint, resolve, reject);
        });
      } else {
        this.doConnect(endpoint, resolve, reject);
      }
    });
  }

  private doConnect(endpoint: string, resolve: () => void, reject: (err: any) => void) {
    const socket = new SockJS(endpoint);
    this.client = StompJs.over(socket);
    this.client.debug = () => {};

    this.client.connect(
      {},
      () => {
        this.currentEndpoint = endpoint;
        resolve();
      },
      (err) => reject(err)
    );
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
