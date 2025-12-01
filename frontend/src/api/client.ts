import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Token storage keys
const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const USER_KEY = 'user';

// Token management utilities
export const TokenManager = {
  getAccessToken: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  getUser: (): AuthUser | null => {
    const user = localStorage.getItem(USER_KEY);
    return user ? JSON.parse(user) : null;
  },
  
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  
  setUser: (user: AuthUser) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  
  clearAll: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem(ACCESS_TOKEN_KEY);
  }
};

// Auth types
export interface AuthUser {
  id: string;
  username: string;
  email: string;
  fullName?: string;
  roles: string[];
}

export interface UserProfile extends AuthUser {
  companyName?: string;
  emailVerified?: boolean;
  lastLogin?: string;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  fullName?: string;
  companyName?: string;
}

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = TokenManager.getAccessToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Flag to prevent multiple refresh requests
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor for error handling and token refresh
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    
    // Handle 401 errors (unauthorized)
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue the request while refreshing
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = TokenManager.getRefreshToken();
      
      if (refreshToken) {
        try {
          const response = await axios.post<AuthResponse>(
            `${API_BASE_URL}/api/auth/refresh`,
            { refreshToken }
          );
          
          const { accessToken, refreshToken: newRefreshToken } = response.data;
          TokenManager.setTokens(accessToken, newRefreshToken);
          
          processQueue(null, accessToken);
          
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }
          return api(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError as Error, null);
          // Refresh failed, logout user
          TokenManager.clearAll();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      } else {
        // No refresh token, redirect to login
        TokenManager.clearAll();
        window.location.href = '/login';
      }
    }

    // Handle 403 errors (forbidden)
    if (error.response?.status === 403) {
      console.error('Access denied:', error.response.data);
    }

    // Handle 429 errors (rate limited)
    if (error.response?.status === 429) {
      const retryAfter = error.response.headers['retry-after'];
      console.warn(`Rate limited. Retry after ${retryAfter} seconds`);
    }

    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// Auth API functions
export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/auth/login', credentials);
    const { accessToken, refreshToken, user } = response.data;
    TokenManager.setTokens(accessToken, refreshToken);
    TokenManager.setUser(user);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/auth/register', data);
    const { accessToken, refreshToken, user } = response.data;
    TokenManager.setTokens(accessToken, refreshToken);
    TokenManager.setUser(user);
    return response.data;
  },

  logout: async (): Promise<void> => {
    try {
      await api.post('/api/auth/logout');
    } finally {
      TokenManager.clearAll();
    }
  },

  getCurrentUser: async (): Promise<AuthUser> => {
    const response = await api.get<UserProfile>('/api/auth/me');
    // Convert UserProfile to AuthUser format for storage
    const { id, username, email, fullName, roles } = response.data;
    return { id, username, email, fullName, roles };
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.post('/api/auth/change-password', { currentPassword, newPassword });
  }
};

export default api;
