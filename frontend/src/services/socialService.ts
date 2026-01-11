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
  }
};
