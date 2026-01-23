import {
  CatalogControllerApi,
  CatalogItemResponse,
  CatalogItemResponseItemTypeEnum,
} from '../client';
import { apiInstance } from './apiInstance';

const catalogApi = new CatalogControllerApi(apiInstance.config);

export type CatalogItem = CatalogItemResponse;
export type CatalogItemType = CatalogItemResponseItemTypeEnum;
export type InvoiceType = 'PURCHASE' | 'SALE';
export { CatalogItemResponseItemTypeEnum };

export const catalogService = {
  search: (
    query?: string,
    consignorPartyId?: string,
    type?: CatalogItemType,
    invoiceType?: InvoiceType
  ) =>
    catalogApi
      .searchCatalogItems(
        query,
        consignorPartyId,
        type as any,
        invoiceType as any
      )
      .then((r) => r.data),
};
