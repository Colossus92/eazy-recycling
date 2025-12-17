import { Button } from '@/components/ui/button/Button';
import { SplitButton } from '@/components/ui/button/SplitButton';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { DateFormField } from '@/components/ui/form/DateFormField';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { useEffect } from 'react';
import { FormProvider } from 'react-hook-form';
import { ClipLoader } from 'react-spinners';
import { InvoiceLinesSection } from './InvoiceLinesSection';
import { useInvoiceFormHook } from './useInvoiceFormHook';

interface InvoiceFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  invoiceId?: number;
  onComplete: () => void;
  onDelete: (id: number) => void;
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
    loadInvoice,
    resetForm,
    handleSubmit,
    handleSubmitAndFinalize,
  } = useInvoiceFormHook();

  const isEditMode = invoiceId !== undefined;

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
    const success = await handleSubmit(invoiceId);
    if (success) {
      onComplete();
      setIsOpen(false);
    }
  };

  const onSubmitAndFinalize = async () => {
    const success = await handleSubmitAndFinalize(invoiceId);
    if (success) {
      onComplete();
      setIsOpen(false);
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
    <FormDialog isOpen={isOpen} setIsOpen={handleClose} width="w-[800px]">
      <FormProvider {...formContext}>
        <form
          onSubmit={formContext.handleSubmit(onSubmit)}
          className="flex flex-col h-full w-full"
        >
          <FormTopBar
            title={getFormTitle()}
            actions={
              isEditMode && !isReadOnly && (
                <FormActionMenu
                  onDelete={() => {
                    if (invoiceId) {
                      onDelete(invoiceId);
                      setIsOpen(false);
                    }
                  }}
                />
              )
            }
            onClick={handleClose}
          />

          {/* Content */}
          <div className="flex flex-col self-stretch p-4 max-h-[70vh] overflow-y-auto flex-1">
            {isLoading ? (
              <div className="flex justify-center items-center h-48">
                <ClipLoader size={20} aria-label="Laden..." />
              </div>
            ) : (
              <div className="flex flex-col gap-6">
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
                secondaryLabel={isEditMode ? 'Verwerken' : 'Opslaan en verwerken'}
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
