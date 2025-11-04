import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { FormEvent, useEffect } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm, FormProvider } from "react-hook-form";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { WasteContainerRequest, WasteContainerView } from "@/api/client/models";
import { TextAreaFormField } from "@/components/ui/form/TextAreaFormField";
import { AddressFormField } from "@/components/ui/form/addressformfield/AddressFormField";
import { LocationFormValue } from "@/types/forms/LocationFormValue";
import { pickupLocationViewToFormValue, locationFormValueToPickupLocationRequest } from "@/types/forms/locationConverters";

interface WasteContainerFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    onCancel: () => void;
    onSubmit: (eural: WasteContainerRequest) => void;
    initialData?: WasteContainerView;
}

export interface WasteContainerFormValues {
    id: string;
    containerType: string;
    location: LocationFormValue;
    notes?: string;
}

function toWasteContainer(
    data: WasteContainerFormValues
): WasteContainerRequest {
    const container: WasteContainerRequest = {
        id: data.id,
        location: locationFormValueToPickupLocationRequest(data.location),
        notes: data.notes,
    };

    return container;
}

export const WasteContainerForm = ({ isOpen, setIsOpen, onCancel, onSubmit, initialData }: WasteContainerFormProps) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const formContext = useForm<WasteContainerFormValues>();

    // Reset form when initialData changes (for edit mode)
    useEffect(() => {
        if (initialData) {
            formContext.reset({
                id: initialData.id,
                containerType: '',
                location: pickupLocationViewToFormValue(initialData.location),
                notes: initialData.notes,
            });
        } else {
            // Clear form when creating new container
            formContext.reset({
                id: '',
                containerType: '',
                location: { type: 'none' },
                notes: '',
            });
        }
    }, [initialData, formContext.reset]);



    const cancel = () => {
        formContext.reset({ id: '', containerType: '', location: { type: 'none' }, notes: '' });
        onCancel();
    }

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await formContext.handleSubmit(async (data) => {
            try {
                await onSubmit(toWasteContainer(data));
                cancel();
            } catch (error) {
                handleError(error);
            }
        })();
    };

    return (
        <FormDialog isOpen={isOpen} setIsOpen={setIsOpen}>
            <ErrorBoundary fallbackRender={fallbackRender}>
                <FormProvider {...formContext}>
                    <form
                        className="flex flex-col items-center self-stretch"
                        onSubmit={(e) => submitForm(e)}
                    >
                        <FormTopBar title={initialData ? "Container bewerken" : "Container toevoegen"} onClick={cancel} />
                        <div className="flex flex-col items-center self-stretch p-4 gap-4">
                            <TextFormField
                                title={'Containerkenmerk'}
                                placeholder={'Vul kenmerk in'}
                                disabled={Boolean(initialData?.id)}
                                formHook={{
                                    register: formContext.register,
                                    name: 'id',
                                    rules: {
                                        required: 'Kenmerk is verplicht',
                                        validate: ((value: string) => {
                                            const trimmed = value?.trim() || '';
                                            return trimmed !== '' || 'Kenmerk mag niet leeg zijn';
                                        }) as any,
                                    },
                                    errors: formContext.formState.errors,
                                }}
                            />

                            <AddressFormField
                                control={formContext.control}
                                name="location"
                                label="Locatie van container"
                                required={false}
                                isNoLocationAllowed={true}
                            />

                            <TextAreaFormField
                                title={'Opmerkingen'}
                                placeholder={'Plaats opmerkingen'}
                                formHook={{
                                    register: formContext.register,
                                    name: 'notes',
                                    rules: {},
                                    errors: formContext.formState.errors,
                                }}
                                value={initialData?.notes}
                            />
                        </div>
                        <FormActionButtons onClick={cancel} item={undefined} />
                    </form>
                </FormProvider>
                <ErrorDialogComponent />
            </ErrorBoundary>
        </FormDialog>
    )
}
