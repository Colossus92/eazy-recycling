import { ReactNode, Fragment, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { ErrorBoundary } from 'react-error-boundary';
import { ClipLoader } from 'react-spinners';
import { useCompanyCrud } from '@/features/companies/useCompanyCrud.ts';
import { Company, CompanyBranch } from '@/api/services/companyService';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import { ContentTitleBar } from '@/features/crud/ContentTitleBar.tsx';
import { ContentContainer } from '@/components/layouts/ContentContainer.tsx';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { DeleteDialog } from '@/components/ui/dialog/DeleteDialog.tsx';
import { PaginationRow } from '@/features/crud/pagination/PaginationRow.tsx';
import { fallbackRender } from '@/utils/fallbackRender';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import Plus from '@/assets/icons/Plus.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CaretDown from '@/assets/icons/CaretDown.svg?react';
import SortIcon from '@/assets/icons/Sort.svg?react';
import { CompanyForm } from '@/features/companies/CompanyForm.tsx';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar.tsx';
import { ActionMenu, AdditionalAction } from '@/features/crud/ActionMenu';
import { useBranchManagement } from '@/features/companies/useBranchManagement.tsx';
import { Button } from '@/components/ui/button/Button.tsx';

interface ColumnConfig {
  id: string;
  label: string;
  sortable?: boolean;
  render: (item: Company) => ReactNode;
}

export const CompanyManagement = () => {
  const queryClient = useQueryClient();
  const {
    displayedCompanies,
    setQuery,
    isAdding,
    setIsAdding,
    setEditing,
    setDeleting,
    editing,
    deleting,
    create,
    update,
    remove,
    error,
    isLoading,
    page,
    setPage,
    rowsPerPage,
    setRowsPerPage,
    totalElements,
    totalPages,
    sortConfig,
    setSortConfig,
  } = useCompanyCrud();
  const { handlers, renderDialogs } = useBranchManagement();
  const [expandedRows, setExpandedRows] = useState<Set<number>>(new Set());

  const columns: ColumnConfig[] = [
    {
      id: 'code',
      label: 'Code',
      sortable: true,
      render: (item: Company) => item.code ?? '-',
    },
    {
      id: 'name',
      label: 'Bedrijfsnaam',
      sortable: true,
      render: (item: Company) => (
        <div className="flex items-center gap-2">
          <JdenticonAvatar value={item.name} size={32} />
          <span>{item.name}</span>
        </div>
      ),
    },
    {
      id: 'address',
      label: 'Plaats',
      sortable: false,
      render: (item: Company) => item.address.city,
    },
    {
      id: 'chamberOfCommerceId',
      label: 'KvK',
      sortable: false,
      render: (item: Company) => item.chamberOfCommerceId ?? '',
    },
    {
      id: 'vihbId',
      label: 'VIHB',
      sortable: false,
      render: (item: Company) => item.vihbId ?? '',
    },
  ];

  const additionalActions: AdditionalAction<Company>[] = [
    {
      label: 'Vestiging toevoegen',
      icon: BuildingOffice,
      onClick: handlers.handleAddBranch,
    },
  ];

  const handleSortClick = (columnId: string) => {
    let newDirection: 'asc' | 'desc' = 'asc';
    if (sortConfig.sortBy === columnId) {
      newDirection = sortConfig.sortDirection === 'asc' ? 'desc' : 'asc';
    }
    setSortConfig({
      sortBy: columnId,
      sortDirection: newDirection,
    });
  };

  const renderSortableHeader = (column: ColumnConfig) => {
    const isActive = sortConfig.sortBy === column.id;
    
    if (!column.sortable) {
      return <span>{column.label}</span>;
    }

    return (
      <div
        className="flex items-center gap-1 cursor-pointer select-none"
        onClick={() => handleSortClick(column.id)}
      >
        <span>{column.label}</span>
        <SortIcon
          className={`w-4 h-4 transition-transform ${
            isActive
              ? sortConfig.sortDirection === 'desc'
                ? 'rotate-180 text-color-brand-primary'
                : 'text-color-brand-primary'
              : 'text-color-text-tertiary'
          }`}
        />
      </div>
    );
  };

  const toggleRowExpansion = (index: number) => {
    const newExpanded = new Set(expandedRows);
    if (expandedRows.has(index)) {
      newExpanded.delete(index);
    } else {
      newExpanded.add(index);
    }
    setExpandedRows(newExpanded);
  };

  const hasSubItems = (company: Company) => {
    return !!(company.branches && company.branches.length > 0);
  };

  const renderBranchRow = (branch: CompanyBranch) => {
    const handleEditBranch = (branch: CompanyBranch) => {
      const company = displayedCompanies.find((c) => c.id === branch.companyId);
      if (company) {
        handlers.editBranch(branch, company);
      }
    };

    return (
      <tr
        key={branch.id}
        className="text-body-2 border-b border-solid border-color-border-primary bg-color-surface-secondary"
      >
        {columns.map((col, colIndex) => {
          let content: ReactNode = '';

          if (col.id === 'name') {
            content = (
              <div className="flex items-center gap-2 ml-7 pl-4">
                <BuildingOffice className="w-5 h-5 text-color-text-secondary" />
              </div>
            );
          } else if (col.id === 'address') {
            content = `${branch.address.street} ${branch.address.houseNumber}${branch.address.houseNumberAddition ?? ''} ${branch.address.city}`;
          }

          return (
            <td className="p-4" key={col.id}>
              <div className={colIndex === 0 ? 'ml-7' : ''}>{content}</div>
            </td>
          );
        })}
        <td className="p-4 text-center">
          <ActionMenu<CompanyBranch>
            onEdit={handleEditBranch}
            onDelete={handlers.removeBranch}
            item={branch}
          />
        </td>
      </tr>
    );
  };

  const handleFormSubmit = async (data: Company, restoreCompanyId?: string) => {
    if (editing) {
      await update(data);
    } else {
      await create(data, restoreCompanyId);
    }
  };

  const renderTable = () => {
    if (error) {
      throw error;
    }

    return (
      <div className="flex-1 items-start self-stretch border-t-solid border-t border-t-color-border-primary h-full overflow-y-auto">
        <table className="w-full table-fixed border-collapse">
          <colgroup>
            {columns.map((col) => (
              <col key={col.id} className="w-[calc((100%-64px)/5)]" />
            ))}
            <col className="w-[64px]" />
          </colgroup>
          <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
            <tr className="text-subtitle-1">
              {columns.map((col) => (
                <th className="px-4 py-3 text-left truncate" key={col.id}>
                  {renderSortableHeader(col)}
                </th>
              ))}
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {displayedCompanies.map((item, index) => {
              const actualIndex = (page - 1) * rowsPerPage + index;
              const isExpanded = expandedRows.has(actualIndex);
              const hasBranches = hasSubItems(item);
              const branches = item.branches || [];

              return (
                <Fragment key={item.id}>
                  <tr
                    className="text-body-2 border-b border-solid border-color-border-primary hover:bg-color-surface-secondary"
                    onDoubleClick={() => setEditing(item)}
                  >
                    {columns.map((col, colIndex) => (
                      <td className="p-4" key={col.id}>
                        <div className="flex items-center gap-2">
                          {colIndex === 0 && hasBranches && (
                            <button
                              onClick={() => toggleRowExpansion(actualIndex)}
                              className="p-1 hover:bg-color-surface-secondary rounded transition-colors"
                            >
                              {isExpanded ? (
                                <CaretDown className="w-4 h-4 text-color-text-secondary" />
                              ) : (
                                <CaretRight className="w-4 h-4 text-color-text-secondary" />
                              )}
                            </button>
                          )}
                          <div className={colIndex === 0 && !hasBranches ? 'ml-7' : ''}>
                            {col.render(item)}
                          </div>
                        </div>
                      </td>
                    ))}
                    <td className="p-4 text-center">
                      <ActionMenu<Company>
                        onEdit={setEditing}
                        onDelete={setDeleting}
                        item={item}
                        additionalActions={additionalActions}
                      />
                    </td>
                  </tr>
                  {isExpanded && hasBranches && branches.map(renderBranchRow)}
                </Fragment>
              );
            })}
          </tbody>
          <tfoot className="sticky bottom-0 bg-color-surface-primary border-solid border-y border-color-border-primary z-10">
            <tr className="text-body-2 bg-color-surface-primary">
              <td colSpan={columns.length + 1} className="p-4">
                <PaginationRow
                  page={page}
                  setPage={setPage}
                  rowsPerPage={rowsPerPage}
                  setRowsPerPage={setRowsPerPage}
                  numberOfResults={totalElements}
                  totalPages={totalPages}
                />
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    );
  };

  return (
    <>
      <ContentContainer title="Klantenbeheer">
        <div className="flex-1 flex flex-col items-start self-stretch pt-4 gap-4 border border-solid rounded-radius-xl border-color-border-primary bg-color-surface-primary overflow-hidden">
          <ContentTitleBar setQuery={setQuery}>
            <Button
              variant="primary"
              icon={Plus}
              label="Voeg toe"
              onClick={() => setIsAdding(true)}
            />
          </ContentTitleBar>
          {displayedCompanies.length === 0 && !error ? (
            isLoading ? (
              <div className="flex justify-center items-center h-24 w-full">
                <ClipLoader
                  size={20}
                  color="text-color-text-invert-primary"
                  aria-label="Laad spinner"
                />
              </div>
            ) : (
              <EmptyState
                icon={BuildingOffice}
                text="Geen klanten gevonden"
                onClick={() => setIsAdding(true)}
              />
            )
          ) : (
            <ErrorBoundary
              fallbackRender={fallbackRender}
              onReset={() => {
                queryClient
                  .invalidateQueries({ queryKey: ['companies'] })
                  .catch(() => {});
              }}
            >
              {renderTable()}
            </ErrorBoundary>
          )}
        </div>
      </ContentContainer>

      <FormDialog isOpen={isAdding} setIsOpen={() => setIsAdding(false)}>
        <CompanyForm
          onCancel={() => setIsAdding(false)}
          onSubmit={handleFormSubmit}
          company={undefined}
        />
      </FormDialog>

      <FormDialog isOpen={!!editing} setIsOpen={() => setEditing(undefined)}>
        <CompanyForm
          onCancel={() => setEditing(undefined)}
          onSubmit={handleFormSubmit}
          company={editing}
        />
      </FormDialog>

      <DeleteDialog
        isOpen={!!deleting}
        setIsOpen={() => setDeleting(undefined)}
        onDelete={() => deleting && remove(deleting)}
        title="Bedrijf verwijderen"
        description={`Weet u zeker dat u bedrijf ${deleting?.name} wilt verwijderen?`}
      />

      {renderDialogs()}
    </>
  );
};

export default CompanyManagement;
