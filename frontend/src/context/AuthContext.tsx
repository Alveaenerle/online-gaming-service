import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authService, User, LoginRequest, RegisterRequest } from '../services/authService';
import { lobbyService } from '../services/lobbyService';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  loginAsGuest: () => Promise<void>;
  loginWithGoogle: (idToken: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkSession = async () => {
      try {
        const userData = await authService.getCurrentUser();
        setUser(userData);
      } catch {
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };
    checkSession();
  }, []);

  const login = async (credentials: LoginRequest) => {
    const userData = await authService.login(credentials);
    setUser(userData);
  };

  const register = async (data: RegisterRequest) => {
    await authService.register(data);
    const userData = await authService.login({ email: data.email, password: data.password });
    setUser(userData);
  };

  const loginAsGuest = async () => {
    const userData = await authService.loginAsGuest();
    setUser(userData);
  };

  const loginWithGoogle = async (idToken: string) => {
    const userData = await authService.loginWithGoogle(idToken);
    setUser(userData);
  };

  const logout = async () => {
    try {
      // Attempt to leave any active lobby before destroying the session
      try {
        await lobbyService.leaveRoom();
      } catch (err) {
        // Ignore errors (e.g., user wasn't in a room) and proceed with logout
        console.warn('Lobby leave failed during logout (not critical):', err);
      }
      
      await authService.logout();
    } finally {
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        register,
        loginAsGuest,
        loginWithGoogle,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
