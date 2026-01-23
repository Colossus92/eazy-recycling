import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useEffect } from 'react';
import { toastService } from '@/components/ui/toast/toastService.ts';
import {
  CompanyView,
  WasteStreamDetailView,
  WasteStreamRequest,
} from '@/api/client';
import { wasteStreamService } from '@/api/services/wasteStreamService.ts';
import { WeightTicketDetailViewConsignorParty } from '@/api/client/models/weight-ticket-detail-view-consignor-party';
import { LocationFormValue } from '@/types/forms/LocationFormValue';
import {
  locationFormValueToPickupLocationRequest,
  pickupLocationViewToFormValue,
} from '@/types/forms/locationConverters';
import { AxiosError } from 'axios';
import { WasteStreamValidationResponse } from '@/api/client/models/waste-stream-validation-response';

export interface WasteStreamFormValues {
  /**
   * Afzender
   */
  consignorPartyId: string;
  consignorClassification: string;
  /**
   * Ontdoener
   */
  pickupPartyId: string;
  /**
   * Pickup location using the new LocationFormValue structure
   */
  pickupLocation: LocationFormValue;
  processorPartyId: string;
  /**
   * Waste stream number - required when processor is not the current tenant
   */
  wasteStreamNumber: string;
  goodsName: string;
  processingMethodCode: string;
  euralCode: string;
  /**
   * Reference to the generic material this waste stream is linked to
   */
  catalogItemId: string;
}

export const fieldsToValidate: Array<Array<keyof WasteStreamFormValues>> = [
  // Step 0: Waste stream parties
  [
    'consignorPartyId',
    'consignorClassification',
    'pickupLocation',
    /**
     * Verwerker
     */
    'processorPartyId',
  ],

  // Step 1: Goods section fields
  ['pickupPartyId', 'goodsName', 'euralCode', 'processingMethodCode'],
];

export function useWasteStreamForm(
  wasteStreamNumber?: string,
  onSuccess?: () => void
) {
  const queryClient = useQueryClient();
  const formContext = useForm<WasteStreamFormValues>({
    defaultValues: {
      consignorPartyId: '',
      consignorClassification: '1',
      pickupPartyId: '',
      pickupLocation: { type: 'company', companyId: '', companyName: '' },
      processorPartyId: '',
      wasteStreamNumber: '',
      goodsName: '',
      processingMethodCode: '',
      euralCode: '',
      catalogItemId: '',
    },
  });

  const { data, isLoading } = useQuery({
    queryKey: ['wasteStream', wasteStreamNumber],
    queryFn: () => wasteStreamService.getByNumber(wasteStreamNumber!),
    enabled: !!wasteStreamNumber,
  });

  // Populate form when data is loaded
  useEffect(() => {
    if (data) {
      const formValues = wasteStreamDetailsToFormValues(data);
      formContext.reset(formValues);
    }
  }, [data, formContext]);

  // Draft mutation - saves without validation
  const draftMutation = useMutation({
    mutationFn: async (formData: WasteStreamFormValues) => {
      const request = formValuesToCreateWasteStreamRequest(formData);
      if (wasteStreamNumber) {
        return wasteStreamService.updateDraft(wasteStreamNumber, request);
      } else {
        return wasteStreamService.createDraft(request);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['wasteStreams'] });

      toastService.success(
        !wasteStreamNumber
          ? 'Afvalstroomnummer concept aangemaakt'
          : 'Afvalstroomnummer concept bijgewerkt'
      );
      resetForm();

      if (onSuccess) {
        onSuccess();
      }
    },
    onError: (error: unknown) => {
      console.error('Error saving draft:', error);

      let errorMessage = `Er is een fout opgetreden bij het ${wasteStreamNumber ? 'bijwerken' : 'aanmaken'} van het concept`;

      if (error instanceof AxiosError && error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      toastService.error(errorMessage);
    },
  });

  // Validate mutation - saves and validates
  const validateMutation = useMutation({
    mutationFn: async (formData: WasteStreamFormValues) => {
      const request = formValuesToCreateWasteStreamRequest(formData);
      if (wasteStreamNumber) {
        return wasteStreamService.updateAndValidate(wasteStreamNumber, request);
      } else {
        return wasteStreamService.createAndValidate(request);
      }
    },
    onSuccess: async (validationResponse) => {
      await queryClient.invalidateQueries({ queryKey: ['wasteStreams'] });

      if (validationResponse?.isValid) {
        toastService.success(
          !wasteStreamNumber
            ? 'Afvalstroomnummer aangemaakt en gevalideerd'
            : 'Afvalstroomnummer bijgewerkt en gevalideerd'
        );
        resetForm();

        if (onSuccess) {
          onSuccess();
        }
      }
    },
    onError: (error: unknown) => {
      console.error('Error validating:', error);

      // Check if this is a 409 Conflict error with validation errors
      if (error instanceof AxiosError && error.response?.status === 409) {
        const validationResponse = error.response
          .data as WasteStreamValidationResponse;
        if (
          validationResponse?.errors &&
          validationResponse.errors.length > 0
        ) {
          // Return validation errors so the component can display them
          throw validationResponse.errors;
        }
      }

      let errorMessage = `Er is een fout opgetreden bij het valideren van het afvalstroomnummer`;

      if (error instanceof AxiosError && error.response?.data?.message) {
        errorMessage = error.response.data.message;
      }

      toastService.error(errorMessage);
    },
  });

  const resetForm = () => {
    formContext.reset({
      consignorPartyId: '',
      consignorClassification: '1',
      pickupPartyId: '',
      pickupLocation: { type: 'company', companyId: '', companyName: '' },
      processorPartyId: '',
      wasteStreamNumber: '',
      goodsName: '',
      processingMethodCode: '',
      euralCode: '',
      catalogItemId: '',
    });
  };

  return {
    formContext,
    fieldsToValidate,
    draftMutation,
    validateMutation,
    data,
    isLoading,
    resetForm,
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
): CompanyView | null => {
  if (isCompanyConsignor(consignorParty)) {
    return (consignorParty as any).company;
  }
  return null;
};

const wasteStreamDetailsToFormValues = (
  wasteStreamDetails: WasteStreamDetailView
): WasteStreamFormValues => {
  const consignorCompany = resolveConsignorCompany(
    wasteStreamDetails.consignorParty
  );

  return {
    consignorPartyId: consignorCompany?.id || '',
    consignorClassification:
      wasteStreamDetails.consignorClassification.toString(),
    pickupPartyId: wasteStreamDetails.pickupParty.id,
    pickupLocation: pickupLocationViewToFormValue(
      wasteStreamDetails.pickupLocation
    ),
    processorPartyId:
      wasteStreamDetails.deliveryLocation.processorPartyId || '',
    wasteStreamNumber: wasteStreamDetails.wasteStreamNumber || '',
    goodsName: wasteStreamDetails.wasteType.name,
    processingMethodCode: wasteStreamDetails.wasteType.processingMethod.code,
    euralCode: wasteStreamDetails.wasteType.euralCode.code,
    catalogItemId: wasteStreamDetails.catalogItemId?.toString() || '',
  };
};

/**
 * Converts form values to WasteStreamRequest for create/update operations
 */
const formValuesToCreateWasteStreamRequest = (
  formValues: WasteStreamFormValues
): WasteStreamRequest => {
  return {
    wasteStreamNumber: formValues.wasteStreamNumber || undefined,
    name: formValues.goodsName,
    euralCode: formValues.euralCode,
    processingMethodCode: formValues.processingMethodCode,
    collectionType: 'DEFAULT',
    pickupLocation: locationFormValueToPickupLocationRequest(
      formValues.pickupLocation
    ) as any,
    processorPartyId: formValues.processorPartyId,
    consignorParty: {
      type: 'company',
      companyId: formValues.consignorPartyId,
    } as any,
    consignorClassification: Number(formValues.consignorClassification),
    pickupParty: formValues.pickupPartyId,
    catalogItemId: formValues.catalogItemId || undefined,
  };
};
