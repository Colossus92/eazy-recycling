import { useState, useEffect } from 'react';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import {
  fetchInvoicePdfInfo,
  downloadInvoicePdf,
  InvoicePdfInfo,
} from '@/api/services/invoicePdfService';

interface InvoiceDocumentSectionProps {
  pdfUrl?: string;
  invoiceNumber?: string;
}

export const InvoiceDocumentSection = ({
  pdfUrl,
  invoiceNumber,
}: InvoiceDocumentSectionProps) => {
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState(false);
  const [invoicePdfInfo, setInvoicePdfInfo] = useState<InvoicePdfInfo | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadInvoicePdfInfo = async () => {
      if (!pdfUrl) {
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const info = await fetchInvoicePdfInfo(pdfUrl);
        setInvoicePdfInfo(info);
      } catch (error: any) {
        setError(error.message || 'Onverwachte fout bij het ophalen van factuur informatie');
      } finally {
        setIsLoading(false);
      }
    };

    loadInvoicePdfInfo();
  }, [pdfUrl]);

  const handleDirectDownload = () => {
    if (!invoicePdfInfo || isDownloading || !invoiceNumber) return;

    setIsDownloading(true);
    downloadInvoicePdf(invoicePdfInfo, invoiceNumber);

    // Reset downloading state after a short delay
    setTimeout(() => setIsDownloading(false), 1000);
  };

  if (!pdfUrl) {
    return null;
  }

  return isLoading ? (
    <span className={'text-body-2 text-color-text-secondary'}>
      Factuur laden...
    </span>
  ) : error ? (
    <span className={'text-body-2 text-red-600'}>{error}</span>
  ) : !invoicePdfInfo ? (
    <span className={'text-body-2 text-color-text-secondary'}>
      Geen factuur beschikbaar
    </span>
  ) : (
    <button
      onClick={handleDirectDownload}
      disabled={isDownloading}
      className="flex items-center self-stretch flex-1 gap-2 py-2 pl-2 pr-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary hover:bg-color-surface-secondary disabled:cursor-not-allowed transition-colors"
      data-testid="invoice-pdf-download-button"
    >
      <div className="flex size-10 justify-center items-center border border-solid border-color-border-primary rounded-radius-sm bg-color-surface-secondary">
        <FilePdf className={'size-[25px] text-color-text-secondary'} />
      </div>
      <div className="flex flex-col items-start gap-1 flex-1">
        <div className="flex items-center justify-between self-stretch">
          <span className={'text-subtitle-2 text-color-text-primary'}>
            Factuur PDF
          </span>
          <span className={'text-body-2 text-color-text-secondary'}>
            {invoicePdfInfo.timestamp}
          </span>
        </div>
        <div className="flex items-center justify-between self-stretch">
          <span className={'text-body-2 text-color-text-secondary'}>
            {isDownloading ? 'Factuur downloaden...' : `${invoicePdfInfo.fileSizeKb} KB`}
          </span>
        </div>
      </div>
    </button>
  );
};
