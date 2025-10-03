import { MasterDataTab } from "./MasterDataTab"
import { useEffect, useState } from "react";
import { euralService } from "@/api/services/euralService";
import { DataTableProps } from "./MasterDataTab";
import { Eural } from "@/api/client";
import { Column } from "./MasterDataTab";
import { useMemo } from "react";
import { FormDialog } from "@/components/ui/dialog/FormDialog";
import { ErrorBoundary } from "react-error-boundary";
import { fallbackRender } from "@/utils/fallbackRender";
import { FormTopBar } from "@/components/ui/form/FormTopBar";
import { TextFormField } from "@/components/ui/form/TextFormField";
import { FormActionButtons } from "@/components/ui/form/FormActionButtons";
import { useForm } from "react-hook-form";
import { useErrorHandling } from "@/hooks/useErrorHandling";

export const EuralCodeTab = () => {
    const [items, setItems] = useState<Eural[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [isFormOpen, setIsFormOpen] = useState(false);

    const displayedEurals = useMemo(
        () => items.filter((item) => {
            return (
                item.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
                item.description.toLowerCase().includes(searchQuery.toLowerCase())
            )
        }
        ),
        [items, searchQuery]
    );
    const onSubmit = async (eural: Eural) => {
        setItems([...items, eural]);
    };

    const columns: Column<Eural>[] = [
        { key: "code", label: "Code", width: "20", accessor: (item) => item.code },
        { key: "description", label: "Beschrijving", width: "80", accessor: (item) => item.description },
    ];

    const data: DataTableProps<Eural> = {
        columns,
        items: displayedEurals,
    };

    useEffect(() => {
        const fetchEuralCodes = async () => {
            const euralCodes = await euralService.getAll();
            setItems(euralCodes);
        };
        fetchEuralCodes();
    }, []);

    return (
        <>
            <MasterDataTab
                data={data}
                searchQuery={(query) => setSearchQuery(query)}
                openAddForm={() => setIsFormOpen(true)}
            />
            <EuralForm isOpen={isFormOpen} setIsOpen={setIsFormOpen} onCancel={() => setIsFormOpen(false)} onSubmit={onSubmit} />
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

    return (
        <FormDialog isOpen={isOpen} setIsOpen={setIsOpen}>
            <ErrorBoundary fallbackRender={fallbackRender}>
                <form
                    className="flex flex-col items-center self-stretch"
                    onSubmit={(e) => handleSubmit(onSubmit)(e)}
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
