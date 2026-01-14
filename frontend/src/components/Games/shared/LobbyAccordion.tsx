import { useState, type ReactNode } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ChevronDown, PlusCircle, Users } from "lucide-react";

type AccordionSection = "create" | "join" | null;

interface LobbyAccordionProps {
  createContent: ReactNode;
  joinContent: ReactNode;
}

interface AccordionHeaderProps {
  icon: ReactNode;
  title: string;
  isExpanded: boolean;
  onClick: () => void;
}

function AccordionHeader({ icon, title, isExpanded, onClick }: AccordionHeaderProps) {
  return (
    <button
      onClick={onClick}
      className="w-full bg-[#121018] rounded-2xl lg:rounded-[2rem] border border-white/5 p-4 lg:p-6 flex items-center justify-between shadow-xl hover:bg-[#16131e] transition-all group"
    >
      <div className="flex items-center gap-4">
        <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center flex-shrink-0 group-hover:bg-purple-600/30 transition-colors">
          {icon}
        </div>
        <h2 className="text-xl lg:text-2xl font-bold">{title}</h2>
      </div>
      <motion.div
        animate={{ rotate: isExpanded ? 180 : 0 }}
        transition={{ duration: 0.2 }}
        className="text-gray-400 group-hover:text-white transition-colors"
      >
        <ChevronDown size={24} />
      </motion.div>
    </button>
  );
}

export function LobbyAccordion({ createContent, joinContent }: LobbyAccordionProps) {
  const [expanded, setExpanded] = useState<AccordionSection>(null);

  const handleToggle = (section: AccordionSection) => {
    setExpanded(prev => prev === section ? null : section);
  };

  return (
    <div className="space-y-3">
      {/* Create Lobby Section */}
      <div>
        <AccordionHeader
          icon={<PlusCircle size={24} />}
          title="New Lobby"
          isExpanded={expanded === "create"}
          onClick={() => handleToggle("create")}
        />
        <AnimatePresence initial={false}>
          {expanded === "create" && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.3, ease: "easeInOut" }}
              className="overflow-hidden"
            >
              <div className="pt-3">
                {createContent}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Join Lobby Section */}
      <div>
        <AccordionHeader
          icon={<Users size={24} />}
          title="Quick Join"
          isExpanded={expanded === "join"}
          onClick={() => handleToggle("join")}
        />
        <AnimatePresence initial={false}>
          {expanded === "join" && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.3, ease: "easeInOut" }}
              className="overflow-hidden"
            >
              <div className="pt-3">
                {joinContent}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
