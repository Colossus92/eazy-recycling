import { LmaImportErrorDto } from '@/api/client';
import CheckCircle from '@/assets/icons/CheckCircle.svg?react';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow';
import { useState } from 'react';
import { LmaErrorCodeTag } from './LmaErrorCodeTag';

interface LmaImportErrorsTableProps {
  errors: LmaImportErrorDto[];
  isLoading: boolean;
}

type Column = {
  key: string;
  label: string;
  accessor: (item: LmaImportErrorDto) => React.ReactNode;
  width: string;
};

export const LmaImportErrorsTable = ({
  errors,
  isLoading,
}: LmaImportErrorsTableProps) => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  const columns: Column[] = [
    {
      key: 'rowNumber',
      label: 'Rij',
      accessor: (item) => (
        <span className="text-body-2 font-medium">{item.rowNumber}</span>
      ),
      width: '8%',
    },
    {
      key: 'wasteStreamNumber',
      label: 'Afvalstroomnummer',
      accessor: (item) => (
        <span className="text-body-2 font-mono">
          {item.wasteStreamNumber || '-'}
        </span>
      ),
      width: '18%',
    },
    {
      key: 'errorCode',
      label: 'Fouttype',
      accessor: (item) => <LmaErrorCodeTag errorCode={item.errorCode} />,
      width: '18%',
    },
    {
      key: 'errorMessage',
      label: 'Foutmelding',
      accessor: (item) => (
        <span className="text-body-2 text-color-text-secondary">
          {item.errorMessage}
        </span>
      ),
      width: '36%',
    },
    {
      key: 'createdAt',
      label: 'Datum',
      accessor: (item) => (
        <span className="text-caption text-color-text-secondary">
          {new Date(item.createdAt).toLocaleString('nl-NL', {
            dateStyle: 'short',
            timeStyle: 'short',
          })}
        </span>
      ),
      width: '20%',
    },
  ];

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-24 w-full">
        <span className="text-body-2 text-color-text-secondary">
          Fouten laden...
        </span>
      </div>
    );
  }

  if (errors.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-12 w-full">
        <div className="w-16 h-16 rounded-full bg-color-success-light flex items-center justify-center">
          <CheckCircle className="w-8 h-8 text-color-success" />
        </div>
        <div className="text-center">
          <p className="text-subtitle-1 text-color-text-primary">
            Geen importfouten
          </p>
          <p className="text-body-2 text-color-text-secondary">
            Alle afvalstromen zijn succesvol geïmporteerd
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
          {errors
            .slice((page - 1) * rowsPerPage, page * rowsPerPage)
            .map((item) => (
              <tr
                key={item.id}
                className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
              >
                {columns.map((col) => (
                  <td className="p-4" key={col.key}>
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
                numberOfResults={errors.length}
              />
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};
