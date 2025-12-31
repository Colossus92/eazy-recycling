import { useCallback, useEffect, useRef, useState } from 'react';
import { EmailComposerValues } from '@/components/ui/email/EmailComposer';
import { invoiceService } from '@/api/services/invoiceService';
import { openInvoicePdfInNewTab } from '@/api/services/invoicePdfService';
import { toastService } from '@/components/ui/toast/toastService';
import { TenantView } from '@/api/client/models/tenant-view';

export type EmailStep = 'form' | 'processing-invoice' | 'email-compose';

export interface InvoiceEmailData {
  customerEmail: string;
  customerName: string;
  invoiceNumber: string;
  pdfUrl?: string;
  pdfFileName: string;
  tenant: TenantView;
}

export const useInvoiceEmailFormHook = () => {
  const [emailStep, setEmailStep] = useState<EmailStep>('form');
  const [emailData, setEmailData] = useState<InvoiceEmailData | null>(null);
  const [isPdfReady, setIsPdfReady] = useState(false);
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  const stopPolling = useCallback(() => {
    if (pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
  }, []);

  const resetEmailState = useCallback(() => {
    setEmailStep('form');
    setEmailData(null);
    setIsPdfReady(false);
    stopPolling();
  }, [stopPolling]);

  // Cleanup polling on unmount
  useEffect(() => {
    return () => stopPolling();
  }, [stopPolling]);

  const startPdfPolling = useCallback(
    (invoiceId: string) => {
      stopPolling();

      const pollForPdf = async () => {
        try {
          const invoice = await invoiceService.getById(invoiceId);
          if (invoice.pdfUrl) {
            setIsPdfReady(true);
            setEmailData((prev) =>
              prev ? { ...prev, pdfUrl: invoice.pdfUrl } : null
            );
            stopPolling();
          }
        } catch (error) {
          console.error('Error polling for PDF:', error);
        }
      };

      // Poll immediately, then every 2 seconds
      pollForPdf();
      pollingIntervalRef.current = setInterval(pollForPdf, 2000);
    },
    [stopPolling]
  );

  const prepareEmailData = useCallback(
    (
      invoiceNumber: string,
      customerName: string,
      customerEmail: string,
      tenant: TenantView,
      pdfUrl?: string
    ) => {
      const emailData: InvoiceEmailData = {
        customerEmail,
        customerName,
        invoiceNumber,
        pdfUrl,
        pdfFileName: `factuur-${invoiceNumber.toLowerCase()}.pdf`,
        tenant,
      };

      setEmailData(emailData);

      // Check if PDF is already ready
      if (pdfUrl) {
        setIsPdfReady(true);
      }

      return emailData;
    },
    []
  );

  const getEmailDefaultValues = useCallback(
    (emailData: InvoiceEmailData | null): EmailComposerValues => {
      return {
        to: emailData?.customerEmail || '',
        bcc: emailData?.tenant.financialEmail || '',
        subject: `Factuur van ${emailData?.tenant.companyName || ''}`,
        body: `Beste,

Hierbij ontvangt u de factuur ${emailData?.invoiceNumber || ''}.

Mocht u vragen hebben over deze factuur, neem dan gerust contact met ons op.

Met vriendelijke groet,
${emailData?.tenant.companyName || ''}`,
      };
    },
    []
  );

  const handleOpenPdfAttachment = useCallback(async (pdfUrl?: string) => {
    if (!pdfUrl) {
      toastService.error('PDF is nog niet beschikbaar');
      return;
    }

    try {
      await openInvoicePdfInNewTab(pdfUrl);
    } catch (error) {
      console.error('Error opening PDF:', error);
      toastService.error('Fout bij het openen van de PDF');
    }
  }, []);

  const cancelEmailStep = useCallback(() => {
    resetEmailState();
  }, [resetEmailState]);

  return {
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
  };
};
