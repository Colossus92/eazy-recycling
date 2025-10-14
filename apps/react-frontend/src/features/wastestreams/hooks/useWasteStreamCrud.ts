import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { WasteStreamDetailView, WasteStreamListView } from '@/api/client';
import { WasteStreamRequest } from '@/api/client/models/waste-stream-request';
import { CreateWasteStreamRequest } from '@/api/client/models/create-waste-stream-request';
import { WasteStreamFilterFormValues } from '../components/filter/WasteStreamFilterForm';

interface WasteStreamFilterParams {
    statuses?: string[];
}

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
  const [itemToEdit, setItemToEdit] = useState<WasteStreamDetailView | undefined>(undefined);
  const [itemToDelete, setItemToDelete] = useState<WasteStreamListView | undefined>(undefined);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filters, setFilters] = useState<WasteStreamFilterParams>({statuses: undefined});
  const [currentFilterFormValues, setCurrentFilterFormValues] = useState<WasteStreamFilterFormValues>({
    isDraft: false,
    isActive: false,
    isInactive: false,
    isExpired: false,
  });
  const displayedWasteStreams = useMemo(
    () => {
      console.log(JSON.stringify(filters))
      console.log(JSON.stringify(wasteStreams))
      return wasteStreams.filter((wasteStream) => {
        // Apply search query filter (OR logic for different fields)
        const matchesQuery = query === '' || (
          wasteStream.wasteStreamNumber.toLowerCase().includes(query.toLowerCase()) 
          || wasteStream.wasteName.toLowerCase().includes(query.toLowerCase()) 
          || wasteStream.consignorPartyName.toLowerCase().includes(query.toLowerCase()) 
          || wasteStream.pickupLocation?.toLowerCase().includes(query.toLowerCase()) 
          || wasteStream.deliveryLocation?.toLowerCase().includes(query.toLowerCase())
        );
        
        // Apply status filter (AND logic - must match if filter is active)
        const matchesStatusFilter = !filters.statuses || filters.statuses.includes(wasteStream.status);
        
        return matchesQuery && matchesStatusFilter;
      })
    },
    [wasteStreams, query, filters]
  );


  const applyFilterFormValues = (values: WasteStreamFilterFormValues) => {
    const statuses: string[] = [];

    if (values.isDraft) statuses.push('DRAFT');
    if (values.isActive) statuses.push('ACTIVE');
    if (values.isInactive) statuses.push('INACTIVE');
    if (values.isExpired) statuses.push('EXPIRED');

    setCurrentFilterFormValues(values);
    setFilters({
      statuses: statuses.length > 0 ? statuses : undefined,
    });
  };


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
      updateMutation.mutate({ wasteStreamNumber, item }, {
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
      filter: {
        isFilterOpen,
        setIsFilterOpen,
        applyFilterFormValues,
        currentFormValues: currentFilterFormValues,
      },
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
