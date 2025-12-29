import {
  MaterialGroupRequest,
  MaterialGroupResponse,
} from '@/api/client/models';
import { materialGroupService } from '@/api/services/materialGroupService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useMaterialGroupsCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: materialGroups = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['materialGroups'],
    queryFn: () => materialGroupService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<
    MaterialGroupResponse | undefined
  >(undefined);
  const [itemToDelete, setItemToDelete] = useState<
    MaterialGroupResponse | undefined
  >(undefined);

  const displayedMaterialGroups = useMemo(
    () =>
      materialGroups.filter((materialGroup) => {
        return (
          materialGroup.code
            .toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          materialGroup.name
            .toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          materialGroup.description
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase())
        );
      }),
    [materialGroups, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (item: MaterialGroupRequest) =>
      materialGroupService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialGroups'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: MaterialGroupResponse) =>
      materialGroupService.delete(item.id),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialGroups'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (item: MaterialGroupRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: MaterialGroupResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (params: { id: string; data: MaterialGroupRequest }) =>
      materialGroupService.update(params.id, params.data),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialGroups'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const update = async (
    id: string,
    item: MaterialGroupRequest
  ): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(
        { id, data: item },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(error),
        }
      );
    });
  };

  return {
    read: {
      items: displayedMaterialGroups,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['materialGroups'] });
        },
      },
    },
    form: {
      isOpen: isFormOpen,
      item: itemToEdit,
      openForCreate: () => {
        setItemToEdit(undefined);
        setIsFormOpen(true);
      },
      openForEdit: (item: MaterialGroupResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: MaterialGroupRequest) => {
        if (itemToEdit) {
          return update(itemToEdit.id, item);
        } else {
          return create(item);
        }
      },
    },
    deletion: {
      item: itemToDelete,
      initiate: setItemToDelete,
      confirm: remove,
      cancel: () => setItemToDelete(undefined),
    },
  };
};
