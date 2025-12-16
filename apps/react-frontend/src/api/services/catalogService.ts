import { CatalogControllerApi, CatalogItemResponse } from "../client";
import { apiInstance } from "./apiInstance";


const catalogApi = new CatalogControllerApi(apiInstance.config);

export type CatalogItem = CatalogItemResponse;

export const catalogService = {
  search: (query?: string, consignorPartyId?: string) => catalogApi.searchCatalogItems(query, consignorPartyId).then((r) => r.data),
};