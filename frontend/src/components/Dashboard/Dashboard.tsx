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
  X,
  Spade,
  Dice5,
} from "lucide-react";
import Navbar from "../Shared/Navbar";
import { BackgroundGradient } from "../Shared/BackgroundGradient";
import { SocialCenter } from "../Shared/SocialCenter";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { authService } from "../../services/authService";

// Mock statistics data (to be replaced with real API later)
const mockStatsMakao = {
  gamesPlayed: 28,
  gamesWon: 15,
  winRatio: "54%",
};

const mockStatsLudo = {
  gamesPlayed: 14,
  gamesWon: 6,
  winRatio: "43%",
};

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
    className="bg-[#121018] rounded-2xl border border-white/5 p-6 hover:border-white/10 transition-colors"
  >
    <div className="flex items-center gap-4">
      <div className={`w-12 h-12 rounded-xl ${color} flex items-center justify-center`}>
        <Icon size={24} className="text-white" />
      </div>
      <div>
        <p className="text-gray-500 text-xs font-semibold uppercase tracking-wider">{label}</p>
        <p className="text-2xl font-black text-white">{value}</p>
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
  <div className="flex items-center gap-4 py-3 border-b border-white/5 last:border-0">
    <div className="w-10 h-10 rounded-lg bg-purple-500/10 flex items-center justify-center">
      <Icon size={18} className="text-purple-400" />
    </div>
    <div className="flex-1">
      <p className="text-gray-500 text-xs font-semibold uppercase tracking-wider">{label}</p>
      <p className="text-white font-medium">{value}</p>
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
    <div className="min-h-screen bg-[#050508] text-white overflow-hidden font-sans">
      <Navbar />
      <BackgroundGradient />

      <main className="relative z-10 pt-32 pb-20 px-6 max-w-6xl mx-auto">
        {/* Header */}
        <motion.header
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: [0.16, 1, 0.3, 1] }}
          className="text-center mb-12"
        >
          <div className="inline-flex items-center gap-2 mb-4">
            <div className="h-px w-8 bg-purple-500/50" />
            <span className="text-[10px] font-black uppercase tracking-[0.4em] text-purple-500">
              Player Dashboard
            </span>
            <div className="h-px w-8 bg-purple-500/50" />
          </div>
          <h1 className="text-4xl md:text-5xl font-black tracking-tighter mb-2">
            Welcome,{" "}
            <span className="bg-gradient-to-r from-purple-400 via-fuchsia-500 to-purple-600 bg-clip-text text-transparent">
              {user?.username || "Player"}
            </span>
          </h1>
          {isGuest && (
            <p className="text-yellow-500/80 text-sm font-medium mt-2 flex items-center justify-center gap-2">
              <AlertCircle size={16} />
              You are playing as a guest
            </p>
          )}
        </motion.header>

        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="space-y-8"
        >
          {/* Statistics Section */}
          <motion.section variants={itemVariants}>
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Trophy size={20} className="text-purple-400" />
              Statistics
            </h2>

            {/* Makao Statistics */}
            <div className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <Spade size={18} className="text-purple-400" />
                <h3 className="text-lg font-bold text-purple-400">Makao</h3>
              </div>
              <div className="grid md:grid-cols-3 gap-4">
                <StatCard
                  icon={Gamepad2}
                  label="Games Played"
                  value={mockStatsMakao.gamesPlayed}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
                <StatCard
                  icon={Trophy}
                  label="Games Won"
                  value={mockStatsMakao.gamesWon}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
                <StatCard
                  icon={Percent}
                  label="Win Ratio"
                  value={mockStatsMakao.winRatio}
                  color="bg-gradient-to-br from-purple-600 to-fuchsia-500"
                />
              </div>
            </div>

            {/* Ludo Statistics */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <Dice5 size={18} className="text-blue-400" />
                <h3 className="text-lg font-bold text-blue-400">Ludo</h3>
              </div>
              <div className="grid md:grid-cols-3 gap-4">
                <StatCard
                  icon={Gamepad2}
                  label="Games Played"
                  value={mockStatsLudo.gamesPlayed}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
                <StatCard
                  icon={Trophy}
                  label="Games Won"
                  value={mockStatsLudo.gamesWon}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
                <StatCard
                  icon={Percent}
                  label="Win Ratio"
                  value={mockStatsLudo.winRatio}
                  color="bg-gradient-to-br from-blue-600 to-cyan-500"
                />
              </div>
            </div>
          </motion.section>

          {/* Account Info Section */}
          <motion.section variants={itemVariants}>
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <User size={20} className="text-purple-400" />
              Account Information
            </h2>
            <div className="bg-[#121018] rounded-2xl border border-white/5 p-6">
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
            <h2 className="text-xl font-bold text-white mb-4 flex items-center gap-2">
              <Edit3 size={20} className="text-purple-400" />
              Profile Management
            </h2>

            {isGuest ? (
              <div className="bg-[#121018] rounded-2xl border border-yellow-500/20 p-8 text-center">
                <div className="w-16 h-16 rounded-full bg-yellow-500/10 flex items-center justify-center mx-auto mb-4">
                  <Lock size={32} className="text-yellow-500" />
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Guest Account</h3>
                <p className="text-gray-400 text-sm max-w-md mx-auto">
                  Guest accounts cannot change credentials. Please register for a full account to
                  access profile management features.
                </p>
              </div>
            ) : (
              <div className="grid md:grid-cols-2 gap-6">
                {/* Change Username Form */}
                <div className="bg-[#121018] rounded-2xl border border-white/5 p-6">
                  <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <User size={18} className="text-purple-400" />
                    Change Username
                  </h3>
                  <form onSubmit={handleUpdateUsername} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                        New Username
                      </label>
                      <input
                        type="text"
                        value={newUsername}
                        onChange={(e) => setNewUsername(e.target.value)}
                        placeholder="Enter new username"
                        className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors"
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
                      className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-fuchsia-500 text-white font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isUpdatingUsername ? (
                        "Updating..."
                      ) : (
                        <>
                          <Check size={16} />
                          Update Username
                        </>
                      )}
                    </motion.button>
                  </form>
                </div>

                {/* Change Password Form */}
                <div className="bg-[#121018] rounded-2xl border border-white/5 p-6">
                  <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                    <Lock size={18} className="text-purple-400" />
                    Change Password
                  </h3>
                  <form onSubmit={handleUpdatePassword} className="space-y-4">
                    <div>
                      <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                        Current Password
                      </label>
                      <input
                        type="password"
                        value={currentPassword}
                        onChange={(e) => setCurrentPassword(e.target.value)}
                        placeholder="Enter current password"
                        className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                        New Password
                      </label>
                      <input
                        type="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Enter new password"
                        className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors"
                        required
                        minLength={6}
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                        Confirm New Password
                      </label>
                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Confirm new password"
                        className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 transition-colors"
                        required
                        minLength={6}
                      />
                    </div>
                    <motion.button
                      type="submit"
                      disabled={isUpdatingPassword}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                      className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-fuchsia-500 text-white font-bold text-sm uppercase tracking-wider flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isUpdatingPassword ? (
                        "Updating..."
                      ) : (
                        <>
                          <Check size={16} />
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
