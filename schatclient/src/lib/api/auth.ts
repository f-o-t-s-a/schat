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
};
