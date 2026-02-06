import { VatRateControllerApi } from '@/api/client';
import { apiInstance } from './apiInstance';

const vatRateApi = new VatRateControllerApi(apiInstance.config);

export const vatRateService = {
  getAll: () => vatRateApi.getAllVatRates().then((r) => r.data),
};
