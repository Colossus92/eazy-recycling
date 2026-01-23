import { useQuery } from '@tanstack/react-query';
import {
  CatalogItem,
  CatalogItemType,
  catalogService,
  InvoiceType,
} from '../services/catalogService';

interface UseCatalogItemsOptions {
  consignorPartyId?: string;
  type?: CatalogItemType;
  invoiceType?: InvoiceType;
  enabled?: boolean;
}

/**
 * React Query hook for fetching and caching catalog items.
 * The query key includes consignorPartyId, type, and invoiceType to ensure proper caching per combination.
 * All components using this hook with the same parameters will share the cached data.
 *
 * Note: invoiceType affects the defaultPrice sign - PURCHASE invoices get negative prices for products.
 */
export const useCatalogItems = ({
  consignorPartyId,
  type,
  invoiceType,
  enabled = true,
}: UseCatalogItemsOptions = {}) => {
  return useQuery<CatalogItem[]>({
    queryKey: [
      'catalogItems',
      consignorPartyId ?? 'all',
      type ?? 'all',
      invoiceType ?? 'all',
    ],
    queryFn: () =>
      catalogService.search(undefined, consignorPartyId, type, invoiceType),
    enabled,
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
    gcTime: 10 * 60 * 1000, // Keep in cache for 10 minutes
  });
};

/**
 * Search/filter catalog items from the cached data.
 * This allows for client-side filtering without additional API calls.
 */
export const filterCatalogItems = (
  items: CatalogItem[],
  query?: string
): CatalogItem[] => {
  if (!query || query.trim() === '') {
    return items;
  }

  const lowerQuery = query.toLowerCase();
  return items.filter(
    (item) =>
      item.name.toLowerCase().includes(lowerQuery) ||
      item.code?.toLowerCase().includes(lowerQuery) ||
      item.wasteStreamNumber?.toLowerCase().includes(lowerQuery)
  );
};
