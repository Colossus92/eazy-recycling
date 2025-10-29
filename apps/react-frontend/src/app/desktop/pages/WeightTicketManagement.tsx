import { WeightTicketListView } from '@/api/client/models';
import Plus from '@/assets/icons/Plus.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Button } from '@/components/ui/button/Button';
import { Drawer } from '@/components/ui/drawer/Drawer';
import { ActionMenu } from '@/features/crud/ActionMenu';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { WeightTicketCancellationForm } from '@/features/weighttickets/components/WeightTicketCancellationForm';
import { WeightTicketFilterForm } from '@/features/weighttickets/components/WeightTicketFilterForm';
import { WeightTicketForm } from '@/features/weighttickets/components/weightticketform/WeightTicketForm';
import { WeightTicketStatusTag, WeightTicketStatusTagProps } from '@/features/weighttickets/components/WeightTicketStatusTag';
import { useWeightTicketCrud } from '@/features/weighttickets/hooks/useWeightTicketCrud';
import { fallbackRender } from '@/utils/fallbackRender';
import { useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';

type Column = {
  key: keyof WeightTicketListView;
  label: string;
  accessor: (value: WeightTicketListView) => React.ReactNode;
  title: (value: WeightTicketListView) => string | undefined;
  width: string;
};

export const WeightTicketManagement = () => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const { read, form, deletion } = useWeightTicketCrud();

  const columns: Column[] = [
    { key: 'id', label: 'Nummer', accessor: (item) => item.id, title: (item) => String(item.id), width: '14%' },
    { key: 'consignorPartyName', label: 'Afzender', accessor: (item) => item.consignorPartyName, title: (item) => item.consignorPartyName, width: '14%' },
    { key: 'note', label: 'Opmerking', accessor: (item) => item.note, title: (item) => item.note, width: '64%' },
    { key: 'status', label: 'Status', accessor: (item) => <WeightTicketStatusTag status={item.status as WeightTicketStatusTagProps['status']} />, title: (item) => item.status, width: '8%' },
  ];

  return (
    <>
      <ContentContainer title={"Weegbonnen"}>
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
                icon={Scale}
                text="Geen weegbonnen gevonden"
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
                        <tr
                          key={index}
                          className="text-body-2 border-b border-solid border-color-border-primary cursor-pointer hover:bg-color-surface-secondary"
                          onDoubleClick={() => form.openForEdit(item)}
                        >
                          {columns.map((col) => (
                            <td className="p-4 truncate" key={String(col.key)} title={col.title(item)}>
                              {col.accessor(item)}
                            </td>
                          ))}
                          <td className="p-4 text-center">
                            {item.status !== 'CANCELLED' &&
                              <ActionMenu<WeightTicketListView>
                                onEdit={item.status === 'DRAFT' ? form.openForEdit : undefined}
                                onDelete={(weightTicket) => deletion.initiate(weightTicket.id)}
                                deleteText="Annuleren"
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
      <WeightTicketCancellationForm
        isOpen={Boolean(deletion.item)}
        setIsOpen={deletion.cancel}
        weightTicketId={deletion.item}
        onCancel={deletion.confirm}
      />
      <WeightTicketForm
        isOpen={form.isOpen}
        setIsOpen={form.close}
        weightTicketNumber={form.item?.id}
        status={form.item?.status}
        onDelete={deletion.initiate}
      />
      <Drawer
        title={'Weegbon filter'}
        isOpen={read.filter.isFilterOpen}
        setIsOpen={read.filter.setIsFilterOpen}
      >
        <WeightTicketFilterForm
          onSubmit={read.filter.applyFilterFormValues}
          closeDialog={() => read.filter.setIsFilterOpen(false)}
          currentValues={read.filter.currentFormValues}
        />
      </Drawer>
    </>
  );
};

export default WeightTicketManagement;
