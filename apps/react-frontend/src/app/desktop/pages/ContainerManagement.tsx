import { useQueryClient } from '@tanstack/react-query';
import { WasteContainer } from '@/api/client';
import { Column } from '@/features/crud/ContentTable.tsx';
import { CrudPage } from '@/features/crud/CrudPage.tsx';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import ShippingContainer from '@/assets/icons/ShippingContainer.svg?react';
import { useWasteContainerCrud } from '@/features/containers/useWasteContainerCrud.ts';
import { WasteContainerForm } from '@/features/containers/WasteContainerForm.tsx';

export function getWasteContainerLocation(container: WasteContainer): string {
  if (container.location?.companyName) {
    return `${container.location.companyName}, ${container.location.address?.city}`;
  }
  if (container.location?.address) {
    const address = container.location.address;
    return `${address.streetName} ${address.buildingNumber}, ${address.city}`;
  }
  return '';
}

export const ContainerManagement = () => {
  const {
    displayedContainers,
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
  } = useWasteContainerCrud();
  const queryClient = useQueryClient();

  const columns: Column<WasteContainer>[] = [
    { key: 'id', label: 'Kenmerk' },
    {
      key: 'location',
      label: 'Huidige locatie',
      accessor: (item: WasteContainer) => getWasteContainerLocation(item),
    },
    { key: 'notes', label: 'Opmerkingen' },
  ];

  return (
    <CrudPage<WasteContainer>
      title={'Containerbeheer'}
      data={{
        items: displayedContainers,
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
          title: 'Container verwijderen',
          description: `Weet u zeker dat u container met kenmerk ${deleting?.id} wilt verwijderen?`,
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
        <WasteContainerForm
          onCancel={close}
          onSubmit={onSubmit}
          wasteContainer={itemToEdit}
        />
      )}
      renderEmptyState={(open) => (
        <EmptyState
          icon={ShippingContainer}
          text={'Geen containers gevonden'}
          onClick={open}
        />
      )}
      error={error}
      onReset={() => {
        queryClient
          .invalidateQueries({ queryKey: ['containers'] })
          .catch(() => {});
      }}
    />
  );
};

export default ContainerManagement;
