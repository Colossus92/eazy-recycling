import { apiInstance } from './apiInstance';
import { AmiceControllerApi, Pageable } from '../client';

const lmaDeclarationApi = new AmiceControllerApi(apiInstance.config);

export const lmaDeclarationService = {
  getAll: (pageable?: Pageable) => {
    const defaultPageable: Pageable = { page: 0, size: 10 };
    return lmaDeclarationApi
      .getDeclarations(pageable || defaultPageable)
      .then((r) => r.data);
  },
  approve: (declarationId: string) => {
    return lmaDeclarationApi
      .approveDeclaration(declarationId)
      .then((r) => r.data);
  },
  hasPendingApprovals: () => {
    return lmaDeclarationApi.hasPendingApprovals().then((r) => r.data);
  },
};
