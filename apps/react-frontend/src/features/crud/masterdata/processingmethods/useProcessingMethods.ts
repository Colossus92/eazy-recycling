import { ProcessingMethodDto as ProcessingMethod } from "@/api/client/models";
import { processingMethodService } from "@/api/services/processingMethodService";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useMemo } from "react";

const queryKey = 'processingMethods';

export const useProcessingMethodsCrud = () => {
    const queryClient = useQueryClient();
    const [searchQuery, setSearchQuery] = useState('');
    const { data: processingMethods = [], error, isLoading } = useQuery({
        queryKey: [queryKey],
        queryFn: () => processingMethodService.getAll(),
    });
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [itemToEdit, setItemToEdit] = useState<ProcessingMethod | undefined>(undefined);
    const [itemToDelete, setItemToDelete] = useState<ProcessingMethod | undefined>(undefined)

    const displayedProcessingMethods = useMemo(
        () => processingMethods.filter((processingMethod) => {
            return (
                processingMethod.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
                processingMethod.description.toLowerCase().includes(searchQuery.toLowerCase())
            )
        }
        ),
        [processingMethods, searchQuery]
    );

    const createMutation = useMutation({
        mutationFn: (item: Omit<ProcessingMethod, 'id'>) => processingMethodService.create(item),
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
        mutationFn: (item: ProcessingMethod) => processingMethodService.delete(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => setItemToDelete(undefined));
        },
    });

    const create = async (item: ProcessingMethod): Promise<void> => {
        return new Promise((resolve, reject) => {
            createMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const remove = async (item: ProcessingMethod): Promise<void> => {
        return new Promise((resolve, reject) => {
            removeMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const updateMutation = useMutation({
        mutationFn: (item: ProcessingMethod) => processingMethodService.update(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: [queryKey] })
                .then(() => {
                    setItemToEdit(undefined);
                    setIsFormOpen(false);
                });
        },
    });

    const update = async (item: ProcessingMethod): Promise<void> => {
        return new Promise((resolve, reject) => {
            updateMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    return {
        read: {
            items: displayedProcessingMethods,
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
            openForEdit: (item: ProcessingMethod) => {
                setItemToEdit(item);
                setIsFormOpen(true);
            },
            close: () => {
                setItemToEdit(undefined);
                setIsFormOpen(false);
            },
            submit: async (item: ProcessingMethod) => {
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
