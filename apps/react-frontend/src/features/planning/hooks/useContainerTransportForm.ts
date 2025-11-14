import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import {
  formValuesToCreateContainerTransportRequest,
  transportDetailViewToContainerTransportFormValues,
  transportService,
} from '@/api/services/transportService.ts';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { LocationFormValue } from '@/types/forms/LocationFormValue';

export interface ContainerTransportFormValues {
  consignorPartyId: string;
  carrierPartyId: string;
  containerOperation: string;
  transportType: string;
  pickupLocation: LocationFormValue;
  pickupDateTime: string;
  deliveryLocation: LocationFormValue;
  deliveryDateTime?: string;
  truckId: string;
  driverId: string;
  containerId: string;
  note?: string;
}

const fieldsToValidate: Array<Array<keyof ContainerTransportFormValues>> = [
  // Step 0: Main section fields
  ['consignorPartyId', 'carrierPartyId', 'containerOperation'],

  // Step 1: Pickup section fields
  ['pickupLocation', 'pickupDateTime'],

  // Step 2: Delivery section fields
  ['deliveryLocation', 'deliveryDateTime'],

  // Step 3: Transport details
  ['truckId', 'driverId', 'containerId', 'note'],
];

export function useContainerTransportForm(
  transportId?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  
  const formContext = useForm<ContainerTransportFormValues>({
    defaultValues: {
      consignorPartyId: '',
      carrierPartyId: '',
      containerOperation: 'DELIVERY',
      transportType: 'CONTAINER',
      pickupLocation: { type: 'none' },
      pickupDateTime: '',
      deliveryLocation: { type: 'none' },
      deliveryDateTime: '',
      truckId: '',
      driverId: '',
      containerId: '',
      note: '',
    },
  });
  
  const { data, isLoading } = useQuery({
    queryKey: ['transport', transportId],
    queryFn: async () => {
      const response = await transportService.getTransportById(transportId!);
      const formValues =
        transportDetailViewToContainerTransportFormValues(response);
      formContext.reset(formValues);

      return response; // Return the original response, not the form values
    },
    enabled: !!transportId,
  });
  const mutation = useMutation({
    mutationFn: async (data: ContainerTransportFormValues) => {
      if (!!data && transportId) {
        return transportService.updateContainerTransport(
          transportId,
          formValuesToCreateContainerTransportRequest(data)
        );
      } else {
        return transportService.createContainerTransport(
          formValuesToCreateContainerTransportRequest(data)
        );
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['planning'] });
      if (transportId) {
        queryClient.invalidateQueries({ queryKey: ['transport', transportId] });
      }

      toastService.success(
        !data ? 'Transport bijgewerkt' : 'Transport aangemaakt'
      );
      formContext.reset();

      if (onSuccess) {
        onSuccess();
      }
    },
    onError: (error) => {
      console.error('Error submitting form:', error);
      toastService.error(
        `Er is een fout opgetreden bij het ${data ? 'bijwerken' : 'aanmaken'} van het transport`
      );
    },
  });

  return {
    formContext,
    fieldsToValidate,
    mutation,
    data,
    isLoading,
  };
}
