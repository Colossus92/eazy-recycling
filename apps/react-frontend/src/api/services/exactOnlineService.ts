import { ExactOnlineControllerApi } from "../client";
import { apiInstance } from "./apiInstance";

const exactOnlineApi = new ExactOnlineControllerApi(apiInstance.config);

export interface SyncFromExactResponse {
  success: boolean;
  message: string;
  recordsSynced: number;
  recordsCreated: number;
  recordsUpdated: number;
}

export const exactOnlineService = {
  getConnectionStatus: () => exactOnlineApi.getConnectionStatus(),
  getAuthorizationUrl: () => exactOnlineApi.getAuthorizationUrl(),
  refreshToken: () => exactOnlineApi.refreshToken(),
  syncFromExact: () => exactOnlineApi.syncFromExact(),
};
  

