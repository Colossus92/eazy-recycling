import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { MaterialGroupResponse } from '@/api/client';
import { useMaterialGroupsCrud } from '@/features/crud/masterdata/materialgroups/useMaterialGroups';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { MaterialGroupForm } from './MaterialGroupForm';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';

export const MaterialGroupsTab = () => {
  const { read, form, deletion } = useMaterialGroupsCrud();

  const columns: Column<MaterialGroupResponse>[] = [
    {
      key: 'code',
      label: 'Code',
      width: '15',
      accessor: (item) => item.code,
    },
    {
      key: 'name',
      label: 'Naam',
      width: '25',
      accessor: (item) => item.name,
    },
    {
      key: 'description',
      label: 'Beschrijving',
      width: '60',
      accessor: (item) => item.description,
    },
  ];

  const data: DataTableProps<MaterialGroupResponse> = {
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
            text={'Geen materiaalgroepen gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      {/*
                Form to add or edit material groups
             */}
      <MaterialGroupForm
        isOpen={form.isOpen}
        onCancel={form.close}
        onSubmit={form.submit}
        initialData={form.item}
      />
      {/*
                Dialog to confirm deletion of material groups
             */}
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Materiaalgroep verwijderen'}
        description={`Weet u zeker dat u materiaalgroep "${deletion.item?.name}" wilt verwijderen?`}
      />
    </>
  );
};
