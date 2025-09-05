import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { DeleteResponse, WasteContainer } from '@/types/api.ts';
import { containerService } from '@/api/containerService.ts';

function filter(container: WasteContainer, query: string) {
  return (
    container.id.toLowerCase().includes(query.toLowerCase()) ||
    container.location.companyName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location.address?.streetName
      .toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location.address?.city
      .toLowerCase()
      .includes(query.toLowerCase()) ||
    container.notes?.toLowerCase().includes(query.toLowerCase())
  );
}

export const useWasteContainerCrud = () => {
  const queryClient = useQueryClient();
  const { data: containers = [], error } = useQuery<WasteContainer[]>({
    queryKey: ['containers'],
    queryFn: () => containerService.list(),
  });
  const [query, setQuery] = useState<string>('');
  const displayedContainers = useMemo(
    () =>
      containers.filter((container) => {
        return filter(container, query);
      }),
    [containers, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<WasteContainer | undefined>(undefined);
  const [deleting, setDeleting] = useState<WasteContainer | undefined>(
    undefined
  );

  const createMutation = useMutation({
    mutationFn: (item: Omit<WasteContainer, 'id'>) =>
      containerService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['containers'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: WasteContainer) => containerService.remove(item),
    onSuccess: (response: DeleteResponse) => {
      if (response.success) {
        queryClient
          .invalidateQueries({ queryKey: ['containers'] })
          .then(() => setDeleting(undefined));
      }
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: WasteContainer) => containerService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['containers'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: Omit<WasteContainer, 'id'>): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: WasteContainer): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: WasteContainer): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    displayedContainers,
    setQuery,
    isAdding,
    setIsAdding,
    editing,
    setEditing,
    deleting,
    setDeleting,
    create,
    update,
    remove,
    error,
  };
};
