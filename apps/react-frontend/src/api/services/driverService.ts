import { DriverControllerApi } from '../client';
import { apiInstance } from './apiInstance';

const driverApi = new DriverControllerApi(apiInstance.config);

export const driverService = {
  getAllDrivers: () => driverApi.getAllDrivers().then((r) => r.data),
};
