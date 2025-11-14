import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { lmaDeclarationService } from '@/api/services/lmaDeclarationService';

export function useLmaDeclarations() {
  const queryClient = useQueryClient();
  const {
    data: lmaDeclarationsPage,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['lmaDeclarations'],
    queryFn: () => lmaDeclarationService.getAll(),
  });

  const [query, setQuery] = useState<string>('');

  const lmaDeclarations = lmaDeclarationsPage?.content ?? [];

  const displayedDeclarations = useMemo(() => {
    return lmaDeclarations.filter((declaration) => {
      // Apply search query filter (OR logic for different fields)
      const matchesQuery =
        query === '' ||
        declaration.wasteStreamNumber
          .toLowerCase()
          .includes(query.toLowerCase()) ||
        declaration.wasteName.toLowerCase().includes(query.toLowerCase()) ||
        declaration.pickupLocation
          ?.toLowerCase()
          .includes(query.toLowerCase()) ||
        declaration.period.toLowerCase().includes(query.toLowerCase()) ||
        declaration.status.toLowerCase().includes(query.toLowerCase());

      return matchesQuery;
    });
  }, [lmaDeclarations, query]);

  return {
    items: displayedDeclarations,
    setQuery,
    isFetching,
    errorHandling: {
      error,
      reset: () => {
        queryClient.resetQueries({ queryKey: ['lmaDeclarations'] });
      },
    },
  };
}
