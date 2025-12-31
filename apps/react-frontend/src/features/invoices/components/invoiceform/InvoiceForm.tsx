import { Button } from '@/components/ui/button/Button';
import { SplitButton } from '@/components/ui/button/SplitButton';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import {
  EmailComposer,
  EmailComposerValues,
} from '@/components/ui/email/EmailComposer';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { DateFormField } from '@/components/ui/form/DateFormField';
import { FormActionMenu } from '@/components/ui/form/FormActionMenu';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { Tab } from '@/components/ui/tab/Tab';
import { toastService } from '@/components/ui/toast/toastService';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { useEffect } from 'react';
import { FormProvider } from 'react-hook-form';
import { ClipLoader } from 'react-spinners';
import { InvoiceLinesSection } from './InvoiceLinesSection';
import { InvoiceRelatedTab } from './InvoiceRelatedTab';
import { useInvoiceFormHook } from './useInvoiceFormHook';
import { useInvoiceEmailFormHook } from './useInvoiceEmailFormHook';
import { invoiceService } from '@/api/services/invoiceService';

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
  } = useInvoiceFormHook();

  const {
    emailStep,
    setEmailStep,
    emailData,
    isPdfReady,
    startPdfPolling,
    prepareEmailData,
    getEmailDefaultValues,
    handleOpenPdfAttachment,
    cancelEmailStep,
    resetEmailState,
  } = useInvoiceEmailFormHook();

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

  const onSubmitAndSend = async () => {
    const isValid = await formContext.trigger();
    if (!isValid) return;

    const values = formContext.getValues();
    const validLines = values.lines.filter((line) => line.catalogItemId);
    if (validLines.length === 0) {
      toastService.error('Minimaal één factuurregel is verplicht');
      return;
    }

    setEmailStep('processing-invoice');

    try {
      let finalizedInvoiceId: string;
      let invoice;

      if (currentInvoiceId) {
        // Update existing invoice, then finalize
        await invoiceService.update(
          currentInvoiceId,
          {
            invoiceDate: values.invoiceDate,
            lines: values.lines
              .filter((line) => line.catalogItemId)
              .map((line) => ({
                id: line.id || undefined,
                date: line.date || values.invoiceDate,
                catalogItemId: line.catalogItemId,
                description: line.description,
                quantity: parseFloat(line.quantity) || 0,
                unitPrice: parseFloat(line.unitPrice) || 0,
                orderReference: line.orderReference || undefined,
              })),
          }
        );
        await invoiceService.finalize(currentInvoiceId);
        finalizedInvoiceId = currentInvoiceId;
      } else {
        // Create and finalize in one call
        const result = await invoiceService.createCompleted({
          invoiceType: values.invoiceType,
          documentType: values.documentType,
          customerId: values.customerId,
          invoiceDate: values.invoiceDate,
          lines: values.lines
            .filter((line) => line.catalogItemId)
            .map((line) => ({
              id: line.id || undefined,
              date: line.date || values.invoiceDate,
              catalogItemId: line.catalogItemId,
              description: line.description,
              quantity: parseFloat(line.quantity) || 0,
              unitPrice: parseFloat(line.unitPrice) || 0,
              orderReference: line.orderReference || undefined,
            })),
        });
        finalizedInvoiceId = result.invoiceId;
      }

      // Load the finalized invoice to get customer details
      invoice = await invoiceService.getById(finalizedInvoiceId);
      await loadInvoice(finalizedInvoiceId);

      // Prepare email data
      const invoiceNum = invoice.invoiceNumber || 'DRAFT';
      prepareEmailData(
        invoiceNum,
        invoice.customer.name,
        invoice.customerEmail || '',
        invoice.tenant,
        invoice.pdfUrl
      );

      // Start polling if PDF is not ready
      if (!invoice.pdfUrl) {
        startPdfPolling(finalizedInvoiceId);
      }

      // Move to email compose step
      setEmailStep('email-compose');

      // Refresh the list
      onComplete();
    } catch (error) {
      console.error('Error saving and preparing email:', error);
      toastService.error(
        'Er is een fout opgetreden bij het verwerken van de factuur'
      );
      setEmailStep('form');
    }
  };

  const handleSendEmail = (values: EmailComposerValues) => {
    // TODO: Implement in next phase - call backend to send email
    console.log('Sending email:', values);
    toastService.success('E-mail verzonden (simulatie)');
    handleClose();
  };

  const handleClose = () => {
    setIsOpen(false);
    resetForm();
    resetEmailState();
  };

  const invoiceTypeOptions = [
    { value: 'PURCHASE', label: 'Inkoop' },
    { value: 'SALE', label: 'Verkoop' },
  ];

  const documentTypeOptions = [
    { value: 'INVOICE', label: 'Factuur' },
    { value: 'CREDIT_NOTE', label: 'Creditnota' },
  ];

  // Render email composer when in email step
  if (emailStep === 'email-compose' && emailData) {
    return (
      <FormDialog
        isOpen={isOpen}
        setIsOpen={handleClose}
        width="w-[800px] h-[90vh]"
      >
        <EmailComposer
          emailHook={{
            emailStep,
            setEmailStep,
            emailData,
            isPdfReady,
            startPdfPolling,
            prepareEmailData,
            getEmailDefaultValues,
            handleOpenPdfAttachment,
            cancelEmailStep,
            resetEmailState,
          }}
          onSend={handleSendEmail}
        />
      </FormDialog>
    );
  }

  // Render processing invoice state
  if (emailStep === 'processing-invoice') {
    return (
      <FormDialog
        isOpen={isOpen}
        setIsOpen={handleClose}
        width="w-[800px] h-[90vh]"
      >
        <div className="flex flex-col items-center justify-center h-full gap-4">
          <ClipLoader size={40} />
          <span className="text-body-1 text-color-text-secondary">
            Factuur wordt verwerkt...
          </span>
        </div>
      </FormDialog>
    );
  }

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
                secondaryLabel="Opslaan & verzenden..."
                onPrimaryClick={onSubmit}
                onSecondaryClick={onSubmitAndSend}
                isSubmitting={isSaving}
              />
            )}
          </div>
        </form>
      </FormProvider>
    </FormDialog>
  );
};
