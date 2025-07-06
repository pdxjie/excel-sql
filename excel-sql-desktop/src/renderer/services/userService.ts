import api from './api';

export interface User {
  id: string;
  username: string;
  email: string;
  role: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  token: string;
}

const userService = {
  /**
   * Login user
   */
  login(credentials: LoginRequest): Promise<LoginResponse> {
    return api.post('/auth/login', credentials)
      .then(response => {
        const data = response.data;
        // Store token in localStorage
        if (data.token) {
          localStorage.setItem('token', data.token);
          localStorage.setItem('user', JSON.stringify(data.user));
        }
        return data;
      });
  },

  /**
   * Logout user
   */
  logout(): Promise<void> {
    return api.post('/auth/logout')
      .then(() => {
        // Clear local storage
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      });
  },

  /**
   * Get current user
   */
  getCurrentUser(): Promise<User> {
    return api.get('/users/me')
      .then(response => response.data);
  },

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  },

  /**
   * Get stored user
   */
  getStoredUser(): User | null {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  }
};

export default userService; 