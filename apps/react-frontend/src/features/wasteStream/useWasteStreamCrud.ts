import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { wasteStreamService, WasteStream } from '@/api/services/wasteStreamService';

export function useWasteStreamCrud() {
  const queryClient = useQueryClient();
  const {
    data: wasteStreams = [],
    error,
    isLoading,
  } = useQuery<WasteStream[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => wasteStreamService.getAll(),
  });
  const [query, setQuery] = useState<string>('');
  const displayedWasteStreams = useMemo(
    () =>
      wasteStreams.filter((wasteStream) => {
        return (
          wasteStream.number.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.name.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [wasteStreams, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<WasteStream | undefined>(undefined);
  const [deleting, setDeleting] = useState<WasteStream | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: Omit<WasteStream, 'id'>) =>
      wasteStreamService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: WasteStream) => wasteStreamService.delete(item.number),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setDeleting(undefined));
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: WasteStream) => wasteStreamService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: Omit<WasteStream, 'id'>): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: WasteStream): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: WasteStream): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    displayedWasteStreams,
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
    isLoading,
  };
}
