import { useNavigate } from 'react-router-dom';
import { formatInstantInCET } from '@/utils/dateUtils';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import EyeSolid from '@/assets/icons/EyeSolid.svg?react';
import { Button } from '@/components/ui/button/Button';
import { InvoiceStatusTag } from './InvoiceStatusTag';
import { useState } from 'react';
import { openInvoicePdfInNewTab } from '@/api/services/invoicePdfService';

interface InvoiceCardProps {
  invoiceId: string;
  invoiceNumber: string | null;
  invoiceDate: string;
  status: 'DRAFT' | 'FINAL';
  pdfUrl?: string | null;
}

export const InvoiceCard = ({
  invoiceId,
  invoiceNumber,
  invoiceDate,
  status,
  pdfUrl,
}: InvoiceCardProps) => {
  const navigate = useNavigate();
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  const handlePdfClick = async () => {
    if (!pdfUrl || isDownloadingPdf) return;

    setIsDownloadingPdf(true);
    try {
      await openInvoicePdfInNewTab(pdfUrl);
    } catch (error) {
      console.error('Error opening invoice PDF:', error);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  const handleViewDetails = () => {
    navigate(`/financials?invoiceId=${invoiceId}`);
  };

  return (
    <div className="flex items-center gap-10 p-3 border border-solid border-color-border-primary rounded-radius-md bg-color-surface-primary w-full">
      <div className="flex flex-col justify-center items-start gap-1 flex-1">
        <InvoiceStatusTag status={status} />
        <span className="text-subtitle-2 text-color-text-secondary">
          {invoiceNumber ? `#${invoiceNumber}` : `#-`}
        </span>
        <span className="text-body-2 text-color-text-secondary">
          Factuurdatum: {formatInstantInCET(invoiceDate, 'dd-MM-yyyy')}
        </span>
      </div>
      <div className="flex items-center gap-2">
        {pdfUrl && (
          <Button
            variant="icon"
            icon={FilePdf}
            onClick={handlePdfClick}
            disabled={isDownloadingPdf}
            title="Download factuur PDF"
            showText={false}
            type="button"
          />
        )}
        <Button
          variant="icon"
          icon={EyeSolid}
          onClick={handleViewDetails}
          title="Bekijk factuur"
          showText={false}
          type="button"
        />
      </div>
    </div>
  );
};
