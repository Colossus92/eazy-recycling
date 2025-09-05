import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { transportService } from '@/api/transportService.ts';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { transportDataService } from '@/features/planning/transportDataService.ts';

export interface WasteTransportFormValues {
  consignorPartyId: string;
  consignorClassification: number;
  consigneePartyId: string;
  carrierPartyId: string;
  transportType: string;
  containerOperation: string;
  /**
   * The party actually disposing the goods
   */
  pickupPartyId: string;
  /**
   * The company located at the pickup location
   */
  pickupCompanyId?: string;
  pickupCompanyBranchId?: string;
  pickupStreet: string;
  pickupBuildingNumber: string;
  pickupPostalCode: string;
  pickupCity: string;
  pickupDateTime: string;
  deliveryCompanyId?: string;
  deliveryCompanyBranchId?: string;
  deliveryStreet: string;
  deliveryBuildingNumber: string;
  deliveryPostalCode: string;
  deliveryCity: string;
  deliveryDateTime?: string;
  truckId: string;
  driverId: string;
  containerId: string;
  wasteStreamNumber?: string;
  weight: number;
  unit: string;
  quantity: number;
  goodsName: string;
  processingMethodCode: string;
  euralCode: string;
  note?: string;
}

export const fieldsToValidate: Array<Array<keyof WasteTransportFormValues>> = [
  // Step 0: Main section fields
  [
    'consignorPartyId',
    'consignorClassification',
    'carrierPartyId',
    'containerOperation',
  ],

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

  // Step 3: Goods section fields
  [
    'pickupPartyId',
    'wasteStreamNumber',
    'goodsName',
    'euralCode',
    'processingMethodCode',
    'weight',
    'quantity',
  ],

  // Step 4: Transport details
  ['truckId', 'driverId', 'containerId', 'note'],
];

export function useWasteTransportForm(
  transportId?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['transport', transportId],
    queryFn: async () => {
      const response = await transportService.getTransportById(transportId!);
      const formValues =
        transportDataService.apiToWasteTransportFormValues(response);
      formContext.reset(formValues);

      return response;
    },
    enabled: !!transportId,
  });
  const formContext = useForm<WasteTransportFormValues>({
    defaultValues: data,
  });
  const mutation = useMutation({
    mutationFn: async (data: WasteTransportFormValues) => {
      if (!!data && transportId) {
        return transportService.updateWasteTransport(transportId, data);
      } else {
        return transportService.createWasteTransport(data);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['planning'] });

      toastService.success(
        !data ? 'Transport aangemaakt' : 'Transport bijgewerkt'
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
