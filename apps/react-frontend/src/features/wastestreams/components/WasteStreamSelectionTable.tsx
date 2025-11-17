import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button/Button.tsx';
import X from '@/assets/icons/X.svg?react';
import BxRecycle from '@/assets/icons/BxRecycle.svg?react';
import MagnifyingGlass from '@/assets/icons/MagnifyingGlass.svg?react';
import Funnel from '@/assets/icons/Funnel.svg?react';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import { TextInput } from '@/components/ui/form/TextInput';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { WasteStreamListView } from '@/api/client/models/waste-stream-list-view';
import { ClipLoader } from 'react-spinners';
import { Tooltip } from '@/components/ui/tooltip';
import { FormDialog } from '@/components/ui/dialog/FormDialog';
import { EmptyState } from '@/features/crud/EmptyState';

export interface WasteStreamData extends WasteStreamListView { }

interface WasteStreamSelectionTableProps {
  isOpen: boolean;
  setIsOpen: (value: boolean) => void;
  data?: WasteStreamData[];
  onSelect?: (wasteStream: WasteStreamData) => void;
  onBack?: () => void;
  consignorId?: string;
}


export const WasteStreamSelectionTable = ({
  isOpen,
  setIsOpen,
  data,
  onSelect,
  consignorId,
}: WasteStreamSelectionTableProps) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedWasteStream, setSelectedWasteStream] = useState<WasteStreamData | null>(null);
  const [wasteStreams, setWasteStreams] = useState<WasteStreamData[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (isOpen && !data) {
      setIsLoading(true);
      wasteStreamService.getAll(consignorId, 'ACTIVE')
        .then((streams) => {
          setWasteStreams(streams as WasteStreamData[]);
        })
        .catch((error) => {
          console.error('Failed to fetch waste streams:', error);
          setWasteStreams([]);
        })
        .finally(() => {
          setIsLoading(false);
        });
    } else if (data) {
      setWasteStreams(data);
    }
  }, [isOpen, data, consignorId]);

  if (!isOpen) return null;

  const filteredData = wasteStreams.filter(item =>
    (item.wasteStreamNumber?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
    (item.wasteName?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
    (item.consignorPartyName?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
    (item.pickupLocation?.toLowerCase().includes(searchTerm.toLowerCase()) || false) ||
    (item.deliveryLocation?.toLowerCase().includes(searchTerm.toLowerCase()) || false)
  );

  const handleRowClick = (wasteStream: WasteStreamData) => {
    setSelectedWasteStream(wasteStream);
  };

  const handleSelectStream = () => {
    if (selectedWasteStream && onSelect) {
      onSelect(selectedWasteStream);
    }
    setIsOpen(false);
  };

  const close = () => {
    setSearchTerm('');
    setIsOpen(false);
  }

  return (
    <FormDialog isOpen={isOpen} setIsOpen={setIsOpen} width="w-full max-w-4xl">
      <div className="flex flex-col h-[80vh] overflow-hidden rounded-radius-lg">
        {/* Header */}
        <div className="bg-color-surface-primary border-b border-color-border-primary flex items-center gap-4 px-4 py-3 flex-shrink-0">
          <div className="flex-1">
            <h4 className="text-headline-4 text-color-text-primary font-bold">Selecteer afvalstroomnummer</h4>
          </div>
          <Button
            variant="tertiary"
            icon={X}
            showText={false}
            onClick={close}
            className="p-1.5"
          />
        </div>

        {/* Search and Filter Actions */}
        <div className="flex items-center gap-4 p-3">
          <TextInput
            icon={MagnifyingGlass}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Zoek..."
          />
          <Button
            variant="secondary"
            icon={Funnel}
            label="Filter"
            iconPosition="left"
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
                icon={BxRecycle}
                text="Maak een afvalstroomnummer aan via afvalstroombeheer"
                onClick={() => { }}
                showButton={false}
              />
            </div>
          ) : (
            <table className="w-full table-fixed border-collapse">
              <colgroup>
                <col style={{ width: '120px' }} />
                <col style={{ width: '20%' }} />
                <col style={{ width: '40%' }} />
                <col style={{ width: '40%' }} />
              </colgroup>
              <thead className="sticky top-0 bg-color-surface-secondary border-solid border-b border-color-border-primary">
                <tr className="text-subtitle-1">
                  <th className="px-4 py-3 text-left">Nummer</th>
                  <th className="px-4 py-3 text-left truncate">Materiaal</th>
                  <th className="px-4 py-3 text-left truncate">Herkomstlocatie</th>
                  <th className="px-4 py-3 text-left truncate">Bestemmingslocatie</th>
                </tr>
              </thead>
              <tbody>
                {filteredData.map((item) => (
                  <tr
                    key={item.wasteStreamNumber}
                    className={`text-body-2 border-b border-solid border-color-border-primary cursor-pointer ${selectedWasteStream?.wasteStreamNumber === item.wasteStreamNumber ? 'bg-color-brand-light' : 'hover:bg-color-surface-secondary'}`}
                    onClick={() => handleRowClick(item)}
                  >
                    <td className="p-4">{item.wasteStreamNumber}</td>
                    <td className="p-4 truncate">
                      <Tooltip content={item.wasteName} position="top">
                        <span>{item.wasteName}</span>
                      </Tooltip>
                    </td>
                    <td className="p-4 truncate">
                      <Tooltip content={item.pickupLocation} position="top">
                        <span>{item.pickupLocation}</span>
                      </Tooltip>
                    </td>
                    <td className="p-4 truncate">
                      <Tooltip content={item.deliveryLocation} position="top">
                        <span>{item.deliveryLocation}</span>
                      </Tooltip>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Footer Buttons */}
        <div className="bg-color-surface-primary border-t border-color-border-primary flex items-center justify-end gap-4 px-4 py-3 flex-shrink-0">
          <Button
            variant="secondary"
            icon={CaretLeft}
            label="Terug"
            iconPosition="left"
            onClick={close}
          />
          <Button
            variant="primary"
            icon={CaretRight}
            label="Selecteer afvalstroom"
            iconPosition="right"
            onClick={handleSelectStream}
            disabled={!selectedWasteStream}
          />
        </div>
      </div>
    </FormDialog>
  );
};
