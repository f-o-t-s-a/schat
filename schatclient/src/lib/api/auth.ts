// ============================================
// src/lib/api/auth.ts - COMPLETE FIXED VERSION
// ============================================

import { apiClient } from './client';
import { LoginRequest, SignupRequest, AuthResponse, TokenRefreshRequest } from '../types/auth';

export const authApi = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    console.log('🔵 authApi.login called with:', credentials.username);
    console.log('🔵 API URL:', process.env.NEXT_PUBLIC_API_URL);
    
    try {
      const response = await apiClient.getClient().post(
        '/api/auth/login',
        credentials
      );
      
      console.log('🟢 Login response received:', response.data);
      
      const data = response.data;
      
      // Your backend returns: { id, username, email, roles, refreshToken, token, type }
      const authResponse: AuthResponse = {
        accessToken: data.token || data.accessToken,
        refreshToken: data.refreshToken,
        tokenType: data.type || 'Bearer',
        id: data.id,
        username: data.username,
        email: data.email,
        roles: data.roles || []
      };
      
      console.log('🟢 Processed auth response:', authResponse);
      
      apiClient.setToken(authResponse.accessToken);
      apiClient.setRefreshToken(authResponse.refreshToken);
      apiClient.setUser({
        id: authResponse.id,
        username: authResponse.username,
        email: authResponse.email,
        roles: authResponse.roles
      });
      
      console.log('🟢 Tokens and user saved to localStorage');
      
      return authResponse;
    } catch (error: any) {
      console.error('🔴 authApi.login error:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
        url: error.config?.url
      });
      throw error;
    }
  },

  async register(userData: SignupRequest): Promise<any> {
    console.log('🔵 authApi.register called');
    
    try {
      const response = await apiClient.getClient().post(
        '/api/auth/register',
        userData
      );
      
      console.log('🟢 Registration response:', response.data);
      return response.data;
    } catch (error: any) {
      console.error('🔴 authApi.register error:', error.response?.data || error.message);
      throw error;
    }
  },

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    try {
      const response = await apiClient.getClient().post<any>(
        '/api/auth/refreshtoken',
        { refreshToken } as TokenRefreshRequest
      );
      
      const data = response.data;
      const authResponse: AuthResponse = {
        accessToken: data.token || data.accessToken,
        refreshToken: data.refreshToken,
        tokenType: data.type || 'Bearer',
        id: data.id,
        username: data.username,
        email: data.email,
        roles: data.roles || []
      };
      
      apiClient.setToken(authResponse.accessToken);
      if (authResponse.refreshToken) {
        apiClient.setRefreshToken(authResponse.refreshToken);
      }
      
      return authResponse;
    } catch (error) {
      console.error('Token refresh failed:', error);
      throw error;
    }
  },

  logout(): void {
    apiClient.clearTokens();
  },

  getCurrentUser(): any {
    return apiClient.getUser();
  },

  isAuthenticated(): boolean {
    return !!apiClient.getUser();
  },
};

/*
import { apiClient } from './client';
import { LoginRequest, SignupRequest, AuthResponse, TokenRefreshRequest } from '../types/auth';

export const authApi = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.getClient().post<AuthResponse>(
      '/api/auth/signin',
      credentials
    );
    
    const { accessToken, refreshToken, ...userData } = response.data;
    
    apiClient.setToken(accessToken);
    apiClient.setRefreshToken(refreshToken);
    apiClient.setUser(userData);
    
    return response.data;
  },

  async register(userData: SignupRequest): Promise<any> {
    const response = await apiClient.getClient().post(
      '/api/auth/signup',
      userData
    );
    return response.data;
  },

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.getClient().post<AuthResponse>(
      '/api/auth/refreshtoken',
      { refreshToken } as TokenRefreshRequest
    );
    
    const { accessToken, refreshToken: newRefreshToken } = response.data;
    
    apiClient.setToken(accessToken);
    if (newRefreshToken) {
      apiClient.setRefreshToken(newRefreshToken);
    }
    
    return response.data;
  },

  logout(): void {
    apiClient.clearTokens();
  },

  getCurrentUser(): any {
    return apiClient.getUser();
  },

  isAuthenticated(): boolean {
    return !!apiClient.getUser();
  },
};*/
