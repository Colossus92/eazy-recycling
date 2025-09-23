import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { WasteContainer, CreateContainerRequest } from '@/api/client';
import { containerService } from '@/api/services/containerService';

function filter(container: WasteContainer, query: string) {
  return (
    container.id.toLowerCase().includes(query.toLowerCase()) ||
    container.location?.companyName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.address?.streetName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.address?.city
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.notes?.toLowerCase().includes(query.toLowerCase())
  );
}

export const useWasteContainerCrud = () => {
  const queryClient = useQueryClient();
  const { data: containers = [], error } = useQuery<WasteContainer[]>({
    queryKey: ['containers'],
    queryFn: () => containerService.getAll(),
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
    mutationFn: (item: CreateContainerRequest) => containerService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['containers'] })
        .then(() => setIsAdding(false));
    },
  });

  const removeMutation = useMutation({
    mutationFn: (container: WasteContainer) => containerService.delete(container.uuid),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['containers'] })
        .then(() => setDeleting(undefined));
    },
  });

  const updateMutation = useMutation({
    mutationFn: (container: WasteContainer) => containerService.update(container),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['containers'] })
        .then(() => setEditing(undefined));
    },
  });

  const create = async (item: Omit<WasteContainer, 'id'> | CreateContainerRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      // Workaround to accept the Omit type
      const createRequest: CreateContainerRequest = 'id' in item 
        ? item as CreateContainerRequest
        : {
            // If we're getting an Omit<WasteContainer, 'id'>, we need to add an id
            // This should not happen in practice as the form always provides an id
            id: '',
            location: item.location,
            notes: item.notes
          };
      
      createMutation.mutate(createRequest, {
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
