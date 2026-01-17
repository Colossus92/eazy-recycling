import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';
import { WasteTransportRequest } from '@/api/client/models/waste-transport-request';
import { TransportDetailView } from '@/api/client/models/transport-detail-view';
import { WasteContainerViewLocation } from '@/api/client/models/waste-container-view-location';
import { transportService } from '@/api/services/transportService';
import { pickupLocationViewToFormValue } from '@/types/forms/locationConverters';
import { format } from 'date-fns';

export interface WasteStreamLineFormValue {
  wasteStreamNumber: string;
  wasteStreamData?: WasteStreamListView;
  quantity: string;
  weight: string;
}

export interface WasteStreamTransportFormValues {
  // Step 1: Select Waste Stream
  consignorPartyId: string;
  wasteStreamLines: WasteStreamLineFormValue[];

  // Step 2: Transport Details
  carrierPartyId: string;
  truckId?: string;
  driverId?: string;
  pickupDate?: string;
  deliveryDate?: string;
  comments?: string;
  containerId?: string;
  containerOperation?: string;

  // Step 3: Additional Info (future)
}

export const fieldsToValidate: Array<
  Array<keyof WasteStreamTransportFormValues>
> = [
  // Step 0: Select Waste Stream
  ['consignorPartyId', 'wasteStreamLines'],

  // Step 1: Transport Details
  ['pickupDate'],
];

/**
 * Converts WasteContainerViewLocation to a readable string using existing converters
 */
const locationToString = (
  location: WasteContainerViewLocation | undefined
): string => {
  if (!location) return '';

  // Use existing converter to convert to LocationFormValue
  const formValue = pickupLocationViewToFormValue(location as any);

  // Convert LocationFormValue to readable string
  switch (formValue.type) {
    case 'dutch_address':
      return [
        formValue.streetName,
        `${formValue.buildingNumber}${formValue.buildingNumberAddition ? ` ${formValue.buildingNumberAddition}` : ''}`,
        formValue.city,
      ]
        .filter(Boolean)
        .join(', ');

    case 'company':
      return formValue.companyName || '';

    case 'project_location':
      return [
        formValue.companyName,
        formValue.streetName,
        `${formValue.buildingNumber}${formValue.buildingNumberAddition ? ` ${formValue.buildingNumberAddition}` : ''}`,
        formValue.city,
      ]
        .filter(Boolean)
        .join(', ');

    case 'proximity':
      return `${formValue.description}, ${formValue.city}` || '';

    case 'none':
    default:
      return 'Geen';
  }
};

/**
 * Converts form values to WasteTransportRequest for API submission
 */
const formValuesToWasteTransportRequest = (
  formValues: WasteStreamTransportFormValues
): WasteTransportRequest => {
  return {
    carrierPartyId: formValues.carrierPartyId,
    containerOperation: (formValues.containerOperation || 'PICKUP') as any,
    pickupDateTime: formValues.pickupDate || '',
    deliveryDateTime: formValues.deliveryDate,
    truckId: formValues.truckId,
    driverId: formValues.driverId,
    containerId: formValues.containerId,
    note: formValues.comments || '',
    transportType: 'WASTE',
    goods: formValues.wasteStreamLines
      .filter(
        (line) => line.wasteStreamNumber && (line.weight || line.quantity)
      )
      .map((line) => ({
        wasteStreamNumber: line.wasteStreamNumber,
        weight: line.weight ? parseFloat(line.weight) : undefined,
        unit: 'kg',
        quantity: parseInt(line.quantity || '1', 10),
      })),
  };
};

/**
 * Converts TransportDetailView from API to form values
 */
const transportDetailToFormValues = (
  transport: TransportDetailView
): WasteStreamTransportFormValues => {
  const wasteStreamLines: WasteStreamLineFormValue[] = (
    transport.goodsItem || []
  ).map((goods) => {
    const wasteStreamData: WasteStreamListView = {
      wasteStreamNumber: goods.wasteStreamNumber,
      wasteName: goods.name,
      euralCode: goods.euralCode,
      processingMethodCode: goods.processingMethodCode,
      consignorPartyName: transport.consignorParty?.name || '',
      consignorPartyId: transport.consignorParty?.id || '',
      pickupLocation: locationToString(transport.pickupLocation),
      deliveryLocation: locationToString(transport.deliveryLocation),
      status: transport.status,
      lastActivityAt: {} as any, // Not available in TransportDetailView
      isEditable: false, // Not available in TransportDetailView
    };

    return {
      wasteStreamNumber: goods.wasteStreamNumber || '',
      wasteStreamData,
      quantity: goods.quantity?.toString() || '1',
      weight: goods.netNetWeight?.toString() || '',
    };
  });

  return {
    consignorPartyId: transport.consignorParty?.id || '',
    wasteStreamLines,
    carrierPartyId: transport.carrierParty.id,
    truckId: transport.truck?.licensePlate || undefined,
    driverId: transport.driver?.id || undefined,
    pickupDate: transport.pickupDateTime
      ? format(new Date(transport.pickupDateTime), "yyyy-MM-dd'T'HH:mm")
      : '',
    deliveryDate: transport.deliveryDateTime
      ? format(new Date(transport.deliveryDateTime), "yyyy-MM-dd'T'HH:mm")
      : '',
    comments: transport.note || undefined,
    containerId: transport.wasteContainer?.id || undefined,
    containerOperation: transport.containerOperation || undefined,
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
      wasteStreamLines: [],
      carrierPartyId: undefined,
      truckId: undefined,
      driverId: undefined,
      pickupDate: '',
      deliveryDate: '',
      containerId: undefined,
      containerOperation: undefined,
      comments: undefined,
    },
  });


  // Fetch transport details when editing
  const { data: transportData, isLoading } = useQuery({
    queryKey: ['transport', transportId],
    queryFn: () => transportService.getTransportById(transportId!),
    enabled: !!transportId,
  });

  // Populate form when transport data is loaded
  useEffect(() => {
    if (transportData) {
      const formValues = transportDetailToFormValues(transportData);
      formContext.reset(formValues);
    }
  }, [transportData, formContext]);

  const mutation = useMutation({
    mutationFn: async (formData: WasteStreamTransportFormValues) => {
      const request = formValuesToWasteTransportRequest(formData);

      if (transportId) {
        const response = await transportService.updateWasteTransport(
          transportId,
          request
        );
        return response.data;
      } else {
        const response = await transportService.createWasteTransport(request);
        return response.data;
      }
    },
    onSuccess: async () => {
      // Invalidate planning query to reload CalendarGrid
      await queryClient.invalidateQueries({ queryKey: ['planning'] });

      // Invalidate transport detail query to reload TransportDetailsDrawer if open
      if (transportId) {
        await queryClient.invalidateQueries({
          queryKey: ['transport', transportId],
        });
      }

      // Also invalidate transports list
      await queryClient.invalidateQueries({ queryKey: ['transports'] });

      toastService.success(
        transportId
          ? 'Transport bijgewerkt'
          : 'Transport aangemaakt'
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
      wasteStreamLines: [],
      carrierPartyId: undefined,
      truckId: undefined,
      driverId: undefined,
      pickupDate: '',
      deliveryDate: '',
      containerId: undefined,
      containerOperation: undefined,
      comments: undefined,
    });
  };

  return {
    formContext,
    fieldsToValidate,
    mutation,
    resetForm,
    data: transportData,
    isLoading,
  };
}
