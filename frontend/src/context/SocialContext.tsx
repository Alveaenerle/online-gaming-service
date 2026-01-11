import React, { createContext, useContext, useEffect, useState, ReactNode, useCallback } from 'react';
import { useAuth } from './AuthContext';
import { socialService, Friend, FriendRequest } from '../services/socialService';
import { socialSocketService } from '../services/socialSocketService';
import { useToast } from './ToastContext';

interface SocialContextType {
  friends: Friend[];
  pendingRequests: FriendRequest[];
  sentRequests: FriendRequest[];
  isLoading: boolean;
  sendFriendRequest: (userId: string) => Promise<void>;
  acceptFriendRequest: (requestId: string) => Promise<void>;
  rejectFriendRequest: (requestId: string) => Promise<void>;
  removeFriend: (friendId: string) => Promise<void>;
  refreshSocialData: () => Promise<void>;
}

const SocialContext = createContext<SocialContextType | undefined>(undefined);

export const SocialProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const [friends, setFriends] = useState<Friend[]>([]);
  const [pendingRequests, setPendingRequests] = useState<FriendRequest[]>([]);
  const [sentRequests, setSentRequests] = useState<FriendRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Fetch initial data
  const refreshSocialData = useCallback(async () => {
    if (!isAuthenticated || user?.isGuest) {
      console.log('[SocialContext] Skipping refresh - not authenticated or guest');
      return;
    }
    
    setIsLoading(true);
    try {
      console.log('[SocialContext] Fetching social data...');
      const [friendsData, requestsData, sentRequestsData] = await Promise.all([
        socialService.getFriends(),
        socialService.getPendingRequests(),
        socialService.getSentRequests()
      ]);
      console.log('[SocialContext] Received data:', { 
        friends: friendsData.length, 
        pending: requestsData.length, 
        sent: sentRequestsData.length 
      });
      setFriends(friendsData);
      setPendingRequests(requestsData);
      setSentRequests(sentRequestsData);
    } catch (error) {
      console.error('[SocialContext] Failed to fetch social data:', error);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user?.isGuest]);

  useEffect(() => {
    refreshSocialData();
  }, [refreshSocialData]);

  // WebSocket connection
  useEffect(() => {
    if (!isAuthenticated || user?.isGuest) return;

    let mounted = true;

    const setupSocket = async () => {
      try {
        await socialSocketService.connect();
        
        // Listen for notifications (friend requests, accepts)
        socialSocketService.subscribe('/user/queue/notifications', (notification: any) => {
           if (!mounted) return;
           
           if (notification.type === 'NOTIFICATION_RECEIVED') {
             if (notification.subType === 'FRIEND_REQUEST') {
                showToast(`${notification.senderName || 'Someone'} sent you a friend request`, 'info');
                refreshSocialData();
             } else if (notification.subType === 'REQUEST_ACCEPTED') {
                showToast(`${notification.accepterName || 'A user'} accepted your friend request`, 'success');
                refreshSocialData(); // Refresh to update friend list
             }
           }
        });

        // Listen for presence updates (friends going online/offline)
        socialSocketService.subscribe('/user/queue/presence', (presenceUpdate: { userId: string; status: 'ONLINE' | 'OFFLINE' }) => {
           if (!mounted) return;
           
           console.log('[SocialContext] Presence update:', presenceUpdate);
           
           // Update the friend's status in local state
           setFriends(prev => prev.map(friend => 
             friend.id === presenceUpdate.userId 
               ? { ...friend, status: presenceUpdate.status }
               : friend
           ));
        });

      } catch (err) {
        console.error('Social socket connection failed', err);
      }
    };

    setupSocket();

    return () => {
      mounted = false;
      socialSocketService.disconnect();
    };
  }, [isAuthenticated, user?.isGuest, showToast, refreshSocialData]);

  const sendFriendRequest = async (userId: string) => {
    // Optimistic update: immediately add to sentRequests to disable the button
    const optimisticRequest: FriendRequest = {
      id: `temp-${Date.now()}`,
      requesterId: user?.id || '',
      requesterUsername: user?.username || '',
      addresseeId: userId,
      status: 'PENDING',
      createdAt: new Date().toISOString()
    };
    setSentRequests(prev => [...prev, optimisticRequest]);
    
    try {
      await socialService.sendFriendRequest(userId);
      showToast("Friend request sent", 'success');
      // Refresh to get the real request ID from server
      await refreshSocialData();
    } catch (error) {
      // Rollback optimistic update on failure
      setSentRequests(prev => prev.filter(r => r.id !== optimisticRequest.id));
      showToast("Failed to send request", 'error');
      console.error(error);
    }
  };

  const acceptFriendRequest = async (requestId: string) => {
     try {
       await socialService.acceptFriendRequest(requestId);
       showToast("Friend request accepted", 'success');
       refreshSocialData();
     } catch (error) {
        showToast("Failed to accept request", 'error');
     }
  };

  const rejectFriendRequest = async (requestId: string) => {
      try {
          await socialService.rejectFriendRequest(requestId);
          showToast("Friend request rejected", 'info');
          refreshSocialData();
      } catch (error) {
        showToast("Failed to reject request", 'error');
      }
  };

  const removeFriend = async (friendId: string) => {
      try {
          await socialService.removeFriend(friendId);
          // Optimistic update: remove from local state
          setFriends(prev => prev.filter(f => f.id !== friendId));
          showToast("Friend removed", 'info');
      } catch (error) {
        showToast("Failed to remove friend", 'error');
        // Refresh to get actual state
        refreshSocialData();
      }
  };

  return (
    <SocialContext.Provider value={{
      friends,
      pendingRequests,
      sentRequests,
      isLoading,
      sendFriendRequest,
      acceptFriendRequest,
      rejectFriendRequest,
      removeFriend,
      refreshSocialData
    }}>
      {children}
    </SocialContext.Provider>
  );
};

export const useSocial = (): SocialContextType => {
  const context = useContext(SocialContext);
  if (!context) {
    throw new Error('useSocial must be used within a SocialProvider');
  }
  return context;
};
