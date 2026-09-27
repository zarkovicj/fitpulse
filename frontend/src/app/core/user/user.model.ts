import { Role } from '../auth/auth.model';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  birthDate: string | null;
  weight: number | null;
  height: number | null;
  role: Role;
  createdAt: string;
}

export interface UpdateUserRequest {
  firstName: string;
  lastName: string;
  birthDate: string | null;
  height: number | null;
}
