import { Eural } from "@/api/client/models";
import { euralService } from "@/api/services/euralService";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useMemo } from "react";

export const useEuralCodeCrud = () => {
    const queryClient = useQueryClient();
    const [searchQuery, setSearchQuery] = useState('');
    const { data: eurals = [], error, isLoading } = useQuery({
        queryKey: ['eurals'],
        queryFn: () => euralService.getAll(),
    });
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [itemToDelete, setItemToDelete] = useState<Eural | undefined>(undefined)

    const displayedEurals = useMemo(
        () => eurals.filter((eural) => {
            return (
                eural.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
                eural.description.toLowerCase().includes(searchQuery.toLowerCase())
            )
        }
        ),
        [eurals, searchQuery]
    );

    const createMutation = useMutation({
        mutationFn: (item: Omit<Eural, 'id'>) => euralService.create(item),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: ['eurals'] });
        },
    });

    const removeMutation = useMutation({
        mutationFn: (item: Eural) => euralService.delete(item.code),
        onSuccess: () => {
            queryClient
                .invalidateQueries({ queryKey: ['eurals'] })
                .then(() => setItemToDelete(undefined));
        },
    });

    const create = async (item: Eural): Promise<void> => {
        return new Promise((resolve, reject) => {
            createMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    const remove = async (item: Eural): Promise<void> => {
        return new Promise((resolve, reject) => {
            removeMutation.mutate(item, {
                onSuccess: () => resolve(),
                onError: (error) => reject(error),
            });
        });
    };

    return {
        error,
        setSearchQuery,
        isLoading,
        displayedEurals,
        creation: {
            isOpen: isFormOpen,
            open: () => setIsFormOpen(true),
            close: () => setIsFormOpen(false),
            confirm: create,
        },
        deletion: {
            item: itemToDelete,
            initiate: setItemToDelete,
            confirm: remove,
            cancel: () => setItemToDelete(undefined),
        },
    }
};
