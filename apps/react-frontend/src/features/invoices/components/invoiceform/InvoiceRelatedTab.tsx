import { invoiceService } from '@/api/services/invoiceService';
import { weightTicketService } from '@/api/services/weightTicketService';
import IcBaselineEuro from '@/assets/icons/IcBaselineEuro.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import { useQuery } from '@tanstack/react-query';
import { ClipLoader } from 'react-spinners';
import { InvoiceCard } from '@/features/invoices/components/InvoiceCard';
import { WeightTicketCard } from '@/features/weighttickets/components/WeightTicketCard';
import { useEffect } from 'react';

interface InvoiceRelatedTabProps {
  invoiceId?: string;
}

export const InvoiceRelatedTab = ({ invoiceId }: InvoiceRelatedTabProps) => {
  const {
    data: invoiceDetails,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ['invoice-details', invoiceId],
    queryFn: async () => {
      if (!invoiceId) return null;
      const response = await invoiceService.getById(invoiceId);
      return response;
    },
    enabled: !!invoiceId,
  });

  // Poll for new documents every 5 seconds when not in DRAFT state
  useEffect(() => {
    if (!invoiceDetails || invoiceDetails.status === 'DRAFT') {
      return;
    }

    const intervalId = setInterval(() => {
      refetch();
    }, 5000);

    return () => clearInterval(intervalId);
  }, [invoiceDetails, refetch]);

  const sourceWeightTicketId = invoiceDetails?.sourceWeightTicketId;

  // Fetch source weight ticket details if available - MUST be called before any early returns
  const { data: sourceWeightTicket } = useQuery({
    queryKey: ['source-weight-ticket', sourceWeightTicketId],
    queryFn: async () => {
      if (!sourceWeightTicketId) return null;
      const response =
        await weightTicketService.getByNumber(sourceWeightTicketId);
      return response;
    },
    enabled: !!sourceWeightTicketId,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <ClipLoader
          size={20}
          color={'text-color-text-invert-primary'}
          aria-label="Laad spinner"
        />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex justify-center items-center p-8">
        <span className="text-body-2 text-color-error">
          Fout bij het laden van gerelateerde informatie
        </span>
      </div>
    );
  }

  const hasInvoicePdf = invoiceDetails?.pdfUrl;
  const hasSourceWeightTicket = !!sourceWeightTicketId;

  if (!hasInvoicePdf && !hasSourceWeightTicket) {
    return (
      <div className="flex justify-center items-center p-8">
        <span className="text-body-2 text-color-text-secondary">
          Geen gerelateerde informatie gevonden
        </span>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-start gap-6">
      {/* Invoice PDF Section */}
      {hasInvoicePdf && invoiceDetails && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <IcBaselineEuro />
            <span className="text-subtitle-1">Factuur</span>
          </div>
          <InvoiceCard
            invoiceId={invoiceDetails.id}
            invoiceNumber={invoiceDetails.invoiceNumber ?? null}
            invoiceDate={invoiceDetails.invoiceDate}
            status={invoiceDetails.status as 'DRAFT' | 'FINAL'}
            pdfUrl={invoiceDetails.pdfUrl ?? null}
          />
        </div>
      )}

      {/* Source Weight Ticket Section */}
      {hasSourceWeightTicket && sourceWeightTicket && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <Scale />
            <span className="text-subtitle-1">Weegbon</span>
          </div>
          <WeightTicketCard
            weightTicketId={sourceWeightTicket.id}
            createdAt={
              typeof sourceWeightTicket.createdAt === 'object' &&
              sourceWeightTicket.createdAt !== null
                ? (sourceWeightTicket.createdAt as any).value$kotlinx_datetime
                : sourceWeightTicket.createdAt || ''
            }
            status={
              sourceWeightTicket.status as
                | 'DRAFT'
                | 'COMPLETED'
                | 'INVOICED'
                | 'CANCELLED'
            }
            pdfUrl={sourceWeightTicket.pdfUrl ?? null}
          />
        </div>
      )}
    </div>
  );
};
