import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import LandingPage from "./components/LandingPage/LandingPage";
import Login from "./components/Auth/Login";
import Register from "./components/Auth/Register";
import Home from "./components/HomePage/HomePage";
import MakaoGame from "./components/Games/Makao/MakaoGame";
import { MakaoLobby } from "./components/Games/Makao/MakaoLobby";
import { MakaoTitle } from "./components/Games/Makao/MakaoTitle";

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/home" element={<Home />} />
          <Route path="/makao/game" element={<MakaoGame />} />
          <Route path="/makao" element={<MakaoTitle />} />
          <Route path="/lobby/makao" element={<MakaoLobby />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
};

export default App;
