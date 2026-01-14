import React from "react";
import {
  render,
  screen,
  waitFor,
  act,
  fireEvent,
} from "@testing-library/react";
import { AuthProvider, useAuth } from "./AuthContext";
import { authService } from "../services/authService";
import { lobbyService } from "../services/lobbyService";

jest.mock("../services/authService");
jest.mock("../services/lobbyService");

const TestConsumer = () => {
  const {
    user,
    isLoading,
    isAuthenticated,
    login,
    logout,
    loginAsGuest,
    register,
  } = useAuth();
  if (isLoading) return <div data-testid="loading">Loading...</div>;
  return (
    <div>
      <div data-testid="auth-status">
        {isAuthenticated ? "Authenticated" : "Not Authenticated"}
      </div>
      <div data-testid="username">{user?.username || "Guest"}</div>
      <button onClick={() => login({ email: "test@wp.pl", password: "123" })}>
        Login
      </button>
      <button onClick={() => logout()}>Logout</button>
      <button onClick={() => loginAsGuest()}>Guest Login</button>
      <button
        onClick={() =>
          register({ username: "new", email: "n@n.pl", password: "123" })
        }
      >
        Register
      </button>
    </div>
  );
};

describe("AuthContext", () => {
  const mockUser = { id: "1", username: "testuser", isGuest: false };

  beforeEach(() => {
    jest.clearAllMocks();
    // Domyślnie sesja wygasła, żeby testy akcji startowały z czystym kontem
    (authService.getCurrentUser as jest.Mock).mockRejectedValue(new Error());
  });

  it("should set user when session exists on mount", async () => {
    (authService.getCurrentUser as jest.Mock).mockResolvedValue(mockUser);
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("auth-status")).toHaveTextContent(
        "Authenticated"
      );
    });
  });

  it("should update user after successful login", async () => {
    (authService.login as jest.Mock).mockResolvedValue(mockUser);
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    // Czekamy aż Loading zniknie
    await waitFor(() =>
      expect(screen.queryByTestId("loading")).not.toBeInTheDocument()
    );

    await act(async () => {
      fireEvent.click(screen.getByText("Login"));
    });

    expect(screen.getByTestId("username")).toHaveTextContent("testuser");
  });

  it("should register and then login automatically", async () => {
    (authService.register as jest.Mock).mockResolvedValue("Success");
    (authService.login as jest.Mock).mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() =>
      expect(screen.queryByTestId("loading")).not.toBeInTheDocument()
    );

    await act(async () => {
      fireEvent.click(screen.getByText("Register"));
    });

    expect(authService.register).toHaveBeenCalled();
    expect(screen.getByTestId("username")).toHaveTextContent("testuser");
  });

  it("should login as guest", async () => {
    const guestUser = { ...mockUser, isGuest: true, username: "Guest_123" };
    (authService.loginAsGuest as jest.Mock).mockResolvedValue(guestUser);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() =>
      expect(screen.queryByTestId("loading")).not.toBeInTheDocument()
    );

    await act(async () => {
      fireEvent.click(screen.getByText("Guest Login"));
    });

    expect(screen.getByTestId("username")).toHaveTextContent("Guest_123");
  });

  it("should attempt to leave room and then logout", async () => {
    // Startujemy jako zalogowani
    (authService.getCurrentUser as jest.Mock).mockResolvedValue(mockUser);
    (authService.logout as jest.Mock).mockResolvedValue(undefined);
    (lobbyService.leaveRoom as jest.Mock).mockResolvedValue({
      message: "Left",
    });

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() =>
      expect(screen.getByTestId("auth-status")).toHaveTextContent(
        "Authenticated"
      )
    );

    await act(async () => {
      fireEvent.click(screen.getByText("Logout"));
    });

    expect(lobbyService.leaveRoom).toHaveBeenCalled();
    expect(authService.logout).toHaveBeenCalled();
    expect(screen.getByTestId("auth-status")).toHaveTextContent(
      "Not Authenticated"
    );
  });

  it("should proceed with logout even if leaveRoom fails", async () => {
    (authService.getCurrentUser as jest.Mock).mockResolvedValue(mockUser);
    (lobbyService.leaveRoom as jest.Mock).mockRejectedValue(new Error("Fail"));
    (authService.logout as jest.Mock).mockResolvedValue(undefined);

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() =>
      expect(screen.getByTestId("auth-status")).toHaveTextContent(
        "Authenticated"
      )
    );

    await act(async () => {
      fireEvent.click(screen.getByText("Logout"));
    });

    expect(authService.logout).toHaveBeenCalled();
    expect(screen.getByTestId("auth-status")).toHaveTextContent(
      "Not Authenticated"
    );
  });
});
