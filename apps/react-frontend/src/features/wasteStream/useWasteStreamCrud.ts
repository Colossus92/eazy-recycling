import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { WasteStreamListView } from '@/api/client';
import { WasteStreamDto } from '@/api/client/models/waste-stream-dto';
import { WasteStreamRequest } from '@/api/client/models/waste-stream-request';

export function useWasteStreamCrud() {
  const queryClient = useQueryClient();
  const {
    data: wasteStreams = [],
    error,
    isLoading,
  } = useQuery<WasteStreamListView[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => wasteStreamService.getAll(),
  });
  const [query, setQuery] = useState<string>('');
  const displayedWasteStreams = useMemo(
    () =>
      wasteStreams.filter((wasteStream) => {
        return (
          wasteStream.wasteStreamNumber.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.wasteStreamNumber.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [wasteStreams, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<WasteStreamDto | undefined>(undefined);
  const [deleting, setDeleting] = useState<WasteStreamDto | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: WasteStreamRequest) =>
      wasteStreamService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: WasteStreamDto) => wasteStreamService.delete(item.number),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setDeleting(undefined));
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: WasteStreamRequest) => wasteStreamService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: WasteStreamRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: WasteStreamRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: WasteStreamDto): Promise<void> => {
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
