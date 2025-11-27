import { SyncConflictDto } from '@/api/client';
import CheckCircle from '@/assets/icons/CheckCircle.svg?react';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { useState } from 'react';
import { ConflictTypeTag } from './ConflictTypeTag';
import { SyncStatusTag } from './SyncStatusTag';

interface SyncConflictsTableProps {
  conflicts: SyncConflictDto[];
  isLoading: boolean;
}

type Column = {
  key: string;
  label: string;
  accessor: (item: SyncConflictDto) => React.ReactNode;
  title?: (item: SyncConflictDto) => string | undefined;
  width: string;
};

export const SyncConflictsTable = ({
  conflicts,
  isLoading,
}: SyncConflictsTableProps) => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const columns: Column[] = [
    {
      key: 'status',
      label: 'Status',
      accessor: (item) => <SyncStatusTag status={item.syncStatus} />,
      width: '12%',
    },
    {
      key: 'conflictType',
      label: 'Probleem',
      accessor: (item) =>
        item.conflictDetails ? (
          <ConflictTypeTag
            type={item.conflictDetails.conflictType}
            description={item.conflictDetails.errorMessage}
          />
        ) : (
          <span className="text-color-text-secondary">-</span>
        ),
      width: '20%',
    },
    {
      key: 'exactInfo',
      label: 'Exact Online',
      accessor: (item) => (
        <div className="flex flex-col gap-1">
          <span className="text-body-2 font-medium">
            {item.conflictDetails?.exactName || 'Onbekend'}
          </span>
          <span className="text-caption text-color-text-secondary">
            Code: {item.externalId || '-'}
          </span>
          <span className="text-caption text-color-text-secondary">
            Adres: {item.conflictDetails?.exactAddress || '-'}
          </span>
          {item.exactGuid && (
            <span className="text-caption text-color-text-tertiary truncate">
              GUID: {item.exactGuid}
            </span>
          )}
        </div>
      ),
      width: '25%',
    },
    {
      key: 'matchedCompany',
      label: 'Gekoppeld Bedrijf',
      accessor: (item) => (
        <div className="flex flex-col gap-1">
          <span className="text-body-2 font-medium">
            {item.conflictDetails?.matchedCompanyName || '-'}
          </span>
          <span className="text-caption text-color-text-secondary">
            Adres: {item.conflictDetails?.matchedCompanyAddress || '-'}
          </span>
          {item.companyId && (
            <span className="text-caption text-color-text-tertiary truncate">
              ID: {item.companyId}
            </span>
          )}
        </div>
      ),
      width: '25%',
    },
    {
      key: 'conflictField',
      label: 'Conflicterend Veld',
      accessor: (item) => {
        if (!item.conflictDetails?.field) return '-';
        return (
          <div className="flex flex-col gap-1">
            <span className="text-body-2 font-medium capitalize">
              {item.conflictDetails.field.replace(/_/g, ' ')}
            </span>
            <span className="text-caption text-color-text-secondary">
              {item.conflictDetails.value || '-'}
            </span>
          </div>
        );
      },
      width: '18%',
    },
  ];

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-24 w-full">
        <span className="text-body-2 text-color-text-secondary">
          Conflicten laden...
        </span>
      </div>
    );
  }

  if (conflicts.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-12 w-full">
        <div className="w-16 h-16 rounded-full bg-color-success-light flex items-center justify-center">
          <CheckCircle className="w-8 h-8 text-color-success" />
        </div>
        <div className="text-center">
          <p className="text-subtitle-1 text-color-text-primary">
            Geen conflicten gevonden
          </p>
          <p className="text-body-2 text-color-text-secondary">
            Alle bedrijven zijn correct gesynchroniseerd met Exact Online
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary overflow-y-auto">
      <table className="w-full table-fixed border-collapse">
        <colgroup>
          {columns.map((col) => (
            <col key={col.key} style={{ width: col.width }} />
          ))}
        </colgroup>
        <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
          <tr className="text-subtitle-1">
            {columns.map((col) => (
              <th className="px-4 py-3 text-left truncate" key={col.key}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {conflicts
            .slice((page - 1) * rowsPerPage, page * rowsPerPage)
            .map((item) => (
              <tr
                key={item.id}
                className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
              >
                {columns.map((col) => (
                  <td className="p-4" key={col.key} title={col.title?.(item)}>
                    {col.accessor(item)}
                  </td>
                ))}
              </tr>
            ))}
        </tbody>
        <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
          <tr className="text-body-2 bg-color-surface-primary">
            <td colSpan={columns.length} className="p-4">
              <PaginationRow
                page={page}
                setPage={setPage}
                rowsPerPage={rowsPerPage}
                setRowsPerPage={setRowsPerPage}
                numberOfResults={conflicts.length}
              />
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};
