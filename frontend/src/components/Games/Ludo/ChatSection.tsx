import { MessageSquare, Send } from "lucide-react";

interface ChatSectionProps {
  message: string;
  onMessageChange: (val: string) => void;
}

export function ChatSection({ message, onMessageChange }: ChatSectionProps) {
  return (
    <div className="flex-1 p-8 flex flex-col min-h-0">
      <div className="flex items-center gap-2 mb-4">
        <MessageSquare size={14} className="text-purple-500" />
        <h3 className="text-white text-[10px] font-black uppercase tracking-[0.2em]">
          Chat
        </h3>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 pr-2 mb-6 scrollbar-hide">
        <ChatMessage user="Player BLUE" text="Ready to deploy!" color="blue" />
        <ChatMessage
          user="You"
          text="Initiating Red Protocol."
          color="purple"
          isOwn
        />
      </div>

      <div className="relative">
        <input
          type="text"
          value={message}
          onChange={(e) => onMessageChange(e.target.value)}
          placeholder="Send message..."
          className="w-full bg-white/5 border border-white/10 rounded-2xl py-3.5 pl-5 pr-14 text-[12px] text-white focus:outline-none focus:border-purple-500/50 transition-all"
        />
        <button className="absolute right-3 top-1/2 -translate-y-1/2 p-2 bg-purple-500/10 hover:bg-purple-500 text-purple-500 hover:text-white rounded-xl transition-all">
          <Send size={14} />
        </button>
      </div>
    </div>
  );
}

function ChatMessage({
  user,
  text,
  color: _color,
  isOwn,
}: {
  user: string;
  text: string;
  color: string;
  isOwn?: boolean;
}) {
  return (
    <div className={`flex flex-col gap-1.5 ${isOwn ? "items-end" : ""}`}>
      <span
        className={`text-[9px] font-black uppercase tracking-widest ${
          isOwn ? "text-purple-400 mr-1" : "text-blue-400 ml-1"
        }`}
      >
        {user}
      </span>
      <div
        className={`${
          isOwn
            ? "bg-purple-600/20 border-purple-500/20 rounded-tr-none"
            : "bg-white/5 border-white/5 rounded-tl-none"
        } p-3 rounded-2xl border text-[12px] text-white max-w-[85%]`}
      >
        {text}
      </div>
    </div>
  );
}
