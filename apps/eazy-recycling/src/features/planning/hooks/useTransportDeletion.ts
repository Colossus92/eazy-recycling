import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { transportService } from '@/api/transportService.ts';
import { PlanningItem } from '@/features/planning/hooks/usePlanning';

export const useTransportDeletion = () => {
  const queryClient = useQueryClient();
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [transportToDelete, setTransportToDelete] =
    useState<PlanningItem | null>(null);

  const deleteTransportMutation = useMutation({
    mutationFn: (id: string) => transportService.deleteTransport(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['planning'] });
      toast.success('Transport verwijderd');
      setIsDeleting(false);
      setShowDeleteDialog(false);
      setTransportToDelete(null);
    },
    onError: (error) => {
      console.error('Error deleting transport:', error);
      toast.error(
        'Er is een fout opgetreden bij het verwijderen van het transport'
      );
      setIsDeleting(false);
      setShowDeleteDialog(false);
    },
  });

  const confirmDelete = (transport: PlanningItem) => {
    setTransportToDelete(transport);
    setShowDeleteDialog(true);
  };

  const handleDelete = () => {
    if (!transportToDelete) return;

    setIsDeleting(true);
    deleteTransportMutation.mutate(transportToDelete.id);
  };

  const cancelDelete = () => {
    setShowDeleteDialog(false);
    setTransportToDelete(null);
  };

  return {
    isDeleting,
    showDeleteDialog,
    transportToDelete,
    confirmDelete,
    handleDelete,
    cancelDelete,
  };
};
