import { WasteStreamByCompanyView } from '@/api/client';
import { companyService } from '@/api/services/companyService';
import { WasteStreamStatusTag } from '@/features/wastestreams/components/WasteStreamStatusTag';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

interface CompanyWasteStreamsTabProps {
  companyId: string;
}

export const CompanyWasteStreamsTab = ({
  companyId,
}: CompanyWasteStreamsTabProps) => {
  const navigate = useNavigate();
  const { data, isLoading, error } = useQuery({
    queryKey: ['company-waste-streams', companyId],
    queryFn: () => companyService.getWasteStreamsByCompany(companyId),
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
        Er is een fout opgetreden bij het laden van afvalstromen.
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="p-4 text-color-text-secondary">
        Geen afvalstromen gevonden voor dit bedrijf.
      </div>
    );
  }

  return (
    <div className="w-full overflow-x-auto px-4">
      <table className="w-full table-fixed">
        <thead>
          <tr className="border-b border-color-border-primary">
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Afvalstroomnummer
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Gebruikelijke benaming
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Herkomstlocatie
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Status
            </th>
          </tr>
        </thead>
        <tbody>
          {data.map((wasteStream: WasteStreamByCompanyView) => (
            <tr
              key={wasteStream.wasteStreamNumber}
              className="border-b border-color-border-primary hover:bg-color-surface-secondary"
            >
              <td className="p-3">
                <button
                  type="button"
                  onClick={() =>
                    navigate(
                      `/waste-streams?wasteStreamDrawerId=${wasteStream.wasteStreamNumber}`
                    )
                  }
                  className="text-color-brand-primary hover:underline cursor-pointer bg-transparent border-none p-0"
                >
                  {wasteStream.wasteStreamNumber}
                </button>
              </td>
              <td className="p-3 text-body-2">{wasteStream.wasteName}</td>
              <td className="p-3 text-body-2">{wasteStream.pickupLocation}</td>
              <td className="p-3">
                <WasteStreamStatusTag
                  status={
                    wasteStream.status as
                      | 'DRAFT'
                      | 'ACTIVE'
                      | 'INACTIVE'
                      | 'EXPIRED'
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
