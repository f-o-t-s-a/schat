import { apiClient } from './client';
import { Message, MessageResponse } from '../types/message';

export const chatApi = {
  async getMessages(): Promise<Message[]> {
    const response = await apiClient.getClient().get<Message[]>('/api/chat/messages');
    return response.data;
  },

  async sendMessage(content: string): Promise<MessageResponse> {
    const response = await apiClient.getClient().post<MessageResponse>(
      '/api/chat/send',
      { content }
    );
    return response.data;
  },

  async getMessageHistory(roomId?: string): Promise<Message[]> {
    const url = roomId ? `/api/chat/history/${roomId}` : '/api/chat/history';
    const response = await apiClient.getClient().get<Message[]>(url);
    return response.data;
  },
};
