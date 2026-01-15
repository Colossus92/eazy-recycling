import { ReactNode, useState } from 'react';
import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
  type SortingState,
} from '@tanstack/react-table';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow.tsx';
import SortIcon from '@/assets/icons/Sort.svg?react';

export type SortDirection = 'asc' | 'desc';

export interface SortConfig {
  sortBy: string | null;
  sortDirection: SortDirection;
}

export interface ColumnConfig<T> {
  id: string;
  header: string;
  accessorKey?: keyof T;
  cell?: (item: T) => ReactNode;
  enableSorting?: boolean;
}

export interface PaginationConfig {
  page: number;
  setPage: (page: number) => void;
  rowsPerPage: number;
  setRowsPerPage: (rowsPerPage: number) => void;
  totalElements: number;
  totalPages: number;
}

interface SortableDataTableProps<T> {
  data: T[];
  columns: ColumnConfig<T>[];
  pagination?: PaginationConfig;
  sorting?: SortConfig;
  onSortChange?: (sortConfig: SortConfig) => void;
  onRowDoubleClick?: (item: T) => void;
  renderActions?: (item: T) => ReactNode;
  isLoading?: boolean;
  emptyMessage?: string;
}

export const SortableDataTable = <T,>({
  data,
  columns,
  pagination,
  sorting,
  onSortChange,
  onRowDoubleClick,
  renderActions,
  isLoading = false,
  emptyMessage = 'Geen resultaten gevonden',
}: SortableDataTableProps<T>) => {
  const [internalSorting, setInternalSorting] = useState<SortingState>([]);

  const handleSortClick = (columnId: string) => {
    if (!onSortChange) return;

    let newDirection: SortDirection = 'asc';
    if (sorting?.sortBy === columnId) {
      newDirection = sorting.sortDirection === 'asc' ? 'desc' : 'asc';
    }

    onSortChange({
      sortBy: columnId,
      sortDirection: newDirection,
    });
  };

  const tableColumns: ColumnDef<T>[] = columns.map((col) => ({
    id: col.id,
    accessorKey: col.accessorKey as string,
    header: () => {
      const isSortable = col.enableSorting !== false && onSortChange;
      const isActive = sorting?.sortBy === col.id;

      return (
        <div
          className={`flex items-center gap-1 ${isSortable ? 'cursor-pointer select-none' : ''}`}
          onClick={isSortable ? () => handleSortClick(col.id) : undefined}
        >
          <span>{col.header}</span>
          {isSortable && (
            <SortIcon
              className={`w-4 h-4 transition-transform ${
                isActive
                  ? sorting?.sortDirection === 'desc'
                    ? 'rotate-180 text-color-brand-primary'
                    : 'text-color-brand-primary'
                  : 'text-color-text-tertiary'
              }`}
            />
          )}
        </div>
      );
    },
    cell: ({ row }) => {
      if (col.cell) {
        return col.cell(row.original);
      }
      const value = col.accessorKey ? row.original[col.accessorKey] : '';
      return value !== null && value !== undefined ? String(value) : '';
    },
  }));

  if (renderActions) {
    tableColumns.push({
      id: 'actions',
      header: () => null,
      cell: ({ row }) => renderActions(row.original),
    });
  }

  const table = useReactTable({
    data,
    columns: tableColumns,
    getCoreRowModel: getCoreRowModel(),
    onSortingChange: setInternalSorting,
    state: {
      sorting: internalSorting,
    },
    manualSorting: true,
    manualPagination: true,
  });

  return (
    <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary h-full overflow-y-auto">
      <table className="w-full table-fixed border-collapse">
        <colgroup>
          {columns.map((col) => (
            <col key={col.id} className={'w-[calc((100%-64px)/3)]'} />
          ))}
          {renderActions && <col className="w-[64px]" />}
        </colgroup>
        <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id} className="text-subtitle-1">
              {headerGroup.headers.map((header) => (
                <th
                  className="px-4 py-3 text-left truncate"
                  key={header.id}
                >
                  {header.isPlaceholder
                    ? null
                    : flexRender(
                        header.column.columnDef.header,
                        header.getContext()
                      )}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {isLoading ? (
            <tr>
              <td
                colSpan={columns.length + (renderActions ? 1 : 0)}
                className="p-4 text-center text-color-text-secondary"
              >
                Laden...
              </td>
            </tr>
          ) : table.getRowModel().rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length + (renderActions ? 1 : 0)}
                className="p-4 text-center text-color-text-secondary"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
                onDoubleClick={
                  onRowDoubleClick
                    ? () => onRowDoubleClick(row.original)
                    : undefined
                }
              >
                {row.getVisibleCells().map((cell) => (
                  <td className="p-4" key={cell.id}>
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
        {pagination && (
          <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
            <tr className="text-body-2 bg-color-surface-primary">
              <td
                colSpan={columns.length + (renderActions ? 1 : 0)}
                className="p-4"
              >
                <PaginationRow
                  page={pagination.page}
                  setPage={pagination.setPage}
                  rowsPerPage={pagination.rowsPerPage}
                  setRowsPerPage={pagination.setRowsPerPage}
                  numberOfResults={pagination.totalElements}
                  totalPages={pagination.totalPages}
                />
              </td>
            </tr>
          </tfoot>
        )}
      </table>
    </div>
  );
};
