'use client';

import { useState, useEffect, useCallback } from 'react';
import { chatApi } from '@/lib/api/chat';
import { Message } from '@/lib/types/message';
import { useWebSocket } from './useWebSocket';

export const useChat = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { connected, messages: wsMessages, sendMessage: wsSendMessage } = useWebSocket();

  // Load message history on mount
  useEffect(() => {
    loadMessages();
  }, []);

  // Add WebSocket messages to the list
  useEffect(() => {
    if (wsMessages.length > 0) {
      setMessages((prev) => [...prev, ...wsMessages]);
    }
  }, [wsMessages]);

  const loadMessages = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const history = await chatApi.getMessageHistory();
      setMessages(history);
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to load messages';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  }, []);

  const sendMessage = useCallback(async (content: string) => {
    try {
      setError(null);
      
      // Send via WebSocket if connected
      if (connected) {
        wsSendMessage('/app/chat.send', { content });
      } else {
        // Fallback to HTTP
        await chatApi.sendMessage(content);
        await loadMessages();
      }
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || 'Failed to send message';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  }, [connected, wsSendMessage, loadMessages]);

  return {
    messages,
    loading,
    error,
    connected,
    sendMessage,
    loadMessages,
  };
};
