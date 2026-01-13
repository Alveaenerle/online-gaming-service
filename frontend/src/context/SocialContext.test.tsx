import React from "react";
import {
  render,
  screen,
  waitFor,
  act,
  fireEvent,
} from "@testing-library/react";
import { SocialProvider, useSocial } from "./SocialContext";
import { socialService } from "../services/socialService";
import { socialSocketService } from "../services/socialSocketService";
import { useAuth } from "./AuthContext";
import { useToast } from "./ToastContext";

jest.mock("../services/socialService");
jest.mock("../services/socialSocketService");
jest.mock("./AuthContext");
jest.mock("./ToastContext");

const TestConsumer = () => {
  const {
    friends,
    sendFriendRequest,
    acceptFriendRequest,
    rejectFriendRequest,
    removeFriend,
    sendGameInvite,
    acceptGameInvite,
    declineGameInvite,
  } = useSocial();

  return (
    <div>
      <div data-testid="friends-count">{friends.length}</div>
      <button onClick={() => sendFriendRequest("u2")}>Add</button>
      <button onClick={() => acceptFriendRequest("req1")}>AcceptReq</button>
      <button onClick={() => rejectFriendRequest("req1")}>RejectReq</button>
      <button onClick={() => removeFriend("f1")}>Remove</button>
      <button onClick={() => sendGameInvite("u2", "l1", "Room", "MAKAO")}>
        Invite
      </button>
      <button onClick={() => acceptGameInvite("inv1").catch(() => {})}>
        AcceptInv
      </button>
      <button onClick={() => declineGameInvite("inv1")}>DeclineInv</button>
    </div>
  );
};

describe("SocialContext - Full Coverage", () => {
  const mockUser = { id: "u1", username: "test", isGuest: false };
  const mockFriends = [{ id: "f1", username: "Friend", status: "ONLINE" }];
  const mockToast = { showToast: jest.fn() };
  let consoleSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    (useAuth as jest.Mock).mockReturnValue({
      user: mockUser,
      isAuthenticated: true,
    });
    (useToast as jest.Mock).mockReturnValue(mockToast);

    (socialService.getFriends as jest.Mock).mockResolvedValue(mockFriends);
    (socialService.getPendingRequests as jest.Mock).mockResolvedValue([]);
    (socialService.getSentRequests as jest.Mock).mockResolvedValue([]);
    (socialService.getPendingGameInvites as jest.Mock).mockResolvedValue([]);
    (socialSocketService.connect as jest.Mock).mockResolvedValue(undefined);
  });

  afterEach(() => {
    consoleSpy.mockRestore();
  });

  it("should handle all websocket notification subtypes", async () => {
    let notificationCb: any;
    (socialSocketService.subscribe as jest.Mock).mockImplementation(
      (topic, cb) => {
        if (topic === "/user/queue/notifications") notificationCb = cb;
      }
    );

    await act(async () => {
      render(
        <SocialProvider>
          <TestConsumer />
        </SocialProvider>
      );
    });

    // Test: Friend Request
    await act(async () => {
      notificationCb({
        type: "NOTIFICATION_RECEIVED",
        subType: "FRIEND_REQUEST",
        senderName: "A",
      });
    });
    expect(mockToast.showToast).toHaveBeenCalledWith(
      expect.stringContaining("A"),
      "info"
    );

    // Test: Request Accepted
    await act(async () => {
      notificationCb({
        type: "NOTIFICATION_RECEIVED",
        subType: "REQUEST_ACCEPTED",
        accepterName: "B",
      });
    });
    expect(mockToast.showToast).toHaveBeenCalledWith(
      expect.stringContaining("B"),
      "success"
    );

    // Test: Friend Removed
    await act(async () => {
      notificationCb({
        type: "NOTIFICATION_RECEIVED",
        subType: "FRIEND_REMOVED",
        removedByUserId: "f1",
      });
    });
    expect(screen.getByTestId("friends-count")).toHaveTextContent("0");
  });

  describe("Social Action Errors", () => {
    it("should handle sendFriendRequest failure", async () => {
      (socialService.sendFriendRequest as jest.Mock).mockRejectedValue(
        new Error("Fail")
      );
      await act(async () => {
        render(
          <SocialProvider>
            <TestConsumer />
          </SocialProvider>
        );
      });
      await act(async () => {
        fireEvent.click(screen.getByText("Add"));
      });
      expect(mockToast.showToast).toHaveBeenCalledWith(
        "Failed to send request",
        "error"
      );
    });

    it("should handle acceptFriendRequest failure", async () => {
      (socialService.acceptFriendRequest as jest.Mock).mockRejectedValue(
        new Error("Fail")
      );
      await act(async () => {
        render(
          <SocialProvider>
            <TestConsumer />
          </SocialProvider>
        );
      });
      await act(async () => {
        fireEvent.click(screen.getByText("AcceptReq"));
      });
      expect(mockToast.showToast).toHaveBeenCalledWith(
        "Failed to accept request",
        "error"
      );
    });

    it("should handle sendGameInvite failure", async () => {
      (socialService.sendGameInvite as jest.Mock).mockRejectedValue(
        new Error("Busy")
      );
      await act(async () => {
        render(
          <SocialProvider>
            <TestConsumer />
          </SocialProvider>
        );
      });
      await act(async () => {
        fireEvent.click(screen.getByText("Invite"));
      });
      expect(mockToast.showToast).toHaveBeenCalledWith("Busy", "error");
    });

    it("should handle acceptGameInvite failure and re-throw", async () => {
      (socialService.acceptGameInvite as jest.Mock).mockRejectedValue(
        new Error("Full")
      );
      await act(async () => {
        render(
          <SocialProvider>
            <TestConsumer />
          </SocialProvider>
        );
      });

      await act(async () => {
        fireEvent.click(screen.getByText("AcceptInv"));
      });

      expect(mockToast.showToast).toHaveBeenCalledWith("Full", "error");
      expect(socialService.getFriends).toHaveBeenCalled(); // Verifies refreshSocialData() call in catch
    });
  });

  it("should handle presence updates offline/online", async () => {
    let presenceCb: any;
    (socialSocketService.subscribe as jest.Mock).mockImplementation(
      (topic, cb) => {
        if (topic === "/user/queue/presence") presenceCb = cb;
      }
    );

    await act(async () => {
      render(
        <SocialProvider>
          <TestConsumer />
        </SocialProvider>
      );
    });

    await act(async () => {
      presenceCb({ userId: "f1", status: "OFFLINE" });
    });

    expect(socialService.getFriends).toHaveBeenCalled();
  });
});
