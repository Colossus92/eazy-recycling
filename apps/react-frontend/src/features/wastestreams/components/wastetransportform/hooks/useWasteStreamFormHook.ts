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

export interface WasteStreamFormValues {
    /**
     * Afzender
     */
    consignorPartyId: string;
    consignorClassification: number;
    /**
     * Ontdoener
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
        'pickupCompanyId',
        'pickupCompanyBranchId',
        'pickupStreet',
        'pickupBuildingNumber',
        'pickupPostalCode',
        'pickupCity',
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
            consignorClassification: 1,
            pickupPartyId: '',
            pickupCompanyId: '',
            pickupCompanyBranchId: '',
            pickupStreet: '',
            pickupBuildingNumber: '',
            pickupPostalCode: '',
            pickupCity: '',
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
        onError: (error) => {
            console.error('Error submitting form:', error);
            toastService.error(
                `Er is een fout opgetreden bij het ${data ? 'bijwerken' : 'aanmaken'} van het afvalstroomnummer`
            );
        },
    });

    const resetForm = () => {
            formContext.reset({
                consignorPartyId: '',
                consignorClassification: 1,
                pickupPartyId: '',
                pickupCompanyId: '',
                pickupCompanyBranchId: '',
                pickupStreet: '',
                pickupBuildingNumber: '',
                pickupPostalCode: '',
                pickupCity: '',
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
    const pickupLocation = wasteStreamDetails.pickupLocation as any;

    // Determine if pickupLocation is a company or dutch_address
    const isCompanyPickup = pickupLocation?.type === 'company';
    const pickupAddress = isCompanyPickup ? pickupLocation?.company?.address : pickupLocation;

    return {
        consignorPartyId: consignorCompany?.id || '',
        consignorClassification: wasteStreamDetails.consignorClassification,
        pickupPartyId: wasteStreamDetails.pickupParty.id,
        pickupCompanyId: isCompanyPickup ? pickupLocation?.company?.id : undefined,
        pickupCompanyBranchId: (wasteStreamDetails as any).pickupCompanyBranchId,
        pickupStreet: pickupAddress?.streetName || pickupAddress?.street || '',
        pickupBuildingNumber: pickupAddress?.buildingNumber || pickupAddress?.houseNumber || '',
        pickupPostalCode: pickupAddress?.postalCode || '',
        pickupCity: pickupAddress?.city || '',
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
    // Determine pickupLocation type based on whether pickupCompanyId is specified
    const pickupLocation = formValues.pickupCompanyId
        ? {
            type: 'company',
            companyId: formValues.pickupCompanyId
        }
        : {
            type: 'dutch_address',
            streetName: formValues.pickupStreet,
            buildingNumber: formValues.pickupBuildingNumber,
            postalCode: formValues.pickupPostalCode,
            city: formValues.pickupCity,
            country: 'Nederland'
        };

    return {
        name: formValues.goodsName,
        euralCode: formValues.euralCode,
        processingMethodCode: formValues.processingMethodCode,
        collectionType: 'DEFAULT',
        pickupLocation: pickupLocation as any,
        processorPartyId: formValues.processorPartyId,
        consignorParty: {
            type: 'company',
            companyId: formValues.consignorPartyId
        } as any,
        consignorClassification: formValues.consignorClassification,
        pickupParty: formValues.pickupPartyId,
    };
}
