import { weightTicketService } from '@/api/services/weightTicketService';
import { supabase } from '@/api/supabaseClient';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import { Button } from '@/components/ui/button/Button';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ClipLoader } from 'react-spinners';
import { TransportCard } from './TransportCard';

interface WeightTicketRelatedTabProps {
  weightTicketId?: number;
}

export const WeightTicketRelatedTab = ({
  weightTicketId,
}: WeightTicketRelatedTabProps) => {
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: ['weight-ticket-transports', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;

      const response = await weightTicketService.getWasteTransportsByWeightTicketId(weightTicketId);
      return response;
    },
    enabled: !!weightTicketId,
  });

  const { data: weightTicketDetails } = useQuery({
    queryKey: ['weight-ticket-details', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;

      const response = await weightTicketService.getByNumber(weightTicketId);
      return response;
    },
    enabled: !!weightTicketId,
  });

  const handleDownloadPdf = async () => {
    // Type assertion until API client is regenerated with pdfUrl field
    const pdfUrl = (weightTicketDetails as any)?.pdfUrl as string | undefined;
    if (!pdfUrl) return;

    setIsDownloadingPdf(true);
    try {
      // Download PDF from Supabase storage
      const { data, error } = await supabase.storage
        .from('weight-tickets')
        .download(pdfUrl);

      if (error) throw error;

      // Create download link
      const url = URL.createObjectURL(data);
      const link = document.createElement('a');
      link.href = url;
      link.download = pdfUrl.split('/').pop() || 'weegbon.pdf';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading PDF:', error);
    } finally {
      setIsDownloadingPdf(false);
    }
  };

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

  // Type assertion until API client is regenerated with pdfUrl field
  const hasPdf = (weightTicketDetails as any)?.pdfUrl;
  const hasTransports = data && data.transports.length > 0;

  if (!hasPdf && !hasTransports) {
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
      {hasPdf && (
        <div className="flex flex-col items-start gap-3 w-full">
          <div className="flex items-center gap-2">
            <Scale />
            <span className="text-subtitle-1">Weegbon</span>
          </div>
          <Button
            onClick={handleDownloadPdf}
            disabled={isDownloadingPdf}
            variant={"primary"}
            icon={FilePdf}
            label="Download PDF"
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
    </div>
  );
};
