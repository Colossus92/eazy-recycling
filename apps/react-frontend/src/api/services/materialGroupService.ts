import { MaterialGroupControllerApi, MaterialGroupRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialGroupApi = new MaterialGroupControllerApi(apiInstance.config);

export const materialGroupService = {
  getAll: () => materialGroupApi.getAllMaterialGroups().then((r) => r.data),
  create: (materialGroup: MaterialGroupRequest) =>
    materialGroupApi.createMaterialGroup(materialGroup).then((r) => r.data),
  update: (id: number, materialGroup: MaterialGroupRequest) =>
    materialGroupApi.updateMaterialGroup(id, materialGroup).then((r) => r.data),
  delete: (id: number) => materialGroupApi.deleteMaterialGroup(id),
};
