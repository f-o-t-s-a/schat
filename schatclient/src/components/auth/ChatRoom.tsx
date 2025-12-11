'use client';

import { useChat } from '@/hooks/useChat';
import { useAuth } from '@/hooks/useAuth';
import MessageList from './MessageList';
import MessageInput from './MessageInput';

export default function ChatRoom() {
  const { messages, loading, error, connected, sendMessage } = useChat();
  const { user, logout } = useAuth();

  return (
    <div className="flex flex-col h-screen bg-gray-100">
      {/* Header */}
      <div className="bg-white shadow-md px-4 py-3 flex justify-between items-center">
        <div>
          <h1 className="text-xl font-bold text-gray-800">SChat</h1>
          <p className="text-sm text-gray-500">
            {connected ? (
              <span className="text-green-600">● Connected</span>
            ) : (
              <span className="text-red-600">● Disconnected</span>
            )}
          </p>
        </div>
        <div className="flex items-center space-x-4">
          <span className="text-gray-700">Welcome, {user?.username}</span>
          <button
            onClick={logout}
            className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm"
          >
            Logout
          </button>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 m-4 rounded">
          {error}
        </div>
      )}

      {/* Messages */}
      <MessageList messages={messages} loading={loading} currentUser={user} />

      {/* Input */}
      <MessageInput onSendMessage={sendMessage} disabled={!connected} />
    </div>
  );
}
