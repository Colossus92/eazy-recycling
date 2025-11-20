import { MaterialPriceControllerApi, MaterialPriceRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialPriceApi = new MaterialPriceControllerApi(apiInstance.config);

export const materialPriceService = {
  getAll: () => materialPriceApi.getAllActivePrices().then((r) => r.data),
  create: (materialPrice: MaterialPriceRequest) =>
    materialPriceApi.createPrice(materialPrice).then((r) => r.data),
  update: (id: number, materialPrice: MaterialPriceRequest) =>
    materialPriceApi.updatePrice(id, materialPrice).then((r) => r.data),
  delete: (id: number) => materialPriceApi.deletePrice(id),
};
