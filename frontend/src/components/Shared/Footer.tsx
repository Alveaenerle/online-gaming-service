import React from "react";
import { Link } from "react-router-dom";

const Footer: React.FC = () => (
  <footer className="border-t border-white/10 mt-12 sm:mt-16 lg:mt-20 py-6 sm:py-8 bg-[#121018] text-gray-300">
    <div className="px-4 sm:px-6 md:px-12 lg:px-24 flex flex-col md:flex-row justify-between items-center gap-4">
      <div className="text-xs sm:text-sm text-center md:text-left">
        © {new Date().getFullYear()} OnlineGames. All rights reserved.
      </div>

      <div className="flex flex-wrap justify-center gap-4 sm:gap-6 text-xs sm:text-sm">
        <Link to="/about" className="hover:text-purple-400 transition-colors min-h-[44px] flex items-center">
          About Us
        </Link>
        <Link to="/privacy" className="hover:text-purple-400 transition-colors min-h-[44px] flex items-center">
          Privacy
        </Link>
        <Link to="/terms" className="hover:text-purple-400 transition-colors min-h-[44px] flex items-center">
          Terms
        </Link>
        <Link to="/support" className="hover:text-purple-400 transition-colors min-h-[44px] flex items-center">
          Support
        </Link>
      </div>
    </div>
  </footer>
);

export default Footer;
