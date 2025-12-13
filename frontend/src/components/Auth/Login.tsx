import React, { useState } from "react";
import { motion } from "framer-motion";
import { Link } from "react-router-dom";

const Login: React.FC = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // TODO: Implement login logic
    console.log("Login:", { email, password });
  };

  return (
    <div className="min-h-screen bg-bg text-white antialiased flex items-center justify-center px-6">
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
          Welcome back
        </h2>
        <p className="text-gray-400 text-center mb-8">
          Log in to continue your adventure
        </p>

        <form onSubmit={handleSubmit} className="space-y-6">
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
              className="w-full px-4 py-3 bg-[#1a1a27] border border-white/10 rounded-xl focus:outline-none focus:ring-2 focus:ring-purpleEnd transition-all"
              placeholder="Enter your password"
            />
          </div>

          <div className="flex items-center justify-between text-sm">
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
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="w-full py-3 rounded-xl bg-gradient-to-br from-purpleStart to-purpleEnd text-white font-semibold shadow-neon transition-transform"
          >
            Log in
          </motion.button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-400">
          Don't have an account?{" "}
          <Link
            to="/register"
            className="text-purpleEnd hover:text-purple-400 font-semibold"
          >
            Sign up
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

export default Login;
