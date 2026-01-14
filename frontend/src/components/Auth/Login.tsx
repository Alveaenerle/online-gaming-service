import React, { useState } from "react";
import { motion } from "framer-motion";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import GoogleSignInButton from "./GoogleSignInButton";

const Login: React.FC = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const { login, loginAsGuest, loginWithGoogle } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsLoading(true);

    try {
      await login({ email, password });
      navigate("/home");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGuestLogin = async () => {
    setError("");
    setIsLoading(true);
    try {
      await loginAsGuest();
      navigate("/home");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Guest login failed");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSuccess = async (credential: string) => {
    setError("");
    setIsLoading(true);
    try {
      await loginWithGoogle(credential);
      navigate("/home");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Google login failed");
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleError = () => {
    setError("Google login failed. Please try again.");
  };

  return (
    <div className="min-h-screen bg-bg text-white antialiased flex items-center justify-center px-4 sm:px-6 py-8 sm:py-12">
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] animate-gradient-bg" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-md bg-[#121018] p-5 sm:p-8 rounded-2xl sm:rounded-3xl border border-white/10 shadow-neon"
      >
        <div className="flex justify-center mb-4 sm:mb-6">
          <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-neon flex items-center justify-center text-white font-bold text-xl sm:text-2xl">
            OG
          </div>
        </div>

        <h2 className="text-2xl sm:text-3xl font-extrabold text-center mb-1 sm:mb-2">
          Welcome back
        </h2>
        <p className="text-gray-400 text-center text-sm sm:text-base mb-6 sm:mb-8">
          Log in to continue your adventure
        </p>

        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 p-2.5 sm:p-3 bg-red-500/20 border border-red-500/50 rounded-lg sm:rounded-xl text-red-400 text-xs sm:text-sm text-center"
          >
            {error}
          </motion.div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-6">
          <div>
            <label htmlFor="email" className="block text-xs sm:text-sm font-medium mb-1.5 sm:mb-2">
              Email
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-3 sm:px-4 py-2.5 sm:py-3 bg-[#1a1a27] border border-white/10 rounded-lg sm:rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all text-sm sm:text-base"
              placeholder="your@email.com"
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="block text-xs sm:text-sm font-medium mb-1.5 sm:mb-2"
            >
              Password
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-3 sm:px-4 py-2.5 sm:py-3 bg-[#1a1a27] border border-white/10 rounded-lg sm:rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all text-sm sm:text-base"
              placeholder="Enter your password"
            />
          </div>

          <div className="flex flex-col xs:flex-row items-start xs:items-center justify-between gap-2 text-xs sm:text-sm">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                className="w-4 h-4 rounded bg-[#1a1a27] border-white/10 focus:ring-2 focus:ring-purpleEnd"
              />
              <span className="text-gray-300">Remember me</span>
            </label>
            <a href="#" className="text-purpleEnd hover:text-purple-400">
              Forgot password?
            </a>
          </div>

          <motion.button
            type="submit"
            disabled={isLoading}
            whileHover={{ scale: isLoading ? 1 : 1.02 }}
            whileTap={{ scale: isLoading ? 1 : 0.98 }}
            className="w-full py-2.5 sm:py-3 rounded-lg sm:rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-semibold shadow-neon transition-transform disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px] text-sm sm:text-base"
          >
            {isLoading ? "Logging in..." : "Log in"}
          </motion.button>
        </form>

        <div className="mt-4 sm:mt-6 flex items-center gap-3 sm:gap-4">
          <div className="flex-1 h-px bg-white/10" />
          <span className="text-gray-400 text-xs sm:text-sm">or continue with</span>
          <div className="flex-1 h-px bg-white/10" />
        </div>

        <div className="mt-3 sm:mt-4">
          <GoogleSignInButton
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            text="signin"
            disabled={isLoading}
          />
        </div>

        <motion.button
          onClick={handleGuestLogin}
          disabled={isLoading}
          whileHover={{ scale: isLoading ? 1 : 1.02 }}
          whileTap={{ scale: isLoading ? 1 : 0.98 }}
          className="w-full mt-3 sm:mt-4 py-2.5 sm:py-3 rounded-lg sm:rounded-xl border border-white/20 text-white font-semibold hover:bg-white/5 transition disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px] text-sm sm:text-base"
        >
          {isLoading ? "Please wait..." : "Play as Guest"}
        </motion.button>

        <div className="mt-4 sm:mt-6 text-center text-xs sm:text-sm text-gray-400">
          Don't have an account?{" "}
          <Link
            to="/register"
            className="text-purpleEnd hover:text-purple-400 font-semibold"
          >
            Sign up
          </Link>
        </div>

        <div className="mt-4 sm:mt-6">
          <Link
            to="/"
            className="text-gray-400 hover:text-white text-xs sm:text-sm flex items-center justify-center gap-2"
          >
            ← Back to home
          </Link>
        </div>
      </motion.div>
    </div>
  );
};

export default Login;
