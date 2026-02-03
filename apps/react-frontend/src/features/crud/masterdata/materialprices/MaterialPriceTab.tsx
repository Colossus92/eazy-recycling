import { MaterialPriceResponse } from '@/api/client';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { MaterialPriceForm } from './MaterialPriceForm';
import { MaterialPriceSyncDialog } from './MaterialPriceSyncDialog';
import { EmptyState } from '../../EmptyState';
import ArchiveBook from '@/assets/icons/ArchiveBook.svg?react';
import ArrowCounterClockwise from '@/assets/icons/ArrowCounterClockwise.svg?react';
import SortIcon from '@/assets/icons/Sort.svg?react';
import { useMaterialPricesCrud } from './useMaterialPrices';
import { useMaterialPriceSync } from './useMaterialPriceSync';
import { Button } from '@/components/ui/button/Button';
import { Toggle } from '@/components/ui/toggle/Toggle';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { TabPanel } from '@headlessui/react';
import { ErrorBoundary } from 'react-error-boundary';
import { fallbackRender } from '@/utils/fallbackRender';
import { ActionMenu } from '../../ActionMenu';
import { PaginationRow } from '../../pagination/PaginationRow';
import { useState } from 'react';
import { ClipLoader } from 'react-spinners';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';

interface ColumnConfig {
  id: string;
  label: string;
  sortable?: boolean;
  width: string;
  render: (item: MaterialPriceResponse) => string | React.ReactNode;
}

const formatDateTime = (instant: string | undefined): string => {
  if (!instant) return '-';
  try {
    return new Intl.DateTimeFormat('nl-NL', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(instant));
  } catch {
    return '-';
  }
};

export const MaterialPricesTab = () => {
  const { read, form, deletion } = useMaterialPricesCrud();
  const sync = useMaterialPriceSync();
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const columns: ColumnConfig[] = [
    {
      id: 'code',
      label: 'Code',
      sortable: true,
      width: '10%',
      render: (item) => item.code,
    },
    {
      id: 'name',
      label: 'Naam',
      sortable: true,
      width: '20%',
      render: (item) => item.name,
    },
    {
      id: 'defaultPrice',
      label: 'Prijs',
      sortable: false,
      width: '12%',
      render: (item) =>
        item.defaultPrice
          ? new Intl.NumberFormat('nl-NL', {
              style: 'currency',
              currency: 'EUR',
            }).format(item.defaultPrice)
          : '-',
    },
    {
      id: 'publishToPricingApp',
      label: 'Sync naar app',
      sortable: false,
      width: '12%',
      render: (item) => (item.publishToPricingApp ? 'Ja' : 'Nee'),
    },
    {
      id: 'lastModifiedAt',
      label: 'Laatst gewijzigd',
      sortable: false,
      width: '18%',
      render: (item) => formatDateTime(item.lastModifiedAt),
    },
    {
      id: 'externalPricingAppSyncedAt',
      label: 'Laatst gesynchroniseerd',
      sortable: false,
      width: '18%',
      render: (item) => formatDateTime(item.externalPricingAppSyncedAt),
    },
  ];

  const handleSortClick = (columnId: string) => {
    let newDirection: 'asc' | 'desc' = 'asc';
    if (read.sortConfig.sortBy === columnId) {
      newDirection = read.sortConfig.sortDirection === 'asc' ? 'desc' : 'asc';
    }
    read.setSortConfig({
      sortBy: columnId,
      sortDirection: newDirection,
    });
  };

  const renderSortableHeader = (column: ColumnConfig) => {
    const isActive = read.sortConfig.sortBy === column.id;

    if (!column.sortable) {
      return <span>{column.label}</span>;
    }

    return (
      <div
        className="flex items-center gap-1 cursor-pointer select-none"
        onClick={() => handleSortClick(column.id)}
      >
        <span>{column.label}</span>
        <SortIcon
          className={`w-4 h-4 transition-transform ${
            isActive
              ? read.sortConfig.sortDirection === 'desc'
                ? 'rotate-180 text-color-brand-primary'
                : 'text-color-brand-primary'
              : 'text-color-text-tertiary'
          }`}
        />
      </div>
    );
  };

  const setQueryAndResetPage = (query: string) => {
    read.setSearchQuery(query);
    setPage(1);
  };

  return (
    <>
      <TabPanel
        className={
          'flex-1 flex flex-col items-start self-stretch gap-4 overflow-hidden'
        }
      >
        <ContentTitleBar
          setQuery={setQueryAndResetPage}
          leftActions={
            <Toggle
              checked={read.publishedOnly}
              onChange={read.setPublishedOnly}
              label="Alleen app prijzen"
              testId="published-only-toggle"
            />
          }
        >
          <Button
            variant="secondary"
            label="Push prijzen naar app"
            icon={ArrowCounterClockwise}
            onClick={sync.openPreview}
          />
        </ContentTitleBar>
        <ErrorBoundary
          fallbackRender={fallbackRender}
          onReset={read.errorHandling.reset}
        >
          <ErrorThrowingComponent error={read.errorHandling.error} />
          {read.items.length === 0 ? (
            read.isLoading ? (
              <div className="flex justify-center items-center h-24 w-full">
                <ClipLoader
                  size={20}
                  color={'text-color-text-invert-primary'}
                  aria-label="Laad spinner"
                />
              </div>
            ) : (
              <EmptyState
                icon={ArchiveBook}
                text={'Geen materiaalprijzen gevonden'}
              />
            )
          ) : (
            <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary overflow-y-auto">
              <table className="w-full table-fixed border-collapse">
                <colgroup>
                  {columns.map((col) => (
                    <col key={col.id} style={{ width: col.width }} />
                  ))}
                  <col style={{ width: '63px' }} />
                </colgroup>
                <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
                  <tr className="text-subtitle-1">
                    {columns.map((col) => (
                      <th
                        className={'px-4 py-3 text-left truncate'}
                        key={col.id}
                      >
                        {renderSortableHeader(col)}
                      </th>
                    ))}
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody>
                  {read.items
                    .slice((page - 1) * rowsPerPage, page * rowsPerPage)
                    .map((item, index) => (
                      <tr
                        key={index}
                        className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
                        onDoubleClick={() => form.openForEdit(item)}
                      >
                        {columns.map((col) => (
                          <td className="p-4" key={col.id}>
                            {col.render(item)}
                          </td>
                        ))}
                        <td className="p-4 text-center">
                          <ActionMenu<MaterialPriceResponse>
                            onEdit={form.openForEdit}
                            onDelete={deletion.initiate}
                            item={item}
                          />
                        </td>
                      </tr>
                    ))}
                </tbody>
                <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
                  <tr className="text-body-2 bg-color-surface-primary">
                    <td colSpan={columns.length + 1} className="p-4">
                      <PaginationRow
                        page={page}
                        setPage={setPage}
                        rowsPerPage={rowsPerPage}
                        setRowsPerPage={setRowsPerPage}
                        numberOfResults={read.items.length}
                      />
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </ErrorBoundary>
      </TabPanel>
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
