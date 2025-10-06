import React, { ReactNode, useState } from 'react';
import { ActionMenu, AdditionalAction } from './ActionMenu.tsx';
import { CrudDataProps } from './CrudPage.tsx';
import { PaginationRow } from './pagination/PaginationRow.tsx';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CaretDown from '@/assets/icons/CaretDown.svg?react';

export type Column<T> = {
  key: keyof T;
  label: string;
  accessor?: (value: T) => string | ReactNode;
};

export interface ExpandableConfig<T, S = T> {
  getSubItems: (item: T) => S[] | undefined;
  renderSubItem: (subItem: S, columns: Column<T>[]) => ReactNode;
  hasSubItems: (item: T) => boolean;
}

interface ContentTableProps<T, S = T> {
  data: CrudDataProps<T>;
  onEdit: (item: T) => void;
  onDelete: (value: T) => void;
  additionalActions?: AdditionalAction<T>[];
  expandableConfig?: ExpandableConfig<T, S>;
  isLoading?: boolean;
}

export const ContentTable = <T, S = T>({
  data,
  onEdit,
  onDelete,
  additionalActions,
  expandableConfig,
}: ContentTableProps<T, S>) => {
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());

  return (
    <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary h-full overflow-y-auto">
      <table className="w-full table-fixed border-collapse">
        <colgroup>
          {data.columns.map((col) => (
            <col key={String(col.key)} className={'w-[calc((100%-64px)/3)]'} />
          ))}
          <col className="w-[64px]" />
        </colgroup>
        <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
          <tr className="text-subtitle-1">
            {data.columns.map((col) => (
              <th className={'px-4 py-3 text-left'} key={String(col.key)}>
                {col.label}
              </th>
            ))}
            <th className="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody>
          {data.items
            .slice((page - 1) * rowsPerPage, page * rowsPerPage)
            .map((item, index) => {
              const actualIndex = (page - 1) * rowsPerPage + index;
              const isExpanded = expandedRows.has(actualIndex);
              const hasSubItems = expandableConfig?.hasSubItems(item) || false;
              const subItems = expandableConfig?.getSubItems(item) || [];

              return (
                <React.Fragment key={index}>
                  {/* Main row */}
                  <tr className="text-body-2 border-b border-solid border-color-border-primary">
                    {data.columns.map((col, colIndex) => (
                      <td className="p-4" key={String(col.key)}>
                        <div className="flex items-center gap-2">
                          {/* Expand/collapse button only in first column */}
                          {colIndex === 0 && hasSubItems && (
                            <button
                              onClick={() => {
                                const newExpanded = new Set(expandedRows);
                                if (isExpanded) {
                                  newExpanded.delete(actualIndex);
                                } else {
                                  newExpanded.add(actualIndex);
                                }
                                setExpandedRows(newExpanded);
                              }}
                              className="p-1 hover:bg-color-surface-secondary rounded transition-colors"
                            >
                              {isExpanded ? (
                                <CaretDown className="w-4 h-4 text-color-text-secondary" />
                              ) : (
                                <CaretRight className="w-4 h-4 text-color-text-secondary" />
                              )}
                            </button>
                          )}
                          {/* Column content */}
                          <div
                            className={
                              colIndex === 0 && !hasSubItems ? 'ml-7' : ''
                            }
                          >
                            {col.accessor
                              ? col.accessor(item)
                              : item[col.key]
                                ? String(item[col.key])
                                : ''}
                          </div>
                        </div>
                      </td>
                    ))}
                    <td className="p-4 text-center">
                      <ActionMenu<T>
                        onEdit={onEdit}
                        onDelete={onDelete}
                        item={item}
                        additionalActions={additionalActions}
                      />
                    </td>
                  </tr>

                  {/* Sub-items rows */}
                  {isExpanded &&
                    hasSubItems &&
                    subItems.map((subItem, subIndex) => (
                      <React.Fragment key={`sub-${actualIndex}-${subIndex}`}>
                        {expandableConfig?.renderSubItem(subItem, data.columns)}
                      </React.Fragment>
                    ))}
                </React.Fragment>
              );
            })}
        </tbody>
        <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
          <tr className="text-body-2 bg-color-surface-primary">
            <td colSpan={data.columns.length + 1} className="p-4">
              <PaginationRow
                page={page}
                setPage={setPage}
                rowsPerPage={rowsPerPage}
                setRowsPerPage={setRowsPerPage}
                numberOfResults={data.items.length}
              />
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};
