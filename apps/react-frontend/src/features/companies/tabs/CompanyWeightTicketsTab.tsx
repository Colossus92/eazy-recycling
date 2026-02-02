import { WeightTicketByCompanyView } from '@/api/client';
import { companyService } from '@/api/services/companyService';
import { WeightTicketStatusTag } from '@/features/weighttickets/components/WeightTicketStatusTag';
import { formatInstantInCET } from '@/utils/dateUtils';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

interface CompanyWeightTicketsTabProps {
  companyId: string;
}

export const CompanyWeightTicketsTab = ({
  companyId,
}: CompanyWeightTicketsTabProps) => {
  const navigate = useNavigate();
  const { data, isLoading, error } = useQuery({
    queryKey: ['company-weight-tickets', companyId],
    queryFn: () => companyService.getWeightTicketsByCompany(companyId),
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
        Er is een fout opgetreden bij het laden van weegbonnen.
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="p-4 text-color-text-secondary">
        Geen weegbonnen gevonden voor dit bedrijf.
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
              Totaal gewicht
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Weegdatum
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
          {data.map((weightTicket: WeightTicketByCompanyView) => (
            <tr
              key={weightTicket.id}
              className="border-b border-color-border-primary hover:bg-color-surface-secondary"
            >
              <td className="p-3">
                <button
                  type="button"
                  onClick={() =>
                    navigate(
                      `/weight-tickets?weightTicketDrawerId=${weightTicket.id}`
                    )
                  }
                  className="text-color-brand-primary hover:underline cursor-pointer bg-transparent border-none p-0"
                >
                  {weightTicket.id}
                </button>
              </td>
              <td className="p-3 text-body-2">
                {weightTicket.totalWeight != null
                  ? `${weightTicket.totalWeight.toFixed(2)} kg`
                  : '-'}
              </td>
              <td className="p-3 text-body-2">
                {weightTicket.weighingDate
                  ? formatInstantInCET(weightTicket.weighingDate)
                  : '-'}
              </td>
              <td className="p-3 text-body-2">
                {weightTicket.pickupLocation || '-'}
              </td>
              <td className="p-3">
                <WeightTicketStatusTag
                  status={
                    weightTicket.status as
                      | 'DRAFT'
                      | 'COMPLETED'
                      | 'INVOICED'
                      | 'CANCELLED'
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
