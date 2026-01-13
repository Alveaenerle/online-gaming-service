import { authService, LoginRequest, RegisterRequest } from "./authService";

describe("authService", () => {
  const mockUser = { id: "1", username: "testuser", isGuest: false };

  beforeEach(() => {
    jest.resetAllMocks();
    global.fetch = jest.fn();
  });

  describe("login()", () => {
    it("should login and return user data on success", async () => {
      // First fetch: login (ok), second fetch: getCurrentUser (user data)
      (global.fetch as jest.Mock)
        .mockResolvedValueOnce({ ok: true })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => mockUser,
        });

      const credentials: LoginRequest = {
        email: "test@example.com",
        password: "123",
      };
      const result = await authService.login(credentials);

      expect(result).toEqual(mockUser);
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/login"),
        expect.any(Object)
      );
    });

    it("should throw an error when API returns a JSON error (e.g., 401)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 401,
        headers: new Headers({ "content-type": "application/json" }),
        json: async () => ({ message: "Invalid credentials" }),
      });

      await expect(
        authService.login({ email: "err@test.com", password: "p" })
      ).rejects.toThrow("Invalid credentials");
    });
  });

  describe("register()", () => {
    it("should return a success message upon registration", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        text: async () => "Registered successfully",
      });

      const data: RegisterRequest = {
        username: "user",
        email: "test@example.com",
        password: "p",
      };
      const result = await authService.register(data);

      expect(result).toBe("Registered successfully");
    });

    it("should throw a text error when response is not JSON", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 400,
        headers: new Headers({ "content-type": "text/plain" }),
        text: async () => "Email already taken",
      });

      await expect(authService.register({} as any)).rejects.toThrow(
        "Email already taken"
      );
    });
  });

  describe("loginAsGuest()", () => {
    it("should login as guest and fetch profile", async () => {
      (global.fetch as jest.Mock)
        .mockResolvedValueOnce({ ok: true })
        .mockResolvedValueOnce({
          ok: true,
          json: async () => ({ ...mockUser, isGuest: true }),
        });

      const user = await authService.loginAsGuest();
      expect(user.isGuest).toBe(true);
    });
  });

  describe("logout()", () => {
    it("should call logout without errors", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
      await expect(authService.logout()).resolves.not.toThrow();
    });
  });

  describe("getCurrentUser()", () => {
    it("should throw a specific error for expired session (401)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 401,
      });

      await expect(authService.getCurrentUser()).rejects.toThrow(
        "Session expired. Please log in again."
      );
    });
  });

  describe("updateUsername() & updatePassword()", () => {
    it("should update the username", async () => {
      const updatedUser = { ...mockUser, username: "newName" };
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => updatedUser,
      });

      const result = await authService.updateUsername("newName");
      expect(result.username).toBe("newName");
    });

    it("should call password update using PUT method", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
      await authService.updatePassword("oldPass", "newPass");

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("/update-password"),
        expect.objectContaining({ method: "PUT" })
      );
    });
  });

  describe("getUserEmail()", () => {
    it("should retrieve user email", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: true,
        json: async () => ({ email: "test@example.com" }),
      });

      const email = await authService.getUserEmail();
      expect(email).toBe("test@example.com");
    });
  });

  describe("parseErrorResponse logic (status handling)", () => {
    const testStatus = async (status: number, expectedMsg: string) => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: status,
        headers: new Headers(),
        text: async () => "",
      });
      await expect(authService.getUserEmail()).rejects.toThrow(expectedMsg);
    };

    it("should return correct messages for mapped HTTP statuses", async () => {
      await testStatus(400, "Invalid request. Please check your input.");
      await testStatus(404, "Service not found. Please try again later.");
      await testStatus(409, "Account already exists with this email.");
      await testStatus(500, "Server error. Please try again later.");
    });

    it("should handle raw error text for unknown status (lines 40-42)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 418, // I'm a teapot - status not in switch
        headers: new Headers({ "content-type": "text/plain" }),
        text: async () => "Raw error text",
      });
      await expect(authService.getUserEmail()).rejects.toThrow(
        "Raw error text"
      );
    });

    it("should return fallbackMessage when body is empty and status is unknown (lines 57-59)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 418,
        headers: new Headers(),
        text: async () => "",
      });
      // Fallback for getUserEmail is 'Failed to retrieve email.'
      await expect(authService.getUserEmail()).rejects.toThrow(
        "Failed to retrieve email."
      );
    });
  });

  describe("Error handling in specific services", () => {
    it("register() should throw server error (status 500)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 500,
      });
      await expect(authService.register({} as any)).rejects.toThrow(
        "Server error. Please try again later."
      );
    });

    it("loginAsGuest() should throw server error (status 500)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 500,
      });
      await expect(authService.loginAsGuest()).rejects.toThrow(
        "Server error. Please try again later."
      );
    });

    it("getCurrentUser() should throw server error (status 500)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 500,
      });
      await expect(authService.getCurrentUser()).rejects.toThrow(
        "Server error. Please try again later."
      );
    });

    it("updateUsername() should throw validation error (status 400)", async () => {
      (global.fetch as jest.Mock).mockResolvedValueOnce({
        ok: false,
        status: 400,
      });
      await expect(authService.updateUsername("test")).rejects.toThrow(
        "Invalid request. Please check your input."
      );
    });
  });
});
