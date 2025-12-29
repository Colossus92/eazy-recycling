import { Button } from '@/components/ui/button/Button';
import { SplitButton } from '@/components/ui/button/SplitButton';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { DateFormField } from '@/components/ui/form/DateFormField';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { Tab } from '@/components/ui/tab/Tab';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { useEffect } from 'react';
import { FormProvider } from 'react-hook-form';
import { ClipLoader } from 'react-spinners';
import { InvoiceLinesSection } from './InvoiceLinesSection';
import { InvoiceRelatedTab } from './InvoiceRelatedTab';
import { useInvoiceFormHook } from './useInvoiceFormHook';

interface InvoiceFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  invoiceId?: string;
  onComplete: () => void;
  onDelete: (id: string) => void;
}

export const InvoiceForm = ({
  isOpen,
  setIsOpen,
  invoiceId,
  onComplete,
  onDelete,
}: InvoiceFormProps) => {
  const {
    formContext,
    isLoading,
    isSaving,
    isReadOnly,
    invoiceNumber,
    currentInvoiceId,
    loadInvoice,
    resetForm,
    handleSubmit,
    handleSubmitAndFinalize,
  } = useInvoiceFormHook();

  // Use the hook's currentInvoiceId to determine edit mode (handles newly created invoices)
  const isEditMode = invoiceId !== undefined || currentInvoiceId !== null;

  const getFormTitle = () => {
    if (invoiceNumber) {
      return `Factuur #${invoiceNumber}`;
    }
    return isEditMode ? 'Factuur bewerken' : 'Nieuwe factuur';
  };

  useEffect(() => {
    if (isOpen) {
      if (invoiceId) {
        loadInvoice(invoiceId);
      } else {
        resetForm();
      }
    }
  }, [isOpen, invoiceId, loadInvoice, resetForm]);

  const onSubmit = async () => {
    const savedInvoiceId = await handleSubmit();
    if (savedInvoiceId !== null) {
      // Refresh the list, form stays open with the saved invoice loaded
      onComplete();
    }
  };

  const onSubmitAndFinalize = async () => {
    const finalizedInvoiceId = await handleSubmitAndFinalize();
    if (finalizedInvoiceId !== null) {
      // Refresh the list, form stays open with the finalized invoice loaded
      onComplete();
    }
  };

  const handleClose = () => {
    setIsOpen(false);
    resetForm();
  };

  const invoiceTypeOptions = [
    { value: 'PURCHASE', label: 'Inkoop' },
    { value: 'SALE', label: 'Verkoop' },
  ];

  const documentTypeOptions = [
    { value: 'INVOICE', label: 'Factuur' },
    { value: 'CREDIT_NOTE', label: 'Creditnota' },
  ];

  return (
    <FormDialog
      isOpen={isOpen}
      setIsOpen={handleClose}
      width="w-[800px] h-[90vh]"
    >
      <FormProvider {...formContext}>
        <form
          onSubmit={formContext.handleSubmit(onSubmit)}
          className="flex flex-col h-full w-full"
        >
          <FormTopBar
            title={getFormTitle()}
            actions={
              isEditMode &&
              !isReadOnly && (
                <FormActionMenu
                  onDelete={() => {
                    const idToDelete = currentInvoiceId ?? invoiceId;
                    if (idToDelete) {
                      onDelete(idToDelete);
                      setIsOpen(false);
                    }
                  }}
                />
              )
            }
            onClick={handleClose}
          />

          {/* Content */}
          <div className="flex flex-col items-start self-stretch flex-1 gap-5 p-4 h-full">
            {isLoading ? (
              <div className="flex justify-center items-center w-full p-8">
                <ClipLoader size={20} aria-label="Laden..." />
              </div>
            ) : (
              <div className="flex flex-col items-start self-stretch gap-4 flex-1">
                <TabGroup className="w-full flex-1 flex flex-col h-full">
                  <TabList className="relative z-10">
                    <Tab label="Gegevens" />
                    <Tab label="Gerelateerd" />
                  </TabList>
                  <TabPanels className="flex flex-col flex-1 bg-color-surface-primary border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary pt-4 gap-4 min-h-0 -mt-[2px] overflow-y-auto">
                    {/* General Tab */}
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                      <div className="flex flex-col gap-6 w-full">
                        {/* Invoice header section */}
                        <div className="grid grid-cols-2 gap-4">
                          <CompanySelectFormField
                            title="Klant"
                            placeholder="Selecteer een klant"
                            name="customerId"
                            rules={{ required: 'Klant is verplicht' }}
                            disabled={isReadOnly}
                          />
                          <DateFormField
                            title="Factuurdatum"
                            placeholder="Selecteer een datum"
                            disabled={isReadOnly}
                            formHook={{
                              register: formContext.register,
                              name: 'invoiceDate',
                              rules: { required: 'Factuurdatum is verplicht' },
                              errors: formContext.formState.errors,
                            }}
                          />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                          <SelectFormField
                            title="Type"
                            placeholder="Selecteer type"
                            options={invoiceTypeOptions}
                            disabled={isReadOnly}
                            formHook={{
                              register: formContext.register,
                              name: 'invoiceType',
                              rules: { required: 'Type is verplicht' },
                              errors: formContext.formState.errors,
                            }}
                          />
                          <SelectFormField
                            title="Document type"
                            placeholder="Selecteer document type"
                            options={documentTypeOptions}
                            disabled={isReadOnly}
                            formHook={{
                              register: formContext.register,
                              name: 'documentType',
                              rules: { required: 'Document type is verplicht' },
                              errors: formContext.formState.errors,
                            }}
                          />
                        </div>

                        {/* Invoice lines section */}
                        <InvoiceLinesSection isReadOnly={isReadOnly} />
                      </div>
                    </TabPanel>

                    {/* Related Tab */}
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                      <InvoiceRelatedTab
                        invoiceId={currentInvoiceId ?? invoiceId}
                      />
                    </TabPanel>
                  </TabPanels>
                </TabGroup>
              </div>
            )}
          </div>

          {/* Footer with actions */}
          <div className="flex py-3 px-4 justify-end items-center self-stretch gap-4 border-t border-solid border-color-border-primary">
            <Button
              variant="secondary"
              label={isReadOnly ? 'Sluiten' : 'Annuleren'}
              onClick={handleClose}
              type="button"
            />
            {!isReadOnly && (
              <SplitButton
                primaryLabel={isEditMode ? 'Opslaan' : 'Concept opslaan'}
                secondaryLabel={
                  isEditMode ? 'Verwerken' : 'Opslaan en verwerken'
                }
                onPrimaryClick={onSubmit}
                onSecondaryClick={onSubmitAndFinalize}
                isSubmitting={isSaving}
              />
            )}
          </div>
        </form>
      </FormProvider>
    </FormDialog>
  );
};
