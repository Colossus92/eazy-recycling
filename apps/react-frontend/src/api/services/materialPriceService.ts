import { MaterialPriceControllerApi, MaterialPriceRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialPriceApi = new MaterialPriceControllerApi(apiInstance.config);

export const materialPriceService = {
  getAll: () => materialPriceApi.getAllMaterialsWithPrices().then((r) => r.data),
  getById: (id: number) => materialPriceApi.getMaterialPrice(id).then((r) => r.data),
  create: (id: number, materialPrice: MaterialPriceRequest) =>
    materialPriceApi.createMaterialPrice(id, materialPrice).then((r) => r.data),
  update: (id: number, materialPrice: MaterialPriceRequest) =>
    materialPriceApi.updateMaterialPrice(id, materialPrice).then((r) => r.data),
  delete: (id: number) => materialPriceApi.deleteMaterialPrice(id),
};
