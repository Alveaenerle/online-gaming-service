import React from "react";
import Navbar from "../Shared/Navbar";
import Title from "./Title";
import Features from "./Features";
import Footer from "../Shared/Footer";
import { SocialCenter } from "../Shared/SocialCenter";

const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-bg text-white antialiased">
      <Navbar />
      <main className="pb-10">
        <Title />
        <div className="px-6 md:px-12 lg:px-24 mb-20">
          <Features />
        </div>
      </main>
      <Footer />
      <SocialCenter />
    </div>
  );
};

export default LandingPage;
