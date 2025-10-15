import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import Plus from '@/assets/icons/Plus.svg?react';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Button } from '@/components/ui/button/Button';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';
import { ActionMenu } from '@/features/crud/ActionMenu';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { useWasteStreamCrud } from '@/features/wastestreams/hooks/useWasteStreamCrud';
import { WasteStreamForm } from '@/features/wastestreams/components/wastetransportform/components/WasteStreamForm';
import { fallbackRender } from '@/utils/fallbackRender';
import { useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import { WasteStreamStatusTag, WasteStreamStatusTagProps } from '@/features/wastestreams/components/WasteStreamStatusTag';
import { Drawer } from '@/components/ui/drawer/Drawer';
import { WasteStreamFilterForm } from '@/features/wastestreams/components/WasteStreamFilterForm';

type Column = {
  key: keyof WasteStreamListView;
  label: string;
  accessor: (value: WasteStreamListView) => React.ReactNode;
  title: (value: WasteStreamListView) => string | undefined;
  width: string;
};

export const WasteStreamManagement = () => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const { read, form, deletion } = useWasteStreamCrud();

  const columns: Column[] = [
    { key: 'wasteStreamNumber', label: 'Afvalstroomnummer', accessor: (item) => item.wasteStreamNumber, title: (item) => item.wasteStreamNumber, width: '14%' },
    { key: 'wasteName', label: 'Gebruikelijke benaming', accessor: (item) => item.wasteName, title: (item) => item.wasteName, width: '14%' },
    { key: 'consignorPartyName', label: 'Afzender', accessor: (item) => item.consignorPartyName, title: (item) => item.consignorPartyName, width: '14%' },
    { key: 'pickupLocation', label: 'Herkomstlocatie', accessor: (item) => item.pickupLocation, title: (item) => item.pickupLocation, width: '25%' },
    { key: 'deliveryLocation', label: 'Bestemmingslocatie', accessor: (item) => item.deliveryLocation, title: (item) => item.deliveryLocation, width: '25%' },
    { key: 'status', label: 'Status', accessor: (item) => <WasteStreamStatusTag status={item.status as WasteStreamStatusTagProps['status']} />, title: (item) => item.status, width: '8%' },
  ];

  return (
    <>
      <ContentContainer title={"Afvalstroomnummers"}>
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar setQuery={read.setQuery} setIsFilterOpen={read.filter.setIsFilterOpen}>
            <Button
              variant={'primary'}
              icon={Plus}
              label={'Voeg toe'}
              onClick={form.openForCreate}
            />
          </ContentTitleBar>
          <ErrorBoundary
            fallbackRender={fallbackRender}
            onReset={read.errorHandling.reset}
          >
            <ErrorThrowingComponent error={read.errorHandling.error} />
            {read.isFetching ? (
              <div className="flex justify-center items-center h-24 w-full">
                <ClipLoader
                  size={20}
                  color={'text-color-text-invert-primary'}
                  aria-label="Laad spinner"
                />
              </div>
            ) : read.items.length === 0 ? (
              <EmptyState
                icon={BxRecycle}
                text="Geen afvalstromen gevonden"
                onClick={form.openForCreate}
              />
            ) : (
              <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary overflow-y-auto">
                <table className="w-full table-fixed border-collapse">
                  <colgroup>
                    {columns.map((col) => (
                      <col key={String(col.key)} style={{ width: col.width }} />
                    ))}
                    <col style={{ width: '63px' }} />
                  </colgroup>
                  <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
                    <tr className="text-subtitle-1">
                      {columns.map((col) => (
                        <th className={'px-4 py-3 text-left'} key={String(col.key)}>
                          {col.label}
                        </th>
                      ))}
                      <th className="px-4 py-3"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {read.items
                      .slice((page - 1) * rowsPerPage, page * rowsPerPage)
                      .map((item, index) => (
                        <tr key={index} className="text-body-2 border-b border-solid border-color-border-primary">
                          {columns.map((col) => (
                            <td className="p-4 truncate" key={String(col.key)} title={col.title(item)}>
                              {col.accessor(item)}
                            </td>
                          ))}
                          <td className="p-4 text-center">
                            {item.status !== 'INACTIVE' && item.status !== 'EXPIRED' &&
                              <ActionMenu<WasteStreamListView>
                                onEdit={item.status === 'DRAFT' ? form.openForEdit : undefined}
                                onDelete={(wasteStream) => deletion.initiate(wasteStream)}
                                item={item}
                              />
                            }
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
        </div>
      </ContentContainer>
      {/*
        Dialog to confirm deletion of waste stream number
      */}
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        onDelete={() =>
          deletion.item &&
          deletion.confirm(deletion.item.wasteStreamNumber)
        }
        title={"Afvalstroomnummer verwijderen"}
        description={`Weet u zeker dat u afvalstroomnummer met code ${deletion.item?.wasteStreamNumber} wilt verwijderen?`}
      />
      <WasteStreamForm
        isOpen={form.isOpen}
        setIsOpen={form.close}
        wasteStreamNumber={form.item?.wasteStreamNumber}
      />
      <Drawer
        title={'Afvalstroomnummer filter'}
        isOpen={read.filter.isFilterOpen}
        setIsOpen={read.filter.setIsFilterOpen}
      >
        <WasteStreamFilterForm
          onSubmit={read.filter.applyFilterFormValues}
          closeDialog={() => read.filter.setIsFilterOpen(false)}
          currentValues={read.filter.currentFormValues}
        />
      </Drawer>
    </>
  );
};

export default WasteStreamManagement;
