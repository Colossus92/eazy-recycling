import { MaterialPriceSyncControllerApi } from '@/api/client/apis/material-price-sync-controller-api';
import { apiInstance } from './apiInstance';

const api = new MaterialPriceSyncControllerApi(
  apiInstance.config,
  undefined,
  apiInstance.axios
);

export const materialPriceSyncService = {
  getSyncPreview: async () => {
    const response = await api.getSyncPreview();
    return response.data;
  },

  executeSyncAsync: async () => {
    const response = await api.executeSyncAsync();
    return response.data;
  },

  getExternalProducts: async () => {
    const response = await api.getExternalProducts();
    return response.data;
  },
};
