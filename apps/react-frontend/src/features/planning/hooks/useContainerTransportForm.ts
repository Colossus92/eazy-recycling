import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import {
  formValuesToCreateContainerTransportRequest,
  transportDetailViewToContainerTransportFormValues,
  transportService,
} from '@/api/services/transportService.ts';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { LocationFormValue } from '@/types/forms/LocationFormValue';
import {
  TimingConstraint,
  createEmptyTimingConstraint,
} from '@/types/forms/TimingConstraint';
import { useTenantCompany } from '@/hooks/useTenantCompany';
import { useCallback } from 'react';

export interface ContainerTransportFormValues {
  consignorPartyId: string;
  carrierPartyId: string;
  containerOperation: string;
  transportType: string;
  pickupLocation: LocationFormValue;
  pickupTiming: TimingConstraint;
  deliveryLocation: LocationFormValue;
  deliveryTiming?: TimingConstraint;
  truckId: string;
  driverId: string;
  containerId: string;
  note?: string;
}

const fieldsToValidate: Array<Array<keyof ContainerTransportFormValues>> = [
  // Step 0: Main section fields
  ['consignorPartyId', 'carrierPartyId', 'containerOperation'],

  // Step 1: Pickup section fields
  ['pickupLocation', 'pickupTiming'],

  // Step 2: Delivery section fields
  ['deliveryLocation', 'deliveryTiming'],

  // Step 3: Transport details
  ['truckId', 'driverId', 'containerId', 'note'],
];

function getDefaultValues(tenantCompanyId?: string): ContainerTransportFormValues {
  return {
    consignorPartyId: '',
    carrierPartyId: tenantCompanyId ?? '',
    containerOperation: 'DELIVERY',
    transportType: 'CONTAINER',
    pickupLocation: { type: 'none' },
    pickupTiming: createEmptyTimingConstraint(),
    deliveryLocation: { type: 'none' },
    deliveryTiming: createEmptyTimingConstraint(),
    truckId: '',
    driverId: '',
    containerId: '',
    note: '',
  };
}

export function useContainerTransportForm(
  transportId?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const { data:  tenantCompany } = useTenantCompany();
  
  const formContext = useForm<ContainerTransportFormValues>({
    defaultValues: getDefaultValues(),
  });

  // Reset form with tenant company as default carrier for new transports
  const resetForm = useCallback(() => {
    if (!transportId) {
      formContext.reset(getDefaultValues(tenantCompany?.id));
    } else {
      formContext.reset();
    }
  }, [transportId, tenantCompany?.id, formContext]);
  
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
    resetForm,
  };
}
