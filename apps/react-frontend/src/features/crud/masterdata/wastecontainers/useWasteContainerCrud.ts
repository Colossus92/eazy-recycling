import { CreateContainerRequest,  WasteContainerRequest, WasteContainerView } from '@/api/client/models';
import { containerService } from '@/api/services/containerService';
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";

const queryKey = 'containers';

function filter(container: WasteContainerView, query: string) {
  return (
    container.id.toLowerCase().includes(query.toLowerCase()) ||
    container.location?.companyName
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.addressView?.street
      ?.toLowerCase()
      .includes(query.toLowerCase()) ||
    container.location?.addressView?.city
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
    const [itemToEdit, setItemToEdit] = useState<WasteContainerView | undefined>(undefined);
    const [itemToDelete, setItemToDelete] = useState<WasteContainerView | undefined>(undefined)

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
        mutationFn: (item: WasteContainerView) => containerService.delete(item.uuid),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => setItemToDelete(undefined));
        },
    });

    const create = async (item: CreateContainerRequest): Promise<void> => {
        return new Promise((resolve, reject) => {
            createMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const remove = async (item: WasteContainerView): Promise<void> => {
        return new Promise((resolve, reject) => {
            removeMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const updateMutation = useMutation({
        mutationFn: (item: WasteContainerRequest) => containerService.update(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => {
                    setItemToEdit(undefined);
                    setIsFormOpen(false);
                });
        },
    });

    const update = async (item: WasteContainerRequest): Promise<void> => {
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
            openForEdit: (item: WasteContainerView) => {
                setItemToEdit(item);
                setIsFormOpen(true);
            },
            close: () => {
                setItemToEdit(undefined);
                setIsFormOpen(false);
            },
            submit: async (item: WasteContainerRequest) => {
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
