import React from "react";
import { Link } from "react-router-dom";

const Footer: React.FC = () => (
  <footer className="border-t border-white/10 mt-20 py-8 bg-[#121018] text-gray-300">
    <div className="px-6 md:px-12 lg:px-24 flex flex-col md:flex-row justify-between items-center gap-4">
      <div className="text-sm">
        © {new Date().getFullYear()} OnlineGames. All rights reserved.
      </div>

      <div className="flex gap-6 text-sm">
        <Link to="/about" className="hover:text-purple-400 transition-colors">
          About Us
        </Link>
        <Link to="/privacy" className="hover:text-purple-400 transition-colors">
          Privacy
        </Link>
        <Link to="/terms" className="hover:text-purple-400 transition-colors">
          Terms
        </Link>
        <Link to="/support" className="hover:text-purple-400 transition-colors">
          Support
        </Link>
      </div>
    </div>
  </footer>
);

export default Footer;
