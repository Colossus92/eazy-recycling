import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { VatRateResponse } from '@/api/client';
import { useVatRatesCrud } from '@/features/crud/masterdata/vatrates/useVatRates';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { VatRateForm } from './VatRateForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const VatRatesTab = () => {
    const {
        read,
        form,
        deletion,
    } = useVatRatesCrud();

    const columns: Column<VatRateResponse>[] = [
        { key: "vatCode", label: "BTW Code", width: "15", accessor: (item) => item.vatCode },
        { key: "percentage", label: "Percentage", width: "15", accessor: (item) => `${item.percentage}%` },
        { key: "countryCode", label: "Land", width: "10", accessor: (item) => item.countryCode },
        { key: "validFrom", label: "Geldig vanaf", width: "15", accessor: (item) => item.validFrom },
        { key: "validTo", label: "Geldig tot", width: "15", accessor: (item) => item.validTo || '-' },
        { key: "description", label: "Beschrijving", width: "30", accessor: (item) => item.description },
    ];

    const data: DataTableProps<VatRateResponse> = {
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
                        text={'Geen BTW tarieven gevonden'}
                        onClick={open}
                    />
                )}
                isLoading={read.isLoading}
                errorHandling={read.errorHandling}
            />
            {/*
                Form to add or edit VAT rates
             */}
            <VatRateForm
                isOpen={form.isOpen}
                onCancel={form.close}
                onSubmit={form.submit}
                initialData={form.item}
            />
            {/*
                Dialog to confirm deletion of VAT rates
             */}
            <DeleteDialog
                isOpen={Boolean(deletion.item)}
                setIsOpen={deletion.cancel}
                onDelete={() =>
                    deletion.item &&
                    deletion.confirm(deletion.item)
                }
                title={"BTW tarief verwijderen"}
                description={`Weet u zeker dat u BTW tarief met code ${deletion.item?.vatCode} wilt verwijderen?`}
            />
        </>
    )
}
