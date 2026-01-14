import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import {
  User,
  Mail,
  Calendar,
  Trophy,
  Gamepad2,
  Percent,
  Lock,
  Edit3,
  AlertCircle,
  Check,
  Spade,
  Dice5,
} from "lucide-react";
import Navbar from "../Shared/Navbar";
import { BackgroundGradient } from "../Shared/BackgroundGradient";
import { SocialCenter } from "../Shared/SocialCenter";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { authService } from "../../services/authService";
import { statisticsService } from "../../services/statisticsService";

interface GameStats {
  gamesPlayed: number;
  gamesWon: number;
  winRatio: string;
}

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.5,
      ease: [0.16, 1, 0.3, 1] as const,
    },
  },
};

interface StatCardProps {
  icon: React.ElementType;
  label: string;
  value: string | number;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ icon: Icon, label, value, color }) => (
  <motion.div
    variants={itemVariants}
    className="bg-[#121018] rounded-xl sm:rounded-2xl border border-white/5 p-3 sm:p-4 md:p-6 hover:border-white/10 transition-colors"
  >
    <div className="flex items-center gap-3 sm:gap-4">
      <div className={`w-10 h-10 sm:w-12 sm:h-12 rounded-lg sm:rounded-xl ${color} flex items-center justify-center flex-shrink-0`}>
        <Icon size={20} className="text-white sm:w-6 sm:h-6" />
      </div>
      <div className="min-w-0">
        <p className="text-gray-500 text-[10px] sm:text-xs font-semibold uppercase tracking-wider truncate">{label}</p>
        <p className="text-lg sm:text-xl md:text-2xl font-black text-white">{value}</p>
      </div>
    </div>
  </motion.div>
);

interface InfoRowProps {
  icon: React.ElementType;
  label: string;
  value: string;
}

const InfoRow: React.FC<InfoRowProps> = ({ icon: Icon, label, value }) => (
  <div className="flex items-center gap-3 sm:gap-4 py-2 sm:py-3 border-b border-white/5 last:border-0">
    <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-purple-500/10 flex items-center justify-center flex-shrink-0">
      <Icon size={16} className="text-purple-400 sm:w-[18px] sm:h-[18px]" />
    </div>
    <div className="flex-1 min-w-0">
      <p className="text-gray-500 text-[10px] sm:text-xs font-semibold uppercase tracking-wider">{label}</p>
      <p className="text-white font-medium text-sm sm:text-base truncate">{value}</p>
    </div>
  </div>
);

const Dashboard: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const { showToast } = useToast();
  const isGuest = user?.isGuest ?? false;

  const [newUsername, setNewUsername] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isUpdatingUsername, setIsUpdatingUsername] = useState(false);
  const [isUpdatingPassword, setIsUpdatingPassword] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(null);

  // Statistics state
  const [statsMakao, setStatsMakao] = useState<GameStats>({
    gamesPlayed: 0,
    gamesWon: 0,
    winRatio: "0%",
  });
  const [statsLudo, setStatsLudo] = useState<GameStats>({
    gamesPlayed: 0,
    gamesWon: 0,
    winRatio: "0%",
  });
  const [statsLoading, setStatsLoading] = useState(true);

  // Calculate win ratio helper
  const calculateWinRatio = (played: number, won: number): string => {
    if (played === 0) return "0%";
    return `${Math.round((won / played) * 100)}%`;
  };

  // Fetch statistics on mount
  useEffect(() => {
    const fetchStatistics = async () => {
      if (!user) return;
      
      setStatsLoading(true);
      try {
        const [makaoStats, ludoStats] = await Promise.all([
          statisticsService.getMyStatistics("MAKAO").catch(() => null),
          statisticsService.getMyStatistics("LUDO").catch(() => null),
        ]);

        if (makaoStats) {
          setStatsMakao({
            gamesPlayed: makaoStats.gamesPlayed,
            gamesWon: makaoStats.gamesWon,
            winRatio: calculateWinRatio(makaoStats.gamesPlayed, makaoStats.gamesWon),
          });
        }

        if (ludoStats) {
          setStatsLudo({
            gamesPlayed: ludoStats.gamesPlayed,
            gamesWon: ludoStats.gamesWon,
            winRatio: calculateWinRatio(ludoStats.gamesPlayed, ludoStats.gamesWon),
          });
        }
      } catch (error) {
        console.error("Failed to fetch statistics:", error);
      } finally {
        setStatsLoading(false);
      }
    };

    fetchStatistics();
  }, [user]);

  // Fetch user email on mount
  useEffect(() => {
    const fetchEmail = async () => {
      if (!isGuest && user) {
        try {
          const email = await authService.getUserEmail();
          setUserEmail(email);
        } catch (error) {
          console.error("Failed to fetch email:", error);
        }
      }
    };
    fetchEmail();
  }, [isGuest, user]);

  const handleUpdateUsername = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUsername.trim()) {
      showToast("Please enter a new username", "error");
      return;
    }
    if (newUsername.length < 3 || newUsername.length > 20) {
      showToast("Username must be between 3 and 20 characters", "error");
      return;
    }

    setIsUpdatingUsername(true);
    try {
      await authService.updateUsername(newUsername.trim());
      showToast("Username updated successfully!", "success");
      setNewUsername("");
      // Refresh the page to get updated user info
      window.location.reload();
    } catch (error: any) {
      showToast(error.message || "Failed to update username", "error");
    } finally {
      setIsUpdatingUsername(false);
    }
  };

  const handleUpdatePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      showToast("Please fill in all password fields", "error");
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast("New passwords do not match", "error");
      return;
    }
    if (newPassword.length < 6) {
      showToast("New password must be at least 6 characters", "error");
      return;
    }

    setIsUpdatingPassword(true);
    try {
      await authService.updatePassword(currentPassword, newPassword);
      showToast("Password updated successfully! Logging out...", "success");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      // Log out after successful password change
      setTimeout(async () => {
        await logout();
      }, 3000);
    } catch (error: any) {
      showToast(error.message || "Failed to update password", "error");
    } finally {
      setIsUpdatingPassword(false);
    }
  };

  // Format joining date (mock for now since we don't have createdAt in User model)
  const joiningDate = "January 2026";

  return (
    <div className="min-h-screen bg-[#050508] text-white overflow-x-hidden font-sans">
      <Navbar />
      <BackgroundGradient />

      <main className="relative z-10 pt-20 sm:pt-24 lg:pt-32 pb-10 sm:pb-16 lg:pb-20 px-3 sm:px-4 md:px-6 max-w-6xl mx-auto">
        {/* Header */}
        <motion.header
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          className="text-center mb-6 sm:mb-8 lg:mb-12"
        >
          <div className="inline-flex items-center gap-2 mb-3 sm:mb-4">
            <div className="h-px w-6 sm:w-8 bg-purple-500/50" />
            <span className="text-[8px] sm:text-[10px] font-black uppercase tracking-[0.3em] sm:tracking-[0.4em] text-purple-500">
              Player Dashboard
            </span>
            <div className="h-px w-6 sm:w-8 bg-purple-500/50" />
          </div>
          <h1 className="text-2xl sm:text-3xl md:text-4xl lg:text-5xl font-black tracking-tighter mb-2">
            Welcome,{" "}
            <span className="bg-gradient-to-r from-purple-400 via-fuchsia-500 to-purple-600 bg-clip-text text-transparent">
              {user?.username || "Player"}
            </span>
          </h1>
          {isGuest && (
            <p className="text-yellow-500/80 text-xs sm:text-sm font-medium mt-2 flex items-center justify-center gap-2">
              <AlertCircle size={14} className="sm:w-4 sm:h-4" />
              You are playing as a guest
            </p>
          )}
        </motion.header>

        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="space-y-6 sm:space-y-8"
        >
          {/* Statistics Section */}
          <motion.section variants={itemVariants}>
            <h2 className="text-lg sm:text-xl font-bold text-white mb-3 sm:mb-4 flex items-center gap-2">
              <Trophy size={18} className="text-purple-400 sm:w-5 sm:h-5" />
              Statistics
            </h2>

            {/* Makao Statistics */}
            <div className="mb-4 sm:mb-6">
              <div className="flex items-center gap-2 mb-2 sm:mb-3">
                <Spade size={16} className="text-purple-400 sm:w-[18px] sm:h-[18px]" />
                <h3 className="text-base sm:text-lg font-bold text-purple-400">Makao</h3>
              </div>
              <div className="grid grid-cols-1 xs:grid-cols-2 md:grid-cols-3 gap-3 sm:gap-4">
                <StatCard
                  icon={Gamepad2}
                  label="Games Played"
                  value={statsLoading ? "..." : statsMakao.gamesPlayed}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
                <StatCard
                  icon={Trophy}
                  label="Games Won"
                  value={statsLoading ? "..." : statsMakao.gamesWon}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
                <StatCard
                  icon={Percent}
                  label="Win Ratio"
                  value={statsLoading ? "..." : statsMakao.winRatio}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
              </div>
            </div>

            {/* Ludo Statistics */}
            <div>
              <div className="flex items-center gap-2 mb-2 sm:mb-3">
                <Dice5 size={16} className="text-blue-400 sm:w-[18px] sm:h-[18px]" />
                <h3 className="text-base sm:text-lg font-bold text-blue-400">Ludo</h3>
              </div>
              <div className="grid grid-cols-1 xs:grid-cols-2 md:grid-cols-3 gap-3 sm:gap-4">
                <StatCard
                  icon={Gamepad2}
                  label="Games Played"
                  value={statsLoading ? "..." : statsLudo.gamesPlayed}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
                <StatCard
                  icon={Trophy}
                  label="Games Won"
                  value={statsLoading ? "..." : statsLudo.gamesWon}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
                <StatCard
                  icon={Percent}
                  label="Win Ratio"
                  value={statsLoading ? "..." : statsLudo.winRatio}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
              </div>
            </div>
          </motion.section>

          {/* Account Info Section */}
          <motion.section variants={itemVariants}>
            <h2 className="text-lg sm:text-xl font-bold text-white mb-3 sm:mb-4 flex items-center gap-2">
              <User size={18} className="text-purple-400 sm:w-5 sm:h-5" />
              Account Information
            </h2>
            <div className="bg-[#121018] rounded-xl sm:rounded-2xl border border-white/5 p-4 sm:p-6">
              <InfoRow icon={User} label="Username" value={user?.username || "N/A"} />
              {isGuest ? (
                <>
                  <InfoRow icon={Mail} label="Email" value="Guest account - No email" />
                  <InfoRow icon={Calendar} label="Account Type" value="Temporary Guest Session" />
                </>
              ) : (
                <>
                  <InfoRow icon={Mail} label="Email" value={userEmail || "Loading..."} />
                  <InfoRow icon={Calendar} label="Member Since" value={joiningDate} />
                </>
              )}
            </div>
          </motion.section>

          {/* Profile Management Section */}
          <motion.section variants={itemVariants}>
            <h2 className="text-lg sm:text-xl font-bold text-white mb-3 sm:mb-4 flex items-center gap-2">
              <Edit3 size={18} className="text-purple-400 sm:w-5 sm:h-5" />
              Profile Management
            </h2>

            {isGuest ? (
              <div className="bg-[#121018] rounded-xl sm:rounded-2xl border border-yellow-500/20 p-6 sm:p-8 text-center">
                <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-full bg-yellow-500/10 flex items-center justify-center mx-auto mb-3 sm:mb-4">
                  <Lock size={24} className="text-yellow-500 sm:w-8 sm:h-8" />
                </div>
                <h3 className="text-base sm:text-lg font-bold text-white mb-2">Guest Account</h3>
                <p className="text-gray-400 text-xs sm:text-sm max-w-md mx-auto">
                  Guest accounts cannot change credentials. Please register for a full account to
                  access profile management features.
                </p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                {/* Change Username Form */}
                <div className="bg-[#121018] rounded-xl sm:rounded-2xl border border-white/5 p-4 sm:p-6">
                  <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4 flex items-center gap-2">
                    <User size={18} className="text-purple-400" />
                    Change Username
                  </h3>
                  <form onSubmit={handleUpdateUsername} className="space-y-4">
                    <div>
                      <label className="block text-[10px] sm:text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5 sm:mb-2">
                        New Username
                      </label>
                      <input
                        type="text"
                        value={newUsername}
                        onChange={(e) => setNewUsername(e.target.value)}
                        placeholder="Enter new username"
                        className="w-full px-3 sm:px-4 py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors text-sm sm:text-base"
                        required
                        minLength={3}
                        maxLength={20}
                      />
                    </div>
                    <motion.button
                      type="submit"
                      disabled={isUpdatingUsername}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      className="w-full py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-gradient-to-r from-purple-600 to-fuchsia-500 text-white font-bold text-xs sm:text-sm uppercase tracking-wider flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px]"
                    >
                      {isUpdatingUsername ? (
                        "Updating..."
                      ) : (
                        <>
                          <Check size={14} className="sm:w-4 sm:h-4" />
                          Update Username
                        </>
                      )}
                    </motion.button>
                  </form>
                </div>

                {/* Change Password Form */}
                <div className="bg-[#121018] rounded-xl sm:rounded-2xl border border-white/5 p-4 sm:p-6">
                  <h3 className="text-base sm:text-lg font-bold text-white mb-3 sm:mb-4 flex items-center gap-2">
                    <Lock size={16} className="text-purple-400 sm:w-[18px] sm:h-[18px]" />
                    Change Password
                  </h3>
                  <form onSubmit={handleUpdatePassword} className="space-y-3 sm:space-y-4">
                    <div>
                      <label className="block text-[10px] sm:text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5 sm:mb-2">
                        Current Password
                      </label>
                      <input
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        placeholder="Enter current password"
                        className="w-full px-3 sm:px-4 py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors text-sm sm:text-base"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] sm:text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5 sm:mb-2">
                        New Password
                      </label>
                      <input
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Enter new password"
                        className="w-full px-3 sm:px-4 py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors text-sm sm:text-base"
                        required
                        minLength={6}
                      />
                    </div>
                    <div>
                      <label className="block text-[10px] sm:text-xs font-semibold text-gray-500 uppercase tracking-wider mb-1.5 sm:mb-2">
                        Confirm New Password
                      </label>
                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Confirm new password"
                        className="w-full px-3 sm:px-4 py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors text-sm sm:text-base"
                        required
                        minLength={6}
                      />
                    </div>
                    <motion.button
                      type="submit"
                      disabled={isUpdatingPassword}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      className="w-full py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-gradient-to-r from-purple-600 to-fuchsia-500 text-white font-bold text-xs sm:text-sm uppercase tracking-wider flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px]"
                    >
                      {isUpdatingPassword ? (
                        "Updating..."
                      ) : (
                        <>
                          <Check size={14} className="sm:w-4 sm:h-4" />
                          Update Password
                        </>
                      )}
                    </motion.button>
                  </form>
                </div>
              </div>
            )}
          </motion.section>
        </motion.div>
      </main>

      {isAuthenticated && <SocialCenter />}
    </div>
  );
};

export default Dashboard;
