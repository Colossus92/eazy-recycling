import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import {
  CompanyView,
  PickupLocationView,
  WeightTicketDetailView,
  WeightTicketDetailViewConsignorParty,
  WeightTicketRequest,
  WeightTicketRequestDirectionEnum,
  WeightTicketRequestTarraWeightUnitEnum,
} from '@/api/client';
import { weightTicketService } from '@/api/services/weightTicketService';

export interface WeightTicketLineFormValues {
  wasteStreamNumber: string;
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
  direction: string;
  pickupCompanyId: string;
  pickupCompanyBranchId?: string;
  pickupStreet: string;
  pickupBuildingNumber: string;
  pickupPostalCode: string;
  pickupCity: string;
  deliveryCompanyId: string;
  deliveryCompanyBranchId?: string;
  deliveryStreet: string;
  deliveryBuildingNumber: string;
  deliveryPostalCode: string;
  deliveryCity: string;
}

export function useWeightTicketForm(
  weightTicketNumber?: number,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
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
      direction: 'INBOUND',
      pickupCompanyId: '',
      pickupCompanyBranchId: '',
      pickupStreet: '',
      pickupBuildingNumber: '',
      pickupPostalCode: '',
      pickupCity: '',
      deliveryCompanyId: '',
      deliveryCompanyBranchId: '',
      deliveryStreet: '',
      deliveryBuildingNumber: '',
      deliveryPostalCode: '',
      deliveryCity: '',
    },
  });

  const { data, isLoading } = useQuery({
    queryKey: ['weightTickets', weightTicketNumber],
    queryFn: async () => {
      const response = await weightTicketService.getByNumber(
        weightTicketNumber!!
      );
      const formValues = weightTicketDetailsToFormValues(response);
      formContext.reset(formValues);

      return response;
    },
    enabled: !!weightTicketNumber,
  });
  const mutation = useMutation({
    mutationFn: async (data: WeightTicketFormValues) => {
      const request = formValuesToWeightTicketRequest(data);
      if (!!data && weightTicketNumber) {
        return weightTicketService.update(weightTicketNumber, request);
      } else {
        return weightTicketService.create(request);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['weightTickets'] });

      toastService.success(!data ? 'Weegbon aangemaakt' : 'Weegbon bijgewerkt');
      resetForm();

      if (onSuccess) {
        onSuccess();
      }
    },
    onError: (error) => {
      console.error('Error submitting form:', error);
      toastService.error(
        `Er is een fout opgetreden bij het ${data ? 'bijwerken' : 'aanmaken'} van het weegbon`
      );
    },
  });

  const resetForm = () => {
    formContext.reset({
      consignorPartyId: '',
      carrierPartyId: '',
      truckLicensePlate: '',
      reclamation: '',
      note: '',
      lines: [],
      tarraWeightValue: NaN,
      tarraWeightUnit: undefined,
      direction: 'INBOUND',
      pickupCompanyId: '',
      pickupCompanyBranchId: '',
      pickupStreet: '',
      pickupBuildingNumber: '',
      pickupPostalCode: '',
      pickupCity: '',
      deliveryCompanyId: '',
      deliveryCompanyBranchId: '',
      deliveryStreet: '',
      deliveryBuildingNumber: '',
      deliveryPostalCode: '',
      deliveryCity: '',
    });
  };

  return {
    formContext,
    mutation,
    data,
    isLoading,
    resetForm,
  };
}

/**
 * Resolves location address from WeightTicketDetailViewPickupLocation
 */
const resolveLocationAddress = (
  location?: PickupLocationView
): {
  companyId?: string;
  branchId?: string;
  street: string;
  buildingNumber: string;
  postalCode: string;
  city: string;
} => {
  if (!location) {
    return { street: '', buildingNumber: '', postalCode: '', city: '' };
  }

  const locationAny = location as any;

  // Handle PickupCompanyView (type === 'company')
  if (locationAny.type === 'company' && locationAny.company?.address) {
    const address = locationAny.company.address;
    return {
      companyId: locationAny.company.id,
      street: address.street || '',
      buildingNumber: address.houseNumber || '',
      postalCode: address.postalCode || '',
      city: address.city || '',
    };
  }

  // Handle DutchAddressView (type === 'dutch_address')
  if (locationAny.type === 'dutch_address') {
    return {
      street: locationAny.streetName || locationAny.street || '',
      buildingNumber:
        locationAny.buildingNumber || locationAny.houseNumber || '',
      postalCode: locationAny.postalCode || '',
      city: locationAny.city || '',
    };
  }

  // Handle ProjectLocationView (type === 'project_location')
  if (locationAny.type === 'project_location') {
    return {
      companyId: locationAny.company?.id,
      branchId: locationAny.id,
      street: locationAny.streetName || '',
      buildingNumber: locationAny.buildingNumber || '',
      postalCode: locationAny.postalCode || '',
      city: locationAny.city || '',
    };
  }

  return { street: '', buildingNumber: '', postalCode: '', city: '' };
};

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
  const pickupLocationAddress = resolveLocationAddress(
    weightTicketDetails.pickupLocation
  );
  const deliveryLocationAddress = resolveLocationAddress(
    weightTicketDetails.deliveryLocation
  );

  return {
    consignorPartyId: consignorCompany.id || '',
    carrierPartyId: weightTicketDetails.carrierParty?.id || '',
    truckLicensePlate: weightTicketDetails.truckLicensePlate || '',
    reclamation: weightTicketDetails.reclamation || '',
    note: weightTicketDetails.note || '',
    lines: (weightTicketDetails.lines || []).map((line) => ({
      wasteStreamNumber: line.wasteStreamNumber || '',
      weightValue: line.weightValue?.toString() || '',
      weightUnit: line.weightUnit || 'KG',
    })),
    tarraWeightValue: weightTicketDetails.tarraWeightValue,
    tarraWeightUnit: weightTicketDetails.tarraWeightUnit,
    direction: weightTicketDetails.direction || 'INBOUND',
    pickupCompanyId: pickupLocationAddress.companyId || '',
    pickupCompanyBranchId: pickupLocationAddress.branchId || '',
    pickupStreet: pickupLocationAddress.street,
    pickupBuildingNumber: pickupLocationAddress.buildingNumber,
    pickupPostalCode: pickupLocationAddress.postalCode,
    pickupCity: pickupLocationAddress.city,
    deliveryCompanyId: deliveryLocationAddress.companyId || '',
    deliveryCompanyBranchId: deliveryLocationAddress.branchId || '',
    deliveryStreet: deliveryLocationAddress.street,
    deliveryBuildingNumber: deliveryLocationAddress.buildingNumber,
    deliveryPostalCode: deliveryLocationAddress.postalCode,
    deliveryCity: deliveryLocationAddress.city,
  };
};

/**
 * Converts form values to WeightTicketRequest for create/update operations
 */
const formValuesToWeightTicketRequest = (
  formValues: WeightTicketFormValues
): WeightTicketRequest => {
  // Build pickup location
  let pickupLocation: any = undefined;
  if (formValues.pickupCompanyBranchId) {
    pickupLocation = {
      type: 'project_location',
      companyId: formValues.pickupCompanyId,
      projectLocationId: formValues.pickupCompanyBranchId,
    };
  } else if (formValues.pickupCompanyId) {
    pickupLocation = {
      type: 'company',
      companyId: formValues.pickupCompanyId,
    };
  } else if (formValues.pickupStreet) {
    pickupLocation = {
      type: 'dutch_address',
      streetName: formValues.pickupStreet,
      buildingNumber: formValues.pickupBuildingNumber,
      postalCode: formValues.pickupPostalCode,
      city: formValues.pickupCity,
    };
  }

  // Build delivery location
  let deliveryLocation: any = undefined;
  if (formValues.deliveryCompanyBranchId) {
    deliveryLocation = {
      type: 'project_location',
      companyId: formValues.deliveryCompanyId,
      projectLocationId: formValues.deliveryCompanyBranchId,
    };
  } else if (formValues.deliveryCompanyId) {
    deliveryLocation = {
      type: 'company',
      companyId: formValues.deliveryCompanyId,
    };
  } else if (formValues.deliveryStreet) {
    deliveryLocation = {
      type: 'dutch_address',
      streetName: formValues.deliveryStreet,
      buildingNumber: formValues.deliveryBuildingNumber,
      postalCode: formValues.deliveryPostalCode,
      city: formValues.deliveryCity,
    };
  }

  return {
    consignorParty: {
      type: 'company',
      companyId: formValues.consignorPartyId,
    } as any,
    carrierParty: formValues.carrierPartyId || undefined,
    truckLicensePlate: formValues.truckLicensePlate || undefined,
    reclamation: formValues.reclamation || undefined,
    note: formValues.note || undefined,
    lines: formValues.lines.map((line) => ({
      wasteStreamNumber: line.wasteStreamNumber,
      weight: {
        value: line.weightValue,
        unit: 'KG',
      },
    })),
    tarraWeightValue: formValues.tarraWeightValue?.toString(),
    tarraWeightUnit: WeightTicketRequestTarraWeightUnitEnum.Kg,
    direction:
      (formValues.direction as WeightTicketRequestDirectionEnum) ||
      WeightTicketRequestDirectionEnum.Inbound,
    pickupLocation,
    deliveryLocation,
  };
};
