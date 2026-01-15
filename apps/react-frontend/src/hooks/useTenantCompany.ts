import { useQuery } from '@tanstack/react-query';
import { companyService } from '@/api/services/companyService';

/**
 * Hook to fetch the tenant company (the company with isTenantCompany = true).
 * This is cached and shared across all components using React Query.
 */
export const useTenantCompany = () => {
  return useQuery({
    queryKey: ['tenantCompany'],
    queryFn: () => companyService.getTenantCompany(),
    staleTime: 10 * 60 * 1000, // Cache for 10 minutes
    gcTime: 30 * 60 * 1000, // Keep in cache for 30 minutes
  });
};
