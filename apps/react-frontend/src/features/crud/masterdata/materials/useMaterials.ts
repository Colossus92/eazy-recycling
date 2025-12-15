import { MaterialRequest, MaterialResponse } from '@/api/client/models';
import { materialService } from '@/api/services/materialService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useMaterialsCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: materials = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['materials'],
    queryFn: () => materialService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<MaterialResponse | undefined>(
    undefined
  );
  const [itemToDelete, setItemToDelete] = useState<
    MaterialResponse | undefined
  >(undefined);

  const displayedMaterials = useMemo(
    () =>
      materials.filter((material) => {
        return (
          material.code?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          material.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          material.materialGroupCode
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          material.materialGroupName
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          material.unitOfMeasure
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          material.vatCode?.toLowerCase().includes(searchQuery.toLowerCase())
        );
      }),
    [materials, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (item: MaterialRequest) => materialService.create(item),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materials'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: MaterialResponse) => materialService.delete(item.id),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materials'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (item: MaterialRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: MaterialResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (params: { id: number; data: MaterialRequest }) =>
      materialService.update(params.id, params.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['materials'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const update = async (id: number, item: MaterialRequest): Promise<void> => {
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
      items: displayedMaterials,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['materials'] });
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
      openForEdit: (item: MaterialResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: MaterialRequest) => {
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
