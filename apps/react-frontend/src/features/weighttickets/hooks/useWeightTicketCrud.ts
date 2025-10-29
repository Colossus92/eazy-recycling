import { WeightTicketRequest, WeightTicketListView, WeightTicketDetailView } from '@/api/client';
import { weightTicketService } from '@/api/services/weightTicketService';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { WeightTicketFilterFormValues } from '../components/WeightTicketFilterForm';

interface WeightTicketFilterParams {
  statuses?: string[];
}

export function useWeightTicketCrud() {
  const queryClient = useQueryClient();
  const {
    data: weightTickets = [],
    error,
    isFetching,
  } = useQuery<WeightTicketListView[]>({
    queryKey: ['weightTickets'],
    queryFn: () => weightTicketService.getAll(),
  });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [query, setQuery] = useState<string>('');
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [itemToEdit, setItemToEdit] = useState<WeightTicketDetailView | undefined>(undefined);
  const [itemToDelete, setItemToDelete] = useState<number | undefined>(undefined);
  const [filters, setFilters] = useState<WeightTicketFilterParams>({ statuses: undefined });
  const [currentFilterFormValues, setCurrentFilterFormValues] = useState<WeightTicketFilterFormValues>({
    isDraft: false,
    isCompleted: false,
    isInvoice: false,
    isCancelled: false,
  });

  const displayedWeightTickets = useMemo(
    () => {
      return weightTickets.filter((weightTicket) => {
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
    [weightTickets, query, filters]
  );

  const applyFilterFormValues = (values: WeightTicketFilterFormValues) => {
    const statuses: string[] = [];

    if (values.isDraft) statuses.push('DRAFT');
    if (values.isCompleted) statuses.push('ACTIVE');
    if (values.isInvoice) statuses.push('INACTIVE');
    if (values.isCancelled) statuses.push('CANCELLED');

    setCurrentFilterFormValues(values);
    setFilters({
      statuses: statuses.length > 0 ? statuses : undefined,
    });
  };

  const createMutation = useMutation({
    mutationFn: (item: WeightTicketRequest) =>
      weightTicketService.create(item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['weightTickets'] })
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ weightTicketNumber, item }: { weightTicketNumber: number; item: WeightTicketRequest }) =>
      weightTicketService.update(weightTicketNumber, item),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['weightTickets'] })
    },
  });

  const create = async (item: WeightTicketRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      createMutation.mutate(item, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const update = async (weightTicketNumber: number, item: WeightTicketRequest): Promise<void> => {
    return new Promise((resolve, reject) => {
      updateMutation.mutate({ weightTicketNumber, item }, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };

  const cancelMutation = useMutation({
    mutationFn: ({number, cancellationReason}: {number: number, cancellationReason: string}) => weightTicketService.cancel(number, { cancellationReason }),
    onSuccess: () => {
      queryClient
        .invalidateQueries({ queryKey: ['weightTickets'] })
        .then(() => setItemToDelete(undefined));
    },
  });


  const cancel = async (number: number, cancellationReason: string): Promise<void> => {
    return new Promise((resolve, reject) => {
      cancelMutation.mutate({ number, cancellationReason }, {
        onSuccess: () => resolve(),
        onError: (error) => reject(error),
      });
    });
  };


  return {
    read: {
      items: displayedWeightTickets,
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
          queryClient.resetQueries({ queryKey: ['weightTickets'] });
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
      openForEdit: async (item: WeightTicketListView) => {
        const weightTicketDetails = await weightTicketService.getByNumber(item.id);
        setItemToEdit(weightTicketDetails);
        setIsFormOpen(true);
      },
      close: () => {
        setItemToEdit(undefined);
        setIsFormOpen(false);
      },
      submit: async (item: WeightTicketRequest) => {
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
      confirm: cancel,
      cancel: () => setItemToDelete(undefined),
    },
  };
}
