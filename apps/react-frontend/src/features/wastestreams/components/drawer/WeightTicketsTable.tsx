import { WeightTicketsByWasteStreamView } from '@/api/client/models/weight-tickets-by-waste-stream-view';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import MagnifyingGlass from '@/assets/icons/MagnifyingGlass.svg?react';
import Scale from '@/assets/icons/Scale.svg?react';
import X from '@/assets/icons/X.svg?react';
import { Button } from '@/components/ui/button/Button';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { TextInput } from '@/components/ui/form/TextInput';
import { EmptyState } from '@/features/crud/EmptyState';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { ClipLoader } from 'react-spinners';
import { format } from 'date-fns';

interface WeightTicketsTableProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  wasteStreamNumber: string;
  onWeightTicketClick?: (weightTicketNumber: number) => void;
}

export const WeightTicketsTable = ({
  isOpen,
  setIsOpen,
  wasteStreamNumber,
  onWeightTicketClick,
}: WeightTicketsTableProps) => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: weightTickets, isLoading } = useQuery({
    queryKey: ['weightTicketsByWasteStream', wasteStreamNumber],
    queryFn: () => wasteStreamService.getWeightTicketsByWasteStream(wasteStreamNumber),
    enabled: isOpen && !!wasteStreamNumber,
  });

  // Reset search when dialog closes
  useEffect(() => {
    if (!isOpen) {
      setSearchTerm('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const filteredData = (weightTickets || []).filter(item =>
    String(item.weightTicketNumber).includes(searchTerm) ||
    (item.createdBy?.toLowerCase().includes(searchTerm.toLowerCase()) || false)
  );

  const handleRowClick = (weightTicket: WeightTicketsByWasteStreamView) => {
    if (onWeightTicketClick) {
      onWeightTicketClick(weightTicket.weightTicketNumber);
      setIsOpen(false);
    }
  };

  const close = () => {
    setSearchTerm('');
    setIsOpen(false);
  };

  return (
    <FormDialog isOpen={isOpen} setIsOpen={setIsOpen} width="w-full max-w-4xl">
      <div className="flex flex-col h-[80vh] overflow-hidden rounded-radius-lg">
        {/* Header */}
        <div className="bg-color-surface-primary border-b border-color-border-primary flex items-center gap-4 px-4 py-3 flex-shrink-0">
          <div className="flex-1">
            <h4 className="text-headline-4 text-color-text-primary font-bold">
              Weegbonnen voor {wasteStreamNumber}
            </h4>
          </div>
          <Button
            variant="tertiary"
            icon={X}
            showText={false}
            onClick={close}
            className="p-1.5"
          />
        </div>

        {/* Search */}
        <div className="flex items-center gap-4 p-3">
          <TextInput
            icon={MagnifyingGlass}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Zoek op nummer of aangemaakt door..."
          />
        </div>

        {/* Table */}
        <div className="border-t border-color-border-primary flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex justify-center items-center h-full w-full">
              <ClipLoader
                size={20}
                color={'text-color-text-invert-primary'}
                aria-label="Laad spinner"
              />
            </div>
          ) : filteredData.length === 0 ? (
            <div className="flex items-center justify-center h-full w-full p-4">
              <EmptyState
                icon={Scale}
                text="Geen weegbonnen gevonden voor dit afvalstroomnummer"
                onClick={() => {}}
                showButton={false}
              />
            </div>
          ) : (
            <table className="w-full table-fixed border-collapse">
              <colgroup>
                <col style={{ width: '20%' }} />
                <col style={{ width: '20%' }} />
                <col style={{ width: '20%' }} />
                <col style={{ width: '40%' }} />
              </colgroup>
              <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
                <tr className="text-subtitle-1">
                  <th className="px-4 py-3 text-left">Nummer</th>
                  <th className="px-4 py-3 text-left truncate">Gewogen op</th>
                  <th className="px-4 py-3 text-right truncate">Hoeveelheid (kg)</th>
                  <th className="px-4 py-3 text-left truncate">Aangemaakt door</th>
                </tr>
              </thead>
              <tbody>
                {filteredData.map((item) => (
                  <tr
                    key={item.weightTicketNumber}
                    className="text-body-2 border-b border-solid border-color-border-primary cursor-pointer hover:bg-color-surface-secondary"
                    onClick={() => handleRowClick(item)}
                  >
                    <td className="p-4">
                      <span className="text-color-brand-primary underline">
                        {item.weightTicketNumber}
                      </span>
                    </td>
                    <td className="p-4 truncate">
                      {item.weightedAt ? format(new Date(item.weightedAt.toString()), 'dd-MM-yyyy') : '-'}
                    </td>
                    <td className="p-4 text-right">
                      {item.amount?.toFixed(2) || '-'}
                    </td>
                    <td className="p-4 truncate">
                      {item.createdBy || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Footer */}
        <div className="bg-color-surface-primary border-t border-color-border-primary flex items-center justify-end gap-4 px-4 py-3 flex-shrink-0">
          <Button
            variant="secondary"
            icon={CaretLeft}
            label="Terug"
            iconPosition="left"
            onClick={close}
          />
        </div>
      </div>
    </FormDialog>
  );
};
