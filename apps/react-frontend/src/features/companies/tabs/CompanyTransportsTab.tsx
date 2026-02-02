import { TransportByCompanyView } from '@/api/client';
import { companyService } from '@/api/services/companyService';
import { TransportStatusTag } from '@/features/planning/components/tag/TransportStatusTag';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

interface CompanyTransportsTabProps {
  companyId: string;
}

export const CompanyTransportsTab = ({
  companyId,
}: CompanyTransportsTabProps) => {
  const navigate = useNavigate();
  const { data, isLoading, error } = useQuery({
    queryKey: ['company-transports', companyId],
    queryFn: () => companyService.getTransportsByCompany(companyId),
    enabled: !!companyId,
  });

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-color-brand-primary"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 text-color-status-error-primary">
        Er is een fout opgetreden bij het laden van transporten.
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="p-4 text-color-text-secondary">
        Geen transporten gevonden voor dit bedrijf.
      </div>
    );
  }

  return (
    <div className="w-full overflow-x-auto px-4">
      <table className="w-full table-fixed">
        <thead>
          <tr className="border-b border-color-border-primary">
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Nummer
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Datum
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Ophaallocatie
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Status
            </th>
          </tr>
        </thead>
        <tbody>
          {data.map((transport: TransportByCompanyView) => (
            <tr
              key={transport.id}
              className="border-b border-color-border-primary hover:bg-color-surface-secondary"
            >
              <td className="p-3">
                <button
                  type="button"
                  onClick={() => {
                    const dateParam =
                      transport.date || new Date().toISOString().split('T')[0];
                    navigate(
                      `/?highlightTransportId=${transport.id}&date=${dateParam}`
                    );
                  }}
                  className="text-color-brand-primary hover:underline cursor-pointer bg-transparent border-none p-0"
                >
                  {transport.displayNumber || transport.id}
                </button>
              </td>
              <td className="p-3 text-body-2">{transport.date || '-'}</td>
              <td className="p-3 text-body-2">
                {transport.pickupLocation || '-'}
              </td>
              <td className="p-3">
                <TransportStatusTag
                  status={
                    transport.status as
                      | 'INVOICED'
                      | 'FINISHED'
                      | 'UNPLANNED'
                      | 'ERROR'
                      | 'PLANNED'
                  }
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
