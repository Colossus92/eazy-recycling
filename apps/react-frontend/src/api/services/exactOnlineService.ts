import { ExactOnlineControllerApi } from '../client';
import { apiInstance } from './apiInstance';

const exactOnlineApi = new ExactOnlineControllerApi(apiInstance.config);

export const exactOnlineService = {
  getConnectionStatus: () => exactOnlineApi.getConnectionStatus(),
  getAuthorizationUrl: () => exactOnlineApi.getAuthorizationUrl(),
  revokeConnection: () => exactOnlineApi.revokeConnection(),
  syncFromExact: () => exactOnlineApi.syncFromExact(),
  getConflicts: () => exactOnlineApi.getConflicts(),
};
