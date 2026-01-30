import { MaterialPriceControllerApi, MaterialPriceRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const materialPriceApi = new MaterialPriceControllerApi(apiInstance.config);

export interface MaterialPriceQueryParams {
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
  publishedOnly?: boolean;
}

export const materialPriceService = {
  getAll: (params?: MaterialPriceQueryParams) =>
    materialPriceApi.getAllMaterialsWithPrices(params?.sortBy, params?.sortDirection, params?.publishedOnly).then((r) => r.data),
  getById: (id: string) =>
    materialPriceApi.getMaterialPrice(id).then((r) => r.data),
  create: (id: string, materialPrice: MaterialPriceRequest) =>
    materialPriceApi.createMaterialPrice(id, materialPrice).then((r) => r.data),
  update: (id: string, materialPrice: MaterialPriceRequest) =>
    materialPriceApi.updateMaterialPrice(id, materialPrice).then((r) => r.data),
  delete: (id: string) => materialPriceApi.deleteMaterialPrice(id),
};
