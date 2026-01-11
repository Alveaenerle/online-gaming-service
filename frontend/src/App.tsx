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

const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <LobbyProvider>
          <LobbyIndicator />
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

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
    </AuthProvider>
  );
};

export default App;
