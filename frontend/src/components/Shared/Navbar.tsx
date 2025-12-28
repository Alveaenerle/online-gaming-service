import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { User, LogOut, ChevronDown } from "lucide-react";

const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    };

    if (dropdownOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [dropdownOpen]);

  const handleLogout = async () => {
    await logout();
    setDropdownOpen(false);
    navigate("/");
  };

  return (
    <header className="w-full py-4 px-6 md:px-12 flex items-center justify-between z-50 fixed top-0 left-0 bg-[#121018]/80 backdrop-blur-md">
      <motion.div
        initial={{ x: -20, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        transition={{ duration: 0.5 }}
      >
        <Link to="/" className="text-2xl font-bold tracking-wide flex items-center gap-3">
          <div className="w-10 h-10 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-neon flex items-center justify-center p-2 text-white font-bold">
            OG
          </div>
          <span>OnlineGames</span>
        </Link>
      </motion.div>

      <nav className="hidden md:flex gap-6 items-center opacity-90">
        <a className="hover:text-purple-300 transition-colors duration-300">
          Games
        </a>
        <a className="hover:text-purple-300 transition-colors duration-300">
          Rankings
        </a>

        {isAuthenticated && user ? (
          <div className="relative" ref={dropdownRef}>
            <motion.button
              whileHover={{ scale: 1.05 }}
              onClick={() => setDropdownOpen(!dropdownOpen)}
              aria-expanded={dropdownOpen}
              aria-haspopup="true"
              className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon transition-transform"
            >
              <User size={18} />
              <span className="font-medium">{user.username}</span>
              <ChevronDown
                size={16}
                className={`transition-transform ${dropdownOpen ? "rotate-180" : ""}`}
              />
            </motion.button>

            <AnimatePresence>
              {dropdownOpen && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -10 }}
                  className="absolute right-0 mt-2 w-48 bg-[#1a1a27] border border-white/10 rounded-xl shadow-lg overflow-hidden"
                >
                  <Link
                    to="/home"
                    onClick={() => setDropdownOpen(false)}
                    className="block px-4 py-3 hover:bg-white/5 transition-colors"
                  >
                    Dashboard
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-3 hover:bg-white/5 transition-colors flex items-center gap-2 text-red-400"
                  >
                    <LogOut size={16} />
                    Logout
                  </button>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        ) : (
          <>
            <Link to="/login">
              <motion.button
                whileHover={{ scale: 1.1 }}
                className="px-4 py-2 rounded-md border border-purpleEnd text-white transition-transform"
              >
                Login
              </motion.button>
            </Link>

            <Link to="/register">
              <motion.button
                whileHover={{ scale: 1.1 }}
                className="px-4 py-2 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon transition-transform"
              >
                Register
              </motion.button>
            </Link>
          </>
        )}
      </nav>
    </header>
  );
};

export default Navbar;
