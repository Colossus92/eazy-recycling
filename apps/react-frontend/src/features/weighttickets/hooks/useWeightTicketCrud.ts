import { CreateWeightTicketRequest, WeightTicketListView } from '@/api/client';
import { weightTicketService } from '@/api/services/weightTicketService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';

interface WeightTicketFilterParams {
    statuses?: string[];
}

export function useWeightTicketCrud() {
  const queryClient = useQueryClient();
  const {
    data: wasteStreams = [],
    error,
    isFetching,
  } = useQuery<WeightTicketListView[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => weightTicketService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [query, setQuery] = useState<string>('');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<WeightTicketListView | undefined>(undefined);
  const [filters, setFilters] = useState<WeightTicketFilterParams>({statuses: undefined});

  const displayedWasteStreams = useMemo(
    () => {
      return wasteStreams.filter((weightTicket) => {
        // Apply search query filter (OR logic for different fields)
        const matchesQuery = query === '' || (
          String(weightTicket.id).includes(query.toLowerCase()) 
          || weightTicket.consignorPartyName.toLowerCase().includes(query.toLowerCase()) 
          || weightTicket.note?.toLowerCase().includes(query.toLowerCase()) 
        );
        
        // Apply status filter (AND logic - must match if filter is active)
        const matchesStatusFilter = !filters.statuses || filters.statuses.includes(weightTicket.status);
        
        return matchesQuery && matchesStatusFilter;
      })
    },
    [wasteStreams, query, filters]
  );


  const createMutation = useMutation({
    mutationFn: (item: CreateWeightTicketRequest) =>
      weightTicketService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['wasteStreams'] })
    },
  });

  const create = async (item: CreateWeightTicketRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
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
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: CreateWeightTicketRequest) => {
        if (itemToEdit) {
          // return update(itemToEdit.wasteStreamNumber, item);
        } else {
          return create(item);
        }
      },
    },
  };
}
