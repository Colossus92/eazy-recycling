import { ErrorThrowingComponent } from '@/components/ErrorThrowingComponent';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { fallbackRender } from '@/utils/fallbackRender';
import { useState } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import { useLmaDeclarations } from '@/features/wastestreams/hooks/useLmaDeclarations';
import { LmaDeclarationView } from '@/api/client';
import { LMADeclarationStatusTag, LMADeclarationStatusTagProps } from './LMADeclarationStatusTag';
import { Tooltip } from '@/components/ui/tooltip/Tooltip';
import Warning from '@/assets/icons/Warning.svg?react';
import { Button } from '@/components/ui/button/Button';

type Column = {
  key: keyof LmaDeclarationView | 'actions';
  label: string;
  accessor: (value: LmaDeclarationView) => React.ReactNode;
  title: (value: LmaDeclarationView) => string | undefined;
  width: string;
};

export const LMATab = () => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const { items, setQuery, isFetching, totalElements, errorHandling, approveDeclaration, isApproving, approvingId } = useLmaDeclarations({
    page,
    pageSize: rowsPerPage,
  });

  const columns: Column[] = [
    {
      key: 'wasteStreamNumber',
      label: 'Nummer',
      accessor: (item) => item.wasteStreamNumber,
      title: (item) => item.wasteStreamNumber,
      width: '11%',
    },
    {
      key: 'pickupLocation',
      label: 'Herkomstlocatie',
      accessor: (item) => item.pickupLocation,
      title: (item) => item.pickupLocation,
      width: '17%',
    },
    {
      key: 'wasteName',
      label: 'Gebruikelijke benaming',
      accessor: (item) => item.wasteName,
      title: (item) => item.wasteName,
      width: '14%',
    },
    {
      key: 'totalWeight',
      label: 'Totaal gewicht (kg)',
      accessor: (item) => item.totalWeight.toFixed(2),
      title: (item) => item.totalWeight.toString(),
      width: '10%',
    },
    {
      key: 'period',
      label: 'Periode',
      accessor: (item) => item.period,
      title: (item) => item.period,
      width: '8%',
    },
    {
      key: 'status',
      label: 'Status',
      accessor: (item) => (
        <LMADeclarationStatusTag
          status={item.status as LMADeclarationStatusTagProps['status']}
        />
      ),
      title: (item) => item.status,
      width: '12%',
    },
    {
      key: 'errors',
      label: 'Fouten',
      accessor: (item) => {
        const errors = item.errors;
        if (!errors || errors.length === 0) {
          return <span className="text-color-text-tertiary">-</span>;
        }
        const tooltipContent = errors.join('\n');
        const displayText = errors.length === 1 
          ? errors[0] 
          : `${errors.length} fouten`;
        return (
          <Tooltip content={tooltipContent} position="left">
            <div className="flex items-center gap-1 text-color-error cursor-help">
              <Warning className="w-4 h-4 flex-shrink text-color-status-error-dark" />
              <span className="truncate">{displayText}</span>
            </div>
          </Tooltip>
        );
      },
      title: (item) => item.errors?.join('\n'),
      width: '14%',
    },
    {
      key: 'actions',
      label: 'Acties',
      accessor: (item) => {
        if (item.status !== 'WAITING_APPROVAL') {
          return <span className="text-color-text-tertiary">-</span>;
        }
        const isThisItemApproving = approvingId === item.id;
        return (
          <Button
            variant="secondary"
            size="small"
            label={isThisItemApproving ? 'Bezig...' : 'Goedkeuren'}
            onClick={(e) => {
              e.stopPropagation();
              approveDeclaration(item.id);
            }}
            disabled={isApproving}
          />
            
        );
      },
      title: () => undefined,
      width: '14%',
    },
  ];

  const setQueryAndResetPage = (query: string) => {
    setQuery(query);
    setPage(1);
  };

  return (
    <>
      <ContentTitleBar setQuery={setQueryAndResetPage} >
      </ContentTitleBar>
      <ErrorBoundary
        fallbackRender={fallbackRender}
        onReset={errorHandling.reset}
      >
        <ErrorThrowingComponent error={errorHandling.error} />
        {isFetching ? (
          <div className="flex justify-center items-center h-24 w-full">
            <ClipLoader
              size={20}
              color={'text-color-text-invert-primary'}
              aria-label="Laad spinner"
            />
          </div>
        ) : items.length === 0 ? (
          <EmptyState
            icon={BxRecycle}
            text="Geen LMA gegevens gevonden"
          />
        ) : (
          <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary overflow-y-auto">
            <table className="w-full table-fixed border-collapse">
              <colgroup>
                {columns.map((col) => (
                  <col key={String(col.key)} style={{ width: col.width }} />
                ))}
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
                </tr>
              </thead>
              <tbody>
                {items.map((item, index) => {
                  return (
                    <tr
                      key={index}
                      className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
                    >
                      {columns.map((col) => (
                        <td
                          className="p-4 truncate"
                          key={String(col.key)}
                          title={col.title(item)}
                        >
                          {col.accessor(item)}
                        </td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
              <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
                <tr className="text-body-2 bg-color-surface-primary">
                  <td colSpan={columns.length} className="p-4">
                    <PaginationRow
                      page={page}
                      setPage={setPage}
                      rowsPerPage={rowsPerPage}
                      setRowsPerPage={setRowsPerPage}
                      numberOfResults={totalElements}
                    />
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </ErrorBoundary>
    </>
  );
};
