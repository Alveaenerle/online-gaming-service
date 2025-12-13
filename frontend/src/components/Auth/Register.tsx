import React, { useState } from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";

const Register: React.FC = () => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [shakeKey, setShakeKey] = useState(0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement registration logic
    if (password !== confirmPassword) {
      setPasswordError("Passwords don't match!");
      setPassword("");
      setConfirmPassword("");
      setShakeKey(prev => prev + 1);
      return;
    }
    setPasswordError("");
    console.log("Register:", { username, email, password });
  };

  return (
    <div className="min-h-screen bg-bg text-white antialiased flex items-center justify-center px-6 py-12">
      <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_rgba(108,42,255,0.12),_transparent_20%),radial-gradient(ellipse_at_bottom_right,_rgba(168,85,247,0.08),_transparent_15%)] animate-gradient-bg" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-md bg-[#121018] p-8 rounded-3xl border border-white/10 shadow-neon"
      >
        <div className="flex justify-center mb-6">
          <div className="w-16 h-16 rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-neon flex items-center justify-center text-white font-bold text-2xl">
            OG
          </div>
        </div>

        <h2 className="text-3xl font-extrabold text-center mb-2">
          Create account
        </h2>
        <p className="text-gray-400 text-center mb-8">
          Join our gaming community today
        </p>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label
              htmlFor="username"
              className="block text-sm font-medium mb-2"
            >
              Username
            </label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="w-full px-4 py-3 bg-[#1a1a27] border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all"
              placeholder="Choose a username"
            />
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium mb-2">
              Email
            </label>
            <input
              type="email"
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="w-full px-4 py-3 bg-[#1a1a27] border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all"
              placeholder="your@email.com"
            />
          </div>

          <div>
            <label
              htmlFor="password"
              className="block text-sm font-medium mb-2"
            >
              Password
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              className="w-full px-4 py-3 bg-[#1a1a27] border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all"
              placeholder="Enter your password"
            />
          </div>

          <div>
            <label
              htmlFor="confirmPassword"
              className="block text-sm font-medium mb-2"
            >
              Confirm Password
            </label>
            <input
              type="password"
              id="confirmPassword"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              minLength={8}
              className="w-full px-4 py-3 bg-[#1a1a27] border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all"
              placeholder="Enter your password again"
            />
          </div>

          {passwordError && (
            <motion.div
              key={shakeKey}
              initial={{ opacity: 0, x: 0 }}
              animate={{
                opacity: 1,
                x: [0, -10, 10, -10, 10, -5, 5, 0]
              }}
              transition={{
                x: { duration: 0.5, times: [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7] }
              }}
              className="text-red-500 text-sm font-medium text-center"
            >
              {passwordError}
            </motion.div>
          )}

          <div className="flex items-start gap-2">
            <input
              type="checkbox"
              id="terms"
              required
              className="w-4 h-4 mt-1 rounded bg-[#1a1a27] border-white/10 focus:ring-2 focus:ring-purpleEnd"
            />
            <label htmlFor="terms" className="text-sm text-gray-300">
              I agree to the{" "}
              <a href="#" className="text-purpleEnd hover:text-purple-400">
                Terms of Service
              </a>{" "}
              and{" "}
              <a href="#" className="text-purpleEnd hover:text-purple-400">
                Privacy Policy
              </a>
            </label>
          </div>

          <motion.button
            type="submit"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="w-full py-3 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-semibold shadow-neon transition-transform"
          >
            Create account
          </motion.button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-400">
          Already have an account?{" "}
          <Link
            to="/login"
            className="text-purpleEnd hover:text-purple-400 font-semibold"
          >
            Log in
          </Link>
        </div>

        <div className="mt-6">
          <Link
            to="/"
            className="text-gray-400 hover:text-white text-sm flex items-center justify-center gap-2"
          >
            ← Back to home
          </Link>
        </div>
      </motion.div>
    </div>
  );
};

export default Register;
