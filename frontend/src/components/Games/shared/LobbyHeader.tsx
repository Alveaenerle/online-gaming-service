import { motion } from "framer-motion";
import { Hash, Copy, Check, Lock, Globe } from "lucide-react";
import { useState } from "react";

type Props = {
  title: string;
  accessCode?: string;
  isPrivate?: boolean;
};

export function LobbyHeader({ title, accessCode, isPrivate }: Props) {
  const [copied, setCopied] = useState(false);

  const copyToClipboard = () => {
    if (!accessCode) return;
    navigator.clipboard.writeText(accessCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <motion.div
      className="flex flex-col items-center justify-center space-y-2 sm:space-y-3"
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="flex items-center gap-2 mb-1 flex-wrap justify-center">
        <div className="px-2 sm:px-3 py-1 rounded-full bg-purple-600/10 border border-purple-500/20 text-[8px] sm:text-[10px] font-black uppercase tracking-[0.2em] sm:tracking-[0.3em] text-purple-400">
          Waiting for players
        </div>

        {isPrivate !== undefined && (
          <div
            className={`px-2 sm:px-3 py-1 rounded-full border text-[8px] sm:text-[10px] font-black uppercase tracking-[0.15em] sm:tracking-[0.2em] flex items-center gap-1 sm:gap-2 ${
              isPrivate
                ? "bg-rose-500/10 border-rose-500/20 text-rose-400"
                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-400"
            }`}
          >
            {isPrivate ? <Lock size={10} className="sm:hidden" /> : <Globe size={10} className="sm:hidden" />}
            {isPrivate ? <Lock size={12} className="hidden sm:block" /> : <Globe size={12} className="hidden sm:block" />}
            {isPrivate ? "Private" : "Public"}
          </div>
        )}
      </div>

      <h1 className="text-2xl sm:text-3xl md:text-4xl lg:text-5xl font-black tracking-tighter bg-purple-600 bg-clip-text text-transparent drop-shadow-sm uppercase m-3 sm:m-4 lg:m-5 text-center">
        {title}
      </h1>

      {accessCode && (
        <div className="group relative mt-1 sm:mt-2">
          <button
            onClick={copyToClipboard}
            className="flex items-center gap-2 sm:gap-3 px-3 sm:px-4 md:px-6 py-2 bg-[#121018] border border-white/5 rounded-xl sm:rounded-2xl hover:border-purple-500/50 transition-all duration-300 min-h-[44px]"
          >
            <Hash size={14} className="text-purple-500 sm:hidden" />
            <Hash size={16} className="text-purple-500 hidden sm:block" />
            <span className="font-mono text-base sm:text-lg md:text-xl tracking-[0.15em] sm:tracking-[0.2em] font-bold text-gray-300">
              {accessCode}
            </span>
            <div className="w-[1px] h-3 sm:h-4 bg-white/10 mx-0.5 sm:mx-1" />
            {copied ? (
              <Check size={14} className="text-green-500 sm:hidden" />
            ) : (
              <Copy
                size={14}
                className="text-gray-500 group-hover:text-white transition-colors sm:hidden"
              />
            )}
            {copied ? (
              <Check size={16} className="text-green-500 hidden sm:block" />
            ) : (
              <Copy
                size={16}
                className="text-gray-500 group-hover:text-white transition-colors hidden sm:block"
              />
            )}
          </button>

          <div className="absolute inset-0 bg-purple-600/20 blur-xl opacity-0 group-hover:opacity-100 transition-opacity -z-10" />
        </div>
      )}
    </motion.div>
  );
}
