import { Company, companyService } from '@/api/services/companyService';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { TruckSelectFormField } from '@/components/ui/form/selectfield/TruckSelectFormField';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { Note } from '@/features/planning/components/note/Note';
import { fallbackRender } from '@/utils/fallbackRender';
import { useQuery } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider } from 'react-hook-form';
import { WeightTicketStatusTag } from '../WeightTicketStatusTag';
import { useWeightTicketForm } from './useWeigtTicketFormHook';
import { WeightTicketLinesSection } from './WeightTicketLinesSection';
import { WeightTicketFormActionMenu } from './WeightTicketFormActionMenu';
import { useEffect, useState } from 'react';

interface WeightTicketFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    weightTicketNumber?: number;
    status?: string;
    onDelete: (id: number) => void;
    onSplit?: (id: number) => void;
    onComplete?: (id: number) => void;
    noDialog?: boolean;
}

export const WeightTicketForm = ({
    isOpen,
    setIsOpen,
    weightTicketNumber,
    status,
    onDelete,
    onSplit,
    onComplete,
    noDialog = false,
}: WeightTicketFormProps) => {
    const {
        data,
        isLoading,
        formContext,
        mutation,
        resetForm
    } = useWeightTicketForm(weightTicketNumber, () => setIsOpen(false));
    const [isDisabled, setIsDisabled] = useState(false);

    const handleClose = (value: boolean) => {
        if (!value) {
            resetForm();
        }
        setIsOpen(value);
    };

    const handleCancel = () => {
        resetForm();
        setIsOpen(false);
    };

    useEffect(() => {
        setIsDisabled(Boolean((data?.status || status) && (data?.status || status) !== 'DRAFT'));
    }, [data?.status, status]);

    const { data: companies = [] } = useQuery<Company[]>({
        queryKey: ['companies'],
        queryFn: () => companyService.getAll(),
    });
    const companyOptions = companies.map((company) => ({
        value: company.id || '',
        label: company.name,
    }));

    const onSubmit = formContext.handleSubmit(async (formValues) => {
        // Filter out empty lines (lines where wasteStreamNumber is empty)
        const filteredFormValues = {
            ...formValues,
            lines: formValues.lines.filter(
                (line) => line.wasteStreamNumber && line.wasteStreamNumber.trim() !== ''
            ),
        };
        
        await mutation.mutateAsync(filteredFormValues);
    });

    const handleSubmit = isDisabled ? (e: React.FormEvent) => e.preventDefault() : onSubmit;

    const formContent = (
        <div className={'w-full h-full'}>
            <FormProvider {...formContext}>
                <form
                    className="flex flex-col items-center self-stretch h-full"
                    onSubmit={handleSubmit}
                >
                    <FormTopBar
                        title={
                            data ? `Weegbon ${data.id}` : 'Nieuw Weegbon'
                        }
                        actions={
                            data && (
                                <WeightTicketFormActionMenu 
                                    weightTicket={data}
                                    onDelete={onDelete}
                                    onSplit={onSplit}
                                    onComplete={onComplete}
                                />
                            )
                        }
                        onClick={handleCancel}
                    />
                            <div
                                className={'flex flex-col items-start self-stretch gap-5 p-4 max-h-[calc(100vh-200px)] overflow-y-auto'}
                                >
                                {data?.cancellationReason && <Note note={data?.cancellationReason} />}
                                {isLoading ? (
                                    <div className="flex justify-center items-center w-full p-8">
                                        <p>Weegbon laden...</p>
                                    </div>
                                ) : (
                                    <div className={'flex flex-col items-start self-stretch gap-4'}>
                                        {data?.status && <WeightTicketStatusTag status={data.status as 'DRAFT' | 'COMPLETED' | 'INVOICED' | 'CANCELLED'} />}
                                        <div className="w-1/2">
                                            <SelectFormField
                                                title={'Opdrachtgever'}
                                                placeholder={'Selecteer een opdrachtgever'}
                                                options={companyOptions}
                                                testId="consignor-party-select"
                                                disabled={isDisabled}
                                                formHook={{
                                                    register: formContext.register,
                                                    name: 'consignorPartyId',
                                                    rules: { required: 'Opdrachtgever is verplicht' },
                                                    errors: formContext.formState.errors,
                                                    control: formContext.control,
                                                }}
                                            />
                                        </div>
                                        <div className="w-1/2">
                                            <SelectFormField
                                                title={'Vervoerder'}
                                                placeholder={'Selecteer een vervoerder'}
                                                options={companyOptions}
                                                testId="carrier-party-select"
                                                disabled={isDisabled}
                                                formHook={{
                                                    register: formContext.register,
                                                    name: 'carrierPartyId',
                                                    errors: formContext.formState.errors,
                                                    control: formContext.control,
                                                }}
                                            />
                                        </div>
                                        <div className="w-1/2">
                                            <TruckSelectFormField
                                                disabled={isDisabled}
                                                formHook={{
                                                    register: formContext.register,
                                                    name: 'truckLicensePlate',
                                                    errors: formContext.formState.errors,
                                                    control: formContext.control,
                                                }}
                                            />
                                        </div>
                                        <WeightTicketLinesSection disabled={isDisabled} />
                                        <TextFormField
                                            title={'Reclamatie'}
                                            placeholder={'Vul reclamatie in'}
                                            disabled={isDisabled}
                                            formHook={{
                                                register: formContext.register,
                                                name: 'reclamation',
                                                errors: formContext.formState.errors,
                                            }}
                                            value={formContext.getValues('reclamation')}
                                        />
                                        <TextAreaFormField
                                            title={'Opmerkingen'}
                                            placeholder={'Plaats opmerkingen'}
                                            disabled={isDisabled}
                                            formHook={{
                                                register: formContext.register,
                                                name: 'note',
                                                rules: {},
                                                errors: formContext.formState.errors,
                                            }}
                                            value={formContext.getValues('note')}
                                        />
                                    </div>
                                )}
                            </div>
                            <FormActionButtons onClick={handleCancel} item={data} disabled={isDisabled} />
                        </form>
                    </FormProvider>
                </div>
    );

    if (noDialog) {
        return (
            <ErrorBoundary fallbackRender={fallbackRender}>
                {formContent}
            </ErrorBoundary>
        );
    }

    return (
        <ErrorBoundary fallbackRender={fallbackRender}>
            <FormDialog isOpen={isOpen} setIsOpen={handleClose} width="w-[720px]">
                {formContent}
            </FormDialog>
        </ErrorBoundary>
    );
};
