import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider, useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button/Button.tsx';

interface WeightTicketSplitFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    weightTicketId?: number;
    onSplit: (id: number, originalPercentage: number, newPercentage: number) => Promise<void>;
}

interface SplitFormData {
    originalWeightTicketPercentage: number;
    newWeightTicketPercentage: number;
}

export const WeightTicketSplitForm = ({
    isOpen,
    setIsOpen,
    weightTicketId,
    onSplit,
}: WeightTicketSplitFormProps) => {
    const formContext = useForm<SplitFormData>({
        defaultValues: {
            originalWeightTicketPercentage: 50,
            newWeightTicketPercentage: 50,
        },
    });

    const handleCancel = () => {
        formContext.reset();
        setIsOpen(false);
    };

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const isValid = await formContext.trigger();
        if (!isValid || !weightTicketId) return;

        const values = formContext.getValues();
        
        // Validate that percentages add up to 100
        const total = values.originalWeightTicketPercentage + values.newWeightTicketPercentage;
        if (total !== 100) {
            formContext.setError('originalWeightTicketPercentage', {
                message: 'Percentages moeten optellen tot 100%'
            });
            return;
        }

        await onSplit(
            weightTicketId,
            values.originalWeightTicketPercentage,
            values.newWeightTicketPercentage
        );
        formContext.reset();
        setIsOpen(false);
    };

    const watchOriginal = formContext.watch('originalWeightTicketPercentage');
    const watchNew = formContext.watch('newWeightTicketPercentage');
    const total = (watchOriginal || 0) + (watchNew || 0);
    const isInvalid = total !== 100;

    return (
        <ErrorBoundary fallbackRender={fallbackRender}>
            <FormDialog isOpen={isOpen} setIsOpen={handleCancel}>
                <div className={'w-full'}>
                    <FormProvider {...formContext}>
                        <form
                            className="flex flex-col items-center self-stretch"
                            onSubmit={(e) => submitForm(e)}
                        >
                            <FormTopBar
                                title={`Weegbon ${weightTicketId} splitsen`}
                                onClick={handleCancel}
                            />
                            <div
                                className={'flex flex-col items-start self-stretch gap-5 p-4'}
                            >
                                <div className={'flex flex-col items-start self-stretch gap-4'}>
                                    <NumberFormField
                                        title={'Oorspronkelijk weegbon percentage (%)'}
                                        placeholder={'Vul percentage in'}
                                        step={1}
                                        formHook={{
                                            register: formContext.register,
                                            name: 'originalWeightTicketPercentage',
                                            rules: {
                                                required: 'Percentage is verplicht',
                                                min: { value: 1, message: 'Minimum is 1%' },
                                                max: { value: 99, message: 'Maximum is 99%' },
                                            },
                                            errors: formContext.formState.errors,
                                        }}
                                        value={formContext.watch('originalWeightTicketPercentage')?.toString()}
                                    />
                                    <NumberFormField
                                        title={'Nieuw weegbon percentage (%)'}
                                        placeholder={'Vul percentage in'}
                                        step={1}
                                        formHook={{
                                            register: formContext.register,
                                            name: 'newWeightTicketPercentage',
                                            rules: {
                                                required: 'Percentage is verplicht',
                                                min: { value: 1, message: 'Minimum is 1%' },
                                                max: { value: 99, message: 'Maximum is 99%' },
                                            },
                                            errors: formContext.formState.errors,
                                        }}
                                        value={formContext.watch('newWeightTicketPercentage')?.toString()}
                                    />
                                    {isInvalid && (
                                        <div className="text-caption-1 text-color-status-error-dark">
                                            Totaal: {total}%. Percentages moeten optellen tot 100%.
                                        </div>
                                    )}
                                </div>
                            </div>
                            <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
                                <div className={'flex-1'}>
                                    <Button
                                        variant={'secondary'}
                                        label={'Annuleren'}
                                        onClick={handleCancel}
                                        fullWidth={true}
                                        data-testid={'cancel-button'}
                                    />
                                </div>
                                <div className={'flex-1'}>
                                    <Button
                                        type="submit"
                                        variant={'primary'}
                                        label={'Weegbon splitsen'}
                                        fullWidth={true}
                                        data-testid={'submit-button'}
                                        disabled={isInvalid}
                                    />
                                </div>
                            </div>
                        </form>
                    </FormProvider>
                </div>
            </FormDialog>
        </ErrorBoundary>
    );
};
