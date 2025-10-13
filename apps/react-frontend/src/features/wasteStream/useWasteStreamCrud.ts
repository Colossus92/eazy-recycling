import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { WasteStreamDetailView, WasteStreamListView } from '@/api/client';
import { WasteStreamRequest } from '@/api/client/models/waste-stream-request';
import { CreateWasteStreamRequest } from '@/api/client/models/create-waste-stream-request';

export function useWasteStreamCrud() {
  const queryClient = useQueryClient();
  const {
    data: wasteStreams = [],
    error,
    isFetching,
  } = useQuery<WasteStreamListView[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => wasteStreamService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [query, setQuery] = useState<string>('');
  const displayedWasteStreams = useMemo(
    () =>
      wasteStreams.filter((wasteStream) => {
        return (
          wasteStream.wasteStreamNumber.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.wasteName.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.consignorPartyName.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.pickupLocation?.toLowerCase().includes(query.toLowerCase()) ||
          wasteStream.deliveryLocation?.toLowerCase().includes(query.toLowerCase())
        );
      }),
    [wasteStreams, query]
  );
  const [itemToEdit, setItemToEdit] = useState<WasteStreamDetailView | undefined>(undefined);
  const [itemToDelete, setItemToDelete] = useState<WasteStreamListView | undefined>(undefined);

  const createMutation = useMutation({
    mutationFn: (item: WasteStreamRequest) =>
      wasteStreamService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
    },
  });

  const removeMutation = useMutation({
    mutationFn: (number: string) => wasteStreamService.delete(number),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setItemToDelete(undefined));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ wasteStreamNumber, item }: { wasteStreamNumber: string; item: WasteStreamRequest }) => 
      wasteStreamService.update(wasteStreamNumber, item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
        .then(() => setItemToEdit(undefined));
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

  const update = async (wasteStreamNumber: string, item: WasteStreamRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate({wasteStreamNumber, item }, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const remove = async (number: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      removeMutation.mutate(number, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  return {
    read: {
      items: displayedWasteStreams,
      setQuery,
      isFetching,
      errorHandling: {
        error,
        reset: () => {
          queryClient.resetQueries({ queryKey: ['wasteStreams'] });
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
      openForEdit: async (item: WasteStreamListView) => {
        const wasteStreamDetails = await wasteStreamService.getByNumber(item.wasteStreamNumber);
        setItemToEdit(wasteStreamDetails);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: CreateWasteStreamRequest) => {
        if (itemToEdit) {
          return update(itemToEdit.wasteStreamNumber, item);
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
}
