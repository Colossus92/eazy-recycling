import { TruckControllerApi, Truck } from '@/api/client';
import { apiInstance } from './apiInstance';

const truckApi = new TruckControllerApi(apiInstance.config);

export const truckService = {
  getAll: () => truckApi.getAllTrucks().then((r) => r.data),
  getById: (id: string) => truckApi.getTruckByLicensePlate(id).then((r) => r.data),
  create: (truck: Omit<Truck, 'id'>) => truckApi.createTruck(truck).then((r) => r.data),
  update: (truck: Truck) => truckApi.updateTruck(truck.licensePlate, truck).then((r) => r.data),
  delete: (id: string) => truckApi.deleteTruck(id),
};