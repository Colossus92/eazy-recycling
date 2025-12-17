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
import { useInvoiceCrud } from '../hooks/useInvoiceCrud';
import { fallbackRender } from '@/utils/fallbackRender';
import { useState } from 'react';
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

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(value);
};

export const InvoicesTab = () => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const { read, form, deletion } = useInvoiceCrud();

  const columns: Column[] = [
    {
      key: 'invoiceNumber',
      label: 'Factuurnummer',
      accessor: (item) => item.invoiceNumber ? `#${item.invoiceNumber}` : '-',
      title: (item) => item.invoiceNumber ? `#${item.invoiceNumber}` : undefined,
      width: '20%',
    },
    {
      key: 'customerName',
      label: 'Klant',
      accessor: (item) => item.customerName,
      title: (item) => item.customerName,
      width: '30%',
    },
    {
      key: 'totalInclVat',
      label: 'Totaal',
      accessor: (item) => formatCurrency(item.totalInclVat),
      title: (item) => formatCurrency(item.totalInclVat),
      width: '20%',
    },
    {
      key: 'status',
      label: 'Status',
      accessor: (item) => <InvoiceStatusTag status={item.status} />,
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
                      className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
                      onDoubleClick={() => form.openForEdit(item)}
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
                      <td className="p-4 text-center">
                        {item.status === 'DRAFT' && (
                          <ActionMenu<InvoiceView>
                            onEdit={form.openForEdit}
                            onDelete={(invoice) => deletion.initiate(invoice.id)}
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
    </>
  );
};
