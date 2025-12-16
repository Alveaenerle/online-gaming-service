import React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Users, X } from "lucide-react";

interface Friend {
  id: number;
  name: string;
  status: "online" | "offline";
}

interface FriendsSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const mockFriends: Friend[] = [
  { id: 1, name: "MockFriend", status: "online" },
  { id: 2, name: "MockFriend", status: "online" },
  { id: 3, name: "MockFriend", status: "offline" },
  { id: 4, name: "MockFriend", status: "offline" },
];

export const FriendsSidebar: React.FC<FriendsSidebarProps> = ({
  isOpen,
  onClose,
}) => {
  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />

          <motion.aside
            initial={{ x: 400, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: 400, opacity: 0 }}
            transition={{ type: "spring", stiffness: 120, damping: 18 }}
            className="fixed right-6 top-6 bottom-6 z-50 w-[320px]
                       rounded-3xl border border-purple-500/20
                       bg-white/5 backdrop-blur-xl shadow-2xl shadow-purple-700/30
                       flex flex-col"
          >
            <div className="flex items-center justify-between px-6 py-4 border-b border-white/10">
              <div className="flex items-center gap-3 text-purple-400">
                <Users />
                <h3 className="font-bold text-lg">Friends</h3>
              </div>
              <button
                onClick={onClose}
                className="p-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition"
              >
                <X />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
              {mockFriends.map((friend) => (
                <motion.div
                  key={friend.id}
                  whileHover={{ scale: 1.03 }}
                  className="flex items-center justify-between rounded-xl
                             bg-white/5 px-4 py-3
                             border border-white/10
                             hover:border-purple-500/40 transition"
                >
                  <div>
                    <p className="font-semibold">{friend.name}</p>
                    <p
                      className={`text-xs ${
                        friend.status === "online"
                          ? "text-green-400"
                          : "text-gray-500"
                      }`}
                    >
                      {friend.status}
                    </p>
                  </div>

                  <span
                    className={`h-3 w-3 rounded-full ${
                      friend.status === "online"
                        ? "bg-green-400 shadow-[0_0_10px_rgba(34,197,94,0.8)]"
                        : "bg-gray-600"
                    }`}
                  />
                </motion.div>
              ))}
            </div>

            <div className="px-6 py-4 border-t border-white/10 text-center text-xs text-gray-400">
              Friends online:{" "}
              <span className="text-purple-400 font-semibold">
                {mockFriends.filter((f) => f.status === "online").length}
              </span>
            </div>
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
};
