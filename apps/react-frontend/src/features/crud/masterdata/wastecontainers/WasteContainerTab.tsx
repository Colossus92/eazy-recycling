import { MasterDataTab } from "../MasterDataTab"
import { DataTableProps } from "../MasterDataTab";
import { WasteContainer } from "@/api/client";
import { Column } from "../MasterDataTab";
import { DeleteDialog } from "@/components/ui/dialog/DeleteDialog";
import { EmptyState } from "../../EmptyState";
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import { useWasteContainerCrud } from "./useWasteContainerCrud";
import { WasteContainerForm } from "./WasteContainerForm";

function getWasteContainerLocation(container: WasteContainer): string {
  if (container.location?.companyName) {
    return `${container.location.companyName}, ${container.location.address?.city}`;
  }
  if (container.location?.address) {
    const address = container.location.address;
    return `${address.streetName} ${address.buildingNumber}, ${address.city}`;
  }
  return '';
}

export const WasteContainersTab = () => {
    const {
        read,
        form,
        deletion,
    } = useWasteContainerCrud();

    const columns: Column<WasteContainer>[] = [
        { key: "id", label: "Kenmerk", width: "20", accessor: (item) => item.id },
        { key: "location", label: "Huidige locatie", width: "30", accessor: (item) => getWasteContainerLocation(item) },
        { key: "notes", label: "Opmerkingen", width: "50", accessor: (item) => item.notes },
    ];

    const data: DataTableProps<WasteContainer> = {
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
                        icon={ShippingContainer}
                        text={'Geen containers gevonden'}
                        onClick={open}
                    />
                )}
                isLoading={read.isLoading}
                errorHandling={read.errorHandling}
            />
            {/*
                Form to add or delete eural codes
             */}
            <WasteContainerForm
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
                title={"Container verwijderen"}
                description={`Weet u zeker dat u container metntainer met kenmerk ${deletion.item?.id} wilt verwijderen?`}
            />
        </>
    )
}