import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { toastService } from '@/components/ui/toast/toastService.ts';
import {
    WeightTicketDetailView,
    WeightTicketDetailViewConsignorParty,
    CompanyView,
    WeightTicketRequest,
} from '@/api/client';
import { weightTicketService } from '@/api/services/weightTicketService';

export interface WeightTicketFormValues {
    consignorPartyId: string;
    carrierPartyId: string;
    truckLicensePlate: string;
    reclamation: string;
    note: string;
}

export function useWeightTicketForm(
    weightTicketNumber?: number,
    onSuccess?: () => void
) {
    const queryClient = useQueryClient();
    const { data, isLoading } = useQuery({
        queryKey: ['weightTickets', weightTicketNumber],
        queryFn: async () => {
            const response = await weightTicketService.getByNumber(weightTicketNumber!!);
            const formValues = weightTicketDetailsToFormValues(response);
            formContext.reset(formValues);

            return response;
        },
        enabled: !!weightTicketNumber,
    });
    const formContext = useForm<WeightTicketFormValues>({
        defaultValues: {
            consignorPartyId: '',
            carrierPartyId: '',
            truckLicensePlate: '',
            reclamation: '',
            note: '',
        }
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

            toastService.success(
                !data ? 'Weegbon aangemaakt' : 'Weegbon bijgewerkt'
            );
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
        });
    };

    return {
        formContext,
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
    consignorParty: WeightTicketDetailViewConsignorParty
): consignorParty is WeightTicketDetailViewConsignorParty & { type: 'company'; company: CompanyView } => {
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

const weightTicketDetailsToFormValues = (weightTicketDetails: WeightTicketDetailView): WeightTicketFormValues => {
    const consignorCompany = resolveConsignorCompany(weightTicketDetails.consignorParty);

    return {
        consignorPartyId: consignorCompany?.id || '',
        carrierPartyId: weightTicketDetails.carrierParty?.id || '',
        truckLicensePlate: weightTicketDetails.truckLicensePlate || '',
        reclamation: weightTicketDetails.reclamation || '',
        note: weightTicketDetails.note || '',
    };
}

/**
 * Converts form values to WeightTicketRequest for create/update operations
 */
const formValuesToWeightTicketRequest = (
    formValues: WeightTicketFormValues
): WeightTicketRequest => {
    return {
        consignorParty: {
            type: 'company',
            companyId: formValues.consignorPartyId
        } as any,
        carrierParty: formValues.carrierPartyId,
        truckLicensePlate: formValues.truckLicensePlate,
        reclamation: formValues.reclamation,
        note: formValues.note,
    };
}
