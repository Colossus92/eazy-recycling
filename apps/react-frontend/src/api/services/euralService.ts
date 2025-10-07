import { Eural, EuralControllerApi } from '@/api/client';
import { apiInstance } from './apiInstance';
const euralApi = new EuralControllerApi(apiInstance.config);

export const euralService = {
  getAll: () => euralApi.getEural().then( r => r.data ),
  create: (eural: Eural) => euralApi.createEural(eural).then( r => r.data ),
  delete: (euralCode: string) => euralApi.deleteEural(euralCode),
};