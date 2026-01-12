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
      className="flex flex-col items-center justify-center space-y-3"
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <div className="flex items-center gap-2 mb-1">
        <div className="px-3 py-1 rounded-full bg-purple-600/10 border border-purple-500/20 text-[10px] font-black uppercase tracking-[0.3em] text-purple-400">
          Waiting for players
        </div>

        {isPrivate !== undefined && (
          <div
            className={`px-3 py-1 rounded-full border text-[10px] font-black uppercase tracking-[0.2em] flex items-center gap-2 ${
              isPrivate
                ? "bg-rose-500/10 border-rose-500/20 text-rose-400"
                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-400"
            }`}
          >
            {isPrivate ? <Lock size={12} /> : <Globe size={12} />}
            {isPrivate ? "Private" : "Public"}
          </div>
        )}
      </div>

      <h1 className="text-5xl font-black tracking-tighter bg-purple-600 bg-clip-text text-transparent drop-shadow-sm uppercase m-5">
        {title}
      </h1>

      {accessCode && (
        <div className="group relative mt-2">
          <button
            onClick={copyToClipboard}
            className="flex items-center gap-3 px-6 py-2 bg-[#121018] border border-white/5 rounded-2xl hover:border-purple-500/50 transition-all duration-300"
          >
            <Hash size={16} className="text-purple-500" />
            <span className="font-mono text-xl tracking-[0.2em] font-bold text-gray-300">
              {accessCode}
            </span>
            <div className="w-[1px] h-4 bg-white/10 mx-1" />
            {copied ? (
              <Check size={16} className="text-green-500" />
            ) : (
              <Copy
                size={16}
                className="text-gray-500 group-hover:text-white transition-colors"
              />
            )}
          </button>

          <div className="absolute inset-0 bg-purple-600/20 blur-xl opacity-0 group-hover:opacity-100 transition-opacity -z-10" />
        </div>
      )}
    </motion.div>
  );
}
