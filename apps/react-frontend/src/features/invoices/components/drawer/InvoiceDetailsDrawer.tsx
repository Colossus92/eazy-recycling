import { invoiceService } from '@/api/services/invoiceService';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import Calendar from '@/assets/icons/CalendarDots.svg?react';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';
import Hash from '@/assets/icons/Hash.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import Receipt from '@/assets/icons/Receipt.svg?react';
import { Drawer } from '@/components/ui/drawer/Drawer';
import { DocumentsSection } from '@/features/planning/components/drawer/DocumentsSection';
import { WeightTicketDownloadSection } from '@/features/planning/components/drawer/WeightTicketDownloadSection';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { InvoiceStatusTag } from '../InvoiceStatusTag';
import { InvoiceDocumentSection } from './InvoiceDocumentSection';

interface InvoiceDetailsDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (value: boolean) => void;
  invoiceId: string | null;
  onEdit?: () => void;
  onDelete?: () => void;
}

const formatCurrency = (value: number, isCreditNote: boolean = false) => {
  // For credit notes, show negative values (except 0,00)
  const displayValue = isCreditNote && value !== 0 ? -value : value;
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(displayValue);
};

const getInvoiceTypeLabel = (
  invoiceType: string,
  documentType: string
): string => {
  if (documentType === 'CREDIT_NOTE') {
    return 'Credit';
  }
  return invoiceType === 'PURCHASE' ? 'Inkoop' : 'Verkoop';
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
  const navigate = useNavigate();

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

  const sourceWeightTicketId = data?.sourceWeightTicketId;

  const isFinal = data?.status === 'FINAL';
  const isCreditNote = data?.documentType === 'CREDIT_NOTE';

  return (
    <Drawer
      title={'Factuur details'}
      isOpen={isDrawerOpen}
      setIsOpen={setIsDrawerOpen}
      onEdit={onEdit}
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
                <InvoiceStatusTag status={data.status as 'DRAFT' | 'FINAL'} />
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Receipt className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Type
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {getInvoiceTypeLabel(data.invoiceType, data.documentType)}
                </span>
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
              {sourceWeightTicketId && (
                <div
                  className={
                    'flex items-center gap-2 self-stretch cursor-pointer hover:bg-color-surface-secondary rounded-radius-md -mx-2 px-2 py-1'
                  }
                  onClick={() => {
                    setIsDrawerOpen(false);
                    navigate(
                      `/weight-tickets?weightTicketDrawerId=${sourceWeightTicketId}`
                    );
                  }}
                >
                  <div className="flex items-center flex-1 gap-2">
                    <Scale className={'w-5 h-5 text-color-text-secondary'} />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Weegbon
                    </span>
                  </div>
                  <span
                    className={
                      'text-body-2 truncate text-color-brand-primary underline'
                    }
                  >
                    {sourceWeightTicketId}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Receipt Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <div className="flex items-center gap-2">
              <span className={'text-subtitle-1'}>Overzicht</span>
            </div>
            <div
              className={
                'flex flex-col items-start self-stretch border border-solid border-color-border-primary rounded-radius-md overflow-hidden'
              }
            >
              {/* Invoice Lines */}
              {data.lines.map((line, index) => (
                <div
                  key={index}
                  className={
                    'flex items-center justify-between gap-4 self-stretch px-3 py-2 border-b border-solid border-color-border-primary last:border-b-0'
                  }
                >
                  <div className="flex flex-col flex-1 gap-0.5">
                    <span className={'text-body-2'}>
                      {line.catalogItemName}
                    </span>
                    <span className={'text-caption text-color-text-secondary'}>
                      {line.quantity} x {formatCurrency(line.unitPrice)}
                    </span>
                  </div>
                  <span className={'text-body-2'}>
                    {formatCurrency(line.totalExclVat, isCreditNote)}
                  </span>
                </div>
              ))}
              {/* Totals */}
              <div
                className={
                  'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary'
                }
              >
                <span className={'text-body-2 text-color-text-secondary'}>
                  Subtotaal
                </span>
                <span className={'text-body-2'}>
                  {formatCurrency(data.totals.totalExclVat, isCreditNote)}
                </span>
              </div>
              <div
                className={
                  'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary'
                }
              >
                <span className={'text-body-2 text-color-text-secondary'}>
                  BTW
                </span>
                <span className={'text-body-2'}>
                  {formatCurrency(data.totals.totalVat, isCreditNote)}
                </span>
              </div>
              <div
                className={
                  'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary border-t border-solid border-color-border-primary'
                }
              >
                <span className={'text-body-2 font-semibold'}>Totaal</span>
                <span className={'text-body-2 font-semibold'}>
                  {formatCurrency(data.totals.totalInclVat, isCreditNote)}
                </span>
              </div>
            </div>
          </div>

          {/* Documents Section */}
          <DocumentsSection>
            {data.pdfUrl && (
              <InvoiceDocumentSection
                pdfUrl={data.pdfUrl}
                invoiceNumber={data.invoiceNumber || 'Concept'}
              />
            )}
            <WeightTicketDownloadSection
              weightTicketId={sourceWeightTicketId}
            />
          </DocumentsSection>
        </div>
      )}
    </Drawer>
  );
};
