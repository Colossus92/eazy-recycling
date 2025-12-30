import { weightTicketService } from '@/api/services/weightTicketService';
import Scale from '@/assets/icons/Scale.svg?react';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import IcBaselineEuro from '@/assets/icons/IcBaselineEuro.svg?react';
import { useQuery } from '@tanstack/react-query';
import { ClipLoader } from 'react-spinners';
import { TransportCard } from './TransportCard';
import { invoiceService } from '@/api/services/invoiceService';
import { InvoiceCard } from '@/features/invoices/components/InvoiceCard';
import { WeightTicketCard } from '@/features/weighttickets/components/WeightTicketCard';
import { useEffect } from 'react';

interface WeightTicketRelatedTabProps {
  weightTicketId?: number;
}

export const WeightTicketRelatedTab = ({
  weightTicketId,
}: WeightTicketRelatedTabProps) => {

  const { data, isLoading, error } = useQuery({
    queryKey: ['weight-ticket-transports', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;

      const response = await weightTicketService.getWasteTransportsByWeightTicketId(weightTicketId);
      return response;
    },
    enabled: !!weightTicketId,
  });

  const { data: weightTicketDetails, refetch: refetchWeightTicketDetails } = useQuery({
    queryKey: ['weight-ticket-details', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;

      const response = await weightTicketService.getByNumber(weightTicketId);
      return response;
    },
    enabled: !!weightTicketId,
  });

  // Poll for new documents every 5 seconds when not in DRAFT state
  useEffect(() => {
    if (!weightTicketDetails || weightTicketDetails.status === 'DRAFT') {
      return;
    }

    const intervalId = setInterval(() => {
      refetchWeightTicketDetails();
    }, 5000);

    return () => clearInterval(intervalId);
  }, [weightTicketDetails, refetchWeightTicketDetails]);

  const linkedInvoiceId = weightTicketDetails?.linkedInvoiceId;

  // Fetch linked invoice details if available - MUST be called before any early returns
  const { data: linkedInvoice } = useQuery({
    queryKey: ['linked-invoice', linkedInvoiceId],
    queryFn: async () => {
      if (!linkedInvoiceId) return null;
      const response = await invoiceService.getById(linkedInvoiceId);
      return response;
    },
    enabled: !!linkedInvoiceId,
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
          Fout bij het laden van gerelateerde transporten
        </span>
      </div>
    );
  }

  const hasPdf = weightTicketDetails?.pdfUrl;
  const hasTransports = data && data.transports.length > 0;

  const hasLinkedInvoice = !!linkedInvoiceId;

  if (!hasPdf && !hasTransports && !hasLinkedInvoice) {
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
      {/* PDF Section */}
      {hasPdf && weightTicketDetails && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <Scale />
            <span className="text-subtitle-1">Weegbon</span>
          </div>
          <WeightTicketCard
            weightTicketId={weightTicketDetails.id}
            createdAt={typeof weightTicketDetails.createdAt === 'object' && weightTicketDetails.createdAt !== null 
              ? (weightTicketDetails.createdAt as any).value$kotlinx_datetime 
              : weightTicketDetails.createdAt || ''}
            status={weightTicketDetails.status as 'DRAFT' | 'COMPLETED' | 'INVOICED' | 'CANCELLED'}
            pdfUrl={weightTicketDetails.pdfUrl ?? null}
          />
        </div>
      )}

      {/* Transports Section */}
      {hasTransports && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <TruckTrailer />
            <span className="text-subtitle-1">Transporten</span>
          </div>
          <div className="flex flex-col items-start self-stretch gap-2">
            {data.transports.map((transport) => (
              <TransportCard
                key={transport.transportId}
                transportId={transport.transportId}
                displayNumber={transport.displayNumber}
                pickupDateTime={transport.pickupDateTime}
                status={transport.status}
              />
            ))}
          </div>
        </div>
      )}

      {/* Linked Invoice Section */}
      {hasLinkedInvoice && linkedInvoice && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <IcBaselineEuro />
            <span className="text-subtitle-1">Factuur</span>
          </div>
          <InvoiceCard
            invoiceId={linkedInvoice.id}
            invoiceNumber={linkedInvoice.invoiceNumber ?? null}
            invoiceDate={linkedInvoice.invoiceDate}
            status={linkedInvoice.status as 'DRAFT' | 'FINAL'}
            pdfUrl={linkedInvoice.pdfUrl ?? null}
          />
        </div>
      )}
    </div>
  );
};
