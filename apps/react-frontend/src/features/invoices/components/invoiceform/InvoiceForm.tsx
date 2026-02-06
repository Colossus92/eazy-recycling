import { companyService } from '@/api/services/companyService';
import { invoiceService } from '@/api/services/invoiceService';
import { Button } from '@/components/ui/button/Button';
import { SplitButton } from '@/components/ui/button/SplitButton';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import {
  EmailComposer,
  EmailComposerValues,
} from '@/components/ui/email/EmailComposer';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { DateFormField } from '@/components/ui/form/DateFormField';
import { FormTopBar } from '@/components/ui/form/FormTopBar';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { Tab } from '@/components/ui/tab/Tab';
import { toastService } from '@/components/ui/toast/toastService';
import { Note } from '@/features/planning/components/note/Note';
import { TabGroup, TabList, TabPanel, TabPanels } from '@headlessui/react';
import { useQuery } from '@tanstack/react-query';
import { format } from 'date-fns';
import { useEffect, useMemo, useState } from 'react';
import { FormProvider } from 'react-hook-form';
import { ClipLoader } from 'react-spinners';
import { InvoiceFormActionMenu } from './InvoiceFormActionMenu';
import { InvoiceLinesSection } from './InvoiceLinesSection';
import { InvoiceRelatedTab } from './InvoiceRelatedTab';
import { useInvoiceEmailFormHook } from './useInvoiceEmailFormHook';
import { InvoiceLineFormValue, useInvoiceFormHook } from './useInvoiceFormHook';

interface InvoiceFormProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  invoiceId?: string;
  onComplete: () => void;
  onDelete: (id: string) => void;
  onCopy?: (id: string) => void;
}

export const InvoiceForm = ({
  isOpen,
  setIsOpen,
  invoiceId,
  onComplete,
  onDelete,
  onCopy,
}: InvoiceFormProps) => {
  const {
    formContext,
    isLoading,
    isSaving,
    isReadOnly,
    isFinal,
    invoiceNumber,
    invoiceStatus,
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

  const [isSending, setIsSending] = useState(false);
  const [isCreatingCredit, setIsCreatingCredit] = useState(false);

  // Use the hook's currentInvoiceId to determine edit mode (handles newly created invoices)
  const isEditMode = invoiceId !== undefined || currentInvoiceId !== null;

  const documentType = formContext.watch('documentType');
  const isCreditNote = documentType === 'CREDIT_NOTE';

  const customerId = formContext.watch('customerId');
  const watchedLines = formContext.watch('lines');

  const { data: selectedCompany } = useQuery({
    queryKey: ['company', customerId],
    queryFn: () => companyService.getById(customerId),
    enabled: !!customerId,
    staleTime: 5 * 60 * 1000,
  });

  const showVatNumberWarning = useMemo(() => {
    if (!customerId || !watchedLines) return false;
    const hasReverseCharge = watchedLines.some(
      (line: InvoiceLineFormValue) => line.isReverseCharge
    );
    return hasReverseCharge && !selectedCompany?.vatNumber;
  }, [customerId, watchedLines, selectedCompany]);

  const getFormTitle = () => {
    if (invoiceNumber) {
      const prefix = isCreditNote ? 'Creditnota' : 'Factuur';
      return `${prefix} #${invoiceNumber}`;
    }
    return isEditMode ? 'Factuur bewerken' : 'Nieuwe factuur';
  };

  const handleCreateCreditInvoice = async () => {
    const idToCredit = currentInvoiceId ?? invoiceId;
    if (!idToCredit) {
      toastService.error('Geen factuur geselecteerd');
      return;
    }

    setIsCreatingCredit(true);
    try {
      const result = await invoiceService.createCredit(idToCredit, {
        invoiceDate: format(new Date(), 'yyyy-MM-dd'),
      });
      toastService.success('Creditnota aangemaakt');
      // Load the newly created credit invoice
      await loadInvoice(result.invoiceId);
      onComplete();
    } catch (error) {
      console.error('Error creating credit invoice:', error);
      toastService.error(
        'Er is een fout opgetreden bij het aanmaken van de creditnota'
      );
    } finally {
      setIsCreatingCredit(false);
    }
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
        await invoiceService.update(currentInvoiceId, {
          customerId: values.customerId,
          invoiceType: values.invoiceType,
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
      setEmailStep('form');
      throw error;
    }
  };

  const handleSendFinalInvoice = async () => {
    if (!currentInvoiceId) {
      toastService.error('Geen factuur geselecteerd');
      return;
    }

    setEmailStep('processing-invoice');

    try {
      // Load the invoice to get customer details
      const invoice = await invoiceService.getById(currentInvoiceId);

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
        startPdfPolling(currentInvoiceId);
      }

      // Move to email compose step
      setEmailStep('email-compose');
    } catch (error) {
      console.error('Error preparing email:', error);
      toastService.error(
        'Er is een fout opgetreden bij het voorbereiden van de e-mail'
      );
      setEmailStep('form');
    }
  };

  const handleSendEmail = async (values: EmailComposerValues) => {
    if (!currentInvoiceId) {
      toastService.error('Geen factuur geselecteerd');
      return;
    }

    setIsSending(true);

    try {
      await invoiceService.send(currentInvoiceId, {
        to: values.to,
        bcc: values.bcc || undefined,
        subject: values.subject,
        body: values.body,
      });

      toastService.success('E-mail wordt verzonden');
      handleClose();
    } catch (error) {
      console.error('Error sending email:', error);
      toastService.error(
        'Er is een fout opgetreden bij het verzenden van de e-mail'
      );
    } finally {
      setIsSending(false);
    }
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
          isSending={isSending}
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
              isEditMode && (
                <InvoiceFormActionMenu
                  onDelete={() => {
                    const idToDelete = currentInvoiceId ?? invoiceId;
                    if (idToDelete) {
                      onDelete(idToDelete);
                      setIsOpen(false);
                    }
                  }}
                  onCreateCredit={handleCreateCreditInvoice}
                  onCopy={() => {
                    const idToCopy = currentInvoiceId ?? invoiceId;
                    if (idToCopy && onCopy) {
                      onCopy(idToCopy);
                    }
                  }}
                  isReadOnly={isReadOnly}
                  isCreditNote={isCreditNote}
                />
              )
            }
            onClick={handleClose}
          />

          {/* Content */}
          <div className="flex flex-col items-start self-stretch flex-1 gap-5 p-4 min-h-0">
            {isLoading || isCreatingCredit ? (
              <div className="flex justify-center items-center w-full p-8">
                <ClipLoader size={20} aria-label="Laden..." />
              </div>
            ) : (
              <div className="flex flex-col items-start self-stretch gap-4 flex-1 min-h-0">
                <TabGroup className="w-full flex-1 flex flex-col min-h-0">
                  <TabList className="relative z-10 flex-shrink-0">
                    <Tab label="Gegevens" />
                    <Tab label="Gerelateerd" />
                  </TabList>
                  <TabPanels className="flex flex-col flex-1 bg-color-surface-primary border border-solid rounded-b-radius-lg rounded-tr-radius-lg border-color-border-primary pt-4 gap-4 min-h-0 -mt-[2px] overflow-y-auto">
                    {/* General Tab */}
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4 min-h-0">
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
                          <div className="flex flex-col gap-1">
                            <label className="text-body-2 text-color-text-secondary">
                              Document type
                            </label>
                            <span className="text-body-1 py-2">
                              {isCreditNote ? 'Creditnota' : 'Factuur'}
                            </span>
                          </div>
                        </div>

                        {showVatNumberWarning && (
                          <Note note="Deze factuur bevat btw verlegd regels, maar het geselecteerde bedrijf heeft geen BTW ID. Een BTW ID is verplicht om deze factuur te kunnen verwerken." />
                        )}

                        <InvoiceLinesSection
                          isReadOnly={isReadOnly}
                          isCreditNote={isCreditNote}
                          invoiceType={
                            formContext.watch('invoiceType') as
                              | 'PURCHASE'
                              | 'SALE'
                          }
                        />
                      </div>
                    </TabPanel>

                    {/* Related Tab */}
                    <TabPanel className="flex flex-col items-start gap-4 px-4 pb-4">
                      <InvoiceRelatedTab
                        invoiceId={currentInvoiceId ?? invoiceId}
                        currentStatus={invoiceStatus ?? undefined}
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
            {isFinal && (
              <Button
                variant="primary"
                label="Verzenden"
                onClick={handleSendFinalInvoice}
                type="button"
              />
            )}
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
