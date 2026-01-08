import { Column, DataTableProps, MasterDataTab } from '../MasterDataTab';
import { MaterialPriceResponse } from '@/api/client';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { MaterialPriceForm } from './MaterialPriceForm';
import { MaterialPriceSyncDialog } from './MaterialPriceSyncDialog';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import ArrowCounterClockwise from '@/assets/icons/ArrowCounterClockwise.svg?react';
import { useMaterialPricesCrud } from './useMaterialPrices';
import { useMaterialPriceSync } from './useMaterialPriceSync';
import { Button } from '@/components/ui/button/Button';

export const MaterialPricesTab = () => {
  const { read, form, deletion } = useMaterialPricesCrud();
  const sync = useMaterialPriceSync();

  const columns: Column<MaterialPriceResponse>[] = [
    {
      key: 'id',
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
      key: 'defaultPrice',
      label: 'Prijs',
      width: '20',
      accessor: (item) =>
        item.defaultPrice
          ? new Intl.NumberFormat('nl-NL', {
              style: 'currency',
              currency: 'EUR',
            }).format(item.defaultPrice)
          : '-',
    },
    {
      key: 'publishToPricingApp',
      label: 'Sync naar app',
      width: '40',
      accessor: (item) => (item.publishToPricingApp ? 'Ja' : 'Nee'),
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
        additionalActions={
          <div className="flex justify-end mb-4">
            <Button
              variant="secondary"
              label="Push prijzen naar app"
              icon={ArrowCounterClockwise}
              onClick={sync.openPreview}
            />
          </div>
        }
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
      {/*
        Dialog for sync preview and execution
      */}
      <MaterialPriceSyncDialog
        isOpen={sync.isPreviewOpen}
        onClose={sync.closePreview}
        onSync={sync.executeSync}
        preview={sync.preview}
        isLoading={sync.isLoadingPreview}
        isExecuting={sync.isExecuting}
        hasChanges={sync.hasChanges}
      />
    </>
  );
};
