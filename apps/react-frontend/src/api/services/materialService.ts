import { MaterialControllerApi, MaterialRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialApi = new MaterialControllerApi(apiInstance.config);

export const materialService = {
  getAll: () => materialApi.getAllMaterials().then((r) => r.data),
  create: (material: MaterialRequest) =>
    materialApi.createMaterial(material).then((r) => r.data),
  update: (id: number, material: MaterialRequest) =>
    materialApi.updateMaterial(id, material).then((r) => r.data),
  delete: (id: number) => materialApi.deleteMaterial(id),
};
