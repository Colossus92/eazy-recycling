import { WeightTicketListView } from '@/api/client/models';
import Plus from '@/assets/icons/Plus.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { ContentContainer } from '@/components/layouts/ContentContainer';
import { Button } from '@/components/ui/button/Button';
import { Drawer } from '@/components/ui/drawer/Drawer';
import { ErrorDialog } from '@/components/ui/dialog/ErrorDialog';
import { ActionMenu } from '@/features/crud/ActionMenu';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { WeightTicketCancellationForm } from '@/features/weighttickets/components/WeightTicketCancellationForm';
import { WeightTicketSplitForm } from '@/features/weighttickets/components/WeightTicketSplitForm';
import { WeightTicketFilterForm } from '@/features/weighttickets/components/WeightTicketFilterForm';
import { WeightTicketForm } from '@/features/weighttickets/components/weightticketform/WeightTicketForm';
import { WeightTicketDualForm } from '@/features/weighttickets/components/WeightTicketDualForm';
import { WeightTicketStatusTag, WeightTicketStatusTagProps } from '@/features/weighttickets/components/WeightTicketStatusTag';
import { WeightTicketDetailsDrawer } from '@/features/weighttickets/components/drawer/WeightTicketDetailsDrawer';
import { useWeightTicketCrud } from '@/features/weighttickets/hooks/useWeightTicketCrud';
import { useRowClickHandler } from '@/hooks/useRowClickHandler';
import { fallbackRender } from '@/utils/fallbackRender';
import { useState, useEffect, useCallback } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import { toastService } from '@/components/ui/toast/toastService';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { format } from 'date-fns';

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
  const [isDualFormOpen, setIsDualFormOpen] = useState(false);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedWeightTicketId, setSelectedWeightTicketId] = useState<number | null>(null);
  const { read, form, deletion, split, copy, createTransport, createInvoice, error } = useWeightTicketCrud();

  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Handle URL params for opening weight ticket drawer
  useEffect(() => {
    const weightTicketDrawerId = searchParams.get('weightTicketDrawerId');
    
    if (weightTicketDrawerId) {
      const weightTicketNumber = parseInt(weightTicketDrawerId, 10);
      if (!isNaN(weightTicketNumber)) {
        setSelectedWeightTicketId(weightTicketNumber);
        setIsDrawerOpen(true);
      }
      
      // Clear the URL params after opening
      setSearchParams({});
    }
  }, [searchParams, setSearchParams]);

  const handleSingleClick = useCallback((item: WeightTicketListView) => {
    setSelectedWeightTicketId(item.id);
    setIsDrawerOpen(true);
  }, []);

  const handleDoubleClick = useCallback((item: WeightTicketListView) => {
    setIsDrawerOpen(false);
    form.openForEdit(item);
  }, [form]);

  const { handleClick, handleDoubleClick: handleRowDoubleClick } = useRowClickHandler<WeightTicketListView>({
    onSingleClick: handleSingleClick,
    onDoubleClick: handleDoubleClick,
  });

  const handleDrawerEdit = () => {
    setIsDrawerOpen(false);
    const item = read.items.find((i) => i.id === selectedWeightTicketId);
    if (item) {
      form.openForEdit(item);
    }
  };

  const handleDrawerDelete = () => {
    if (selectedWeightTicketId) {
      setIsDrawerOpen(false);
      deletion.initiate(selectedWeightTicketId);
    }
  };
  
  // Handle URL params for opening weight ticket form from transport details
  useEffect(() => {
    const weightTicketId = searchParams.get('weightTicketId');
    
    if (weightTicketId) {
      const weightTicketNumber = parseInt(weightTicketId, 10);
      if (!isNaN(weightTicketNumber)) {
        // Find the weight ticket in the list
        const weightTicket = read.items.find(item => item.id === weightTicketNumber);
        if (weightTicket) {
          form.openForEdit(weightTicket);
        } else {
          // If not in list yet, create a minimal object to trigger the form
          // The form will fetch the full details using the ID
          form.openForEdit({ 
            id: weightTicketNumber, 
            consignorPartyName: '', 
            status: 'DRAFT' 
          } as WeightTicketListView);
        }
      }
      
      // Clear the URL params after opening
      setSearchParams({});
    }
  }, [searchParams, setSearchParams, read.items, form]);

  // Handle opening the dual form after split or copy
  useEffect(() => {
    if (split.response || copy.response) {
      form.close(); // Close the single form
      setIsDualFormOpen(true); // Open the dual form
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [split.response, copy.response]);

  // Handle transport creation response and show toast
  useEffect(() => {
    if (createTransport.response) {
      const pickupDate = new Date(createTransport.response.pickupDateTime);
      const dateParam = format(pickupDate, 'yyyy-MM-dd');
      
      const toastContent = (
        <div className="flex flex-col gap-1">
          <span>Transport aangemaakt: {createTransport.response.displayNumber}</span>
          <button
            onClick={() => {
              navigate(`/?transportId=${createTransport.response!.transportId}&date=${dateParam}`);
            }}
            className="text-left underline hover:no-underline text-color-brand-primary font-semibold"
          >
            Bekijk in planning →
          </button>
        </div>
      );
      
      toastService.success(toastContent);
      createTransport.clearResponse();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createTransport.response]);

  // Handle invoice creation response and show toast
  useEffect(() => {
    if (createInvoice.response) {
      const toastContent = (
        <div className="flex flex-col gap-1">
          <span>Factuur aangemaakt van weegbon</span>
          <button
            onClick={(e) => {
              e.stopPropagation(); // Prevent toast click handler from interfering
              navigate(`/financials?invoiceId=${createInvoice.response!.invoiceId}`);
            }}
            className="text-left underline hover:no-underline text-color-brand-primary font-semibold"
          >
            Bekijk factuur →
          </button>
        </div>
      );
      
      toastService.success(toastContent);
      createInvoice.clearResponse();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createInvoice.response]);

  const columns: Column[] = [
    { key: 'id', label: 'Nummer', accessor: (item) => item.id, title: (item) => String(item.id), width: '10%' },
    { key: 'consignorPartyName', label: 'Afzender', accessor: (item) => item.consignorPartyName, title: (item) => item.consignorPartyName, width: '14%' },
    { key: 'totalWeight', label: 'Totaal gewicht', accessor: (item) => item.totalWeight ? `${item.totalWeight.toFixed(2)} kg` : '-', title: (item) => item.totalWeight ? `${item.totalWeight.toFixed(2)} kg` : '-', width: '12%' },
    { key: 'weighingDate', label: 'Weegdatum', accessor: (item) => item.weighingDate ? format(new Date(item.weighingDate), 'dd-MM-yyyy') : '-', title: (item) => item.weighingDate ? format(new Date(item.weighingDate), 'dd-MM-yyyy HH:mm') : '-', width: '12%' },
    { key: 'note', label: 'Opmerking', accessor: (item) => item.note, title: (item) => item.note, width: '44%' },
    { key: 'status', label: 'Status', accessor: (item) => <WeightTicketStatusTag status={item.status as WeightTicketStatusTagProps['status']} />, title: (item) => item.status, width: '8%' },
  ];

  const setQueryAndResetPage = (query: string) => {
    read.setQuery(query);
    setPage(1);
  };

  return (
    <>
      <ContentContainer title={"Weegbonnen"}>
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar setQuery={setQueryAndResetPage} setIsFilterOpen={read.filter.setIsFilterOpen}>
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
                        <th className={`px-4 py-3 truncate ${col.key === 'totalWeight' ? 'text-right' : 'text-left'}`} key={String(col.key)}>
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
                          className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary cursor-pointer"
                          onClick={() => handleClick(item)}
                          onDoubleClick={() => handleRowDoubleClick(item)}
                        >
                          {columns.map((col) => (
                            <td className={`p-4 ${col.key !== 'status' ? 'truncate' : ''} ${col.key === 'totalWeight' ? 'text-right' : ''}`} key={String(col.key)} title={col.title(item)}>
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
      <WeightTicketSplitForm
        isOpen={Boolean(split.item)}
        setIsOpen={split.cancel}
        weightTicketId={split.item}
        onSplit={split.confirm}
      />
      {!split.response && !copy.response ? (
        <WeightTicketForm
          isOpen={form.isOpen}
          setIsOpen={form.close}
          weightTicketNumber={form.item?.id}
          status={form.item?.status}
          onDelete={deletion.initiate}
          onComplete={form.complete}
          onSplit={split.initiate}
          onCopy={copy.confirm}
          onCreateTransport={createTransport.confirm}
          onCreateInvoice={createInvoice.confirm}
        />
      ) : split.response ? (
        <WeightTicketDualForm
          isOpen={isDualFormOpen}
          setIsOpen={setIsDualFormOpen}
          originalWeightTicketId={split.response.originalWeightTicketId}
          newWeightTicketId={split.response.newWeightTicketId}
          onDelete={deletion.initiate}
          onSplit={split.initiate}
          onClose={() => {
            split.clearResponse();
            setIsDualFormOpen(false);
          }}
          onComplete={form.complete}
        />
      ) : (
        <WeightTicketDualForm
          isOpen={isDualFormOpen}
          setIsOpen={setIsDualFormOpen}
          originalWeightTicketId={copy.response!.originalWeightTicketId}
          newWeightTicketId={copy.response!.newWeightTicketId}
          onDelete={deletion.initiate}
          onSplit={split.initiate}
          onClose={() => {
            copy.clearResponse();
            setIsDualFormOpen(false);
          }}
          onComplete={form.complete}
        />
      )}
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
      <ErrorDialog
        isOpen={!!error.message}
        setIsOpen={(value) => {
          if (!value) {
            error.clear();
          }
        }}
        errorMessage={error.message || ''}
      />
      <WeightTicketDetailsDrawer
        isDrawerOpen={isDrawerOpen}
        setIsDrawerOpen={setIsDrawerOpen}
        weightTicketId={selectedWeightTicketId}
        onEdit={handleDrawerEdit}
        onDelete={handleDrawerDelete}
      />
    </>
  );
};

export default WeightTicketManagement;
