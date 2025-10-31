import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { FormEvent, useEffect } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { WasteContainerRequest, WasteContainerView } from "@/api/client/models";
import { Company, companyService } from "@/api/services/companyService";
import { useQuery } from "@tanstack/react-query";
import { TextAreaFormField } from "@/components/ui/form/TextAreaFormField";
import { CompanyAddressInput } from "@/components/ui/form/CompanyAddressInput";

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
    companyId?: string;
    street: string;
    buildingNumber: string;
    postalCode?: string;
    city: string;
    notes?: string;
}

function toWasteContainer(
    data: WasteContainerFormValues,
    companies: Company[]
): WasteContainerRequest {
    const container: WasteContainerRequest = {
        id: data.id,
        location: {
            address: {
                streetName: data.street,
                buildingNumber: data.buildingNumber,
                postalCode: data.postalCode || '',
                city: data.city,
                country: 'Nederland',
            }
        },
        notes: data.notes,
    };

    if (data.companyId) {
        const company = companies.find((c) => c.id === data.companyId);
        if (company) {
            container.location = {
                companyId: company.id,
                companyName: company.name,
                address: {
                    streetName: data.street,
                    buildingNumber: data.buildingNumber,
                    postalCode: data.postalCode || '',
                    city: data.city,
                    country: 'Nederland',
                }
            };
        }
    }

    return container;
}

export const WasteContainerForm = ({ isOpen, setIsOpen, onCancel, onSubmit, initialData }: WasteContainerFormProps) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const {
        register,
        handleSubmit,
        reset,
        watch,
        setValue,
        formState: { errors },
        control,
    } = useForm<WasteContainerFormValues>();

    const { data: companies = [] } = useQuery<Company[]>({
        queryKey: ['companies'],
        queryFn: () => companyService.getAll(),
    });

    // Reset form when initialData changes (for edit mode)
    useEffect(() => {
        if (initialData) {
            reset({
                id: initialData.id,
                companyId: initialData?.location?.companyId,
                street: initialData?.location?.addressView?.street,
                buildingNumber: initialData?.location?.addressView?.houseNumber,
                postalCode: initialData?.location?.addressView?.postalCode,
                city: initialData?.location?.addressView?.city,
                notes: initialData.notes,
            });
        } else {
            // Clear form when creating new container
            reset({
                id: '',
                companyId: '',
                street: '',
                buildingNumber: '',
                postalCode: '',
                city: '',
                notes: '',
            });
        }
    }, [initialData, reset]);



    const cancel = () => {
        reset({ id: '', companyId: '', street: '', buildingNumber: '', postalCode: '', city: '', notes: '', });
        onCancel();
    }

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await handleSubmit(async (data) => {
            try {
                await onSubmit(toWasteContainer(data, companies));
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
                    <FormTopBar title={initialData ? "Container bewerken" : "Container toevoegen"} onClick={cancel} />
                    <div className="flex flex-col items-center self-stretch p-4 gap-4">
                        <TextFormField
                            title={'Containerkenmerk'}
                            placeholder={'Vul kenmerk in'}
                            disabled={Boolean(initialData?.id)}
                            formHook={{
                                register,
                                name: 'id',
                                rules: {
                                    required: 'Kenmerk is verplicht',
                                    validate: (value?: string) => {
                                        const trimmed = value?.trim() || '';
                                        return trimmed !== '' || 'Kenmerk mag niet leeg zijn';
                                    },
                                },
                                errors,
                            }}
                        />

                        <CompanyAddressInput
                            formContext={{ register, formState: { errors }, control, watch, setValue } as any}
                            fieldNames={{
                                companyId: 'companyId',
                                street: 'street',
                                buildingNumber: 'buildingNumber',
                                postalCode: 'postalCode',
                                city: 'city',
                            }}
                            required={false}
                        />

                        <TextAreaFormField
                            title={'Opmerkingen'}
                            placeholder={'Plaats opmerkingen'}
                            formHook={{
                                register,
                                name: 'notes',
                                rules: {},
                                errors,
                            }}
                            value={initialData?.notes}
                        />
                    </div>
                    <FormActionButtons onClick={cancel} item={undefined} />
                </form>
                <ErrorDialogComponent />
            </ErrorBoundary>
        </FormDialog>
    )
}
