import { format } from 'date-fns';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useEffect, useState } from 'react';
import { toastService } from '@/components/ui/toast/toastService.ts';
import {
  CompanyView,
  WeightTicketDetailView,
  WeightTicketDetailViewConsignorParty,
  WeightTicketRequest,
  WeightTicketRequestDirectionEnum,
  WeightTicketRequestSecondWeighingUnitEnum,
  WeightTicketRequestTarraWeightUnitEnum,
} from '@/api/client';
import { weightTicketService } from '@/api/services/weightTicketService';
import {
  createEmptyLocationFormValue,
  LocationFormValue,
} from '@/types/forms/LocationFormValue';
import {
  locationFormValueToPickupLocationRequest,
  pickupLocationViewToFormValue,
} from '@/types/forms/locationConverters';

export interface WeightTicketLineFormValues {
  catalogItemId: string;
  wasteStreamNumber?: string;
  weightValue: string;
  weightUnit: string;
}

export interface WeightTicketFormValues {
  consignorPartyId: string;
  carrierPartyId: string;
  truckLicensePlate: string;
  reclamation: string;
  note: string;
  lines: WeightTicketLineFormValues[];
  tarraWeightValue?: number;
  tarraWeightUnit?: string;
  secondWeighingValue?: number;
  secondWeighingUnit?: string;
  weightedAt?: string;
  direction: string;
  pickupLocation: LocationFormValue;
  deliveryLocation: LocationFormValue;
}

export function useWeightTicketForm(initialWeightTicketNumber?: number) {
  const queryClient = useQueryClient();

  // Track the current weight ticket number internally
  // This allows switching from create to edit mode after saving
  const [currentWeightTicketNumber, setCurrentWeightTicketNumber] = useState<
    number | undefined
  >(initialWeightTicketNumber);

  // Sync with prop when it changes (e.g., when opening a different weight ticket)
  useEffect(() => {
    setCurrentWeightTicketNumber(initialWeightTicketNumber);
  }, [initialWeightTicketNumber]);

  const formContext = useForm<WeightTicketFormValues>({
    defaultValues: {
      consignorPartyId: '',
      carrierPartyId: '',
      truckLicensePlate: '',
      reclamation: '',
      note: '',
      lines: [],
      tarraWeightValue: undefined,
      tarraWeightUnit: undefined,
      secondWeighingValue: undefined,
      secondWeighingUnit: undefined,
      weightedAt: format(new Date(), 'yyyy-MM-dd'),
      direction: 'INBOUND',
      pickupLocation: createEmptyLocationFormValue('none'),
      deliveryLocation: createEmptyLocationFormValue('none'),
    },
  });

  const { data, isLoading } = useQuery({
    queryKey: ['weightTickets', currentWeightTicketNumber],
    queryFn: async () => {
      const response = await weightTicketService.getByNumber(
        currentWeightTicketNumber!!
      );
      const formValues = weightTicketDetailsToFormValues(response);
      formContext.reset(formValues);

      return response;
    },
    enabled: !!currentWeightTicketNumber,
  });

  const mutation = useMutation({
    mutationFn: async (data: WeightTicketFormValues) => {
      const request = formValuesToWeightTicketRequest(data);
      if (!!data && currentWeightTicketNumber) {
        return weightTicketService.update(currentWeightTicketNumber, request);
      } else {
        return weightTicketService.create(request);
      }
    },
    onSuccess: async (response) => {
      await queryClient.invalidateQueries({ queryKey: ['weightTickets'] });

      toastService.success(
        !currentWeightTicketNumber ? 'Weegbon aangemaakt' : 'Weegbon bijgewerkt'
      );

      // Reload form with the latest data
      if (currentWeightTicketNumber) {
        // For updates, response is void - fetch the full details
        const fullDetails = await weightTicketService.getByNumber(
          currentWeightTicketNumber
        );
        const formValues = weightTicketDetailsToFormValues(fullDetails);
        formContext.reset(formValues);
      } else if ((response as any).id) {
        // For creates, response is CreateWeightTicketResponse with just id
        // Switch to edit mode by setting the new weight ticket number
        const createdId = (response as any).id;
        setCurrentWeightTicketNumber(createdId);
        // Fetch the full details using the returned id
        const fullDetails = await weightTicketService.getByNumber(createdId);
        const formValues = weightTicketDetailsToFormValues(fullDetails);
        formContext.reset(formValues);
      }
    },
    onError: (error) => {
      console.error('Error submitting form:', error);
      toastService.error(
        `Er is een fout opgetreden bij het ${currentWeightTicketNumber ? 'bijwerken' : 'aanmaken'} van het weegbon`
      );
    },
  });

  const createCompletedMutation = useMutation({
    mutationFn: async (data: WeightTicketFormValues) => {
      const request = formValuesToWeightTicketRequest(data);
      return weightTicketService.createCompleted(request);
    },
    onSuccess: async (response) => {
      await queryClient.invalidateQueries({ queryKey: ['weightTickets'] });

      toastService.success('Weegbon aangemaakt en verwerkt');

      // Reload form with the latest data
      if ((response as any).id) {
        // Response is CreateWeightTicketResponse with just id
        // Switch to edit mode by setting the new weight ticket number
        const createdId = (response as any).id;
        setCurrentWeightTicketNumber(createdId);
        // Fetch the full details using the returned id
        const fullDetails = await weightTicketService.getByNumber(createdId);
        const formValues = weightTicketDetailsToFormValues(fullDetails);
        formContext.reset(formValues);
      }
    },
    onError: (error) => {
      console.error('Error submitting form:', error);
      toastService.error(
        'Er is een fout opgetreden bij het aanmaken van het weegbon'
      );
    },
  });

  const resetForm = () => {
    // Reset to initial state (create mode if no initial number was provided)
    setCurrentWeightTicketNumber(initialWeightTicketNumber);
    formContext.reset({
      consignorPartyId: '',
      carrierPartyId: '',
      truckLicensePlate: '',
      reclamation: '',
      note: '',
      lines: [],
      tarraWeightValue: NaN,
      tarraWeightUnit: undefined,
      secondWeighingValue: NaN,
      secondWeighingUnit: undefined,
      weightedAt: format(new Date(), 'yyyy-MM-dd'),
      direction: 'INBOUND',
      pickupLocation: createEmptyLocationFormValue('none'),
      deliveryLocation: createEmptyLocationFormValue('none'),
    });
  };

  return {
    formContext,
    mutation,
    createCompletedMutation,
    data,
    isLoading,
    resetForm,
    formValuesToWeightTicketRequest,
    currentWeightTicketNumber,
  };
}

/**
 * Type guard to check if consignorParty is a company
 */
const isCompanyConsignor = (
  consignorParty: WeightTicketDetailViewConsignorParty
): consignorParty is WeightTicketDetailViewConsignorParty & {
  type: 'company';
  company: CompanyView;
} => {
  return (consignorParty as any).type === 'company';
};

/**
 * Resolves the consignorParty to CompanyView when it's of type company
 */
const resolveConsignorCompany = (
  consignorParty: WeightTicketDetailViewConsignorParty
): CompanyView => {
  if (isCompanyConsignor(consignorParty)) {
    return (consignorParty as any).company;
  }
  throw new Error(
    'Alleen zakelijke opdrachtgevers worden op dit moment ondersteund'
  );
};

const weightTicketDetailsToFormValues = (
  weightTicketDetails: WeightTicketDetailView
): WeightTicketFormValues => {
  const consignorCompany = resolveConsignorCompany(
    weightTicketDetails.consignorParty
  );

  return {
    consignorPartyId: consignorCompany.id || '',
    carrierPartyId: weightTicketDetails.carrierParty?.id || '',
    truckLicensePlate: weightTicketDetails.truckLicensePlate || '',
    reclamation: weightTicketDetails.reclamation || '',
    note: weightTicketDetails.note || '',
    lines: (weightTicketDetails.lines || []).map((line) => ({
      catalogItemId: line.catalogItemId?.toString() || '',
      wasteStreamNumber: line.wasteStreamNumber || undefined,
      weightValue: line.weightValue?.toString() || '',
      weightUnit: line.weightUnit || 'KG',
    })),
    secondWeighingValue: weightTicketDetails.secondWeighingValue,
    secondWeighingUnit: weightTicketDetails.secondWeighingUnit,
    tarraWeightValue: weightTicketDetails.tarraWeightValue,
    tarraWeightUnit: weightTicketDetails.tarraWeightUnit,
    direction: weightTicketDetails.direction || 'INBOUND',
    weightedAt: weightTicketDetails.weightedAt
      ? format(
          new Date(weightTicketDetails.weightedAt.toString()),
          'yyyy-MM-dd'
        )
      : undefined,
    pickupLocation: pickupLocationViewToFormValue(
      weightTicketDetails.pickupLocation
    ),
    deliveryLocation: pickupLocationViewToFormValue(
      weightTicketDetails.deliveryLocation
    ),
  };
};

/**
 * Normalizes number values by converting commas to periods for backend compatibility
 */
const normalizeNumberForBackend = (
  value: string | number | undefined
): string | undefined => {
  if (value === undefined || value === null || value === '') return undefined;
  if (typeof value === 'number' && isNaN(value)) return undefined;
  const stringValue = String(value);
  if (stringValue === 'NaN') return undefined;
  // Replace comma with period for backend
  return stringValue.replace(',', '.');
};

/**
 * Converts form values to WeightTicketRequest for create/update operations
 */
const formValuesToWeightTicketRequest = (
  formValues: WeightTicketFormValues
): WeightTicketRequest => {
  return {
    consignorParty: {
      type: 'company',
      companyId: formValues.consignorPartyId,
    } as any,
    carrierParty: formValues.carrierPartyId || undefined,
    truckLicensePlate: formValues.truckLicensePlate || undefined,
    reclamation: formValues.reclamation || undefined,
    note: formValues.note || undefined,
    lines: formValues.lines
      .filter((line) => line.catalogItemId)
      .map((line) => ({
        catalogItemId: parseInt(line.catalogItemId, 10),
        wasteStreamNumber: line.wasteStreamNumber || undefined,
        weight: {
          value: normalizeNumberForBackend(line.weightValue) || '0',
          unit: 'KG',
        },
      })),
    tarraWeightValue:
      normalizeNumberForBackend(formValues.tarraWeightValue) || undefined,
    tarraWeightUnit: WeightTicketRequestTarraWeightUnitEnum.Kg,
    secondWeighingValue:
      normalizeNumberForBackend(formValues.secondWeighingValue) || undefined,
    secondWeighingUnit: WeightTicketRequestSecondWeighingUnitEnum.Kg,
    weightedAt: formValues.weightedAt,
    direction:
      (formValues.direction as WeightTicketRequestDirectionEnum) ||
      WeightTicketRequestDirectionEnum.Inbound,
    pickupLocation: locationFormValueToPickupLocationRequest(
      formValues.pickupLocation
    ),
    deliveryLocation: locationFormValueToPickupLocationRequest(
      formValues.deliveryLocation
    ),
  };
};
