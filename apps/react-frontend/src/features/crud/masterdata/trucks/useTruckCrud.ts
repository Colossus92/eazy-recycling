import { Truck } from "@/api/client/models";
import { truckService } from "@/api/services/truckService";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";

const queryKey = 'trucks';

export const useTruckCrud = () => {
    const queryClient = useQueryClient();
    const [searchQuery, setSearchQuery] = useState('');
    const { data: trucks = [], error, isLoading } = useQuery({
        queryKey: [queryKey],
        queryFn: () => truckService.getAll(),
    });
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [itemToEdit, setItemToEdit] = useState<Truck | undefined>(undefined);
    const [itemToDelete, setItemToDelete] = useState<Truck | undefined>(undefined)

    const displayedEurals = useMemo(
        () => trucks.filter((truck) => {
            return (
                truck.licensePlate.toLowerCase().includes(searchQuery.toLowerCase()) ||
                truck.brand?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                truck.model?.toLowerCase().includes(searchQuery.toLowerCase())
            )
        }
        ),
        [trucks, searchQuery]
    );

    const createMutation = useMutation({
        mutationFn: (item: Omit<Truck, 'id'>) => truckService.create(item),
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
        mutationFn: (item: Truck) => truckService.delete(item.licensePlate),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => setItemToDelete(undefined));
        },
    });

    const create = async (item: Truck): Promise<void> => {
        return new Promise((resolve, reject) => {
            createMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const remove = async (item: Truck): Promise<void> => {
        return new Promise((resolve, reject) => {
            removeMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const updateMutation = useMutation({
        mutationFn: (item: Truck) => truckService.update(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => {
                    setItemToEdit(undefined);
                    setIsFormOpen(false);
                });
        },
    });

    const update = async (item: Truck): Promise<void> => {
        return new Promise((resolve, reject) => {
            updateMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    return {
        read: {
            items: displayedEurals,
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
            openForEdit: (item: Truck) => {
                setItemToEdit(item);
                setIsFormOpen(true);
            },
            close: () => {
                setItemToEdit(undefined);
                setIsFormOpen(false);
            },
            submit: async (item: Truck) => {
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
