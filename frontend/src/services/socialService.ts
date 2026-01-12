import { authService } from "./authService";

const API_BASE_URL = import.meta.env.VITE_API_SOCIAL_URL || '/api/social';

export interface Friend {
  id: string;
  username: string;
  status: 'ONLINE' | 'OFFLINE' | 'PLAYING';
  avatarUrl?: string;
}

export interface FriendRequest {
  id: string;
  requesterId: string;
  requesterUsername: string;
  addresseeId: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

export interface GameInvite {
  id: string;
  senderId: string;
  senderUsername: string;
  targetId: string;
  lobbyId: string;
  lobbyName: string;
  gameType: 'MAKAO' | 'LUDO';
  accessCode?: string;
  createdAt: number;
}

const getHeaders = () => {
    return {
        'Content-Type': 'application/json',
    };
};

const fetchWithCredentials = (url: string, options: RequestInit = {}) => {
    return fetch(url, {
        ...options,
        credentials: 'include',
        headers: {
            ...getHeaders(),
            ...options.headers,
        },
    });
};

export const socialService = {
  async sendFriendRequest(targetUserId: string): Promise<void> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/invite`, {
      method: 'POST',
      body: JSON.stringify({ targetUserId }),
    });
    if (!response.ok) throw new Error('Failed to send friend request');
  },

  async acceptFriendRequest(requestId: string): Promise<void> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/accept`, {
      method: 'POST',
      body: JSON.stringify({ requestId }),
    });
    if (!response.ok) throw new Error('Failed to accept friend request');
  },

  async rejectFriendRequest(requestId: string): Promise<void> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/reject`, {
      method: 'POST',
      body: JSON.stringify({ requestId }),
    });
    if (!response.ok) throw new Error('Failed to reject friend request');
  },

  async getFriends(): Promise<Friend[]> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends`, {
        method: 'GET',
    });
    if (!response.ok) throw new Error('Failed to fetch friends');
    return response.json();
  },

  async getPendingRequests(): Promise<FriendRequest[]> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/pending`, {
        method: 'GET',
    });
    if (!response.ok) throw new Error('Failed to fetch pending requests');
    return response.json();
  },

  async getSentRequests(): Promise<FriendRequest[]> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/sent`, {
        method: 'GET',
    });
    if (!response.ok) throw new Error('Failed to fetch sent requests');
    return response.json();
  },

  async removeFriend(friendId: string): Promise<void> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/friends/${friendId}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('Failed to remove friend');
  },

  // ============================================================
  // GAME INVITE METHODS
  // ============================================================

  async sendGameInvite(
    targetUserId: string,
    lobbyId: string,
    lobbyName: string,
    gameType: 'MAKAO' | 'LUDO'
  ): Promise<GameInvite> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/invites/send`, {
      method: 'POST',
      body: JSON.stringify({ targetUserId, lobbyId, lobbyName, gameType }),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Failed to send game invite');
    }
    return response.json();
  },

  async getPendingGameInvites(): Promise<GameInvite[]> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/invites/pending`, {
      method: 'GET',
    });
    if (!response.ok) throw new Error('Failed to fetch pending game invites');
    return response.json();
  },

  async getSentGameInvites(): Promise<GameInvite[]> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/invites/sent`, {
      method: 'GET',
    });
    if (!response.ok) throw new Error('Failed to fetch sent game invites');
    return response.json();
  },

  async acceptGameInvite(inviteId: string): Promise<GameInvite> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/invites/accept`, {
      method: 'POST',
      body: JSON.stringify({ inviteId }),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Failed to accept game invite');
    }
    return response.json();
  },

  async declineGameInvite(inviteId: string): Promise<GameInvite> {
    const response = await fetchWithCredentials(`${API_BASE_URL}/invites/decline`, {
      method: 'POST',
      body: JSON.stringify({ inviteId }),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Failed to decline game invite');
    }
    return response.json();
  }
};
