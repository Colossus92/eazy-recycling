import { CreateContainerRequest, WasteContainer } from '@/api/client/models';
import { containerService } from '@/api/services/containerService';
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";

const queryKey = 'containers';

function filter(container: WasteContainer, query: string) {
  return (
    container.id.toLowerCase().includes(query.toLowerCase()) ||
    container.location?.companyName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.address?.streetName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.address?.city
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.notes?.toLowerCase().includes(query.toLowerCase())
  );
}

export const useWasteContainerCrud = () => {
    const queryClient = useQueryClient();
    const [searchQuery, setSearchQuery] = useState('');
    const { data: containers = [], error, isLoading } = useQuery({
        queryKey: [queryKey],
        queryFn: () => containerService.getAll(),
    });
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [itemToEdit, setItemToEdit] = useState<WasteContainer | undefined>(undefined);
    const [itemToDelete, setItemToDelete] = useState<WasteContainer | undefined>(undefined)

  const displayedContainers = useMemo(
    () =>
      containers.filter((container) => {
        return filter(container, searchQuery);
      }),
    [containers, searchQuery]
  );

    const createMutation = useMutation({
        mutationFn: (item: CreateContainerRequest) => containerService.create(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => {
                    setItemToEdit(undefined);
                    setIsFormOpen(false);
                });
        },
    });

    const removeMutation = useMutation({
        mutationFn: (item: WasteContainer) => containerService.delete(item.uuid),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => setItemToDelete(undefined));
        },
    });

    const create = async (item: WasteContainer): Promise<void> => {
        return new Promise((resolve, reject) => {
            createMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const remove = async (item: WasteContainer): Promise<void> => {
        return new Promise((resolve, reject) => {
            removeMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const updateMutation = useMutation({
        mutationFn: (item: WasteContainer) => containerService.update(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => {
                    setItemToEdit(undefined);
                    setIsFormOpen(false);
                });
        },
    });

    const update = async (item: WasteContainer): Promise<void> => {
        return new Promise((resolve, reject) => {
            updateMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    return {
        read: {
            items: displayedContainers,
            setSearchQuery,
            isLoading,
            errorHandling: {
                error,
                reset: () => {
                    queryClient.invalidateQueries({ queryKey: [queryKey] });
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
            openForEdit: (item: WasteContainer) => {
                setItemToEdit(item);
                setIsFormOpen(true);
            },
            close: () => {
                setItemToEdit(undefined);
                setIsFormOpen(false);
            },
            submit: async (item: WasteContainer) => {
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
    }
};
