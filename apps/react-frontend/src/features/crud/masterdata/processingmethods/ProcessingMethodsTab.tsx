import { MasterDataTab } from "../MasterDataTab"
import { DataTableProps } from "../MasterDataTab";
import { ProcessingMethod } from "@/api/client";
import { Column } from "../MasterDataTab";
import { useProcessingMethodsCrud } from "@/features/crud/masterdata/processingmethods/useProcessingMethods";
import { DeleteDialog } from "@/components/ui/dialog/DeleteDialog";
import { EmptyState } from "../../EmptyState";
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import { ProcessingMethodForm } from "./ProcessingMethodForm";

export const ProcessingMethodsTab = () => {
    const {
        read,
        form,
        deletion,
    } = useProcessingMethodsCrud();

    const columns: Column<ProcessingMethod>[] = [
        { key: "code", label: "Code", width: "20", accessor: (item) => item.code },
        { key: "description", label: "Beschrijving", width: "80", accessor: (item) => item.description },
    ];

    const data: DataTableProps<ProcessingMethod> = {
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
                renderEmptyState={(open) => (
                    <EmptyState
                        icon={ArchiveBook}
                        text={'Geen vewerkingsmethodes gevonden'}
                        onClick={open}
                    />
                )}
                isLoading={read.isLoading}
                errorHandling={read.errorHandling}
            />
            {/*
                Form to add or delete processing methods
             */}
            <ProcessingMethodForm
                isOpen={form.isOpen}
                setIsOpen={form.close}
                onCancel={form.close}
                onSubmit={form.submit}
                initialData={form.item}
            />
            {/*
                Dialog to confirm deletion of processing methods
             */}
            <DeleteDialog
                isOpen={Boolean(deletion.item)}
                setIsOpen={deletion.cancel}
                onDelete={() =>
                    deletion.item &&
                    deletion.confirm(deletion.item)
                }
                title={"Vewerkingsmethode verwijderen"}
                description={`Weet u zeker dat u verwerkingsmethode met code ${deletion.item?.code} wilt verwijderen?`}
            />
        </>
    )
}