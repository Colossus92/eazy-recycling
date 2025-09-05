import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { DeleteResponse, Truck } from '@/types/api.ts';
import { truckService } from '@/api/truckService.ts';

export function useTruckCrud() {
  const queryClient = useQueryClient();
  const {
    data: trucks = [],
    error,
    isLoading,
  } = useQuery<Truck[]>({
    queryKey: ['trucks'],
    queryFn: () => truckService.list(),
  });
  const [query, setQuery] = useState<string>('');
  const displayedTrucks = useMemo(
    () =>
      trucks.filter((truck) => {
        return (
          truck.licensePlate.toLowerCase().includes(query.toLowerCase()) ||
          truck.brand.toLowerCase().includes(query.toLowerCase()) ||
          truck.model.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [trucks, query]
  );
  const [isAdding, setIsAdding] = useState(false);
  const [editing, setEditing] = useState<Truck | undefined>(undefined);
  const [deleting, setDeleting] = useState<Truck | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: Omit<Truck, 'id'>) => truckService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['trucks'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: Truck) => truckService.remove(item.licensePlate),
    onSuccess: (response: DeleteResponse) => {
      if (response.success) {
        queryClient
          .invalidateQueries({ queryKey: ['trucks'] })
          .then(() => setDeleting(undefined));
      }
    },
  });

  const updateMutation = useMutation({
    mutationFn: (item: Truck) => truckService.update(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['trucks'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: Omit<Truck, 'id'>): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (item: Truck): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: Truck): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    displayedTrucks,
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
