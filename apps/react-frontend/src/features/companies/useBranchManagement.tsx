import { CompanyBranchForm } from './CompanyBranchForm.tsx';
import { useCompanyBranchCrud } from './useCompanyBranchCrud.ts';
import { ErrorDialog } from '@/components/ui/dialog/ErrorDialog.tsx';
import { FormDialog } from '@/components/ui/dialog/FormDialog.tsx';
import { Company, CompanyBranch } from '@/types/api.ts';

interface BranchManagementHandlers {
  handleAddBranch: (company: Company) => void;
  editBranch: (branch: CompanyBranch, company: Company) => void;
  removeBranch: (branch: CompanyBranch) => void;
}

export const useBranchManagement = () => {
  const hookState = useCompanyBranchCrud();

  const handlers: BranchManagementHandlers = {
    handleAddBranch: hookState.handleAddBranch,
    editBranch: hookState.editBranch,
    removeBranch: hookState.removeBranch,
  };

  const renderDialogs = () => (
    <>
      <FormDialog
        isOpen={hookState.branchFormOpen}
        setIsOpen={hookState.handleBranchFormClose}
      >
        {hookState.selectedCompanyForBranch && (
          <CompanyBranchForm
            onCancel={hookState.handleBranchFormClose}
            onSubmit={hookState.handleBranchSubmit}
            companyName={hookState.selectedCompanyForBranch.name}
            companyBranch={hookState.branchToEdit}
          />
        )}
      </FormDialog>

      <ErrorDialog
        isOpen={hookState.errorDialogOpen}
        setIsOpen={hookState.setErrorDialogOpen}
        errorMessage={hookState.errorMessage}
      />
    </>
  );

  return {
    handlers,
    renderDialogs,
  };
};
