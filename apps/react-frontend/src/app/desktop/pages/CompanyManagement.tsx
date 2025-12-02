import { ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { CrudPage } from '@/features/crud/CrudPage.tsx';
import { useCompanyCrud } from '@/features/companies/useCompanyCrud.ts';
import { Company, CompanyBranch } from '@/api/services/companyService';
import { Column, ExpandableConfig } from '@/features/crud/ContentTable.tsx';
import { EmptyState } from '@/features/crud/EmptyState.tsx';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import { CompanyForm } from '@/features/companies/CompanyForm.tsx';
import { JdenticonAvatar } from '@/components/ui/icon/JdenticonAvatar.tsx';
import { ActionMenu, AdditionalAction } from '@/features/crud/ActionMenu';
import { useBranchManagement } from '@/features/companies/useBranchManagement.tsx';

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
    // Pagination
    page,
    setPage,
    rowsPerPage,
    setRowsPerPage,
    totalElements,
    totalPages,
  } = useCompanyCrud();
  const { handlers, renderDialogs } = useBranchManagement();

  const columns: Column<Company>[] = [
    {
      key: 'externalCode',
      label: 'Code',
      accessor: (item: Company) => item.externalCode ?? '-',
    },
    {
      key: 'name',
      label: 'Bedrijfsnaam',
      accessor: (item: Company): ReactNode => (
        <div className="flex items-center gap-2">
          <JdenticonAvatar value={item.name} size={32} />
          <span>{item.name}</span>
        </div>
      ),
    },
    {
      key: 'address',
      label: 'Plaats',
      accessor: (item: Company) => item.address.city,
    },
    { key: 'chamberOfCommerceId', label: 'KvK' },
    { key: 'vihbId', label: 'VIHB' },
  ];

  const additionalActions: AdditionalAction<Company>[] = [
    {
      label: 'Vestiging toevoegen',
      icon: BuildingOffice,
      onClick: handlers.handleAddBranch,
    },
  ];

  const expandableConfig: ExpandableConfig<Company, CompanyBranch> = {
    hasSubItems: (company: Company) => {
      return !!(company.branches && company.branches.length > 0);
    },
    getSubItems: (company: Company) => {
      return company.branches || [];
    },
    renderSubItem: (branch: CompanyBranch, columns: Column<Company>[]) => {
      const handleEditBranch = (branch: CompanyBranch) => {
        const company = displayedCompanies.find(
          (c) => c.id === branch.companyId
        );
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

            if (col.key === 'name') {
              content = (
                <div className="flex items-center  gap-2 ml-7 pl-4">
                  <BuildingOffice className="w-5 h-5 text-color-text-secondary" />
                </div>
              );
            } else if (col.key === 'address') {
              content = `${branch.address.street} ${branch.address.houseNumber}${branch.address.houseNumberAddition ?? ''} ${branch.address.city}`;
            }

            return (
              <td className="p-4" key={String(col.key)}>
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
    },
  };

  return (
    <>
      <CrudPage<Company, CompanyBranch>
        title={'Klantenbeheer'}
        data={{
          items: displayedCompanies,
          columns,
          setQuery,
          pagination: {
            page,
            setPage,
            rowsPerPage,
            setRowsPerPage,
            totalElements,
            totalPages,
          },
        }}
        dialogs={{
          add: {
            open: isAdding,
            onClose: () => setIsAdding(false),
            onSave: (company: Omit<Company, 'id'>) => create(company),
          },
          update: {
            open: !!editing,
            item: editing,
            onClose: () => setEditing(undefined),
            onSave: update,
          },
          delete: {
            title: 'Bedrijf verwijderen',
            description: `Weet u zeker dat u bedrijf ${deleting?.name} wilt verwijderen?`,
            open: !!deleting,
            item: deleting,
            onClose: () => setDeleting(undefined),
            onConfirm: remove,
          },
        }}
        actions={{
          onAdd: setIsAdding,
          onDelete: setDeleting,
          onEdit: setEditing,
        }}
        additionalActions={additionalActions}
        expandableConfig={expandableConfig}
        renderForm={(close, onSubmit, itemToEdit) => {
          // Wrapper to handle the restoreCompanyId parameter
          const handleSubmit = async (data: Company, restoreCompanyId?: string) => {
            if (itemToEdit) {
              // For updates, use the regular update function
              await onSubmit(data);
            } else {
              // For creates, pass the restoreCompanyId to create function
              await create(data, restoreCompanyId);
            }
          };

          return (
            <CompanyForm
              onCancel={close}
              onSubmit={handleSubmit}
              company={itemToEdit}
            />
          );
        }}
        renderEmptyState={(open) => (
          <EmptyState
            icon={BuildingOffice}
            text={'Geen klanten gevonden'}
            onClick={open}
          />
        )}
        error={error}
        onReset={() => {
          queryClient
            .invalidateQueries({ queryKey: ['companies'] })
            .catch(() => {});
        }}
        isLoading={isLoading}
      />

      {/* All branch-related dialogs are handled by the hook */}
      {renderDialogs()}
    </>
  );
};

export default CompanyManagement;
