import { useEffect, useState, useRef } from "react";
import { socketService } from "../../../services/socketService";

export function useLobby(
  roomId: string | undefined,
  onUpdate: (data: any) => void
) {
  const [isConnected, setIsConnected] = useState(false);
  const onUpdateRef = useRef(onUpdate);

  useEffect(() => {
    onUpdateRef.current = onUpdate;
  }, [onUpdate]);

  useEffect(() => {
    if (!roomId) return;

    const setup = async () => {
      try {
        await socketService.connect();
        setIsConnected(true);
        socketService.subscribe(`/topic/room/${roomId}`, (data) => {
          onUpdateRef.current(data);
        });
      } catch (err) {
        console.error("Socket connection failed", err);
      }
    };

    setup();

    return () => {
      socketService.unsubscribe(`/topic/room/${roomId}`);
    };
  }, [roomId]);

  return { isConnected };
}
