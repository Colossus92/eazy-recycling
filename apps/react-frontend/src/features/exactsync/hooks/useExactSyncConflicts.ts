import { useQuery } from '@tanstack/react-query';
import { exactOnlineService } from '@/api/services/exactOnlineService';

export const useExactSyncConflicts = () => {
  const {
    data,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useQuery({
    queryKey: ['exactSyncConflicts'],
    queryFn: async () => {
      const response = await exactOnlineService.getConflicts();
      return response.data;
    },
  });

  return {
    conflicts: data?.conflicts ?? [],
    pendingReviews: data?.pendingReviews ?? [],
    isLoading,
    isFetching,
    error,
    refetch,
  };
};
