export type Role = 'USER' | 'ADMIN';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  birthDate: string | null;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  role: Role;
}

export interface Session extends AuthResponse {
  expiresAt: number;
}
