import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { WasteStreamTransportFormValues } from '@/features/wastestreams/hooks/useWasteStreamTransportForm';
import { UnitBadge } from '@/features/weighttickets/components/weightticketform/WeightTicketLinesTab';
import { useState } from 'react';
import { useFieldArray, useFormContext } from 'react-hook-form';
import { WasteStreamData, WasteStreamSelectionTable } from '../WasteStreamSelectionTable';
import Plus from '@/assets/icons/Plus.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { useQuery } from '@tanstack/react-query';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { Button } from '@/components/ui/button/Button';

export const WasteStreamTransportFormSelectSection = () => {
  const { register, watch, control, formState: { errors } } = useFormContext<WasteStreamTransportFormValues>();
  const [isWasteStreamTableOpen, setIsWasteStreamTableOpen] = useState(false);
  const [editingLineIndex, setEditingLineIndex] = useState<number | null>(null);

  const { fields, append, remove, update } = useFieldArray({
    control,
    name: 'wasteStreamLines',
  });

  // Watch the consignor selection to conditionally show waste stream section
  const consignorPartyId = watch('consignorPartyId');

  // Get the first waste stream number for fetching compatible streams
  const firstWasteStreamNumber = fields.length > 0 ? fields[0].wasteStreamNumber : undefined;

  // Fetch compatible waste streams when adding a second or later waste stream
  const { data: compatibleWasteStreams } = useQuery({
    queryKey: ['compatibleWasteStreams', firstWasteStreamNumber],
    queryFn: () => wasteStreamService.getCompatible(firstWasteStreamNumber!),
    enabled: !!firstWasteStreamNumber && editingLineIndex !== null && editingLineIndex > 0,
  });

  const handleWasteStreamSelect = (wasteStream: WasteStreamData) => {
    if (editingLineIndex !== null) {
      // Update existing line
      update(editingLineIndex, {
        wasteStreamNumber: wasteStream.wasteStreamNumber || '',
        wasteStreamData: wasteStream,
        quantity: fields[editingLineIndex]?.quantity || '1',
        weight: fields[editingLineIndex]?.weight || '',
      });
    } else {
      // Add new line
      append({
        wasteStreamNumber: wasteStream.wasteStreamNumber || '',
        wasteStreamData: wasteStream,
        quantity: '1',
        weight: '',
      });
    }
    setIsWasteStreamTableOpen(false);
    setEditingLineIndex(null);
  };

  const handleOpenWasteStreamTable = (index?: number) => {
    setEditingLineIndex(index ?? null);
    setIsWasteStreamTableOpen(true);
  };

  const handleRemoveLine = (index: number) => {
    remove(index);
  };

  return (
    <>
      <div className="flex flex-col items-start self-stretch gap-4">
        {/* Consignor Selection */}
        <CompanySelectFormField
          title="Opdrachtgever"
          placeholder="Selecteer een opdrachtgever"
          name="consignorPartyId"
          rules={{ required: 'Opdrachtgever is verplicht' }}
        />

        {/* Waste Stream Selection - Only show when consignor is selected */}
        {consignorPartyId && (
          <div className="bg-color-surface-secondary flex flex-col gap-4 items-start p-4 rounded-radius-md w-full">
            <div className="flex justify-between items-center self-stretch">
              <h3 className="text-subtitle-1 text-color-text-primary">
                Afvalstromen
              </h3>
              <Button
                onClick={() => handleOpenWasteStreamTable()}
                icon={Plus}
                variant={"icon"}
              />
            </div>

            {fields.length === 0 && (
              <div className="flex items-center max-w-96">
                <span className="text-body-2 text-color-text-secondary whitespace-normal break-words">
                  Klik op de + knop om een afvalstroom toe te voegen
                </span>
              </div>
            )}

            {/* Selected Waste Streams Display */}
            {fields.map((field, index) => (
              <div
                key={field.id}
                className="bg-color-surface-primary border border-color-border-primary rounded-radius-md p-3 w-full"
              >
                <div className="flex justify-between items-center self-stretch mb-2">
                  <span className="text-body-2 font-medium">
                    Afvalstroom {index + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleRemoveLine(index)}
                    className="flex items-center justify-center w-6 h-6 rounded-radius-sm text-color-error hover:bg-color-error hover:text-color-on-error transition-colors"
                    title="Verwijder afvalstroom"
                  >
                    <TrashSimple className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex gap-4 items-center">
                  <div className="flex flex-col items-start self-stretch gap-1 flex-1">
                    <span className="text-caption-2">Afvalstroom</span>
                    <div className="flex items-center h-full">
                      <span className="text-subtitle-2 text-color-text-primary">
                        {field.wasteStreamData?.wasteStreamNumber} - {field.wasteStreamData?.wasteName}
                      </span>
                    </div>
                  </div>

                  <div className="w-20">
                    <NumberFormField<WasteStreamTransportFormValues>
                      title="Hoeveelheid"
                      placeholder="#"
                      step={1}
                      formHook={{
                        register,
                        name: `wasteStreamLines.${index}.quantity` as const,
                        errors,
                      }}
                    />
                  </div>
                  <div className="w-28">
                    <NumberFormField<WasteStreamTransportFormValues>
                      title="Gewicht"
                      placeholder="#"
                      step={0.01}
                      formHook={{
                        register,
                        name: `wasteStreamLines.${index}.weight` as const,
                        errors,
                      }}
                    />
                  </div>
                  <div className="flex flex-col items-start self-stretch gap-1 w-auto">
                    <span className="text-caption-2">Eenheid</span>
                    <div className="flex items-center h-full">
                      <UnitBadge />
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
      {/* Waste Stream Selection Table */}
      <WasteStreamSelectionTable
        isOpen={isWasteStreamTableOpen}
        setIsOpen={setIsWasteStreamTableOpen}
        onSelect={handleWasteStreamSelect}
        consignorId={consignorPartyId}
        data={editingLineIndex !== null && editingLineIndex > 0 ? compatibleWasteStreams : undefined}
      />
    </>
  );
};
