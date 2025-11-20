import { VatRateControllerApi, VatRateRequest } from '@/api/client';
import { apiInstance } from './apiInstance';

const vatRateApi = new VatRateControllerApi(apiInstance.config);

export const vatRateService = {
  getAll: () => vatRateApi.getAllVatRates().then((r) => r.data),
  create: (vatRate: VatRateRequest) =>
    vatRateApi.createVatRate(vatRate).then((r) => r.data),
  update: (vatCode: string, vatRate: VatRateRequest) =>
    vatRateApi.updateVatRate(vatCode, vatRate).then((r) => r.data),
  delete: (vatCode: string) => vatRateApi.deleteVatRate(vatCode),
};
