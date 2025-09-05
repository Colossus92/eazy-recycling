import { useQueryClient } from '@tanstack/react-query';
import { Truck } from '@/types/api.ts';
import { Column } from '@/features/crud/ContentTable.tsx';
import { TruckForm } from '@/features/trucks/TruckForm.tsx';
import { CrudPage } from '@/features/crud/CrudPage.tsx';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import { useTruckCrud } from '@/features/trucks/useTruckCrud.ts';

export const TruckManagement = () => {
  const queryClient = useQueryClient();
  const {
    displayedTrucks,
    setQuery,
    isAdding,
    setIsAdding,
    setEditing,
    setDeleting,
    editing,
    deleting,
    create,
    update,
    remove,
    error,
    isLoading,
  } = useTruckCrud();

  const columns: Column<Truck>[] = [
    { key: 'licensePlate', label: 'Kenteken' },
    { key: 'brand', label: 'Merk' },
    { key: 'model', label: 'Beschrijving' },
  ];

  return (
    <CrudPage<Truck>
      title={'Vrachtwagenbeheer'}
      data={{
        items: displayedTrucks,
        columns,
        setQuery,
      }}
      dialogs={{
        add: {
          open: isAdding,
          onClose: () => setIsAdding(false),
          onSave: (data) => {
            return create(data);
          },
        },
        update: {
          open: !!editing,
          item: editing,
          onClose: () => setEditing(undefined),
          onSave: (data) => {
            return update(data);
          },
        },
        delete: {
          title: 'Vrachtwagen verwijderen',
          description: `Weet u zeker dat u vrachtwagen met kenteken ${deleting?.licensePlate} wilt verwijderen?`,
          open: !!deleting,
          item: deleting,
          onClose: () => setDeleting(undefined),
          onConfirm: (data) => {
            return remove(data);
          },
        },
      }}
      actions={{
        onAdd: setIsAdding,
        onDelete: setDeleting,
        onEdit: setEditing,
      }}
      renderForm={(close, onSubmit, itemToEdit) => (
        <TruckForm onCancel={close} onSubmit={onSubmit} truck={itemToEdit} />
      )}
      renderEmptyState={(open) => (
        <EmptyState
          icon={TruckTrailer}
          text={'Geen vrachtwagens gevonden'}
          onClick={open}
        />
      )}
      error={error}
      onReset={() => {
        queryClient.invalidateQueries({ queryKey: ['trucks'] }).catch(() => {});
      }}
      isLoading={isLoading}
    />
  );
};

export default TruckManagement;
