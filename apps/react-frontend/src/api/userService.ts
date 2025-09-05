import { DeleteResponse, User } from '@/types/api.ts';
import { http } from '@/api/http.ts';

export type UserResponse = User;

export const userService = {
  list: () => http.get<User[]>('/users').then((r) => r.data),
  create: (t: Omit<User, 'id'>) =>
    http.post<UserResponse>('/users', t).then((r) => r.data),
  update: (user: User) =>
    http.put<UserResponse>(`/users/${user.id}`, user).then((r) => r.data),
  updateProfile: (user: User) =>
    http
      .put<UserResponse>(`/users/${user.id}/profile`, user)
      .then((r) => r.data),
  remove: (id: string) =>
    http
      .delete<void>(`/users/${id}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
  listDrivers: () => http.get<User[]>('/users/drivers').then((r) => r.data),
};
