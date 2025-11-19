import { useQuery } from '@tanstack/react-query';
import { WasteTransportControllerApi } from '@/api/client/apis/waste-transport-controller-api';
import { Configuration } from '@/api/client/configuration';
import { supabase } from '@/api/supabaseClient';
import { TransportCard } from './TransportCard';
import { ClipLoader } from 'react-spinners';
import TruckTrailer from '@/assets/icons/TruckTrailer.svg?react';

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

  if (!data || data.transports.length === 0) {
    return (
      <div className="flex justify-center items-center p-8">
        <span className="text-body-2 text-color-text-secondary">
          Geen gerelateerde transporten gevonden
        </span>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-start gap-3">
      <div className="flex items-center gap-2"><TruckTrailer /><span className="text-subtitle-1">Transporten</span></div>
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
  );
};
