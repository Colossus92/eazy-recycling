import { VatRateRequest, VatRateResponse } from '@/api/client/models';
import { vatRateService } from '@/api/services/vatRateService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

export const useVatRatesCrud = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data: vatRates = [],
    error,
    isLoading,
  } = useQuery({
    queryKey: ['vatRates'],
    queryFn: () => vatRateService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<VatRateResponse | undefined>(
    undefined
  );
  const [itemToDelete, setItemToDelete] = useState<VatRateResponse | undefined>(
    undefined
  );

  const displayedVatRates = useMemo(
    () =>
      vatRates.filter((vatRate) => {
        return (
          vatRate.vatCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
          vatRate.description
            .toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          vatRate.countryCode
            .toLowerCase()
            .includes(searchQuery.toLowerCase()) ||
          vatRate.percentage.toLowerCase().includes(searchQuery.toLowerCase())
        );
      }),
    [vatRates, searchQuery]
  );

  const createMutation = useMutation({
    mutationFn: (item: VatRateRequest) => vatRateService.create(item),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vatRates'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const removeMutation = useMutation({
    mutationFn: (item: VatRateResponse) => vatRateService.delete(item.vatCode),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['vatRates'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const create = async (item: VatRateRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (item: VatRateResponse): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const updateMutation = useMutation({
    mutationFn: (item: VatRateRequest) =>
      vatRateService.update(item.vatCode, item),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vatRates'] }).then(() => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      });
    },
  });

  const update = async (item: VatRateRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    read: {
      items: displayedVatRates,
      setSearchQuery,
      isLoading,
      errorHandling: {
        error,
        reset: () => {
          queryClient.invalidateQueries({ queryKey: ['vatRates'] });
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
      openForEdit: (item: VatRateResponse) => {
        setItemToEdit(item);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: VatRateRequest) => {
        if (itemToEdit) {
          return update(item);
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
