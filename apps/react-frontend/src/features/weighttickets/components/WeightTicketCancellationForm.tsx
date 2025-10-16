import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { TextAreaFormField } from '@/components/ui/form/TextAreaFormField';
import { fallbackRender } from '@/utils/fallbackRender';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { FormProvider, useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button/Button.tsx';

interface WeightTicketCancellationFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    weightTicketId?: number;
    onCancel: (id: number, reason: string) => Promise<void>;
}

interface CancellationFormData {
    cancellationReason: string;
}

export const WeightTicketCancellationForm = ({
    isOpen,
    setIsOpen,
    weightTicketId,
    onCancel,
}: WeightTicketCancellationFormProps) => {
    const formContext = useForm<CancellationFormData>({
        defaultValues: {
            cancellationReason: '',
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
        await onCancel(weightTicketId, values.cancellationReason);
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
                                title={`Weegbon ${weightTicketId} annuleren`}
                                onClick={handleCancel}
                            />
                            <div
                                className={'flex flex-col items-start self-stretch gap-5 p-4 w'}
                            >
                                <div className={'flex flex-col items-start self-stretch gap-4'}>
                                    <TextAreaFormField
                                        title={'Annuleringsreden'}
                                        placeholder={'Vul de reden voor annulering in'}
                                        formHook={{
                                            register: formContext.register,
                                            name: 'cancellationReason',
                                            rules: { required: 'Annuleringsreden is verplicht' },
                                            errors: formContext.formState.errors,
                                        }}
                                        value={formContext.watch('cancellationReason')}
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
                                      variant={'destructive'}
                                      label={'Weegbon annuleren'}
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
