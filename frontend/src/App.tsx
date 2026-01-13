import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { LobbyProvider } from "./context/LobbyContext";
import { LobbyIndicator } from "./components/Shared/LobbyIndicator";
import { RequireAuth } from "./components/Auth/RequireAuth";
import LandingPage from "./components/LandingPage/LandingPage";
import Login from "./components/Auth/Login";
import Register from "./components/Auth/Register";
import Home from "./components/HomePage/HomePage";
import MakaoGame from "./components/Games/Makao/MakaoGame";
import { MakaoLobby } from "./components/Games/Makao/MakaoLobby";
import { MakaoTitle } from "./components/Games/Makao/MakaoTitle";
import { LudoTitle } from "./components/Games/Ludo/LudoTitle";
import { LudoLobby } from "./components/Games/Ludo/LudoLobby";
import { LudoArenaPage } from "./components/Games/Ludo/LudoGame";
import AboutPage from "./components/Pages/AboutPage";
import PrivacyPage from "./components/Pages/PrivacyPage";
import TermsPage from "./components/Pages/TermsPage";
import SupportPage from "./components/Pages/SupportPage";
import RankingsPage from "./components/Pages/RankingsPage";
import GamesLibrary from "./components/Games/GamesLibrary";
import Dashboard from "./components/Dashboard/Dashboard";

import { SocialProvider } from "./context/SocialContext";
import { ToastProvider } from "./context/ToastContext";
import { LudoProvider } from "./context/LudoGameContext";

const App: React.FC = () => {
  return (
    <AuthProvider>
      <ToastProvider>
        <SocialProvider>
          <Router>
            <LobbyProvider>
          <LobbyIndicator />
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/privacy" element={<PrivacyPage />} />
            <Route path="/terms" element={<TermsPage />} />
            <Route path="/support" element={<SupportPage />} />
            <Route path="/games" element={<GamesLibrary />} />
            <Route path="/rankings" element={<RankingsPage />} />

            {/* Protected routes */}
            <Route
              path="/home"
              element={
                <RequireAuth>
                  <Home />
                </RequireAuth>
              }
            />
            <Route
              path="/dashboard"
              element={
                <RequireAuth>
                  <Dashboard />
                </RequireAuth>
              }
            />
            <Route
              path="/makao/game"
              element={
                <RequireAuth>
                  <MakaoGame />
                </RequireAuth>
              }
            />
            <Route
              path="/makao"
              element={
                <RequireAuth>
                  <MakaoTitle />
                </RequireAuth>
              }
            />
            <Route
              path="/lobby/makao"
              element={
                <RequireAuth>
                  <MakaoLobby />
                </RequireAuth>
              }
            />
            <Route
              path="/ludo"
              element={
                <RequireAuth>
                  <LudoTitle />
                </RequireAuth>
              }
            />
            <Route
                  path="/ludo/game"
                  element={
                    <RequireAuth>
                      <LudoProvider>
                        <LudoArenaPage />
                      </LudoProvider>
                    </RequireAuth>
                  }
                />
            <Route
              path="/lobby/ludo"
              element={
                <RequireAuth>
                  <LudoLobby />
                </RequireAuth>
              }
            />
          </Routes>
        </LobbyProvider>
          </Router>
        </SocialProvider>
      </ToastProvider>
    </AuthProvider>
  );
};

export default App;
