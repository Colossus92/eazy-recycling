import { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { lmaDeclarationService } from '@/api/services/lmaDeclarationService';
import { Pageable } from '@/api/client';

interface UseLmaDeclarationsOptions {
  page?: number;
  pageSize?: number;
}

export function useLmaDeclarations(options?: UseLmaDeclarationsOptions) {
  const queryClient = useQueryClient();
  const page = options?.page ?? 1;
  const pageSize = options?.pageSize ?? 10;

  // Convert 1-based page to 0-based for API
  const pageable: Pageable = {
    page: page - 1,
    size: pageSize,
  };

  const {
    data: lmaDeclarationsPage,
    error,
    isFetching,
  } = useQuery({
    queryKey: ['lmaDeclarations', page, pageSize],
    queryFn: () => lmaDeclarationService.getAll(pageable),
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
    totalElements: lmaDeclarationsPage?.totalElements ?? 0,
    errorHandling: {
      error,
      reset: () => {
        queryClient.resetQueries({ queryKey: ['lmaDeclarations'] });
      },
    },
  };
}
