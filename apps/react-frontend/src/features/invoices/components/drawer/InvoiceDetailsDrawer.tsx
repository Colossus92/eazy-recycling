import { useQuery } from '@tanstack/react-query';
import { invoiceService } from '@/api/services/invoiceService';
import { Drawer } from '@/components/ui/drawer/Drawer';
import Hash from '@/assets/icons/Hash.svg?react';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';
import Calendar from '@/assets/icons/CalendarDots.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import CurrencyEur from '@/assets/icons/IcBaselineEuro.svg?react';
import { InvoiceStatusTag } from '../InvoiceStatusTag';

interface InvoiceDetailsDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (value: boolean) => void;
  invoiceId: number | null;
  onEdit?: () => void;
  onDelete?: () => void;
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(value);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('nl-NL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

export const InvoiceDetailsDrawer = ({
  isDrawerOpen,
  setIsDrawerOpen,
  invoiceId,
  onEdit,
  onDelete,
}: InvoiceDetailsDrawerProps) => {
  const { data, isLoading } = useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: async () => {
      if (!invoiceId) return null;
      return await invoiceService.getById(invoiceId).catch((error) => {
        console.error('Error fetching invoice:', error);
        return null;
      });
    },
    enabled: isDrawerOpen && !!invoiceId,
  });

  const isFinal = data?.status === 'FINAL';

  return (
    <Drawer
      title={'Factuur details'}
      isOpen={isDrawerOpen}
      setIsOpen={setIsDrawerOpen}
      onEdit={isFinal ? undefined : onEdit}
      onDelete={isFinal ? undefined : onDelete}
    >
      {isLoading && <div>Factuur laden...</div>}
      {!isLoading && data && (
        <div
          className={'flex flex-col flex-1 self-stretch items-start p-4 gap-6'}
          data-testid="invoice-details-drawer-content"
        >
          {/* Header Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <div
              className={
                'flex flex-col justify-center items-start gap-1 self-stretch'
              }
            >
              <h4>
                {data.invoiceNumber ? `#${data.invoiceNumber}` : 'Concept'}
              </h4>
              <span className="text-subtitle-2 text-color-text-secondary">
                {data.customer.name}
              </span>
            </div>

            {/* Details Grid */}
            <div className={'flex flex-col items-start self-stretch gap-2'}>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CheckCircle
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Status
                  </span>
                </div>
                <InvoiceStatusTag status={data.status} />
              </div>
              {data.invoiceNumber && (
                <div className={'flex items-center gap-2 self-stretch'}>
                  <div className="flex items-center flex-1 gap-2">
                    <Hash className={'w-5 h-5 text-color-text-secondary'} />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Factuurnummer
                    </span>
                  </div>
                  <span className={'text-body-2 truncate'}>
                    {data.invoiceNumber}
                  </span>
                </div>
              )}
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Calendar className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Factuurdatum
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {formatDate(data.invoiceDate)}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <BuildingOffice
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Klant
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {data.customer.name}
                </span>
              </div>
            </div>
          </div>

          {/* Totals Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <span className={'text-subtitle-1'}>Bedragen</span>
            <div className={'flex flex-col items-start self-stretch gap-2'}>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CurrencyEur
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Subtotaal (excl. BTW)
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {formatCurrency(data.totals.totalExclVat)}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CurrencyEur
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    BTW
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {formatCurrency(data.totals.totalVat)}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <CurrencyEur
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Totaal (incl. BTW)
                  </span>
                </div>
                <span className={'text-body-2 font-semibold truncate'}>
                  {formatCurrency(data.totals.totalInclVat)}
                </span>
              </div>
            </div>
          </div>

          {/* Lines Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <span className={'text-subtitle-1'}>
              Regels ({data.lines.length})
            </span>
            <div className={'flex flex-col items-start self-stretch gap-2'}>
              {data.lines.map((line, index) => (
                <div
                  key={index}
                  className={
                    'flex items-center gap-2 self-stretch p-2 bg-color-surface-secondary rounded-radius-md'
                  }
                >
                  <div className="flex flex-col flex-1 gap-1">
                    <span className={'text-body-2'}>{line.catalogItemName}</span>
                    <span className={'text-caption text-color-text-secondary'}>
                      {line.quantity} x {formatCurrency(line.unitPrice)}
                    </span>
                  </div>
                  <span className={'text-body-2 font-semibold'}>
                    {formatCurrency(line.totalExclVat)}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </Drawer>
  );
};
