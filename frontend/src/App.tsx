import React from "react";
import Navbar from "./components/Shared/Navbar";
import Title from "./components/LandingPage/Title";
import Features from "./components/LandingPage/Features";
import Footer from "./components/Shared/Footer";

const App: React.FC = () => {
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
    </div>
  );
};

export default App;
