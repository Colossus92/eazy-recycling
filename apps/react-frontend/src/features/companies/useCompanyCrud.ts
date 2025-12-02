import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { companyService, Company, PagedCompanyResponse } from '@/api/services/companyService.ts';

export const useCompanyCrud = () => {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState<string>('');
  const [page, setPage] = useState<number>(0); // 0-indexed for API
  const [rowsPerPage, setRowsPerPage] = useState<number>(10);

  const {
    data: pagedResponse,
    error,
    isLoading,
  } = useQuery<PagedCompanyResponse>({
    queryKey: ['companies', { query, page, size: rowsPerPage }],
    queryFn: () => companyService.getAll({ 
      includeBranches: true, 
      query: query || undefined,
      page,
      size: rowsPerPage,
    }),
  });

  const displayedCompanies = pagedResponse?.content ?? [];
  const totalElements = pagedResponse?.totalElements ?? 0;
  const totalPages = pagedResponse?.totalPages ?? 0;

  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<Company | undefined>(undefined);
  const [deleting, setDeleting] = useState<Company | undefined>(undefined);

  // Reset to first page when query changes
  const handleSetQuery = (newQuery: string) => {
    setQuery(newQuery);
    setPage(0);
  };

  // Handle page change (convert from 1-indexed UI to 0-indexed API)
  const handleSetPage = (uiPage: number) => {
    setPage(uiPage - 1);
  };

  // Handle rows per page change
  const handleSetRowsPerPage = (newRowsPerPage: number) => {
    setRowsPerPage(newRowsPerPage);
    setPage(0);
  };

  const createMutation = useMutation({
    mutationFn: ({ item, restoreCompanyId }: { item: Omit<Company, 'id'>; restoreCompanyId?: string }) => 
      companyService.create(item, restoreCompanyId),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['companies'] })
        .then(() => setIsAdding(false));
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: Company) => companyService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['companies'] })
        .then(() => setEditing(undefined));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: Company) => companyService.delete(item.id),
    onSuccess: () => {
        queryClient
          .invalidateQueries({ queryKey: ['companies'] })
          .then(() => setDeleting(undefined));
    },
  });

  const create = async (item: Omit<Company, 'id'>, restoreCompanyId?: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate({ item, restoreCompanyId }, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: Company): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: Company): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    displayedCompanies,
    setQuery: handleSetQuery,
    isAdding,
    setIsAdding,
    setEditing,
    setDeleting,
    editing,
    deleting,
    create,
    update,
    remove,
    error,
    isLoading,
    // Pagination
    page: page + 1, // Convert to 1-indexed for UI
    setPage: handleSetPage,
    rowsPerPage,
    setRowsPerPage: handleSetRowsPerPage,
    totalElements,
    totalPages,
  };
};
