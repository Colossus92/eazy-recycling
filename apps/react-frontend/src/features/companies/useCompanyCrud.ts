import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { companyService, Company } from '@/api/services/companyService.ts';

export const useCompanyCrud = () => {
  const queryClient = useQueryClient();
  const {
    data: companies = [],
    error,
    isLoading,
  } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(true),
  });
  const [query, setQuery] = useState<string>('');
  const displayedCompanies = useMemo<Company[]>(
    () =>
      companies.filter((company: Company) => {
        return (
          company.name?.toLowerCase().includes(query.toLowerCase()) ||
          company.address?.street
            ?.toLowerCase()
            .includes(query.toLowerCase()) ||
          company.address?.city?.toLowerCase().includes(query.toLowerCase()) ||
          company.chamberOfCommerceId
            ?.toLowerCase()
            .includes(query.toLowerCase()) ||
          company.vihbId?.toLowerCase().includes(query.toLowerCase())||
          company.processorId?.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [companies, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<Company | undefined>(undefined);
  const [deleting, setDeleting] = useState<Company | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: Omit<Company, 'id'>) => companyService.create(item),
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
  });``

  const removeMutation = useMutation({
    mutationFn: (item: Company) => companyService.delete(item.id),
    onSuccess: () => {
        queryClient
          .invalidateQueries({ queryKey: ['companies'] })
          .then(() => setDeleting(undefined));
    },
  });

  const create = async (item: Omit<Company, 'id'>): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
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
    setQuery,
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
  };
};
