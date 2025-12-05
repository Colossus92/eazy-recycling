import { useMemo, useState } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { lmaDeclarationService } from '@/api/services/lmaDeclarationService';
import { Pageable } from '@/api/client';
import { toastService } from '@/components/ui/toast/toastService';

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

  const approveMutation = useMutation({
    mutationFn: (declarationId: string) => lmaDeclarationService.approve(declarationId),
    onSuccess: () => {
      toastService.success('Melding succesvol goedgekeurd en verzonden naar LMA');
      queryClient.invalidateQueries({ queryKey: ['lmaDeclarations'] });
    },
    onError: () => {
      toastService.error('Fout bij goedkeuren van melding');
    },
  });

  return {
    items: displayedDeclarations,
    setQuery,
    isFetching,
    totalElements: lmaDeclarationsPage?.totalElements ?? 0,
    approveDeclaration: approveMutation.mutate,
    isApproving: approveMutation.isPending,
    errorHandling: {
      error,
      reset: () => {
        queryClient.resetQueries({ queryKey: ['lmaDeclarations'] });
      },
    },
  };
}
