const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/auth';

export const GOOGLE_CLIENT_ID = '67980051947-h1ngp505qo2ad20fr9it2m15inp6otff.apps.googleusercontent.com';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface GoogleLoginRequest {
  idToken: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface User {
  id: string;
  username: string;
  isGuest: boolean;
}

async function parseErrorResponse(response: Response, fallbackMessage: string): Promise<string> {
  const status = response.status;

  try {
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      const errorData = await response.json();
      return errorData.message || errorData.error || fallbackMessage;
    }

    const errorText = await response.text();
    if (errorText) {
      // Try to parse as JSON if it looks like JSON
      if (errorText.trim().startsWith('{')) {
        try {
          const parsed = JSON.parse(errorText);
          return parsed.message || parsed.error || errorText;
        } catch {
          // Not valid JSON, return as-is
        }
      }
      return errorText;
    }
  } catch {
    // Failed to parse response body
  }

  switch (status) {
    case 400:
      return 'Invalid request. Please check your input.';
    case 401:
      return 'Invalid credentials. Please try again.';
    case 403:
      return 'Access denied.';
    case 404:
      return 'Service not found. Please try again later.';
    case 409:
      return 'Account already exists with this email.';
    case 429:
      return 'Too many attempts. Please try again later.';
    case 500:
    case 502:
    case 503:
      return 'Server error. Please try again later.';
    default:
      return fallbackMessage;
  }
}

export const authService = {
  async login(credentials: LoginRequest): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Login failed. Please try again.');
      throw new Error(errorMessage);
    }

    return this.getCurrentUser();
  },

  async register(data: RegisterRequest): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Registration failed. Please try again.');
      throw new Error(errorMessage);
    }

    return response.text();
  },

  async loginAsGuest(): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/guest`, {
      method: 'POST',
      credentials: 'include',
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Guest login failed. Please try again.');
      throw new Error(errorMessage);
    }

    return this.getCurrentUser();
  },

  async loginWithGoogle(idToken: string): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/oauth/google`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ idToken }),
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Google login failed. Please try again.');
      throw new Error(errorMessage);
    }

    return this.getCurrentUser();
  },

  async logout(): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/logout`, {
      method: 'POST',
      credentials: 'include',
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Logout failed. Please try again.');
      throw new Error(errorMessage);
    }
  },

  async getCurrentUser(): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/me`, {
      method: 'GET',
      credentials: 'include',
    });

    if (!response.ok) {
      const status = response.status;
      if (status === 401) {
        throw new Error('Session expired. Please log in again.');
      }
      const errorMessage = await parseErrorResponse(response, 'Unable to retrieve user information. Please try again.');
      throw new Error(errorMessage);
    }

    return response.json();
  },

  async updateUsername(newUsername: string): Promise<User> {
    const response = await fetch(`${API_BASE_URL}/update-username`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ newUsername }),
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Failed to update username.');
      throw new Error(errorMessage);
    }

    return response.json();
  },

  async updatePassword(currentPassword: string, newPassword: string): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/update-password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ currentPassword, newPassword }),
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Failed to update password.');
      throw new Error(errorMessage);
    }
  },

  async getUserEmail(): Promise<string> {
    const response = await fetch(`${API_BASE_URL}/email`, {
      method: 'GET',
      credentials: 'include',
    });

    if (!response.ok) {
      const errorMessage = await parseErrorResponse(response, 'Failed to retrieve email.');
      throw new Error(errorMessage);
    }

    const data = await response.json();
    return data.email;
  },
};
