import { MasterDataTab } from "./MasterDataTab"
import { FormEvent } from "react";
import { DataTableProps } from "./MasterDataTab";
import { Eural } from "@/api/client";
import { Column } from "./MasterDataTab";
import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { ErrorBoundary } from "react-error-boundary";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { useForm } from "react-hook-form";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useEuralCodeCrud } from "@/features/masterdata/euralcodes/useEuralCode";
import { DeleteDialog } from "@/components/ui/dialog/DeleteDialog";

export const EuralCodeTab = () => {
    const { 
        read,
        form,
        deletion,
     } = useEuralCodeCrud();

    const columns: Column<Eural>[] = [
        { key: "code", label: "Code", width: "20", accessor: (item) => item.code },
        { key: "description", label: "Beschrijving", width: "80", accessor: (item) => item.description },
    ];

    const data: DataTableProps<Eural> = {
        columns,
        items: read.items,
    };

    return (
        <>
            <MasterDataTab
                data={data}
                searchQuery={(query) => read.setSearchQuery(query)}
                openAddForm={form.openForCreate}
                editAction={(item) => form.openForEdit(item)}
                removeAction={(item) => deletion.initiate(item)}
            />
            <EuralForm
                isOpen={form.isOpen}
                setIsOpen={form.close}
                onCancel={form.close}
                onSubmit={form.submit}
                initialData={form.item}
            />
            <DeleteDialog
                      isOpen={Boolean(deletion.item)}
                      setIsOpen={deletion.cancel}
                      onDelete={() =>
                        deletion.item &&
                        deletion.confirm(deletion.item)
                      }
                      title={"Euralcode verwijderen"}
                      description={`Weet u zeker dat u euralcode met code ${deletion.item?.code} wilt verwijderen?`}
                    />
        </>
    )
}

interface EuralFormProps {
    isOpen: boolean;
    setIsOpen: (value: boolean) => void;
    onCancel: () => void;
    onSubmit: (eural: Eural) => void;
    initialData?: Eural;
}

const EuralForm = ({ isOpen, setIsOpen, onCancel, onSubmit, initialData }: EuralFormProps) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const {
        register,
        handleSubmit,
        reset,
        formState: { errors },
    } = useForm<Eural>({
        values: initialData,
    });

    const cancel = () => {
        reset({code: '', description: ''});
        onCancel();
    }

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await handleSubmit(async (data) => {
            try {
                await onSubmit({
                    code: data.code,
                    description: data.description,
                });
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
                    <FormTopBar title={initialData ? "Eural code bewerken" : "Eural code toevoegen"} onClick={cancel} />
                    <div className="flex flex-col items-center self-stretch p-4 gap-4">
                        <div className="flex items-start self-stretch gap-4">
                            <TextFormField
                                title={'Code'}
                                placeholder={'Vul een code in'}
                                formHook={{
                                    register,
                                    name: 'code',
                                    rules: { required: 'Code is verplicht' },
                                    errors,
                                }}
                            />
                            <TextFormField
                                title={'Beschrijving'}
                                placeholder={'Vul een beschrijving in'}
                                formHook={{
                                    register,
                                    name: 'description',
                                    rules: { required: 'Beschrijving is verplicht' },
                                    errors,
                                }}
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
