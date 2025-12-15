import {
  ProductCategoryRequest,
  ProductCategoryResponse,
} from '@/api/client/models';
import { productCategoryService } from '@/api/services/productCategoryService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useProductCategoriesCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: productCategories = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['productCategories'],
    queryFn: () => productCategoryService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<
    ProductCategoryResponse | undefined
  >(undefined);
  const [itemToDelete, setItemToDelete] = useState<
    ProductCategoryResponse | undefined
  >(undefined);

  const displayedProductCategories = useMemo(
    () =>
      productCategories.filter((category) => {
        return (
          category.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
          category.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          category.description
            ?.toLowerCase()
            .includes(searchQuery.toLowerCase())
        );
      }),
    [productCategories, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (item: ProductCategoryRequest) =>
      productCategoryService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['productCategories'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: ProductCategoryResponse) =>
      productCategoryService.delete(item.id),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['productCategories'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (item: ProductCategoryRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: ProductCategoryResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (params: { id: number; data: ProductCategoryRequest }) =>
      productCategoryService.update(params.id, params.data),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['productCategories'] })
        .then(() => {
          setItemToEdit(undefined);
          setIsFormOpen(false);
        });
    },
  });

  const update = async (
    id: number,
    item: ProductCategoryRequest
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
      items: displayedProductCategories,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['productCategories'] });
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
      openForEdit: (item: ProductCategoryResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: ProductCategoryRequest) => {
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
