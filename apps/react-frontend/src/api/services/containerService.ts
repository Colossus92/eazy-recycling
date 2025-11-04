import { WasteContainerControllerApi, WasteContainerRequest } from '../client';
import { apiInstance } from './apiInstance';

const containerApi = new WasteContainerControllerApi(apiInstance.config);

export const containerService = {
  getAll: () => containerApi.getAllContainers().then((r) => r.data),
  create: (c: WasteContainerRequest) =>
    containerApi.createContainer(c).then((r) => r.data),
  update: (id: string, c: WasteContainerRequest) =>
    containerApi.updateContainer(id, c).then((r) => r.data),
  delete: (id: string) => containerApi.deleteContainer(id),
};
