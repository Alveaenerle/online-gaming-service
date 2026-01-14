import React, { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { User, LogOut, ChevronDown, Menu, X } from "lucide-react";

const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
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

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [navigate]);

  // Prevent body scroll when mobile menu is open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [mobileMenuOpen]);

  const handleLogout = async () => {
    await logout();
    setDropdownOpen(false);
    setMobileMenuOpen(false);
    navigate("/");
  };

  const closeMobileMenu = () => setMobileMenuOpen(false);

  return (
    <>
      <header className="w-full max-w-[100vw] py-3 sm:py-4 px-4 sm:px-6 md:px-12 flex items-center justify-between z-50 fixed top-0 left-0 right-0 bg-[#121018]/80 backdrop-blur-md">
        <motion.div
          initial={{ x: -20, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          transition={{ duration: 0.5 }}
        >
          <Link to="/" className="text-xl sm:text-2xl font-bold tracking-wide flex items-center gap-2 sm:gap-3">
            <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-neon flex items-center justify-center p-1.5 sm:p-2 text-white font-bold text-sm sm:text-base">
              OG
            </div>
            <span className="hidden xs:inline">OnlineGames</span>
          </Link>
        </motion.div>

        {/* Desktop Navigation */}
        <nav className="hidden md:flex gap-4 lg:gap-6 items-center opacity-90">
          <Link to="/games" className="hover:text-purple-300 transition-colors duration-300">
            Games
          </Link>
          <Link to="/rankings" className="hover:text-purple-300 transition-colors duration-300">
            Rankings
          </Link>

          {isAuthenticated && user ? (
            <div className="relative" ref={dropdownRef}>
              <motion.button
                whileHover={{ scale: 1.05 }}
                onClick={() => setDropdownOpen(!dropdownOpen)}
                aria-expanded={dropdownOpen}
                aria-haspopup="true"
                className="flex items-center gap-2 px-3 lg:px-4 py-2 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon transition-transform"
              >
                <User size={18} />
                <span className="font-medium max-w-[100px] truncate">{user.username}</span>
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
                    className="absolute right-0 mt-2 w-48 bg-[#1a1a27] border border-white/10 rounded-xl shadow-lg overflow-hidden z-[100]"
                  >
                    <Link
                      to="/dashboard"
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
                  className="px-3 lg:px-4 py-2 rounded-md border border-purpleEnd text-white transition-transform"
                >
                  Login
                </motion.button>
              </Link>

              <Link to="/register">
                <motion.button
                  whileHover={{ scale: 1.1 }}
                  className="px-3 lg:px-4 py-2 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd text-white shadow-neon transition-transform"
                >
                  Register
                </motion.button>
              </Link>
            </>
          )}
        </nav>

        {/* Mobile Menu Button */}
        <button
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          className="md:hidden p-2 rounded-lg hover:bg-white/10 transition-colors min-w-[44px] min-h-[44px] flex items-center justify-center"
          aria-label="Toggle menu"
        >
          {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </header>

      {/* Mobile Menu Overlay */}
      <AnimatePresence>
        {mobileMenuOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40 md:hidden"
              onClick={closeMobileMenu}
            />
            <motion.nav
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
              className="fixed top-0 right-0 h-full w-[280px] max-w-[85vw] bg-[#121018] border-l border-white/10 z-50 md:hidden flex flex-col"
            >
              <div className="flex items-center justify-between p-4 border-b border-white/10">
                <span className="font-bold text-lg">Menu</span>
                <button
                  onClick={closeMobileMenu}
                  className="p-2 rounded-lg hover:bg-white/10 transition-colors min-w-[44px] min-h-[44px] flex items-center justify-center"
                >
                  <X size={24} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto py-4">
                <div className="space-y-1 px-4">
                  <Link
                    to="/games"
                    onClick={closeMobileMenu}
                    className="block py-3 px-4 rounded-xl hover:bg-white/5 transition-colors min-h-[44px]"
                  >
                    Games
                  </Link>
                  <Link
                    to="/rankings"
                    onClick={closeMobileMenu}
                    className="block py-3 px-4 rounded-xl hover:bg-white/5 transition-colors min-h-[44px]"
                  >
                    Rankings
                  </Link>

                  {isAuthenticated && user && (
                    <>
                      <div className="h-px bg-white/10 my-4" />
                      <div className="flex items-center gap-3 py-3 px-4">
                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purpleStart to-purpleEnd flex items-center justify-center">
                          <User size={20} />
                        </div>
                        <span className="font-medium truncate">{user.username}</span>
                      </div>
                      <Link
                        to="/home"
                        onClick={closeMobileMenu}
                        className="block py-3 px-4 rounded-xl hover:bg-white/5 transition-colors min-h-[44px]"
                      >
                        Home
                      </Link>
                      <Link
                        to="/dashboard"
                        onClick={closeMobileMenu}
                        className="block py-3 px-4 rounded-xl hover:bg-white/5 transition-colors min-h-[44px]"
                      >
                        Dashboard
                      </Link>
                    </>
                  )}
                </div>
              </div>

              <div className="p-4 border-t border-white/10 space-y-3">
                {isAuthenticated && user ? (
                  <button
                    onClick={handleLogout}
                    className="w-full py-3 px-4 rounded-xl bg-red-500/20 text-red-400 font-medium flex items-center justify-center gap-2 min-h-[44px]"
                  >
                    <LogOut size={18} />
                    Logout
                  </button>
                ) : (
                  <>
                    <Link to="/login" onClick={closeMobileMenu} className="block">
                      <button className="w-full py-3 px-4 rounded-xl border border-purpleEnd text-white font-medium min-h-[44px]">
                        Login
                      </button>
                    </Link>
                    <Link to="/register" onClick={closeMobileMenu} className="block">
                      <button className="w-full py-3 px-4 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-medium shadow-neon min-h-[44px]">
                        Register
                      </button>
                    </Link>
                  </>
                )}
              </div>
            </motion.nav>
          </>
        )}
      </AnimatePresence>
    </>
  );
};

export default Navbar;
