# Google OAuth Setup Guide

This guide walks you through setting up Google OAuth (Sign in with Google) for the Online Gaming Service authorization module.

## Prerequisites

- Google Account
- Access to Google Cloud Console
- Backend service running

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top of the page
3. Click "New Project"
4. Enter a project name (e.g., "Online Gaming Service")
5. Click "Create"

## Step 2: Enable Required APIs

1. In your Google Cloud project, go to **APIs & Services** > **Library**
2. Search for "Google+ API" and enable it (or "Google People API" for newer setups)
3. Search for "Google Identity Toolkit API" and enable it

## Step 3: Configure OAuth Consent Screen

1. Go to **APIs & Services** > **OAuth consent screen**
2. Select **External** user type (unless you have a Google Workspace organization)
3. Click "Create"
4. Fill in the required information:
   - **App name**: "Online Gaming Service" (or your preferred name)
   - **User support email**: Your email address
   - **Developer contact email**: Your email address
5. Click "Save and Continue"
6. On the **Scopes** page:
   - Click "Add or Remove Scopes"
   - Select:
     - `email` (View your email address)
     - `profile` (See your personal info)
     - `openid` (Associate you with your personal info)
   - Click "Update"
7. Click "Save and Continue"
8. On the **Test users** page (if in testing mode):
   - Add your email addresses for testing
   - Click "Add Users"
9. Click "Save and Continue"
10. Review and click "Back to Dashboard"

## Step 4: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click "Create Credentials" > "OAuth client ID"
3. Select **Web application** as the application type
4. Enter a name (e.g., "Online Gaming Service Web Client")
5. Add **Authorized JavaScript origins**:
   ```
   http://localhost:5173
   http://localhost:3000
   http://localhost
   https://your-production-domain.com
   ```
6. Add **Authorized redirect URIs**:
   ```
   http://localhost:5173
   http://localhost:3000
   https://your-production-domain.com
   ```
7. Click "Create"
8. **IMPORTANT**: Copy the **Client ID** - you'll need this for both backend and frontend

## Step 5: Configure Backend Environment Variables

Set the following environment variable before starting the authorization service:

### Local Development

```bash
export GOOGLE_OAUTH_CLIENT_ID="your-client-id-here.apps.googleusercontent.com"
```

### Docker Compose

Add to your `docker-compose.yml` or `docker-compose.dev.yml`:

```yaml
services:
  authorization:
    environment:
      - GOOGLE_OAUTH_CLIENT_ID=${GOOGLE_OAUTH_CLIENT_ID}
```

Then set the environment variable on your host machine or in a `.env` file:

```env
GOOGLE_OAUTH_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
```

### Production/Kubernetes

Add to your deployment configuration:

```yaml
env:
  - name: GOOGLE_OAUTH_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: oauth-secrets
        key: google-client-id
```

## Step 6: Configure Frontend

The frontend needs to integrate with the Google Sign-In library and use the same Client ID.

### Install Google Sign-In Library

```bash
npm install @react-oauth/google
```

### Add Google OAuth Provider

In your frontend's `main.tsx` or `App.tsx`:

```tsx
import { GoogleOAuthProvider } from '@react-oauth/google';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      {/* Your app components */}
    </GoogleOAuthProvider>
  );
}
```

### Create Google Login Button

```tsx
import { useGoogleLogin, GoogleLogin } from '@react-oauth/google';

// Option 1: Using the pre-built button
<GoogleLogin
  onSuccess={(credentialResponse) => {
    // Send credentialResponse.credential (ID token) to backend
    loginWithGoogle(credentialResponse.credential);
  }}
  onError={() => {
    console.log('Login Failed');
  }}
/>

// Option 2: Custom button with hook
const googleLogin = useGoogleLogin({
  onSuccess: (tokenResponse) => {
    // Handle success
  },
  flow: 'implicit', // or 'auth-code' for authorization code flow
});

<button onClick={() => googleLogin()}>Sign in with Google</button>
```

### Update Auth Service

Add to `authService.ts`:

```typescript
async loginWithGoogle(idToken: string): Promise<User> {
  const response = await fetch(`${API_BASE_URL}/oauth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ idToken }),
  });

  if (!response.ok) {
    const errorMessage = await parseErrorResponse(response, 'Google sign-in failed.');
    throw new Error(errorMessage);
  }

  return this.getCurrentUser();
}

async isGoogleOAuthConfigured(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/oauth/google/configured`, {
      method: 'GET',
      credentials: 'include',
    });
    const data = await response.json();
    return data.configured;
  } catch {
    return false;
  }
}
```

### Set Frontend Environment Variable

Add to your `.env` or `.env.local`:

```env
VITE_GOOGLE_CLIENT_ID=your-client-id-here.apps.googleusercontent.com
```

## Step 7: Update Auth Context (Optional)

Add Google login to your `AuthContext.tsx`:

```tsx
interface AuthContextType {
  // ... existing properties
  loginWithGoogle: (idToken: string) => Promise<void>;
  isGoogleLoginAvailable: boolean;
}

// In the provider:
const [isGoogleLoginAvailable, setIsGoogleLoginAvailable] = useState(false);

useEffect(() => {
  const checkGoogleOAuth = async () => {
    const configured = await authService.isGoogleOAuthConfigured();
    setIsGoogleLoginAvailable(configured);
  };
  checkGoogleOAuth();
}, []);

const loginWithGoogle = async (idToken: string) => {
  const userData = await authService.loginWithGoogle(idToken);
  setUser(userData);
};
```

## Verification

### Check if OAuth is Configured

After setting up, you can verify the configuration by calling:

```bash
curl http://localhost:8080/oauth/google/configured
```

Expected response when configured:
```json
{"configured": true}
```

### Test the Login Flow

1. Start the backend with the environment variable set
2. Start the frontend with the Google OAuth provider
3. Click "Sign in with Google"
4. Authenticate with your Google account
5. You should be logged in and redirected

## Troubleshooting

### "Google Sign-In is not available" (503 Error)

- Ensure `GOOGLE_OAUTH_CLIENT_ID` environment variable is set
- Restart the authorization service after setting the variable

### "Invalid Google ID token" (401 Error)

- Verify the Client ID matches between frontend and backend
- Check if the token has expired (tokens are short-lived)
- Ensure the JavaScript origin is properly configured in Google Cloud Console

### "Email is not verified with Google"

- The user's Google account email must be verified
- This is a security measure to prevent impersonation

### OAuth Consent Screen Errors

- If in "Testing" mode, only added test users can sign in
- To allow any user, publish the app (requires verification for sensitive scopes)

## Security Considerations

1. **Never expose the Client Secret** - For this implementation (ID token flow), you only need the Client ID
2. **Use HTTPS in production** - Google requires HTTPS for authorized origins in production
3. **Verify tokens server-side** - Always verify Google ID tokens on the backend, never trust the client
4. **Session management** - The existing session management applies to OAuth logins

## API Reference

### POST /oauth/google

Authenticate with Google OAuth.

**Request Body:**
```json
{
  "idToken": "google-id-token-from-client"
}
```

**Success Response (200):**
```
Login successful
```
Sets session cookie.

**Error Responses:**
- `400` - Invalid request (missing token)
- `401` - Invalid token or verification failed
- `503` - Google OAuth not configured

### GET /oauth/google/configured

Check if Google OAuth is configured.

**Response:**
```json
{
  "configured": true
}
```

## Migration Notes

- Existing accounts with the same email will be linked to the Google account on first OAuth login
- Once linked, the `authProvider` field is updated to `GOOGLE`
- Users can still log in with their password after linking (hybrid accounts)
- New OAuth-only accounts cannot use password authentication
