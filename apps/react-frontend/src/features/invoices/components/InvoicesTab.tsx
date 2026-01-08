import { InvoiceView } from '@/api/client/models';
import Plus from '@/assets/icons/Plus.svg?react';
import CarbonDocumentAttachment from '@/assets/icons/CarbonDocumentAttachment.svg?react';
import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { Button } from '@/components/ui/button/Button';
import { ActionMenu } from '@/features/crud/ActionMenu';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { InvoiceForm } from './invoiceform/InvoiceForm';
import { InvoiceStatusTag } from './InvoiceStatusTag';
import { InvoiceDetailsDrawer } from './drawer/InvoiceDetailsDrawer';
import { useInvoiceCrud } from '../hooks/useInvoiceCrud';
import { fallbackRender } from '@/utils/fallbackRender';
import { useEffect, useRef, useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog';

type Column = {
  key: keyof InvoiceView | 'actions';
  label: string;
  accessor: (value: InvoiceView) => React.ReactNode;
  title: (value: InvoiceView) => string | undefined;
  width: string;
};

const formatCurrency = (item: InvoiceView) => {
  // For credit notes, show negative values (except 0,00)
  const displayValue =
    item.documentType === 'CREDIT_NOTE' && item.totalExclVat !== 0
      ? -item.totalExclVat
      : item.totalExclVat;
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(displayValue);
};

const getInvoiceTypeLabel = (item: InvoiceView): string => {
  if (item.documentType === 'CREDIT_NOTE') {
    return 'Credit';
  }
  return item.invoiceType === 'PURCHASE' ? 'Inkoop' : 'Verkoop';
};

interface InvoicesTabProps {
  invoiceIdToOpen?: string | null;
  invoiceDrawerIdToOpen?: string | null;
  onInvoiceOpened?: () => void;
  onInvoiceDrawerOpened?: () => void;
}

export const InvoicesTab = ({
  invoiceIdToOpen,
  invoiceDrawerIdToOpen,
  onInvoiceOpened,
  onInvoiceDrawerOpened,
}: InvoicesTabProps) => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(
    null
  );
  const clickTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const { read, form, deletion } = useInvoiceCrud();

  // Handle opening invoice form from URL parameter
  useEffect(() => {
    if (invoiceIdToOpen && read.items.length > 0) {
      const invoice = read.items.find((item) => item.id === invoiceIdToOpen);
      if (invoice) {
        form.openForEdit(invoice);
        onInvoiceOpened?.();
      }
    }
  }, [invoiceIdToOpen, read.items, form, onInvoiceOpened]);

  // Handle opening invoice drawer from URL parameter
  useEffect(() => {
    if (invoiceDrawerIdToOpen && read.items.length > 0) {
      setSelectedInvoiceId(String(invoiceDrawerIdToOpen));
      setIsDrawerOpen(true);
      onInvoiceDrawerOpened?.();
    }
  }, [invoiceDrawerIdToOpen, read.items, onInvoiceDrawerOpened]);

  const handleRowClick = (item: InvoiceView) => {
    if (clickTimeoutRef.current) {
      clearTimeout(clickTimeoutRef.current);
      clickTimeoutRef.current = null;
    }
    clickTimeoutRef.current = setTimeout(() => {
      setSelectedInvoiceId(item.id);
      setIsDrawerOpen(true);
      clickTimeoutRef.current = null;
    }, 200);
  };

  const handleRowDoubleClick = (item: InvoiceView) => {
    if (clickTimeoutRef.current) {
      clearTimeout(clickTimeoutRef.current);
      clickTimeoutRef.current = null;
    }
    setIsDrawerOpen(false);
    form.openForEdit(item);
  };

  const handleDrawerEdit = () => {
    setIsDrawerOpen(false);
    const item = read.items.find((i) => i.id === selectedInvoiceId);
    if (item) {
      form.openForEdit(item);
    }
  };

  const handleDrawerDelete = () => {
    if (selectedInvoiceId) {
      setIsDrawerOpen(false);
      deletion.initiate(selectedInvoiceId);
    }
  };

  const columns: Column[] = [
    {
      key: 'invoiceNumber',
      label: 'Factuurnummer',
      accessor: (item) => (item.invoiceNumber ? `#${item.invoiceNumber}` : '-'),
      title: (item) =>
        item.invoiceNumber ? `#${item.invoiceNumber}` : undefined,
      width: '20%',
    },
    {
      key: 'customerName',
      label: 'Klant',
      accessor: (item) => item.customerName,
      title: (item) => item.customerName,
      width: '35%',
    },
    {
      key: 'invoiceType',
      label: 'Type',
      accessor: (item) => getInvoiceTypeLabel(item),
      title: (item) => getInvoiceTypeLabel(item),
      width: '10%',
    },
    {
      key: 'totalExclVat',
      label: 'Totaal (excl. BTW)',
      accessor: (item) => formatCurrency(item),
      title: (item) => formatCurrency(item),
      width: '20%',
    },
    {
      key: 'status',
      label: 'Status',
      accessor: (item) => (
        <InvoiceStatusTag status={item.status as 'DRAFT' | 'FINAL'} />
      ),
      title: (item) => item.status,
      width: '15%',
    },
  ];

  const setQueryAndResetPage = (query: string) => {
    read.setQuery(query);
    setPage(1);
  };

  return (
    <>
      <ContentTitleBar setQuery={setQueryAndResetPage}>
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
            icon={CarbonDocumentAttachment}
            text="Geen facturen gevonden"
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
                    <th
                      className={'px-4 py-3 text-left truncate'}
                      key={String(col.key)}
                    >
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
                      onClick={() => handleRowClick(item)}
                      onDoubleClick={() => handleRowDoubleClick(item)}
                    >
                      {columns.map((col) => (
                        <td
                          className={`p-4 ${col.key !== 'status' ? 'truncate' : ''}`}
                          key={String(col.key)}
                          title={col.title(item)}
                        >
                          {col.accessor(item)}
                        </td>
                      ))}
                      <td
                        className="p-4 text-center"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {item.status === 'DRAFT' && (
                          <ActionMenu<InvoiceView>
                            onEdit={form.openForEdit}
                            onDelete={(invoice) =>
                              deletion.initiate(invoice.id)
                            }
                            item={item}
                          />
                        )}
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
      <InvoiceForm
        isOpen={form.isOpen}
        setIsOpen={form.close}
        invoiceId={form.item?.id}
        onComplete={form.complete}
        onDelete={deletion.initiate}
      />
      <DeleteDialog
        isOpen={Boolean(deletion.item)}
        setIsOpen={() => deletion.cancel()}
        title="Factuur verwijderen"
        description="Weet u zeker dat u deze factuur wilt verwijderen?"
        onDelete={deletion.confirm}
      />
      <InvoiceDetailsDrawer
        isDrawerOpen={isDrawerOpen}
        setIsDrawerOpen={setIsDrawerOpen}
        invoiceId={selectedInvoiceId}
        onEdit={handleDrawerEdit}
        onDelete={handleDrawerDelete}
      />
    </>
  );
};
