import { useQuery } from '@tanstack/react-query';
import { lmaDeclarationService } from '@/api/services/lmaDeclarationService';

/**
 * Hook to check if there are any pending LMA approvals
 * Returns true if there are any declarations with WAITING_APPROVAL status
 */
export function usePendingApprovals() {
  const { data } = useQuery({
    queryKey: ['lmaDeclarations', 'pending-approvals'],
    queryFn: async () => {
      const response = await lmaDeclarationService.hasPendingApprovals();
      return response.hasPendingApprovals;
    },
    refetchInterval: 30000, // Refetch every 30 seconds to keep notifications up to date
  });

  return {
    hasPendingApprovals: data ?? false,
  };
}
