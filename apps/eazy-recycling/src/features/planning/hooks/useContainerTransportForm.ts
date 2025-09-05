import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { transportService } from '@/api/transportService.ts';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { transportDataService } from '@/features/planning/transportDataService.ts';

export interface ContainerTransportFormValues {
  consignorPartyId: string;
  carrierPartyId: string;
  containerOperation: string;
  transportType: string;
  pickupCompanyId: string;
  pickupCompanyBranchId?: string;
  pickupStreet: string;
  pickupBuildingNumber: string;
  pickupPostalCode: string;
  pickupCity: string;
  pickupDateTime: string;
  deliveryCompanyId: string;
  deliveryCompanyBranchId?: string;
  deliveryStreet: string;
  deliveryBuildingNumber: string;
  deliveryPostalCode: string;
  deliveryCity: string;
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
  [
    'pickupCompanyId',
    'pickupCompanyBranchId',
    'pickupStreet',
    'pickupBuildingNumber',
    'pickupPostalCode',
    'pickupCity',
    'pickupDateTime',
  ],

  // Step 2: Delivery section fields
  [
    'deliveryCompanyId',
    'deliveryCompanyBranchId',
    'deliveryStreet',
    'deliveryBuildingNumber',
    'deliveryPostalCode',
    'deliveryCity',
    'deliveryDateTime',
  ],

  // Step 3: Transport details
  ['truckId', 'driverId', 'containerId', 'note'],
];

export function useContainerTransportForm(
  transportId?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['transport', transportId],
    queryFn: async () => {
      const response = await transportService.getTransportById(transportId!);
      const formValues =
        transportDataService.apiToContainerTransportFormValues(response);
      console.log('Form values, ', formValues);
      formContext.reset(formValues);

      return response;
    },
    enabled: !!transportId,
  });
  const formContext = useForm<ContainerTransportFormValues>({
    defaultValues: data,
  });
  const mutation = useMutation({
    mutationFn: async (data: ContainerTransportFormValues) => {
      if (!!data && transportId) {
        return transportService.updateContainerTransport(transportId, data);
      } else {
        return transportService.createContainerTransport(data);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['planning'] });

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
