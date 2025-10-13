import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import { 
    WasteStreamDetailView, 
    WasteStreamDetailViewConsignorParty, 
    CompanyView,
    WasteStreamRequest 
} from '@/api/client';
import { wasteStreamService } from '@/api/services/wasteStreamService.ts';

export interface WasteStreamFormValues {
    /**
     * Afzender
     */
    consignorPartyId: string;
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
    const formContext = useForm<WasteStreamFormValues>();
    const mutation = useMutation({
        mutationFn: async (data: WasteStreamFormValues) => {
            const request = formValuesToCreateWasteStreamRequest(data, wasteStreamNumber);
            if (!!data && wasteStreamNumber) {
                return wasteStreamService.update(request);
            } else {
                return wasteStreamService.create(request);
            }
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ['wasteStream'] });

            toastService.success(
                !data ? 'Afvalstroomnummer aangemaakt' : 'Afvalstroomnummer bijgewerkt'
            );
            formContext.reset();

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

    return {
        formContext,
        fieldsToValidate,
        mutation,
        data,
        isLoading,
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
        pickupPartyId: wasteStreamDetails.pickupParty.id,
        pickupCompanyId: wasteStreamDetails.pickupLocation?.type,
        pickupCompanyBranchId: (wasteStreamDetails as any).pickupCompanyBranchId,
        pickupStreet: (wasteStreamDetails.pickupLocation as any)?.street || '',
        pickupBuildingNumber: (wasteStreamDetails.pickupLocation as any)?.buildingNumber || '',
        pickupPostalCode: (wasteStreamDetails.pickupLocation as any)?.postalCode || '',
        pickupCity: (wasteStreamDetails.pickupLocation as any)?.city || '',
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
    formValues: WasteStreamFormValues,
    wasteStreamNumber?: string
): WasteStreamRequest => {
    return {
        wasteStreamNumber: wasteStreamNumber || '',
        name: formValues.goodsName,
        euralCode: formValues.euralCode,
        processingMethodCode: formValues.processingMethodCode,
        collectionType: 'DEFAULT', // Default value, adjust as needed
        pickupLocation: {
            type: 'dutch_address',
            streetName: formValues.pickupStreet,
            buildingNumber: formValues.pickupBuildingNumber,
            postalCode: formValues.pickupPostalCode,
            city: formValues.pickupCity,
            country: 'NL'
        } as any,
        processorPartyId: formValues.processorPartyId,
        consignorParty: {
            type: 'company',
            companyId: formValues.consignorPartyId
        } as any,
        pickupParty: formValues.pickupPartyId,
    };
}
