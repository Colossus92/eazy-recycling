import { ProductRequest, ProductResponse } from '@/api/client/models';
import { productService } from '@/api/services/productService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useProductsCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: products = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['products'],
    queryFn: () => productService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<ProductResponse | undefined>(
    undefined
  );
  const [itemToDelete, setItemToDelete] = useState<ProductResponse | undefined>(
    undefined
  );

  const displayedProducts = useMemo(
    () =>
      products.filter((product) => {
        return (
          product.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
          product.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          product.unitOfMeasure
            .toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          product.vatCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
          product.purchaseAccountNumber
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          product.salesAccountNumber
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase())
        );
      }),
    [products, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (item: ProductRequest) => productService.create(item),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: ProductResponse) => productService.delete(item.id),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['products'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (item: ProductRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: ProductResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (params: { id: number; data: ProductRequest }) =>
      productService.update(params.id, params.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const update = async (id: number, item: ProductRequest): Promise<void> => {
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
      items: displayedProducts,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['products'] });
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
      openForEdit: (item: ProductResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: ProductRequest) => {
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
