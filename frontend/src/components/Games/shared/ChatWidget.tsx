import { useState, useEffect, useRef, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  MessageCircle,
  Send,
  X,
  Loader2,
  UserPlus,
  UserMinus,
  Clock,
} from "lucide-react";
import {
  chatService,
  ChatMessage,
  ChatError,
  TypingIndicator,
  ChatHistoryResponse,
} from "../../../services/chatService";
import { useAuth } from "../../../context/AuthContext";
import { useLobby } from "../../../context/LobbyContext";
import { useSocial } from "../../../context/SocialContext";
import { lobbyService } from "../../../services/lobbyService";
import { useToast } from "../../../context/ToastContext";

interface ChatWidgetProps {
  isHost?: boolean;
}

export function ChatWidget({ isHost = false }: ChatWidgetProps) {
  const { user } = useAuth();
  const { currentLobby } = useLobby();
  const { friends, sendFriendRequest } = useSocial();
  const { showToast } = useToast();

  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState("");
  const [typingUsers, setTypingUsers] = useState<Map<string, string>>(new Map());
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [hasMoreHistory, setHasMoreHistory] = useState(false);
  const [historyOffset, setHistoryOffset] = useState(0);
  const [rateLimitedUntil, setRateLimitedUntil] = useState<number | null>(null);
  const [cooldownRemaining, setCooldownRemaining] = useState(0);
  const [contextMenu, setContextMenu] = useState<{
    userId: string;
    username: string;
    x: number;
    y: number;
  } | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const typingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isTypingRef = useRef(false);
  const isOpenRef = useRef(isOpen);
  
  // Keep isOpenRef in sync with isOpen state
  useEffect(() => {
    isOpenRef.current = isOpen;
  }, [isOpen]);

  // Connect to chat when lobby changes
  useEffect(() => {
    if (!currentLobby?.id) return;

    chatService.connectToLobby(currentLobby.id).then(() => {
      // Request initial history
      chatService.requestHistory(0, 50);
    });

    return () => {
      chatService.disconnect();
    };
  }, [currentLobby?.id]);

  // Track seen message IDs to prevent duplicate unread count
  const seenMessageIdsRef = useRef<Set<string>>(new Set());

  // Handle incoming messages
  useEffect(() => {
    const unsubMessage = chatService.onMessage((message) => {
      // Check if we've already processed this message
      if (seenMessageIdsRef.current.has(message.id)) {
        return;
      }
      seenMessageIdsRef.current.add(message.id);
      
      setMessages((prev) => {
        // Prevent duplicate messages in state
        if (prev.some((m) => m.id === message.id)) {
          return prev;
        }
        return [...prev, message];
      });
      // Increment unread count when chat is closed
      if (!isOpenRef.current) {
        setUnreadCount((count) => count + 1);
      }
    });

    const unsubHistory = chatService.onHistory((history: ChatHistoryResponse) => {
      const incomingMessages = history.messages || [];
      setMessages((prev) => {
        // Prepend history messages (they're older)
        const newMessages = incomingMessages.filter(
          (m) => !prev.some((p) => p.id === m.id)
        );
        return [...newMessages, ...prev];
      });
      setHasMoreHistory(history.hasMore);
      setHistoryOffset((prev) => prev + incomingMessages.length);
      setIsLoadingHistory(false);
    });

    const unsubError = chatService.onError((error: ChatError) => {
      if (error.code === "RATE_LIMIT" && error.retryAfter) {
        const until = Date.now() + error.retryAfter;
        setRateLimitedUntil(until);
        setCooldownRemaining(Math.ceil(error.retryAfter / 1000));
      } else {
        showToast(error.message, "error");
      }
    });

    const unsubTyping = chatService.onTyping((indicator: TypingIndicator) => {
      if (indicator.userId === user?.id) return;

      setTypingUsers((prev) => {
        const next = new Map(prev);
        if (indicator.isTyping) {
          next.set(indicator.userId, indicator.username);
        } else {
          next.delete(indicator.userId);
        }
        return next;
      });
    });

    return () => {
      unsubMessage();
      unsubHistory();
      unsubError();
      unsubTyping();
    };
  }, [showToast, user?.id]);

  // Cooldown timer
  useEffect(() => {
    if (!rateLimitedUntil) return;

    const interval = setInterval(() => {
      const remaining = Math.max(0, Math.ceil((rateLimitedUntil - Date.now()) / 1000));
      setCooldownRemaining(remaining);
      if (remaining === 0) {
        setRateLimitedUntil(null);
      }
    }, 100);

    return () => clearInterval(interval);
  }, [rateLimitedUntil]);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (isOpen && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, isOpen]);

  // Clear unread when opening
  useEffect(() => {
    if (isOpen) {
      setUnreadCount(0);
    }
  }, [isOpen]);

  const handleScroll = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container || isLoadingHistory || !hasMoreHistory) return;

    // Load more when scrolled to top
    if (container.scrollTop < 50) {
      setIsLoadingHistory(true);
      chatService.requestHistory(historyOffset, 50);
    }
  }, [isLoadingHistory, hasMoreHistory, historyOffset]);

  const handleSend = () => {
    if (!inputValue.trim() || rateLimitedUntil) return;

    chatService.sendMessage(inputValue.trim());
    setInputValue("");

    // Stop typing indicator
    if (isTypingRef.current) {
      isTypingRef.current = false;
      chatService.sendTypingIndicator(false);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputValue(e.target.value);

    // Send typing indicator
    if (!isTypingRef.current && e.target.value.trim()) {
      isTypingRef.current = true;
      chatService.sendTypingIndicator(true);
    }

    // Clear previous timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    // Stop typing after 2 seconds of inactivity
    typingTimeoutRef.current = setTimeout(() => {
      if (isTypingRef.current) {
        isTypingRef.current = false;
        chatService.sendTypingIndicator(false);
      }
    }, 2000);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleAvatarClick = (
    e: React.MouseEvent,
    userId: string,
    username: string
  ) => {
    if (userId === user?.id || userId === "SYSTEM") return;

    e.preventDefault();
    setContextMenu({
      userId,
      username,
      x: e.clientX,
      y: e.clientY,
    });
  };

  const handleAddFriend = async () => {
    if (!contextMenu) return;
    await sendFriendRequest(contextMenu.userId);
    setContextMenu(null);
  };

  const handleKick = async () => {
    if (!contextMenu) return;
    try {
      await lobbyService.kickPlayer(contextMenu.userId);
      showToast(`${contextMenu.username} has been kicked`, "info");
    } catch {
      showToast("Failed to kick player", "error");
    }
    setContextMenu(null);
  };

  const isFriend = (userId: string) => friends.some((f) => f.id === userId);

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  };

  return (
    <>
      {/* Chat Toggle Button */}
      <motion.button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-24 right-14 z-40 w-14 h-14 rounded-full 
                   bg-gradient-to-br from-purple-600 to-pink-600 
                   shadow-lg shadow-purple-500/30 
                   flex items-center justify-center
                   hover:scale-110 transition-transform"
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.95 }}
      >
        <MessageCircle className="w-6 h-6 text-white" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-5 h-5 bg-red-500 rounded-full text-xs flex items-center justify-center text-white font-bold">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </motion.button>

      {/* Chat Panel */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            className="fixed bottom-44 right-14 z-50 w-80 h-[28rem] 
                       bg-black/80 backdrop-blur-xl 
                       border border-purple-500/30 rounded-2xl 
                       shadow-2xl shadow-purple-500/20
                       flex flex-col overflow-hidden"
          >
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
              <h3 className="text-white font-semibold flex items-center gap-2">
                <MessageCircle className="w-4 h-4 text-purple-400" />
                Lobby Chat
              </h3>
              <button
                onClick={() => setIsOpen(false)}
                className="text-gray-400 hover:text-white transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Messages */}
            <div
              ref={messagesContainerRef}
              onScroll={handleScroll}
              className="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar"
            >
              {isLoadingHistory && (
                <div className="flex justify-center py-2">
                  <Loader2 className="w-5 h-5 text-purple-400 animate-spin" />
                </div>
              )}

              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex gap-2 ${
                    msg.senderId === user?.id ? "flex-row-reverse" : ""
                  }`}
                >
                  {msg.type === "SYSTEM_MESSAGE" ? (
                    <div className="w-full text-center text-xs text-gray-500 py-1">
                      {msg.content}
                    </div>
                  ) : (
                    <>
                      <button
                        onClick={(e) =>
                          handleAvatarClick(e, msg.senderId, msg.senderUsername)
                        }
                        className="flex-shrink-0 focus:outline-none"
                        disabled={msg.senderId === user?.id}
                      >
                        <img
                          src={msg.senderAvatar || "/avatars/avatar_1.png"}
                          alt=""
                          className={`w-8 h-8 rounded-full border-2 ${
                            msg.senderId === user?.id
                              ? "border-purple-500"
                              : "border-white/20 hover:border-purple-400 transition-colors cursor-pointer"
                          }`}
                        />
                      </button>
                      <div
                        className={`max-w-[70%] ${
                          msg.senderId === user?.id ? "text-right" : ""
                        }`}
                      >
                        <div className="flex items-center gap-2 mb-0.5">
                          <span className="text-xs text-gray-400">
                            {msg.senderUsername}
                          </span>
                          <span className="text-[10px] text-gray-600">
                            {formatTime(msg.timestamp)}
                          </span>
                        </div>
                        <div
                          className={`rounded-2xl px-3 py-2 text-sm ${
                            msg.senderId === user?.id
                              ? "bg-purple-600 text-white rounded-br-sm"
                              : "bg-white/10 text-gray-200 rounded-bl-sm"
                          } ${msg.isBlurred ? "blur-sm hover:blur-none transition-all cursor-pointer" : ""}`}
                          title={msg.isBlurred ? "Click to reveal" : undefined}
                        >
                          {msg.content}
                        </div>
                      </div>
                    </>
                  )}
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {/* Typing Indicator */}
            <AnimatePresence>
              {typingUsers.size > 0 && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  className="px-4 py-1 text-xs text-gray-400"
                >
                  {Array.from(typingUsers.values()).join(", ")}{" "}
                  {typingUsers.size === 1 ? "is" : "are"} typing
                  <span className="inline-flex ml-1">
                    <motion.span
                      animate={{ opacity: [0, 1, 0] }}
                      transition={{ repeat: Infinity, duration: 1.5 }}
                    >
                      .
                    </motion.span>
                    <motion.span
                      animate={{ opacity: [0, 1, 0] }}
                      transition={{ repeat: Infinity, duration: 1.5, delay: 0.2 }}
                    >
                      .
                    </motion.span>
                    <motion.span
                      animate={{ opacity: [0, 1, 0] }}
                      transition={{ repeat: Infinity, duration: 1.5, delay: 0.4 }}
                    >
                      .
                    </motion.span>
                  </span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Input Area */}
            <div className="p-3 border-t border-white/10">
              <div className="flex gap-2">
                <input
                  type="text"
                  value={inputValue}
                  onChange={handleInputChange}
                  onKeyDown={handleKeyDown}
                  placeholder={
                    rateLimitedUntil ? "Slow down..." : "Type a message..."
                  }
                  disabled={!!rateLimitedUntil}
                  className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-2 
                             text-sm text-white placeholder-gray-500
                             focus:outline-none focus:border-purple-500/50 focus:ring-1 focus:ring-purple-500/30
                             disabled:opacity-50 disabled:cursor-not-allowed"
                  maxLength={500}
                />
                <button
                  onClick={handleSend}
                  disabled={!inputValue.trim() || !!rateLimitedUntil}
                  className="relative w-10 h-10 rounded-xl bg-purple-600 
                             flex items-center justify-center
                             hover:bg-purple-500 transition-colors
                             disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {rateLimitedUntil ? (
                    <div className="relative">
                      <Clock className="w-4 h-4 text-white" />
                      <span className="absolute -top-1 -right-1 text-[10px] text-white font-bold">
                        {cooldownRemaining}
                      </span>
                    </div>
                  ) : (
                    <Send className="w-4 h-4 text-white" />
                  )}
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Context Menu */}
      <AnimatePresence>
        {contextMenu && (
          <>
            <div
              className="fixed inset-0 z-[60]"
              onClick={() => setContextMenu(null)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="fixed z-[70] bg-black/90 border border-purple-500/30 rounded-xl 
                         shadow-xl shadow-purple-500/20 py-1 min-w-[150px]"
              style={{ left: contextMenu.x, top: contextMenu.y }}
            >
              <div className="px-3 py-2 text-sm text-gray-400 border-b border-white/10">
                {contextMenu.username}
              </div>
              {/* Hide Add Friend if: current user is guest, or target is a guest, or already friends */}
              {!user?.isGuest && !contextMenu.username.startsWith("Guest_") && !isFriend(contextMenu.userId) && (
                <button
                  onClick={handleAddFriend}
                  className="w-full px-3 py-2 text-left text-sm text-white 
                             hover:bg-purple-500/20 flex items-center gap-2"
                >
                  <UserPlus className="w-4 h-4" />
                  Add Friend
                </button>
              )}
              {isHost && (
                <button
                  onClick={handleKick}
                  className="w-full px-3 py-2 text-left text-sm text-red-400 
                             hover:bg-red-500/20 flex items-center gap-2"
                >
                  <UserMinus className="w-4 h-4" />
                  Kick Player
                </button>
              )}
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
