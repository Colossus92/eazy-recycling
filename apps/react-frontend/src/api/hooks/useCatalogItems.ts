import { useQuery } from '@tanstack/react-query';
import { catalogService, CatalogItem, CatalogItemType } from '../services/catalogService';

interface UseCatalogItemsOptions {
  consignorPartyId?: string;
  type?: CatalogItemType;
  enabled?: boolean;
}

/**
 * React Query hook for fetching and caching catalog items.
 * The query key includes consignorPartyId and type to ensure proper caching per combination.
 * All components using this hook with the same parameters will share the cached data.
 */
export const useCatalogItems = ({
  consignorPartyId,
  type,
  enabled = true,
}: UseCatalogItemsOptions = {}) => {
  return useQuery<CatalogItem[]>({
    queryKey: ['catalogItems', consignorPartyId ?? 'all', type ?? 'all'],
    queryFn: () => catalogService.search(undefined, consignorPartyId, type),
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
