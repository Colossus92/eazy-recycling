import X from '@/assets/icons/X.svg?react';
import { Button } from '@/components/ui/button/Button';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { DateFormField } from '@/components/ui/form/DateFormField';
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
    loadInvoice,
    resetForm,
    handleSubmit,
  } = useInvoiceFormHook();

  const isEditMode = invoiceId !== undefined;

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

  const handleClose = () => {
    setIsOpen(false);
    resetForm();
  };

  const invoiceTypeOptions = [
    { value: 'SALE', label: 'Verkoop' },
    { value: 'PURCHASE', label: 'Inkoop' },
  ];

  const documentTypeOptions = [
    { value: 'INVOICE', label: 'Factuur' },
    { value: 'CREDIT_NOTE', label: 'Creditnota' },
  ];

  return (
    <FormDialog isOpen={isOpen} setIsOpen={handleClose} width="w-[800px]">
      {/* Header */}
      <div className="flex py-3 px-4 items-center gap-4 self-stretch border-b border-solid border-color-border-primary">
        <div className="flex-1">
          <h4>{isEditMode ? 'Factuur bewerken' : 'Nieuwe factuur'}</h4>
        </div>
        <Button
          icon={X}
          showText={false}
          variant="tertiary"
          iconPosition="right"
          onClick={handleClose}
        />
      </div>

      {/* Content */}
      <div className="flex flex-col self-stretch p-4 max-h-[70vh] overflow-y-auto">
        {isLoading ? (
          <div className="flex justify-center items-center h-48">
            <ClipLoader size={20} aria-label="Laden..." />
          </div>
        ) : (
          <FormProvider {...formContext}>
            <form
              onSubmit={formContext.handleSubmit(onSubmit)}
              className="flex flex-col gap-6"
            >
              {/* Invoice header section */}
              <div className="grid grid-cols-2 gap-4">
                <CompanySelectFormField
                  title="Klant"
                  placeholder="Selecteer een klant"
                  name="customerId"
                  rules={{ required: 'Klant is verplicht' }}
                />
                <DateFormField
                  title="Factuurdatum"
                  placeholder="Selecteer een datum"
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
                  formHook={{
                    register: formContext.register,
                    name: 'documentType',
                    rules: { required: 'Document type is verplicht' },
                    errors: formContext.formState.errors,
                  }}
                />
              </div>

              {/* Invoice lines section */}
              <InvoiceLinesSection />

              {/* Footer with actions */}
              <div className="flex justify-between items-center pt-4 border-t border-color-border-primary">
                <div>
                  {isEditMode && (
                    <Button
                      variant="destructive"
                      label="Verwijderen"
                      onClick={() => {
                        if (invoiceId) {
                          onDelete(invoiceId);
                          setIsOpen(false);
                        }
                      }}
                      type="button"
                    />
                  )}
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    label="Annuleren"
                    onClick={handleClose}
                    type="button"
                  />
                  <Button
                    variant="primary"
                    label={isSaving ? 'Opslaan...' : 'Opslaan'}
                    type="submit"
                    disabled={isSaving}
                  />
                </div>
              </div>
            </form>
          </FormProvider>
        )}
      </div>
    </FormDialog>
  );
};
