import React from "react";

const Footer: React.FC = () => (
  <footer className="border-t border-white/10 mt-20 py-8 bg-[#121018] text-gray-300">
    <div className="px-6 md:px-12 lg:px-24 flex flex-col md:flex-row justify-between items-center gap-4">
      <div className="text-sm">
        © {new Date().getFullYear()} OnlineGames. All rights reserved.
      </div>

      <div className="flex gap-6 text-sm">
        <a className="hover:text-purple-400 transition-colors" href="#">
          Privacy
        </a>
        <a className="hover:text-purple-400 transition-colors" href="#">
          Terms
        </a>
        <a className="hover:text-purple-400 transition-colors" href="#">
          Support
        </a>
      </div>
    </div>
  </footer>
);

export default Footer;
