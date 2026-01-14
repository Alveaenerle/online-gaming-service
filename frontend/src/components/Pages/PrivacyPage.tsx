import React from "react";
import { motion } from "framer-motion";
import { Shield, Eye, Lock, Database, Cookie, UserCheck } from "lucide-react";
import Navbar from "../Shared/Navbar";
import Footer from "../Shared/Footer";
import { BackgroundGradient } from "../Shared/BackgroundGradient";

const PrivacyPage: React.FC = () => {
  const sections = [
    {
      icon: <Database size={24} />,
      title: "Information We Collect",
      content: [
        "Account information (username, email address, password hash)",
        "Game statistics and match history",
        "Device and browser information for security purposes",
        "Communication data when you contact our support team"
      ]
    },
    {
      icon: <Eye size={24} />,
      title: "How We Use Your Information",
      content: [
        "To provide and maintain our gaming services",
        "To authenticate your account and ensure security",
        "To improve our games and user experience",
        "To communicate important updates and announcements"
      ]
    },
    {
      icon: <Shield size={24} />,
      title: "Data Protection",
      content: [
        "All data is encrypted in transit using TLS/SSL",
        "Passwords are hashed using industry-standard algorithms",
        "Regular security audits and vulnerability assessments",
        "Access to user data is strictly limited to authorized personnel"
      ]
    },
    {
      icon: <Cookie size={24} />,
      title: "Cookies & Sessions",
      content: [
        "We use essential cookies to maintain your login session",
        "Session data is stored securely and expires after inactivity",
        "We do not use third-party tracking or advertising cookies",
        "You can manage cookie preferences in your browser settings"
      ]
    },
    {
      icon: <UserCheck size={24} />,
      title: "Your Rights",
      content: [
        "Access and download your personal data",
        "Request correction of inaccurate information",
        "Delete your account and associated data",
        "Opt out of non-essential communications"
      ]
    },
    {
      icon: <Lock size={24} />,
      title: "Data Retention",
      content: [
        "Account data is retained while your account is active",
        "Game history is kept for statistical and ranking purposes",
        "Deleted accounts are purged within 30 days",
        "Legal requirements may require longer retention of certain data"
      ]
    }
  ];

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased font-sans overflow-x-hidden">
      <Navbar />
      <BackgroundGradient />
      
      <main className="relative pt-28 pb-20 px-6 md:px-12 lg:px-24 max-w-4xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-6">
            Privacy <span className="bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">Policy</span>
          </h1>
          <p className="text-gray-400">
            Last updated: January 2026
          </p>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="rounded-2xl bg-white/5 border border-white/10 backdrop-blur-sm p-6 md:p-8 mb-8"
        >
          <p className="text-gray-300 leading-relaxed">
            At OnlineGames, we take your privacy seriously. This policy explains how we collect, 
            use, and protect your personal information when you use our platform. By using our 
            services, you agree to the collection and use of information in accordance with this policy.
          </p>
        </motion.div>

        <div className="space-y-6">
          {sections.map((section, index) => (
            <motion.div
              key={section.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 + index * 0.1 }}
              className="rounded-xl bg-white/5 border border-white/10 p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center text-purple-400">
                  {section.icon}
                </div>
                <h2 className="text-lg font-semibold">{section.title}</h2>
              </div>
              <ul className="space-y-2">
                {section.content.map((item, i) => (
                  <li key={i} className="text-gray-400 text-sm flex items-start gap-2">
                    <span className="text-purple-400 mt-1">•</span>
                    {item}
                  </li>
                ))}
              </ul>
            </motion.div>
          ))}
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.8 }}
          className="mt-8 text-center text-gray-500 text-sm"
        >
          <p>
            Questions about our privacy practices? Contact us at{" "}
            <a href="mailto:privacy@onlinegames.com" className="text-purple-400 hover:underline">
              privacy@onlinegames.com
            </a>
          </p>
        </motion.div>
      </main>

      <Footer />
    </div>
  );
};

export default PrivacyPage;
