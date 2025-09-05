import { http } from '@/api/http.ts';
import { DeleteResponse, WasteStream } from '@/types/api.ts';

export type WasteStreamResponse = WasteStream;

export const wasteStreamService = {
  list: () => http.get<WasteStream[]>('/waste-streams').then((r) => r.data),
  create: (t: Omit<WasteStream, 'id'>) =>
    http.post<WasteStreamResponse>('/waste-streams', t).then((r) => r.data),
  update: (wasteStream: WasteStream) =>
    http
      .put<WasteStreamResponse>(
        `/waste-streams/${wasteStream.number}`,
        wasteStream
      )
      .then((r) => r.data),
  remove: (id: string) =>
    http
      .delete<void>(`/waste-streams/${id}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
};
