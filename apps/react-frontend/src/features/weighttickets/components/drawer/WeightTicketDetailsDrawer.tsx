import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { weightTicketService } from '@/api/services/weightTicketService';
import { invoiceService } from '@/api/services/invoiceService';
import { resolveLocationAddress } from '@/api/services/transportService';
import { Drawer } from '@/components/ui/drawer/Drawer';
import Hash from '@/assets/icons/Hash.svg?react';
import CheckCircle from '@/assets/icons/CheckCircleOutline.svg?react';
import Calendar from '@/assets/icons/CalendarDots.svg?react';
import BuildingOffice from '@/assets/icons/BuildingOffice.svg?react';
import Truck from '@/assets/icons/TruckTrailer.svg?react';
import CurrencyEur from '@/assets/icons/IcBaselineEuro.svg?react';
import Ellipse from '@/assets/icons/Ellipse.svg?react';
import MapPin from '@/assets/icons/MapPin.svg?react';
import DottedStroke from '@/assets/icons/DottedStroke.svg?react';
import { WeightTicketStatusTag } from '../WeightTicketStatusTag';
import { CompanyView, WeightTicketDetailViewConsignorParty } from '@/api/client/models';
import { DocumentsSection } from '@/features/planning/components/drawer/DocumentsSection';
import { WeightTicketDownloadSection } from '@/features/planning/components/drawer/WeightTicketDownloadSection';
import { WaybillDownloadSection } from '@/features/planning/components/drawer/WaybillDownloadSection';
import { InvoiceDocumentSection } from '@/features/invoices/components/drawer/InvoiceDocumentSection';
import { CompanyCard } from '@/components/ui/company/CompanyCard';
import { Note } from '@/features/planning/components/note/Note';

interface WeightTicketDetailsDrawerProps {
  isDrawerOpen: boolean;
  setIsDrawerOpen: (value: boolean) => void;
  weightTicketId: number | null;
  onEdit?: () => void;
  onDelete?: () => void;
}

const formatDate = (dateString: string | { value$kotlinx_datetime?: string } | undefined) => {
  if (!dateString) return '-';
  const dateValue = typeof dateString === 'string' ? dateString : dateString.value$kotlinx_datetime;
  if (!dateValue) return '-';
  return new Date(dateValue).toLocaleDateString('nl-NL', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const formatWeight = (value: number | undefined, unit: string | undefined) => {
  if (value === undefined) return '-';
  return `${value} ${unit || 'kg'}`;
};

const isCompanyConsignor = (
  consignorParty: WeightTicketDetailViewConsignorParty
): consignorParty is WeightTicketDetailViewConsignorParty & {
  type: 'company';
  company: CompanyView;
} => {
  return (consignorParty as any).type === 'company';
};

const getConsignorName = (
  consignorParty: WeightTicketDetailViewConsignorParty | undefined
): string | undefined => {
  if (!consignorParty) return undefined;
  if (isCompanyConsignor(consignorParty)) {
    return (consignorParty as any).company?.name;
  }
  return undefined;
};

export const WeightTicketDetailsDrawer = ({
  isDrawerOpen,
  setIsDrawerOpen,
  weightTicketId,
  onEdit,
  onDelete,
}: WeightTicketDetailsDrawerProps) => {
  const navigate = useNavigate();

  const { data, isLoading } = useQuery({
    queryKey: ['weight-ticket-details', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;
      return await weightTicketService.getByNumber(weightTicketId).catch((error) => {
        console.error('Error fetching weight ticket:', error);
        return null;
      });
    },
    enabled: isDrawerOpen && !!weightTicketId,
  });

  const linkedInvoiceId = data?.linkedInvoiceId;

  const { data: linkedInvoice } = useQuery({
    queryKey: ['linked-invoice', linkedInvoiceId],
    queryFn: async () => {
      if (!linkedInvoiceId) return null;
      return await invoiceService.getById(linkedInvoiceId);
    },
    enabled: !!linkedInvoiceId,
  });

  const { data: linkedTransports } = useQuery({
    queryKey: ['linked-transports', weightTicketId],
    queryFn: async () => {
      if (!weightTicketId) return null;
      return await weightTicketService.getWasteTransportsByWeightTicketId(weightTicketId);
    },
    enabled: !!weightTicketId,
  });

  const linkedTransport = linkedTransports?.transports?.[0];

  const isDraft = data?.status === 'DRAFT';

  const handleTransportClick = () => {
    if (linkedTransport) {
      // Navigate to planning page with transport selected
      const pickupDate = linkedTransport.pickupDateTime
        ? new Date(linkedTransport.pickupDateTime).toISOString().split('T')[0]
        : new Date().toISOString().split('T')[0];
      navigate(`/?transportId=${linkedTransport.transportId}&date=${pickupDate}`);
    }
  };

  const handleInvoiceClick = () => {
    if (linkedInvoiceId) {
      // Close current drawer and navigate to financials with drawer open
      setIsDrawerOpen(false);
      navigate(`/financials?invoiceDrawerId=${linkedInvoiceId}`);
    }
  };

  const totalWeight = data?.lines?.reduce((sum, line) => sum + (line.weightValue || 0), 0) || 0;

  return (
    <Drawer
      title={'Weegbon details'}
      isOpen={isDrawerOpen}
      setIsOpen={setIsDrawerOpen}
      onEdit={onEdit}
      onDelete={isDraft ? onDelete : undefined}
    >
      {isLoading && <div>Weegbon laden...</div>}
      {!isLoading && data && (
        <div
          className={'flex flex-col flex-1 self-stretch items-start p-4 gap-6'}
          data-testid="weight-ticket-details-drawer-content"
        >
          {/* Header Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <div
              className={
                'flex flex-col justify-center items-start gap-1 self-stretch'
              }
            >
              <h4>#{data.id}</h4>
              <span className="text-subtitle-2 text-color-text-secondary">
                {getConsignorName(data.consignorParty) || '-'}
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
                <WeightTicketStatusTag status={data.status as 'DRAFT' | 'COMPLETED' | 'INVOICED' | 'CANCELLED'} />
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Hash className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Weegbonnummer
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {data.id}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <Calendar className={'w-5 h-5 text-color-text-secondary'} />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Datum
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {formatDate(data.weightedAt || data.createdAt)}
                </span>
              </div>
              <div className={'flex items-center gap-2 self-stretch'}>
                <div className="flex items-center flex-1 gap-2">
                  <BuildingOffice
                    className={'w-5 h-5 text-color-text-secondary'}
                  />
                  <span className={'text-body-2 text-color-text-secondary'}>
                    Afzender
                  </span>
                </div>
                <span className={'text-body-2 truncate'}>
                  {getConsignorName(data.consignorParty) || '-'}
                </span>
              </div>
              {linkedTransport && (
                <div
                  className={'flex items-center gap-2 self-stretch cursor-pointer hover:bg-color-surface-secondary rounded-radius-md -mx-2 px-2 py-1'}
                  onClick={handleTransportClick}
                >
                  <div className="flex items-center flex-1 gap-2">
                    <Truck
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Transport
                    </span>
                  </div>
                  <span className={'text-body-2 truncate text-color-brand-primary underline'}>
                    {linkedTransport.displayNumber}
                  </span>
                </div>
              )}
              {linkedInvoiceId && (
                <div
                  className={'flex items-center gap-2 self-stretch cursor-pointer hover:bg-color-surface-secondary rounded-radius-md -mx-2 px-2 py-1'}
                  onClick={handleInvoiceClick}
                >
                  <div className="flex items-center flex-1 gap-2">
                    <CurrencyEur
                      className={'w-5 h-5 text-color-text-secondary'}
                    />
                    <span className={'text-body-2 text-color-text-secondary'}>
                      Factuur
                    </span>
                  </div>
                  <span className={'text-body-2 truncate text-color-brand-primary underline'}>
                    {linkedInvoice?.invoiceNumber || linkedInvoiceId}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Summary Section */}
          <div className={'flex flex-col items-start self-stretch gap-3'}>
            <span className={'text-subtitle-1'}>Overzicht</span>
            <div className={'flex flex-col items-start self-stretch border border-solid border-color-border-primary rounded-radius-md overflow-hidden'}>
              {/* Weight Ticket Lines */}
              {data.lines.length === 0 ? (
                <div className={'flex items-center justify-center self-stretch px-3 py-4'}>
                  <span className={'text-body-2 text-color-text-secondary'}>Geen sorteerregels</span>
                </div>
              ) : (
                data.lines.map((line, index) => (
                <div
                  key={index}
                  className={'flex items-center justify-between gap-4 self-stretch px-3 py-2 border-b border-solid border-color-border-primary last:border-b-0'}
                >
                  <div className="flex flex-col flex-1 gap-0.5">
                    <span className={'text-caption text-color-text-secondary'}>
                      {line.itemName} {line.wasteStreamNumber ? `(${line.wasteStreamNumber})` : ''}
                    </span>
                  </div>
                  <span className={'text-body-2'}>
                    {formatWeight(line.weightValue, line.weightUnit)}
                  </span>
                </div>
              ))
              )}
              {/* Totals */}
              {data.lines.length > 0 && (
              <>
                <div className={'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary'}>
                  <span className={'text-body-2 text-color-text-secondary'}>Weging 1</span>
                  <span className={'text-body-2'}>{formatWeight(totalWeight, 'kg')}</span>
                </div>
                <div className={'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary'}>
                  <span className={'text-body-2 text-color-text-secondary'}>Weging 2</span>
                  <span className={'text-body-2'}>
                    {data.secondWeighingValue !== null ? formatWeight(data.secondWeighingValue, data.secondWeighingUnit) : '-'}
                  </span>
                </div>
                <div className={'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary'}>
                  <span className={'text-body-2 text-color-text-secondary'}>Tarra</span>
                  <span className={'text-body-2'}>
                    {data.tarraWeightValue !== null ? formatWeight(data.tarraWeightValue, data.tarraWeightUnit) : '-'}
                  </span>
                </div>
                <div className={'flex items-center justify-between gap-4 self-stretch px-3 py-2 bg-color-surface-secondary border-t border-solid border-color-border-primary'}>
                  <span className={'text-body-2 font-semibold'}>Netto</span>
                  <span className={'text-body-2 font-semibold'}>
                    {formatWeight(
                      totalWeight - (data.tarraWeightValue || 0),
                      'kg'
                    )}
                  </span>
                </div>
              </>
              )}
            </div>
          </div>

          {/* Route Section */}
          {(data.pickupLocation || data.deliveryLocation) && (
            <div className="flex flex-col items-start self-stretch gap-3">
              <span className="subtitle-1">Route</span>
              <div className="flex flex-col items-start self-stretch gap-5 relative">
                {data.pickupLocation && (
                  <div className="flex items-start gap-2 self-stretch">
                    <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
                      <Ellipse />
                    </div>
                    <CompanyCard
                      details={resolveLocationAddress(data.pickupLocation)}
                    />
                  </div>
                )}
                {data.pickupLocation && data.deliveryLocation && (
                  <DottedStroke
                    className="absolute left-[13px]"
                    style={{
                      top: '24px',
                      height: 'calc(100% - 56px - 1rem)',
                      width: '2px',
                      zIndex: 0,
                    }}
                  />
                )}
                {data.deliveryLocation && (
                  <div className="flex items-start gap-2 self-stretch">
                    <div className="flex size-7 p-1 justify-center items-center gap-2 border-[0.875px] border-solid border-color-border-primary bg-color-surface-secondary rounded-full relative z-10">
                      <MapPin />
                    </div>
                    <CompanyCard
                      details={resolveLocationAddress(data.deliveryLocation)}
                    />
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Note Section */}
          {data.note && (
            <div className={'flex flex-col items-start self-stretch gap-3'}>
              <span className={'text-subtitle-1'}>Opmerking</span>
              <Note note={data.note} />
            </div>
          )}

          {/* Documents Section */}
          <DocumentsSection>
            {linkedInvoice?.pdfUrl && (
              <InvoiceDocumentSection
                pdfUrl={linkedInvoice.pdfUrl}
                invoiceNumber={linkedInvoice.invoiceNumber || 'Concept'}
              />
            )}
            {linkedTransport && (
              <WaybillDownloadSection transportId={linkedTransport.transportId} />
            )}
            <WeightTicketDownloadSection weightTicketId={weightTicketId ?? undefined} />
          </DocumentsSection>
        </div>
      )}
    </Drawer>
  );
};
