import { MaterialGroupControllerApi, MaterialGroupRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialGroupApi = new MaterialGroupControllerApi(apiInstance.config);

export const materialGroupService = {
  getAll: () => materialGroupApi.getAllMaterialGroups().then((r) => r.data),
  create: (materialGroup: MaterialGroupRequest) =>
    materialGroupApi.createMaterialGroup(materialGroup).then((r) => r.data),
  update: (id: string, materialGroup: MaterialGroupRequest) =>
    materialGroupApi.updateMaterialGroup(id, materialGroup).then((r) => r.data),
  delete: (id: string) => materialGroupApi.deleteMaterialGroup(id),
};
