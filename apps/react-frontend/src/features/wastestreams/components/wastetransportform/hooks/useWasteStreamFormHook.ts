import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import {
    WasteStreamDetailView,
    CompanyView,
    WasteStreamRequest,
} from '@/api/client';
import { wasteStreamService } from '@/api/services/wasteStreamService.ts';
import { WasteStreamDetailViewConsignorParty } from '@/api/client/models/waste-stream-detail-view-consignor-party';
import { LocationFormValue } from '@/types/forms/LocationFormValue';
import { pickupLocationViewToFormValue, locationFormValueToPickupLocationRequest } from '@/types/forms/locationConverters';
import { AxiosError } from 'axios';

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
    goodsName: string;
    processingMethodCode: string;
    euralCode: string;
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
    [
        'pickupPartyId',
        'goodsName',
        'euralCode',
        'processingMethodCode',
    ],
];

export function useWasteStreamForm(
    wasteStreamNumber?: string,
    onSuccess?: () => void
) {
    const queryClient = useQueryClient();
    const { data, isLoading } = useQuery({
        queryKey: ['wasteStream', wasteStreamNumber],
        queryFn: async () => {
            const response = await wasteStreamService.getByNumber(wasteStreamNumber!);
            const formValues = wasteStreamDetailsToFormValues(response);
            formContext.reset(formValues);

            return response;
        },
        enabled: !!wasteStreamNumber,
    });
    const formContext = useForm<WasteStreamFormValues>({
        defaultValues: {
            consignorPartyId: '',
            consignorClassification: '1',
            pickupPartyId: '',
            pickupLocation: { type: 'none' },
            processorPartyId: '',
            goodsName: '',
            processingMethodCode: '',
            euralCode: '',
        }
    });
    const mutation = useMutation({
        mutationFn: async (data: WasteStreamFormValues) => {
            const request = formValuesToCreateWasteStreamRequest(data);
            if (!!data && wasteStreamNumber) {
                return wasteStreamService.update(wasteStreamNumber, request);
            } else {
                return wasteStreamService.create(request);
            }
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ['wasteStreams'] });

            toastService.success(
                !data ? 'Afvalstroomnummer aangemaakt' : 'Afvalstroomnummer bijgewerkt'
            );
            resetForm();

            if (onSuccess) {
                onSuccess();
            }
        },
        onError: (error: unknown) => {
            console.error('Error submitting form:', error);
            
            let errorMessage = `Er is een fout opgetreden bij het ${data ? 'bijwerken' : 'aanmaken'} van het afvalstroomnummer`;
            
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
                pickupLocation: { type: 'none' },
                processorPartyId: '',
                goodsName: '',
                processingMethodCode: '',
                euralCode: '',
            });
    };

    return {
        formContext,
        fieldsToValidate,
        mutation,
        data,
        isLoading,
        resetForm
    };
}

/**
 * Type guard to check if consignorParty is a company
 */
const isCompanyConsignor = (
    consignorParty: WasteStreamDetailViewConsignorParty
): consignorParty is WasteStreamDetailViewConsignorParty & { type: 'company'; company: CompanyView } => {
    return (consignorParty as any).type === 'company';
};

/**
 * Resolves the consignorParty to CompanyView when it's of type company
 */
const resolveConsignorCompany = (
    consignorParty: WasteStreamDetailViewConsignorParty
): CompanyView | null => {
    if (isCompanyConsignor(consignorParty)) {
        return (consignorParty as any).company;
    }
    return null;
};

const wasteStreamDetailsToFormValues = (wasteStreamDetails: WasteStreamDetailView): WasteStreamFormValues => {
    const consignorCompany = resolveConsignorCompany(wasteStreamDetails.consignorParty);

    return {
        consignorPartyId: consignorCompany?.id || '',
        consignorClassification: wasteStreamDetails.consignorClassification.toString(),
        pickupPartyId: wasteStreamDetails.pickupParty.id,
        pickupLocation: pickupLocationViewToFormValue(wasteStreamDetails.pickupLocation),
        processorPartyId: wasteStreamDetails.deliveryLocation.processorPartyId || '',
        goodsName: wasteStreamDetails.wasteType.name,
        processingMethodCode: wasteStreamDetails.wasteType.processingMethod.code,
        euralCode: wasteStreamDetails.wasteType.euralCode.code,
    };
}

/**
 * Converts form values to WasteStreamRequest for create/update operations
 */
const formValuesToCreateWasteStreamRequest = (
    formValues: WasteStreamFormValues
): WasteStreamRequest => {
    return {
        name: formValues.goodsName,
        euralCode: formValues.euralCode,
        processingMethodCode: formValues.processingMethodCode,
        collectionType: 'DEFAULT',
        pickupLocation: locationFormValueToPickupLocationRequest(formValues.pickupLocation) as any,
        processorPartyId: formValues.processorPartyId,
        consignorParty: {
            type: 'company',
            companyId: formValues.consignorPartyId
        } as any,
        consignorClassification: Number(formValues.consignorClassification),
        pickupParty: formValues.pickupPartyId,
    };
}
