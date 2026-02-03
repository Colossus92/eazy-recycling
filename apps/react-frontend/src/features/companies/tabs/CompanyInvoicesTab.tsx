import { InvoiceByCompanyView } from '@/api/client';
import { companyService } from '@/api/services/companyService';
import { InvoiceStatusTag } from '@/features/invoices/components/InvoiceStatusTag';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

interface CompanyInvoicesTabProps {
  companyId: string;
}

export const CompanyInvoicesTab = ({ companyId }: CompanyInvoicesTabProps) => {
  const navigate = useNavigate();
  const { data, isLoading, error } = useQuery({
    queryKey: ['company-invoices', companyId],
    queryFn: () => companyService.getInvoicesByCompany(companyId),
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
        Er is een fout opgetreden bij het laden van facturen.
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="p-4 text-color-text-secondary">
        Geen facturen gevonden voor dit bedrijf.
      </div>
    );
  }

  return (
    <div className="w-full overflow-x-auto px-4">
      <table className="w-full table-fixed">
        <thead>
          <tr className="border-b border-color-border-primary">
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Factuurnummer
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Type
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Totaal
            </th>
            <th className="text-left p-3 text-body-2 font-semibold text-color-text-secondary">
              Status
            </th>
          </tr>
        </thead>
        <tbody>
          {data.map((invoice: InvoiceByCompanyView) => (
            <tr
              key={invoice.id}
              className="border-b border-color-border-primary hover:bg-color-surface-secondary"
            >
              <td className="p-3">
                <button
                  type="button"
                  onClick={() =>
                    navigate(`/financials?invoiceDrawerId=${invoice.id}`)
                  }
                  className="text-color-brand-primary hover:underline cursor-pointer bg-transparent border-none p-0"
                >
                  {invoice.invoiceNumber || invoice.id}
                </button>
              </td>
              <td className="p-3 text-body-2">{invoice.invoiceType}</td>
              <td className="p-3 text-body-2">
                €{' '}
                {invoice.totalInclVat != null
                  ? Number(invoice.totalInclVat).toFixed(2)
                  : '0.00'}
              </td>
              <td className="p-3">
                <InvoiceStatusTag
                  status={invoice.status as 'DRAFT' | 'FINAL'}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
