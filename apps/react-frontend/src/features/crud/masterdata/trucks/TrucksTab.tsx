import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { TruckView } from '@/api/client';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { TruckForm } from './TruckForm';
import { EmptyState } from '../../EmptyState';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import { useTruckCrud } from './useTruckCrud';

export const TrucksTab = () => {
  const { read, form, deletion } = useTruckCrud();

  const columns: Column<TruckView>[] = [
    {
      key: 'licensePlate',
      label: 'Kenteken',
      width: '20',
      accessor: (item) => item.licensePlate,
    },
    {
      key: 'brand',
      label: 'Merk',
      width: '20',
      accessor: (item) => item.brand,
    },
    {
      key: 'description',
      label: 'Beschrijving',
      width: '80',
      accessor: (item) => item.description,
    },
  ];

  const data: DataTableProps<TruckView> = {
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
            icon={TruckTrailer}
            text={'Geen vrachtwagens gevonden'}
            onClick={open}
          />
        )}
        isLoading={read.isLoading}
        errorHandling={read.errorHandling}
      />
      {/*
                Form to add or delete eural codes
             */}
      <TruckForm
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
        onDelete={() => deletion.item && deletion.confirm(deletion.item)}
        title={'Vrachtwagen verwijderen'}
        description={`Weet u zeker dat u vrachtwagen met kenteken ${deletion.item?.licensePlate} wilt verwijderen?`}
      />
    </>
  );
};
