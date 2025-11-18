import { TruckControllerApi, TruckRequest, TruckView } from '@/api/client';
import { apiInstance } from './apiInstance';

const truckApi = new TruckControllerApi(apiInstance.config);

export type Truck = TruckView;

export const truckService = {
  getAll: () => truckApi.getAllTrucks().then((r) => r.data),
  getById: (id: string) =>
    truckApi.getTruckByLicensePlate(id).then((r) => r.data),
  create: (truck: TruckRequest) =>
    truckApi.createTruck(truck).then((r) => r.data),
  update: (truck: TruckRequest) =>
    truckApi.updateTruck(truck.licensePlate, truck).then((r) => r.data),
  delete: (id: string) => truckApi.deleteTruck(id),
};
