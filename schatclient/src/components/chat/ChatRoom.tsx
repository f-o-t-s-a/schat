'use client';

import { useState, useEffect } from 'react';
import { User } from '@/types';
// import ChatInterface from './ChatInterface'; // Optional: chat later

interface ChatRoomProps {
  user: User | null;
}

export default function ChatRoom({ user }: ChatRoomProps) {
  const [activeTab, setActiveTab] = useState<'profile' | 'messages' | 'settings'>('profile');
  const [isLoading, setIsLoading] = useState(true);
  const [userData, setUserData] = useState<any>(null);

  // Fetch additional user data on component mount
  useEffect(() => {
    const fetchUserData = async () => {
      if (!user) return;
      
      try {
        setIsLoading(true);
        const response = await fetch('/api/user/profile', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        });
        
        if (response.ok) {
          const data = await response.json();
          setUserData(data);
        }
      } catch (error) {
        console.error('Error fetching user data:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, [user]);

  // Handle logout
  const handleLogout = () => {
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-4 md:p-6">
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">Welcome back, {user?.name || user?.email}</p>
        </div>
        <button
          onClick={handleLogout}
          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
        >
          Logout
        </button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200 mb-6">
        <button
          onClick={() => setActiveTab('profile')}
          className={`px-4 py-2 font-medium ${activeTab === 'profile' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-700'}`}
        >
          Profile
        </button>
        <button
          onClick={() => setActiveTab('messages')}
          className={`px-4 py-2 font-medium ${activeTab === 'messages' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-700'}`}
        >
          Messages
        </button>
        <button
          onClick={() => setActiveTab('settings')}
          className={`px-4 py-2 font-medium ${activeTab === 'settings' ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500 hover:text-gray-700'}`}
        >
          Settings
        </button>
      </div>

      {/* Content based on active tab */}
      <div className="bg-white rounded-lg shadow-md p-6">
        {activeTab === 'profile' && (
          <div>
            <h2 className="text-xl font-semibold mb-4">Profile Information</h2>
            <div className="space-y-4">
              <div className="flex items-center space-x-4">
                <div className="w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center">
                  <span className="text-2xl font-bold text-blue-600">
                    {user?.name?.charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div>
                  <h3 className="text-lg font-medium">{user?.name || 'No name provided'}</h3>
                  <p className="text-gray-600">{user?.email}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-medium text-gray-700 mb-2">Account Details</h4>
                  <div className="space-y-2">
                    <p><span className="font-medium">User ID:</span> {user?.id}</p>
                    <p><span className="font-medium">Joined:</span> {new Date(user?.createdAt || Date.now()).toLocaleDateString()}</p>
                    <p><span className="font-medium">Status:</span> <span className="text-green-600">Active</span></p>
                  </div>
                </div>

                <div className="bg-gray-50 p-4 rounded-lg">
                  <h4 className="font-medium text-gray-700 mb-2">Statistics</h4>
                  <div className="space-y-2">
                    <p><span className="font-medium">Total Messages:</span> {userData?.messageCount || 0}</p>
                    <p><span className="font-medium">Chats Joined:</span> {userData?.chatCount || 0}</p>
                    <p><span className="font-medium">Last Active:</span> {userData?.lastActive ? new Date(userData.lastActive).toLocaleString() : 'Just now'}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'messages' && (
          <div>
            <h2 className="text-xl font-semibold mb-4">Recent Messages</h2>
            {userData?.recentMessages && userData.recentMessages.length > 0 ? (
              <div className="space-y-4">
                {userData.recentMessages.map((message: any, index: number) => (
                  <div key={index} className="border-b pb-4 last:border-0">
                    <div className="flex justify-between">
                      <p className="font-medium">{message.sender}</p>
                      <span className="text-sm text-gray-500">
                        {new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-gray-700 mt-1">{message.content}</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500">
                <p>No messages yet. Start a conversation!</p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'settings' && (
          <div>
            <h2 className="text-xl font-semibold mb-4">Account Settings</h2>
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Notification Preferences
                </label>
                <div className="space-y-2">
                  <label className="flex items-center">
                    <input type="checkbox" className="rounded text-blue-600" defaultChecked />
                    <span className="ml-2">Email notifications</span>
                  </label>
                  <label className="flex items-center">
                    <input type="checkbox" className="rounded text-blue-600" defaultChecked />
                    <span className="ml-2">Push notifications</span>
                  </label>
                </div>
              </div>

              <div>
                <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                  Update Preferences
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Optional: Chat interface section */}
      <div className="mt-8">
        <h2 className="text-xl font-semibold mb-4">Quick Chat</h2>
        {/* <ChatInterface /> - Uncomment when you implement chat */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <p className="text-gray-600">Chat functionality coming soon!</p>
          <button className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
            Start New Chat..
          </button>
        </div>
      </div>
    </div>
  );
}
