import { http } from '@/api/http.ts';
import { Eural } from '@/types/api.ts';

export const euralService = {
  list: () => http.get<Eural[]>('/eural').then((r) => r.data),
};
