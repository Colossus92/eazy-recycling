import { useQuery } from '@tanstack/react-query';
import { WasteTransportControllerApi } from '@/api/client/apis/waste-transport-controller-api';
import { WeightTicketControllerApi } from '@/api/client/apis/weight-ticket-controller-api';
import { Configuration } from '@/api/client/configuration';
import { supabase } from '@/api/supabaseClient';
import { TransportCard } from './TransportCard';
import { ClipLoader } from 'react-spinners';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import FilePdf from '@/assets/icons/FilePdf.svg?react';
import { useState } from 'react';
import { Button } from '@/components/ui/button/Button';

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

      const { data: sessionData } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token || '';
      
      const config = new Configuration({
        basePath: import.meta.env.VITE_BACKEND_URL,
        accessToken,
      });

      const api = new WasteTransportControllerApi(config);
      const response = await api.getWasteTransportsByWeightTicketId(weightTicketId);
      return response.data;
    },
    enabled: !!weightTicketId,
  });

  const { data: weightTicketDetails } = useQuery({
    queryKey: ['weight-ticket-details', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;

      const { data: sessionData } = await supabase.auth.getSession();
      const accessToken = sessionData.session?.access_token || '';
      
      const config = new Configuration({
        basePath: import.meta.env.VITE_BACKEND_URL,
        accessToken,
      });

      const api = new WeightTicketControllerApi(config);
      const response = await api.getWeightTicketByNumber(weightTicketId);
      return response.data;
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
