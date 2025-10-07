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
        displayedEurals, 
        setSearchQuery, 
        create, 
        isFormOpen, 
        setIsFormOpen,
        deletion,
     } = useEuralCodeCrud();

    const columns: Column<Eural>[] = [
        { key: "code", label: "Code", width: "20", accessor: (item) => item.code },
        { key: "description", label: "Beschrijving", width: "80", accessor: (item) => item.description },
    ];

    const data: DataTableProps<Eural> = {
        columns,
        items: displayedEurals,
    };

    return (
        <>
            <MasterDataTab
                data={data}
                searchQuery={(query) => setSearchQuery(query)}
                openAddForm={() => setIsFormOpen(true)}
                removeAction={(item) => deletion.initiate(item)}
            />
            <EuralForm
                isOpen={isFormOpen}
                setIsOpen={setIsFormOpen}
                onCancel={() => setIsFormOpen(false)}
                onSubmit={create}
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

const EuralForm = ({ isOpen, setIsOpen, onCancel, onSubmit }: { isOpen: boolean, setIsOpen: (value: boolean) => void, onCancel: () => void; onSubmit: (eural: Eural) => void }) => {
    const { handleError, ErrorDialogComponent } = useErrorHandling();
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<Eural>();

    const submitForm = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        await handleSubmit(async (data) => {
            try {
                await onSubmit({
                    code: data.code,
                    description: data.description,
                });
                onCancel(); // Only close the form if submission was successful
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
                    <FormTopBar title={"Eural code toevoegen"} onClick={onCancel} />
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
                    <FormActionButtons onClick={onCancel} item={undefined} />
                </form>
                <ErrorDialogComponent />
            </ErrorBoundary>
        </FormDialog>
    )
}
