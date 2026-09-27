import { Role } from '../auth/auth.model';

export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
  workoutCount: number;
}
