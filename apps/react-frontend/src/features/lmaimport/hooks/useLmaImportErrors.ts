import { useQuery } from '@tanstack/react-query';
import { lmaImportService } from '@/api/services/lmaImportService';

export const useLmaImportErrors = () => {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['lmaImportErrors'],
    queryFn: async () => {
      const response = await lmaImportService.getErrors();
      return response.data;
    },
  });

  return {
    errors: data?.errors ?? [],
    isLoading,
    error,
    refetch,
  };
};
