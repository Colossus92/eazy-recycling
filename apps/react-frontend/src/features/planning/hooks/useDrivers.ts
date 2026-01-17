import { useQuery } from '@tanstack/react-query';
import { driverService } from '@/api/services/driverService';

export const useDrivers = () => {
  const { data, isLoading, error } = useQuery({
    queryKey: ['drivers'],
    queryFn: driverService.getAllDrivers,
  });

  return {
    drivers: data || [],
    isLoading,
    error,
  };
};
