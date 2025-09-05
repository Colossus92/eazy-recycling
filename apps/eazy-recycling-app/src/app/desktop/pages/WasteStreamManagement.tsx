import { useQueryClient } from '@tanstack/react-query';
import { WasteStream } from '@/types/api.ts';
import { Column } from '@/features/crud/ContentTable.tsx';
import { CrudPage } from '@/features/crud/CrudPage.tsx';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import { useWasteStreamCrud } from '@/features/wasteStream/useWasteStreamCrud.ts';
import { WasteStreamForm } from '@/features/wasteStream/WasteStreamForm';

export const WasteStreamManagement = () => {
  const queryClient = useQueryClient();
  const {
    displayedWasteStreams,
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
  } = useWasteStreamCrud();

  const columns: Column<WasteStream>[] = [
    { key: 'number', label: 'Afvalstroomnummer' },
    { key: 'name', label: 'Gebruikelijke benaming' },
  ];

  return (
    <CrudPage<WasteStream>
      title={'Afvalstroombeheer'}
      data={{
        items: displayedWasteStreams,
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
          title: 'Afvalstroomnummer verwijderen',
          description: `Weet u zeker dat u afvalstroom met nummer ${deleting?.number} wilt verwijderen?`,
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
        <WasteStreamForm
          onCancel={close}
          onSubmit={onSubmit}
          wasteStream={itemToEdit}
        />
      )}
      renderEmptyState={(open) => (
        <EmptyState
          icon={BxRecycle}
          text={'Geen afvalstroomnummers gevonden'}
          onClick={open}
        />
      )}
      error={error}
      onReset={() => {
        queryClient
          .invalidateQueries({ queryKey: ['wasteStreams'] })
          .catch(() => {});
      }}
      isLoading={isLoading}
    />
  );
};

export default WasteStreamManagement;
