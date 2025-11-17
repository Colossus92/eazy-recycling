import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';
import { WasteTransportRequest } from '@/api/client/models/waste-transport-request';
import { transportService } from '@/api/services/transportService';

export interface WasteStreamTransportFormValues {
  // Step 1: Select Waste Stream
  consignorPartyId: string;
  wasteStreamNumber: string;
  wasteStreamData?: WasteStreamListView;
  quantity?: string;
  weight?: string;
  unit?: string;
  
  // Step 2: Transport Details
  truckId?: string;
  driverId?: string;
  pickupDate?: string;
  deliveryDate?: string;
  comments?: string;
  containerId?: string;
  containerOperation?: string;
  
  // Step 3: Additional Info (future)
}

export const fieldsToValidate: Array<Array<keyof WasteStreamTransportFormValues>> = [
  // Step 0: Select Waste Stream
  ['consignorPartyId', 'wasteStreamNumber'],
  
  // Step 1: Transport Details
  ['pickupDate'],
  
  // Step 2: Additional Info (future)  
  [],
];

/**
 * Converts form values to WasteTransportRequest for API submission
 */
const formValuesToWasteTransportRequest = (
  formValues: WasteStreamTransportFormValues
): WasteTransportRequest => {
  return {
    carrierPartyId: '',
    containerOperation: (formValues.containerOperation || 'PICKUP') as any,
    pickupDateTime: formValues.pickupDate || '',
    deliveryDateTime: formValues.deliveryDate,
    truckId: formValues.truckId,
    driverId: formValues.driverId,
    containerId: formValues.containerId,
    note: formValues.comments || '',
    transportType: 'WASTE',
    goods: [
      {
        wasteStreamNumber: formValues.wasteStreamNumber,
        weight: parseFloat(formValues.weight || '0'),
        unit: formValues.unit || 'kg',
        quantity: parseInt(formValues.quantity || '1', 10),
      },
    ],
  };
};

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
      quantity: '',
      weight: '',
      unit: 'kg',
      truckId: '',
      driverId: '',
      pickupDate: '',
      deliveryDate: '',
      containerId: '',
      containerOperation: '',
      comments: '',
    },
  });

  const mutation = useMutation({
    mutationFn: async (formData: WasteStreamTransportFormValues) => {
      const request = formValuesToWasteTransportRequest(formData);
      
      if (transportId) {
        const response = await transportService.updateWasteTransport(transportId, request);
        return response.data;
      } else {
        const response = await transportService.createWasteTransport(request);
        return response.data;
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['transports'] });
      
      toastService.success(
        transportId 
          ? 'Afvalstroom transport bijgewerkt'
          : 'Afvalstroom transport aangemaakt'
      );
      
      // Reset and close form
      resetForm();
      
      if (onSuccess) {
        onSuccess();
      }
    },
    onError: (error: unknown) => {
      console.error('Error saving waste stream transport:', error);
      
      toastService.error(
        `Er is een fout opgetreden bij het ${transportId ? 'bijwerken' : 'aanmaken'} van het transport`
      );
      // Keep form open on error
    },
  });

  const resetForm = () => {
    formContext.reset({
      consignorPartyId: '',
      wasteStreamNumber: '',
      wasteStreamData: undefined,
      quantity: '',
      weight: '',
      unit: 'kg',
      truckId: '',
      driverId: '',
      pickupDate: '',
      deliveryDate: '',
      containerId: '',
      containerOperation: '',
      comments: '',
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
