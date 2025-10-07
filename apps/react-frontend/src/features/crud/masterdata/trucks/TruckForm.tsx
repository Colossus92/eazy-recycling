import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { FormEvent } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { Truck } from "@/api/client/models";

interface TruckFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    onCancel: () => void;
    onSubmit: (eural: Truck) => void;
    initialData?: Truck;
}

export interface TruckFormValues {
    brand: string;
    model: string;
    licensePlate: string;
}

function toTruck(data: TruckFormValues) {
    return {
        brand: data.brand,
        model: data.model,
        licensePlate: data.licensePlate,
    } as Truck;
}

function toFormData(truck?: Truck) {
    return {
        brand: truck?.brand,
        model: truck?.model,
        licensePlate: truck?.licensePlate
    } as TruckFormValues
}

export const TruckForm = ({ isOpen, setIsOpen, onCancel, onSubmit, initialData }: TruckFormProps) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<TruckFormValues>({
        values: toFormData(initialData),
    });

    const cancel = () => {
        reset({ licensePlate: '', model: '', brand: '', });
        onCancel();
    }

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await handleSubmit(async (data) => {
            try {
                await onSubmit(toTruck(data));
                cancel();
            } catch (error) {
                handleError(error);
            }
        })();
    };

    return (
        <FormDialog isOpen={isOpen} setIsOpen={setIsOpen}>
            <ErrorBoundary fallbackRender={fallbackRender}>
                <form
                    className="flex flex-col items-center self-stretch"
                    onSubmit={(e) => submitForm(e)}
                >
                    <FormTopBar title={initialData ? "Vrachtwagen bewerken" : "Vrachtwagen toevoegen"} onClick={cancel} />
                    <div className="flex flex-col items-center self-stretch p-4 gap-4">
                        <TextFormField
                            title={'Merk'}
                            placeholder={'Vul merk in'}
                            formHook={{
                                register,
                                name: 'brand',
                                rules: { required: 'Merk is verplicht' },
                                errors,
                            }}
                        />
                        <div className="flex items-start self-stretch gap-4">
                            <TextFormField
                                title={'Beschrijving'}
                                placeholder={'Vul een beschrijving in'}
                                formHook={{
                                    register,
                                    name: 'model',
                                    rules: { required: 'Beschrijving is verplicht' },
                                    errors,
                                }}
                            />
                            <TextFormField
                                title={'Kenteken'}
                                placeholder={'Vul kenteken in'}
                                formHook={{
                                    register,
                                    name: 'licensePlate',
                                    rules: { required: 'Kenteken is verplicht' },
                                    errors,
                                }}
                                disabled={Boolean(initialData?.licensePlate)}
                            />
                        </div>
                    </div>
                    <FormActionButtons onClick={cancel} item={undefined} />
                </form>
                <ErrorDialogComponent />
            </ErrorBoundary>
        </FormDialog>
    )
}
