import { motion, AnimatePresence } from "framer-motion";

interface StepperProps {
  value: number;
  min?: number;
  max?: number;
  onChange: (val: number) => void;
  label: string;
}

export function GameStepper({
  value,
  min,
  max,
  onChange,
  label,
}: StepperProps) {
  return (
    <div className="space-y-2 sm:space-y-3">
      <label className="text-[10px] font-black text-gray-500 uppercase tracking-[0.2em] ml-1">
        {label}
      </label>
      <div className="flex items-center justify-between bg-white/5 border border-white/10 rounded-xl sm:rounded-[1.25rem] lg:rounded-[1.5rem] p-1.5 sm:p-2 h-16 sm:h-20">
        <button
          onClick={() => onChange(Math.max(min || 1, value - 1))}
          className="w-14 sm:w-16 lg:w-20 h-full flex items-center justify-center rounded-lg sm:rounded-xl bg-white/5 hover:bg-white/10 text-2xl sm:text-3xl font-light transition-all active:scale-90 min-h-[44px]"
        >
          −
        </button>
        <div className="flex flex-col items-center">
          <AnimatePresence mode="wait">
            <motion.span
              key={value}
              initial={{ y: 10, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              exit={{ y: -10, opacity: 0 }}
              className="text-2xl sm:text-3xl lg:text-4xl font-black text-white"
            >
              {value}
            </motion.span>
          </AnimatePresence>
          <span className="text-[8px] sm:text-[10px] text-gray-500 uppercase font-black tracking-tighter">
            Players
          </span>
        </div>
        <button
          onClick={() => onChange(Math.min(max || 8, value + 1))}
          className="w-14 sm:w-16 lg:w-20 h-full flex items-center justify-center rounded-lg sm:rounded-xl bg-purple-600/20 hover:bg-purple-600/40 text-purple-400 text-2xl sm:text-3xl font-light transition-all active:scale-90 min-h-[44px]"
        >
          +
        </button>
      </div>
    </div>
  );
}
