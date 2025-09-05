import { http } from '@/api/http.ts';
import { DeleteResponse, Truck } from '@/types/api.ts';

export type TruckResponse = Truck;

export const truckService = {
  list: () => http.get<Truck[]>('/trucks').then((r) => r.data),
  create: (t: Omit<Truck, 'id'>) =>
    http.post<TruckResponse>('/trucks', t).then((r) => r.data),
  update: (truck: Truck) =>
    http
      .put<TruckResponse>(`/trucks/${truck.licensePlate}`, truck)
      .then((r) => r.data),
  remove: (id: string) =>
    http
      .delete<void>(`/trucks/${id}`)
      .then(() => ({ success: true }) as DeleteResponse)
      .catch(() => ({ success: false }) as DeleteResponse),
};
