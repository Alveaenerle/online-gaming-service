const API_BASE_URL =
  typeof import.meta !== "undefined" && import.meta.env
    ? import.meta.env.VITE_STATISTICS_API_URL || "/api/statistics"
    : "/api/statistics";

export interface PlayerStatistics {
  playerId: string;
  username: string | null;
  gameType: string;
  gamesPlayed: number;
  gamesWon: number;
  winRatio: number;
}

export interface PlayerAllStatistics {
  playerId: string;
  statistics: PlayerStatistics[];
}

export interface Rankings {
  gameType: string;
  topByGamesPlayed: PlayerStatistics[];
  topByGamesWon: PlayerStatistics[];
}

async function parseErrorResponse(
  response: Response,
  fallbackMessage: string
): Promise<string> {
  try {
    const contentType = response.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      const errorData = await response.json();
      return errorData.message || errorData.error || fallbackMessage;
    }
    const errorText = await response.text();
    if (errorText) {
      return errorText;
    }
  } catch {
    // Failed to parse response body
  }
  return fallbackMessage;
}

export const statisticsService = {
  /**
   * Get current user's statistics for a specific game type.
   */
  async getMyStatistics(gameType: string): Promise<PlayerStatistics> {
    const response = await fetch(`${API_BASE_URL}/me/${gameType}`, {
      method: "GET",
      credentials: "include",
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(
        response,
        "Failed to fetch statistics"
      );
      throw new Error(errorMessage);
    }

    return response.json();
  },

  /**
   * Get all statistics for the current user.
   */
  async getAllMyStatistics(): Promise<PlayerAllStatistics> {
    const response = await fetch(`${API_BASE_URL}/me`, {
      method: "GET",
      credentials: "include",
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(
        response,
        "Failed to fetch statistics"
      );
      throw new Error(errorMessage);
    }

    return response.json();
  },

  /**
   * Get statistics for a specific player and game type.
   */
  async getPlayerStatistics(
    playerId: string,
    gameType: string
  ): Promise<PlayerStatistics> {
    const response = await fetch(
      `${API_BASE_URL}/player/${playerId}/${gameType}`,
      {
        method: "GET",
      }
    );

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(
        response,
        "Failed to fetch player statistics"
      );
      throw new Error(errorMessage);
    }

    return response.json();
  },

  /**
   * Get rankings for a specific game type.
   */
  async getRankings(gameType: string, limit: number = 30): Promise<Rankings> {
    const response = await fetch(
      `${API_BASE_URL}/rankings/${gameType}?limit=${limit}`,
      {
        method: "GET",
      }
    );

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(
        response,
        "Failed to fetch rankings"
      );
      throw new Error(errorMessage);
    }

    return response.json();
  },
};

export default statisticsService;
