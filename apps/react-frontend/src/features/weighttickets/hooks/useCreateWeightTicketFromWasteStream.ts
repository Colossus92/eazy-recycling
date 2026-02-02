import { useMutation, useQueryClient } from '@tanstack/react-query';
import { weightTicketService } from '@/api/services/weightTicketService';
import { toastService } from '@/components/ui/toast/toastService';
import { useNavigate } from 'react-router-dom';

export function useCreateWeightTicketFromWasteStream() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const createMutation = useMutation({
    mutationFn: (wasteStreamNumber: string) =>
      weightTicketService.createFromWasteStream(wasteStreamNumber),
    onSuccess: (result) => {
      toastService.success('Weegbon aangemaakt');
      queryClient.invalidateQueries({ queryKey: ['wasteStreams'] });
      navigate(`/weight-tickets?weightTicketId=${result.weightTicketId}`);
    },
  });

  const createWeightTicket = async (wasteStreamNumber: string) => {
    return createMutation.mutateAsync(wasteStreamNumber);
  };

  return {
    createWeightTicket,
    isCreating: createMutation.isPending,
  };
}
