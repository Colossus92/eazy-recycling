import { WasteContainerControllerApi } from "../client";
import { apiInstance } from "./apiInstance";
import { CreateContainerRequest, WasteContainer } from "../client";


const containerApi = new WasteContainerControllerApi(apiInstance.config);

export const containerService = {
  getAll: () => containerApi.getAllContainers().then(r => r.data),
  create: (c: CreateContainerRequest) =>
    containerApi.createContainer(c).then(r => r.data),
  update: (c: WasteContainer) =>
    containerApi.updateContainer(c.id, c).then(r => r.data),
  delete: (id: string) => containerApi.deleteContainer(id),
};
