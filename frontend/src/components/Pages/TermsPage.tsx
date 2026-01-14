import React from "react";
import { motion } from "framer-motion";
import { FileText, AlertTriangle, Ban, Scale, RefreshCw, Gavel } from "lucide-react";
import Navbar from "../Shared/Navbar";
import Footer from "../Shared/Footer";
import { BackgroundGradient } from "../Shared/BackgroundGradient";

const TermsPage: React.FC = () => {
  const sections = [
    {
      icon: <FileText size={24} />,
      title: "1. Acceptance of Terms",
      content: `By accessing or using OnlineGames, you agree to be bound by these Terms of Service. 
        If you do not agree to these terms, please do not use our services. We reserve the right 
        to update these terms at any time, and your continued use of the platform constitutes 
        acceptance of any changes.`
    },
    {
      icon: <Scale size={24} />,
      title: "2. User Accounts",
      content: `You are responsible for maintaining the confidentiality of your account credentials. 
        You must provide accurate information when creating an account. You may not share your 
        account with others or create multiple accounts. Guest accounts have limited functionality 
        and may be subject to automatic deletion after periods of inactivity.`
    },
    {
      icon: <Ban size={24} />,
      title: "3. Prohibited Conduct",
      content: `Users must not: cheat or use exploits in games, harass or abuse other players, 
        attempt to gain unauthorized access to our systems, impersonate other users or staff, 
        use automated tools or bots, engage in any activity that disrupts the gaming experience 
        for others, or violate any applicable laws.`
    },
    {
      icon: <AlertTriangle size={24} />,
      title: "4. Content & Communication",
      content: `You are solely responsible for any content you share through our platform. 
        We do not monitor all communications but reserve the right to remove content that 
        violates these terms. Usernames must not contain offensive, discriminatory, or 
        inappropriate material.`
    },
    {
      icon: <Gavel size={24} />,
      title: "5. Enforcement & Penalties",
      content: `Violations of these terms may result in warnings, temporary suspensions, or 
        permanent bans at our discretion. We reserve the right to remove accounts without 
        prior notice for serious violations. Appeals can be submitted through our support 
        system within 14 days of any enforcement action.`
    },
    {
      icon: <RefreshCw size={24} />,
      title: "6. Service Availability",
      content: `We strive to maintain consistent service availability but do not guarantee 
        uninterrupted access. Scheduled maintenance will be announced in advance when possible. 
        We are not liable for any losses resulting from service interruptions, including 
        game progress or statistics.`
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
            Terms of <span className="bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">Service</span>
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
            Welcome to OnlineGames! These Terms of Service govern your use of our platform 
            and services. Please read them carefully before using our games and features.
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
              <p className="text-gray-400 text-sm leading-relaxed">
                {section.content}
              </p>
            </motion.div>
          ))}
        </div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.8 }}
          className="mt-8 rounded-xl bg-purple-500/10 border border-purple-500/30 p-6 text-center"
        >
          <p className="text-gray-300 text-sm">
            By continuing to use OnlineGames, you acknowledge that you have read, understood, 
            and agree to be bound by these Terms of Service.
          </p>
        </motion.div>
      </main>

      <Footer />
    </div>
  );
};

export default TermsPage;
