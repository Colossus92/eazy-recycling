import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { MaterialPriceResponse } from '@/api/client';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { MaterialPriceForm } from './MaterialPriceForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import { useMaterialPricesCrud } from './useMaterialPrices';

export const MaterialPricesTab = () => {
  const { read, form, deletion } = useMaterialPricesCrud();

  const columns: Column<MaterialPriceResponse>[] = [
    {
      key: 'id',
      label: 'Code',
      width: '20',
      accessor: (item) => item.code,
    },
    {
      key: 'name',
      label: 'Naam',
      width: '20',
      accessor: (item) => item.name,
    },
    {
      key: 'defaultPrice',
      label: 'Prijs',
      width: '60',
      accessor: (item) => item.defaultPrice?.toString() || '-',
    },
  ];

  const data: DataTableProps<MaterialPriceResponse> = {
    columns,
    items: read.items,
  };

  return (
    <>
      <MasterDataTab
        data={data}
        searchQuery={(query) => read.setSearchQuery(query)}
        editAction={(item) => form.openForEdit(item)}
        removeAction={(item) => deletion.initiate(item)}
        renderEmptyState={(open) => (
          <EmptyState
            icon={ArchiveBook}
            text={'Geen materiaalprijzen gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      {/*
                Form to add or edit material prices
             */}
      <MaterialPriceForm
        isOpen={form.isOpen}
        onCancel={form.close}
        onSubmit={form.submit}
        initialData={form.item}
      />
      {/*
                Dialog to confirm deletion of material prices
             */}
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Materiaalprijs verwijderen'}
        description={`Weet u zeker dat u deze materiaalprijs wilt verwijderen?`}
      />
    </>
  );
};
