export interface LoginRequest {
  username: string;
  password: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  role?: string[];
}

export interface AuthResponse {
  id: number;
  username: string;
  email: string;
  roles: string[];
  refreshToken: string;
  token?: string;
  accessToken?: string;    // Kept for compatibility
  type: string;            // "Bearer"
  tokenType?: string;      // Kept for compatibility..
}

export interface JwtResponse {
  token: string;
  type: string;
  refreshToken: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

export interface TokenRefreshRequest {
  refreshToken: string;
}
