import {
  MaterialPriceRequest,
  MaterialPriceResponse,
} from '@/api/client/models';
import { materialPriceService } from '@/api/services/materialPriceService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useMaterialPricesCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: materialPrices = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['materialPrices'],
    queryFn: () => materialPriceService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<
    MaterialPriceResponse | undefined
  >(undefined);
  const [itemToDelete, setItemToDelete] = useState<
    MaterialPriceResponse | undefined
  >(undefined);

  const displayedMaterialPrices = useMemo(
    () =>
      materialPrices.filter((price) => {
        const query = searchQuery.toLowerCase();
        return (
          price.code.toLowerCase().includes(query) ||
          price.name.toLowerCase().includes(query) ||
          (price.materialGroupName?.toLowerCase().includes(query) ?? false) ||
          (price.defaultPrice?.toString().includes(query) ?? false)
        );
      }),
    [materialPrices, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (params: { id: string; data: MaterialPriceRequest }) =>
      materialPriceService.create(params.id, params.data),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialPrices'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: MaterialPriceResponse) =>
      materialPriceService.delete(item.id),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialPrices'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (
    id: string,
    item: MaterialPriceRequest
  ): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(
        { id, data: item },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(error),
        }
      );
    });
  };

  const remove = async (item: MaterialPriceResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (params: { id: string; data: MaterialPriceRequest }) =>
      materialPriceService.update(params.id, params.data),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['materialPrices'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const update = async (
    id: string,
    item: MaterialPriceRequest
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
      items: displayedMaterialPrices,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['materialPrices'] });
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
      openForEdit: (item: MaterialPriceResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (catalogItemId: string, price: number) => {
        const request: MaterialPriceRequest = { price };
        if (itemToEdit) {
          return update(itemToEdit.id, request);
        } else {
          return create(catalogItemId, request);
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
