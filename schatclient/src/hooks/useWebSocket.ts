'use client';

import { useEffect, useCallback, useState } from 'react';
import { webSocketService } from '@/lib/utils/websocket';
import { apiClient } from '@/lib/api/client';
import { Message } from '@/lib/types/message';

export const useWebSocket = () => {
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);

  const onMessageReceived = useCallback((message: Message) => {
    setMessages((prev) => [...prev, message]);
  }, []);

  useEffect(() => {
    const token = apiClient.getClient().defaults.headers.common['Authorization'];
    
    if (token && typeof token === 'string') {
      const bearerToken = token.replace('Bearer ', '');
      webSocketService.connect(bearerToken, onMessageReceived);
      setConnected(true);
    }

    return () => {
      webSocketService.disconnect();
      setConnected(false);
    };
  }, [onMessageReceived]);

  const sendMessage = useCallback((destination: string, message: any) => {
    webSocketService.sendMessage(destination, message);
  }, []);

  return {
    connected,
    messages,
    sendMessage,
  };
};
