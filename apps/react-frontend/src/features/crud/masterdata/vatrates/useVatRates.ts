import { vatRateService } from '@/api/services/vatRateService';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useVatRates = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: vatRates = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['vatRates'],
    queryFn: () => vatRateService.getAll(),
  });

  const items = useMemo(
    () =>
      vatRates.filter((vatRate) => {
        const q = searchQuery.toLowerCase();
        return (
          vatRate.vatCode.toLowerCase().includes(q) ||
          vatRate.description.toLowerCase().includes(q) ||
          vatRate.countryCode.toLowerCase().includes(q) ||
          vatRate.percentage.toLowerCase().includes(q)
        );
      }),
    [vatRates, searchQuery]
  );

  return {
    items,
    setSearchQuery,
    isLoading,
    errorHandling: {
      error,
      reset: () => {
        queryClient.invalidateQueries({ queryKey: ['vatRates'] });
      },
    },
  };
};
