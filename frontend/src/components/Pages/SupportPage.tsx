import React, { useState } from "react";
import { motion } from "framer-motion";
import { MessageCircle, Mail, HelpCircle, Bug, Lightbulb, Send, ChevronDown } from "lucide-react";
import Navbar from "../Shared/Navbar";
import Footer from "../Shared/Footer";
import { BackgroundGradient } from "../Shared/BackgroundGradient";
import { useToast } from "../../context/ToastContext";

const SupportPage: React.FC = () => {
  const { showToast } = useToast();
  const [formData, setFormData] = useState({
    category: "",
    email: "",
    subject: "",
    message: ""
  });
  const [expandedFaq, setExpandedFaq] = useState<number | null>(null);

  const faqs = [
    {
      question: "How do I create a game room?",
      answer: "Navigate to the game you want to play (Makao or Ludo), then click 'Create Room'. You can set a room name, choose the number of players, and decide if the room should be private. Share the access code with friends to let them join!"
    },
    {
      question: "Can I play without creating an account?",
      answer: "Yes! You can play as a guest by clicking 'Play as Guest' on the login page. Guest accounts have some limitations - you can't add friends or track your game statistics across sessions."
    },
    {
      question: "How do I add friends?",
      answer: "When you're in a game lobby with another player, click the '+' button on their player card to send a friend request. They'll receive a notification and can accept from the Social Center. Note: Guest players cannot send or receive friend requests."
    },
    {
      question: "Why can't I start the game?",
      answer: "To start a game, you need at least 2 players and all players must be marked as 'Ready'. Only the room host can start the game. If someone isn't ready, ask them to click the Ready button!"
    },
    {
      question: "What happens if I get disconnected?",
      answer: "Don't worry! If you get disconnected during a game, try refreshing the page. If you were in a lobby, you may need to rejoin using the room code. Game progress in active matches may be affected by disconnections."
    },
    {
      question: "How do I report a player?",
      answer: "Currently, you can report players by contacting our support team using the form below. Include the player's username, the game room, and a description of the issue. We take all reports seriously and investigate promptly."
    }
  ];

  const categories = [
    { value: "technical", label: "Technical Issue", icon: <Bug size={16} /> },
    { value: "account", label: "Account Problem", icon: <HelpCircle size={16} /> },
    { value: "feedback", label: "Feedback & Suggestions", icon: <Lightbulb size={16} /> },
    { value: "report", label: "Report a Player", icon: <MessageCircle size={16} /> },
    { value: "other", label: "Other", icon: <Mail size={16} /> }
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.category || !formData.email || !formData.subject || !formData.message) {
      showToast("Please fill in all fields", "error");
      return;
    }
    // In a real app, this would send to a backend
    showToast("Message sent! We'll get back to you soon.", "success");
    setFormData({ category: "", email: "", subject: "", message: "" });
  };

  return (
    <div className="min-h-screen bg-[#07060b] text-white antialiased font-sans overflow-x-hidden">
      <Navbar />
      <BackgroundGradient />
      
      <main className="relative pt-28 pb-20 px-6 md:px-12 lg:px-24 max-w-5xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center mb-12"
        >
          <h1 className="text-4xl md:text-5xl font-bold mb-6">
            Help & <span className="bg-gradient-to-r from-purple-400 to-pink-500 bg-clip-text text-transparent">Support</span>
          </h1>
          <p className="text-gray-400 max-w-xl mx-auto">
            Need help? Check our FAQs below or send us a message. We're here to make your gaming experience awesome!
          </p>
        </motion.div>

        {/* FAQ Section */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mb-16"
        >
          <h2 className="text-2xl font-bold mb-6 flex items-center gap-3">
            <HelpCircle className="text-purple-400" />
            Frequently Asked Questions
          </h2>
          <div className="space-y-3">
            {faqs.map((faq, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.3 + index * 0.05 }}
                className="rounded-xl bg-white/5 border border-white/10 overflow-hidden"
              >
                <button
                  onClick={() => setExpandedFaq(expandedFaq === index ? null : index)}
                  className="w-full flex items-center justify-between p-4 text-left hover:bg-white/5 transition-colors"
                >
                  <span className="font-medium">{faq.question}</span>
                  <ChevronDown 
                    size={20} 
                    className={`text-purple-400 transition-transform ${expandedFaq === index ? 'rotate-180' : ''}`} 
                  />
                </button>
                {expandedFaq === index && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: "auto", opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    className="px-4 pb-4"
                  >
                    <p className="text-gray-400 text-sm leading-relaxed border-t border-white/10 pt-4">
                      {faq.answer}
                    </p>
                  </motion.div>
                )}
              </motion.div>
            ))}
          </div>
        </motion.section>

        {/* Contact Form */}
        <motion.section
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
        >
          <h2 className="text-2xl font-bold mb-6 flex items-center gap-3">
            <Mail className="text-purple-400" />
            Contact Us
          </h2>
          <form onSubmit={handleSubmit} className="rounded-2xl bg-white/5 border border-white/10 p-6 md:p-8">
            <div className="grid md:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Category</label>
                <select
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:border-purple-500 focus:outline-none transition-colors"
                >
                  <option value="" className="bg-[#1a1a27]">Select a category...</option>
                  {categories.map((cat) => (
                    <option key={cat.value} value={cat.value} className="bg-[#1a1a27]">
                      {cat.label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-2">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="your@email.com"
                  className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-purple-500 focus:outline-none transition-colors"
                />
              </div>
            </div>
            
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-300 mb-2">Subject</label>
              <input
                type="text"
                value={formData.subject}
                onChange={(e) => setFormData({ ...formData, subject: e.target.value })}
                placeholder="Brief description of your issue"
                className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-purple-500 focus:outline-none transition-colors"
              />
            </div>

            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-300 mb-2">Message</label>
              <textarea
                value={formData.message}
                onChange={(e) => setFormData({ ...formData, message: e.target.value })}
                placeholder="Tell us more about your issue or feedback..."
                rows={5}
                className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white placeholder-gray-500 focus:border-purple-500 focus:outline-none transition-colors resize-none"
              />
            </div>

            <motion.button
              type="submit"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="w-full md:w-auto px-8 py-3 rounded-xl bg-gradient-to-r from-purple-500 to-pink-500 text-white font-medium flex items-center justify-center gap-2 hover:opacity-90 transition-opacity"
            >
              <Send size={18} />
              Send Message
            </motion.button>
          </form>
        </motion.section>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6, delay: 0.6 }}
          className="mt-12 text-center text-gray-500 text-sm"
        >
          <p>
            You can also reach us directly at{" "}
            <a href="mailto:support@onlinegames.com" className="text-purple-400 hover:underline">
              support@onlinegames.com
            </a>
          </p>
          <p className="mt-2">Average response time: 24-48 hours</p>
        </motion.div>
      </main>

      <Footer />
    </div>
  );
};

export default SupportPage;
