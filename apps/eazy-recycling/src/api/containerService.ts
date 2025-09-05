import { DeleteResponse, WasteContainer } from '@/types/api.ts';
import { http } from '@/api/http.ts';

export type WasteContainerResponse = WasteContainer;

export const containerService = {
  list: () => http.get<WasteContainer[]>('/containers').then((r) => r.data),
  create: (c: Omit<WasteContainer, 'id'>) =>
    http.post<WasteContainerResponse>('/containers', c).then((r) => r.data),
  update: (c: WasteContainer) =>
    http
      .put<WasteContainerResponse>(`/containers/${c.uuid}`, c)
      .then((r) => r.data),
  remove: (c: WasteContainer) =>
    http
      .delete<void>(`/containers/${c.uuid}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
};
