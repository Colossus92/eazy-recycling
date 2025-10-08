import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { FormEvent, useEffect } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useForm } from "react-hook-form";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { WasteContainer } from "@/api/client/models";
import { Company, companyService } from "@/api/services/companyService";
import { SelectFormField } from "@/components/ui/form/selectfield/SelectFormField";
import { useQuery } from "@tanstack/react-query";
import { PostalCodeFormField } from "@/components/ui/form/PostalCodeFormField";
import { TextAreaFormField } from "@/components/ui/form/TextAreaFormField";

interface WasteContainerFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    onCancel: () => void;
    onSubmit: (eural: WasteContainer) => void;
    initialData?: WasteContainer;
}

export interface WasteContainerFormValues {
    uuid?: string;
    id: string;
    containerType: string;
    companyId?: string;
    street: string;
    houseNumber: string;
    postalCode?: string;
    city: string;
    notes?: string;
}

function toWasteContainer(
    data: WasteContainerFormValues,
    companies: Company[]
): WasteContainer {
    const container: WasteContainer = {
        uuid: data.uuid || crypto.randomUUID(),
        id: data.id,
        location: {
            address: {
                streetName: data.street,
                buildingNumber: data.houseNumber,
                postalCode: data.postalCode || '',
                city: data.city,
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
                    buildingNumber: data.houseNumber,
                    postalCode: data.postalCode || '',
                    city: data.city,
                }
            };
        }
    }

    return container;
}

export const WasteContainerForm = ({ isOpen, setIsOpen, onCancel, onSubmit, initialData }: WasteContainerFormProps) => {
    console.log(JSON.stringify(initialData))
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

    const companyOptions = companies.map((company) => ({
        value: company.id || '',
        label: company.name,
    }));


    const watchCompanyId = watch('companyId');
    const hasCompanySelected =
        watchCompanyId !== undefined &&
        watchCompanyId !== null &&
        watchCompanyId.length > 0;

    // Reset form when initialData changes (for edit mode)
    useEffect(() => {
        if (initialData) {
            reset({
                uuid: initialData.uuid,
                id: initialData.id,
                companyId: initialData?.location?.companyId,
                street: initialData?.location?.address?.streetName,
                houseNumber: initialData?.location?.address?.buildingNumber,
                postalCode: initialData?.location?.address?.postalCode,
                city: initialData?.location?.address?.city,
                notes: initialData.notes,
            });
        } else {
            // Clear form when creating new container
            reset({
                id: '',
                companyId: '',
                street: '',
                houseNumber: '',
                postalCode: '',
                city: '',
                notes: '',
            });
        }
    }, [initialData, reset]);

    // Auto-fill address fields when company is selected
    useEffect(() => {
        if (hasCompanySelected) {
            const company = companies.find((c) => c.id === watchCompanyId);
            if (company) {
                setValue('street', company.address.streetName || '');
                setValue('houseNumber', company.address.buildingNumber || '');
                setValue('postalCode', company.address.postalCode || '');
                setValue('city', company.address.city || '');
            }
        }
    }, [watchCompanyId, companies, setValue, hasCompanySelected]);



    const cancel = () => {
        reset({ id: '', companyId: '', street: '', houseNumber: '', postalCode: '', city: '', notes: '', });
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

                        <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
                            <span className="text-subtitle-1">Huidige locatie</span>

                            <div className="flex-grow-0 flex flex-col items-start self-stretch gap-4">
                                <SelectFormField
                                    title={'Kies bedrijf (optioneel)'}
                                    placeholder={'Selecteer een bedrijf of vul zelf een adres in'}
                                    options={companyOptions}
                                    formHook={{
                                        register,
                                        name: 'companyId',
                                        rules: {},
                                        errors,
                                        control,
                                    }}
                                />

                                {hasCompanySelected && (
                                    <div className="flex items-center max-w-96">
                                        <span className="text-body-2 whitespace-normal break-words">
                                            Adresgegevens worden automatisch ingevuld op basis van het
                                            geselecteerde bedrijf
                                        </span>
                                    </div>
                                )}
                            </div>

                            <div className="flex items-start self-stretch gap-4 flex-grow">
                                <TextFormField
                                    title={'Straat'}
                                    placeholder={'Vul straatnaam in'}
                                    formHook={{
                                        register,
                                        name: 'street',
                                        errors,
                                    }}
                                    value={initialData?.location?.address?.streetName}
                                    disabled={hasCompanySelected}
                                />

                                <TextFormField
                                    title={'Nummer'}
                                    placeholder={'Vul huisnummer in'}
                                    formHook={{
                                        register,
                                        name: 'houseNumber',
                                        errors,
                                    }}
                                    value={initialData?.location?.address?.buildingNumber}
                                    disabled={hasCompanySelected}
                                />
                            </div>

                            <div className="flex items-start self-stretch gap-4">
                                <PostalCodeFormField
                                    register={register}
                                    setValue={setValue}
                                    errors={errors}
                                    name="postalCode"
                                    value={initialData?.location?.address?.postalCode}
                                    required={false}
                                    disabled={hasCompanySelected}
                                />

                                <TextFormField
                                    title={'Plaats'}
                                    placeholder={'Vul Plaats in'}
                                    formHook={{
                                        register,
                                        name: 'city',
                                        errors,
                                    }}
                                    value={initialData?.location?.address?.city}
                                    disabled={hasCompanySelected}
                                />
                            </div>
                        </div>

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
