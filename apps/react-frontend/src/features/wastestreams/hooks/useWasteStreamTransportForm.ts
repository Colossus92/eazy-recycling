import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';

export interface WasteStreamTransportFormValues {
  // Step 1: Select Waste Stream
  consignorPartyId: string;
  wasteStreamNumber: string;
  wasteStreamData?: WasteStreamListView;
  
  // Future steps can be added here
  // Step 2: Transport Details
  // Step 3: Additional Info
}

export const fieldsToValidate: Array<Array<keyof WasteStreamTransportFormValues>> = [
  // Step 0: Select Waste Stream
  ['consignorPartyId', 'wasteStreamNumber'],
  
  // Step 1: Transport Details (future)
  [],
  
  // Step 2: Additional Info (future)  
  [],
];

export function useWasteStreamTransportForm(
  transportId?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();

  const formContext = useForm<WasteStreamTransportFormValues>({
    defaultValues: {
      consignorPartyId: '',
      wasteStreamNumber: '',
      wasteStreamData: undefined,
    },
  });

  // For now, just a placeholder mutation
  const mutation = useMutation({
    mutationFn: async (formData: WasteStreamTransportFormValues) => {
      // This would be replaced with actual API call
      console.log('Submitting waste stream transport:', formData);
      return Promise.resolve(formData);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['wasteStreamTransports'] });
      
      toastService.success(
        transportId 
          ? 'Afvalstroom transport bijgewerkt'
          : 'Afvalstroom transport aangemaakt'
      );
      
      if (onSuccess) {
        onSuccess();
      }
    },
    onError: (error: unknown) => {
      console.error('Error saving waste stream transport:', error);
      
      toastService.error(
        `Er is een fout opgetreden bij het ${transportId ? 'bijwerken' : 'aanmaken'} van het transport`
      );
    },
  });

  const resetForm = () => {
    formContext.reset({
      consignorPartyId: '',
      wasteStreamNumber: '',
      wasteStreamData: undefined,
    });
  };

  return {
    formContext,
    fieldsToValidate,
    mutation,
    resetForm,
    data: undefined, // TODO: Add query for existing transport data
    isLoading: false, // TODO: Add loading state when fetching existing data
  };
}
