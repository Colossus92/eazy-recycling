import { MasterDataTab } from "../MasterDataTab"
import { DataTableProps } from "../MasterDataTab";
import { Eural } from "@/api/client";
import { Column } from "../MasterDataTab";
import { useEuralCodesCrud } from "@/features/crud/masterdata/euralcodes/useEuralCodes";
import { DeleteDialog } from "@/components/ui/dialog/DeleteDialog";
import { EuralCodeForm } from "./EuralCodeForm";
import { EmptyState } from "../../EmptyState";
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const EuralCodesTab = () => {
    const {
        read,
        form,
        deletion,
    } = useEuralCodesCrud();

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
                renderEmptyState={(open) => (
                    <EmptyState
                        icon={ArchiveBook}
                        text={'Geen eural codes gevonden'}
                        onClick={open}
                    />
                )}
                isLoading={read.isLoading}
                errorHandling={read.errorHandling}
            />
            {/*
                Form to add or delete eural codes
             */}
            <EuralCodeForm
                isOpen={form.isOpen}
                setIsOpen={form.close}
                onCancel={form.close}
                onSubmit={form.submit}
                initialData={form.item}
            />
            {/*
                Dialog to confirm deletion of eural codes
             */}
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