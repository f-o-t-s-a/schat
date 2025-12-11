'use client';

import { useEffect, useRef } from 'react';
import { Message } from '@/lib/types/message';

interface MessageListProps {
  messages: Message[];
  loading: boolean;
  currentUser: any;
}

export default function MessageList({ messages, loading, currentUser }: MessageListProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-gray-500">Loading messages...</div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-4">
      {messages.length === 0 ? (
        <div className="flex items-center justify-center h-full text-gray-500">
          No messages yet. Start a conversation!
        </div>
      ) : (
        messages.map((message) => {
          const isOwnMessage = message.sender === currentUser?.username;
          return (
            <div
              key={message.id}
              className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                  isOwnMessage
                    ? 'bg-indigo-600 text-white'
                    : 'bg-white text-gray-800 shadow'
                }`}
              >
                {!isOwnMessage && (
                  <div className="text-xs font-semibold mb-1">{message.sender}</div>
                )}
                <div className="break-words">{message.content}</div>
                <div className={`text-xs mt-1 ${isOwnMessage ? 'text-indigo-200' : 'text-gray-500'}`}>
                  {new Date(message.timestamp).toLocaleTimeString()}
                </div>
              </div>
            </div>
          );
        })
      )}
      <div ref={messagesEndRef} />
    </div>
  );
}
