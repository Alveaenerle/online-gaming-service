import React from "react";
import { render, screen, waitFor, act } from "@testing-library/react";
import { LobbyProvider, useLobby } from "./LobbyContext";
import { lobbyService } from "../services/lobbyService";
import { socketService } from "../services/socketService";
import { useAuth } from "./AuthContext";
import { useToast } from "./ToastContext";
import { MemoryRouter } from "react-router-dom";

jest.mock("../services/lobbyService");
jest.mock("../services/socketService");
jest.mock("./AuthContext");
jest.mock("./ToastContext");

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...(jest.requireActual("react-router-dom") as any),
  useNavigate: () => mockNavigate,
}));

const TestConsumer = () => {
  const { currentLobby, clearLobby } = useLobby();
  return (
    <div>
      <div data-testid="lobby-id">{currentLobby?.id || "no-lobby"}</div>
      <button onClick={clearLobby}>Clear</button>
    </div>
  );
};

describe("LobbyContext", () => {
  const mockUser = { id: "u1", username: "test" };
  const mockLobby = {
    id: "room-123",
    gameType: "MAKAO",
    players: [],
    status: "LOBBY",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useAuth as jest.Mock).mockReturnValue({ user: mockUser });
    (useToast as jest.Mock).mockReturnValue({ showToast: jest.fn() });
    (socketService.connect as jest.Mock).mockResolvedValue(undefined);
    (lobbyService.getRoomInfo as jest.Mock).mockResolvedValue(mockLobby);
  });

  it("should fetch lobby info and subscribe on mount", async () => {
    render(
      <MemoryRouter>
        <LobbyProvider>
          <TestConsumer />
        </LobbyProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(lobbyService.getRoomInfo).toHaveBeenCalled();
      expect(screen.getByTestId("lobby-id")).toHaveTextContent("room-123");
    });

    await waitFor(() => {
      expect(socketService.subscribe).toHaveBeenCalledWith(
        expect.stringContaining("room-123"),
        expect.any(Function)
      );
    });
  });

  it("should handle being kicked", async () => {
    let kickCallback: any;
    (socketService.subscribe as jest.Mock).mockImplementation((topic, cb) => {
      if (topic.includes("kicked")) kickCallback = cb;
    });

    render(
      <MemoryRouter>
        <LobbyProvider>
          <TestConsumer />
        </LobbyProvider>
      </MemoryRouter>
    );

    await waitFor(() => expect(kickCallback).toBeDefined());

    act(() => {
      kickCallback({ type: "KICKED", kickedBy: "admin", message: "Bye" });
    });

    expect(screen.getByTestId("lobby-id")).toHaveTextContent("no-lobby");
    expect(mockNavigate).toHaveBeenCalledWith("/home");
  });

  it("should clear lobby when user logs out", async () => {
    const { rerender } = render(
      <MemoryRouter>
        <LobbyProvider>
          <TestConsumer />
        </LobbyProvider>
      </MemoryRouter>
    );

    await waitFor(() =>
      expect(screen.getByTestId("lobby-id")).toHaveTextContent("room-123")
    );

    // Change auth state to logged out
    (useAuth as jest.Mock).mockReturnValue({ user: null });

    rerender(
      <MemoryRouter>
        <LobbyProvider>
          <TestConsumer />
        </LobbyProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId("lobby-id")).toHaveTextContent("no-lobby");
    });
  });
});
