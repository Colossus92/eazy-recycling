import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { DateTimeInput } from '@/components/ui/form/DateTimeInput';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider, useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button/Button.tsx';

interface TransportFromWeightTicketFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    weightTicketId?: number;
    onCreateTransport: (weightTicketId: number, pickupDateTime: string, deliveryDateTime?: string) => Promise<void>;
}

interface TransportFormData {
    pickupDateTime: string;
    deliveryDateTime?: string;
}

export const TransportFromWeightTicketForm = ({
    isOpen,
    setIsOpen,
    weightTicketId,
    onCreateTransport,
}: TransportFromWeightTicketFormProps) => {
    const formContext = useForm<TransportFormData>({
        defaultValues: {
            pickupDateTime: '',
            deliveryDateTime: '',
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
        
        await onCreateTransport(
            weightTicketId,
            values.pickupDateTime,
            values.deliveryDateTime || undefined
        );
        formContext.reset();
        setIsOpen(false);
    };

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
                                title={'Transport aanmaken'}
                                onClick={handleCancel}
                            />
                            <div
                                className={'flex flex-col items-start self-stretch gap-5 p-4'}
                            >
                                <div className={'flex flex-col items-start self-stretch gap-4'}>
                                    <DateTimeInput
                                        title={'Ophaalmoment (verplicht)'}
                                        testId={'pickup-datetime-input'}
                                        formHook={{
                                            register: formContext.register,
                                            name: 'pickupDateTime',
                                            rules: {
                                                required: 'Ophaalmoment is verplicht',
                                            },
                                            errors: formContext.formState.errors,
                                        }}
                                    />
                                    <DateTimeInput
                                        title={'Aflevermoment (optioneel)'}
                                        testId={'delivery-datetime-input'}
                                        formHook={{
                                            register: formContext.register,
                                            name: 'deliveryDateTime',
                                            rules: {},
                                            errors: formContext.formState.errors,
                                        }}
                                    />
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
                                        label={'Transport aanmaken'}
                                        fullWidth={true}
                                        data-testid={'submit-button'}
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
