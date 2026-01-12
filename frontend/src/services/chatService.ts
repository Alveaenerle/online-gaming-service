import { socketService } from "./socketService";

export interface ChatMessage {
  id: string;
  senderId: string;
  senderUsername: string;
  senderAvatar: string;
  content: string;
  timestamp: string;
  isBlurred: boolean;
  type: "USER_MESSAGE" | "SYSTEM_MESSAGE";
}

export interface ChatHistoryResponse {
  messages: ChatMessage[];
  offset: number;
  limit: number;
  hasMore: boolean;
  totalMessages: number;
}

export interface ChatError {
  code: "RATE_LIMIT" | "NOT_IN_LOBBY" | "INVALID_MESSAGE";
  message: string;
  retryAfter?: number;
}

export interface TypingIndicator {
  userId: string;
  username: string;
  isTyping: boolean;
}

type ChatMessageHandler = (message: ChatMessage) => void;
type ChatErrorHandler = (error: ChatError) => void;
type ChatHistoryHandler = (history: ChatHistoryResponse) => void;
type TypingHandler = (indicator: TypingIndicator) => void;

class ChatService {
  private lobbyId: string | null = null;
  private messageHandlers: ChatMessageHandler[] = [];
  private errorHandlers: ChatErrorHandler[] = [];
  private historyHandlers: ChatHistoryHandler[] = [];
  private typingHandlers: TypingHandler[] = [];
  private subscribed = false;

  /**
   * Connect to chat for a specific lobby
   */
  async connectToLobby(lobbyId: string): Promise<void> {
    if (this.lobbyId === lobbyId && this.subscribed) {
      return;
    }

    // Disconnect from previous lobby if any
    if (this.lobbyId && this.lobbyId !== lobbyId) {
      this.disconnect();
    }

    this.lobbyId = lobbyId;
    
    try {
      await socketService.connect();
      
      // Subscribe to chat messages
      socketService.subscribe(`/topic/room/${lobbyId}/chat`, (message: ChatMessage) => {
        this.messageHandlers.forEach((handler) => handler(message));
      });

      // Subscribe to typing indicators
      socketService.subscribe(`/topic/room/${lobbyId}/typing`, (indicator: TypingIndicator) => {
        this.typingHandlers.forEach((handler) => handler(indicator));
      });

      // Subscribe to user-specific error queue
      socketService.subscribe("/user/queue/chat/error", (error: ChatError) => {
        this.errorHandlers.forEach((handler) => handler(error));
      });

      // Subscribe to user-specific history response
      socketService.subscribe("/user/queue/chat/history", (history: ChatHistoryResponse) => {
        this.historyHandlers.forEach((handler) => handler(history));
      });

      this.subscribed = true;
      console.log("[ChatService] Connected to lobby chat:", lobbyId);
    } catch (err) {
      console.error("[ChatService] Failed to connect:", err);
      throw err;
    }
  }

  /**
   * Send a chat message
   */
  sendMessage(content: string): void {
    if (!this.lobbyId || !socketService) {
      console.warn("[ChatService] Cannot send message - not connected to lobby");
      return;
    }

    socketService.send(`/app/chat/${this.lobbyId}/send`, { content });
  }

  /**
   * Send typing indicator
   */
  sendTypingIndicator(isTyping: boolean): void {
    if (!this.lobbyId || !socketService) return;

    socketService.send(`/app/chat/${this.lobbyId}/typing`, { isTyping });
  }

  /**
   * Request chat history
   */
  requestHistory(offset: number = 0, limit: number = 50): void {
    if (!this.lobbyId || !socketService) return;

    socketService.send(`/app/chat/${this.lobbyId}/history`, { offset, limit });
  }

  /**
   * Register message handler
   */
  onMessage(handler: ChatMessageHandler): () => void {
    this.messageHandlers.push(handler);
    return () => {
      this.messageHandlers = this.messageHandlers.filter((h) => h !== handler);
    };
  }

  /**
   * Register error handler
   */
  onError(handler: ChatErrorHandler): () => void {
    this.errorHandlers.push(handler);
    return () => {
      this.errorHandlers = this.errorHandlers.filter((h) => h !== handler);
    };
  }

  /**
   * Register history handler
   */
  onHistory(handler: ChatHistoryHandler): () => void {
    this.historyHandlers.push(handler);
    return () => {
      this.historyHandlers = this.historyHandlers.filter((h) => h !== handler);
    };
  }

  /**
   * Register typing handler
   */
  onTyping(handler: TypingHandler): () => void {
    this.typingHandlers.push(handler);
    return () => {
      this.typingHandlers = this.typingHandlers.filter((h) => h !== handler);
    };
  }

  /**
   * Disconnect from chat
   */
  disconnect(): void {
    if (this.lobbyId) {
      socketService.unsubscribe(`/topic/room/${this.lobbyId}/chat`);
      socketService.unsubscribe(`/topic/room/${this.lobbyId}/typing`);
      socketService.unsubscribe("/user/queue/chat/error");
      socketService.unsubscribe("/user/queue/chat/history");
    }
    this.lobbyId = null;
    this.subscribed = false;
    this.messageHandlers = [];
    this.errorHandlers = [];
    this.historyHandlers = [];
    this.typingHandlers = [];
  }

  isConnected(): boolean {
    return this.subscribed && this.lobbyId !== null;
  }
}

export const chatService = new ChatService();
