import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { MaterialResponse } from '@/api/client';
import { useMaterialsCrud } from '@/features/crud/masterdata/materials/useMaterials';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { MaterialForm } from './MaterialForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const MaterialsTab = () => {
  const { read, form, deletion } = useMaterialsCrud();

  const columns: Column<MaterialResponse>[] = [
    {
      key: 'code',
      label: 'Code',
      width: '15',
      accessor: (item) => item.code,
    },
    {
      key: 'name',
      label: 'Naam',
      width: '15',
      accessor: (item) => item.name,
    },
    {
      key: 'materialGroupCode',
      label: 'Materiaalgroepcode',
      width: '15',
      accessor: (item) => item.materialGroupCode,
    },
    {
      key: 'materialGroupName',
      label: 'Materiaalgroepnaam',
      width: '15',
      accessor: (item) => item.materialGroupName,
    },
    {
      key: 'unitOfMeasure',
      label: 'Eenheid',
      width: '10',
      accessor: (item) => item.unitOfMeasure,
    },
    {
      key: 'vatCode',
      label: 'BTW Code',
      width: '10',
      accessor: (item) => item.vatCode,
    },
    {
      key: 'salesAccountNumber',
      label: 'Grbk verkoop',
      width: '10',
      accessor: (item) => item.salesAccountNumber,
    },
    {
      key: 'purchaseAccountNumber',
      label: 'Grbk inkoop',
      width: '10',
      accessor: (item) => item.purchaseAccountNumber,
    },
  ];

  const data: DataTableProps<MaterialResponse> = {
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
            text={'Geen materialen gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      {/*
                Form to add or edit materials
             */}
      <MaterialForm
        isOpen={form.isOpen}
        onCancel={form.close}
        onSubmit={form.submit}
        initialData={form.item}
      />
      {/*
                Dialog to confirm deletion of materials
             */}
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Materiaal verwijderen'}
        description={`Weet u zeker dat u materiaal "${deletion.item?.name}" wilt verwijderen?`}
      />
    </>
  );
};
