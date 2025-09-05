import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { companyService } from '@/api/companyService.ts';
import { Company, CompanyBranch } from '@/types/api.ts';
import { getErrorMessage } from '@/utils/error';

export const useCompanyBranchCrud = () => {
  const queryClient = useQueryClient();
  const [branchFormOpen, setBranchFormOpen] = useState(false);
  const [selectedCompanyForBranch, setSelectedCompanyForBranch] = useState<
    Company | undefined
  >();
  const [errorDialogOpen, setErrorDialogOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [branchToEdit, setBranchToEdit] = useState<CompanyBranch | undefined>();

  const handleAddBranch = (company: Company) => {
    setSelectedCompanyForBranch(company);
    setBranchFormOpen(true);
  };

  const handleBranchFormClose = () => {
    setBranchFormOpen(false);
    setSelectedCompanyForBranch(undefined);
    setBranchToEdit(undefined);
  };

  const handleBranchSubmit = async (companyBranch: CompanyBranch) => {
    if (!selectedCompanyForBranch?.id) {
      setErrorMessage('Geen bedrijf geselecteerd voor vestiging.');
      setErrorDialogOpen(true);
      return;
    }

    try {
      if (branchToEdit) {
        if (!branchToEdit.id) {
          setErrorMessage('Vestiging ID ontbreekt voor bewerking.');
          setErrorDialogOpen(true);
          return;
        }
        await companyService.updateBranch(
          selectedCompanyForBranch.id,
          branchToEdit.id,
          companyBranch
        );
      } else {
        await companyService.createBranch(
          selectedCompanyForBranch.id,
          companyBranch
        );
      }
      await queryClient.invalidateQueries({ queryKey: ['companies'] });
      handleBranchFormClose();
    } catch (error: unknown) {
      const errorMessage = getErrorMessage(
        error,
        'Er is een fout opgetreden bij het toevoegen van de vestiging.'
      );
      setErrorMessage(errorMessage);
      setErrorDialogOpen(true);
    }
  };

  const editBranch = (companyBranch: CompanyBranch, company: Company) => {
    setBranchToEdit(companyBranch);
    setSelectedCompanyForBranch(company);
    setBranchFormOpen(true);
  };

  const removeBranch = (companyBranch: CompanyBranch) => {
    if (!companyBranch.companyId || !companyBranch.id) {
      setErrorMessage('Vestiging informatie is onvolledig.');
      setErrorDialogOpen(true);
      return;
    }

    companyService
      .removeBranch(companyBranch.companyId, companyBranch.id)
      .then(() => queryClient.invalidateQueries({ queryKey: ['companies'] }))
      .catch((error: unknown) => {
        const errorMessage = getErrorMessage(
          error,
          'Er is een fout opgetreden bij het verwijderen van de vestiging.'
        );
        setErrorMessage(errorMessage);
        setErrorDialogOpen(true);
      });
  };

  return {
    branchFormOpen,
    selectedCompanyForBranch,
    handleAddBranch,
    handleBranchFormClose,
    handleBranchSubmit,
    errorDialogOpen,
    errorMessage,
    setErrorDialogOpen,
    editBranch,
    removeBranch,
    branchToEdit,
  };
};
