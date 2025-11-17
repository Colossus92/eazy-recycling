import { Button } from '@/components/ui/button/Button.tsx';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { WasteStreamTransportFormValues } from '@/features/wastestreams/hooks/useWasteStreamTransportForm';
import { UnitBadge } from '@/features/weighttickets/components/weightticketform/WeightTicketLinesTab';
import { useState } from 'react';
import { useFormContext } from 'react-hook-form';
import { WasteStreamData, WasteStreamSelectionTable } from '../WasteStreamSelectionTable';

export const WasteStreamTransportFormSelectSection = () => {
  const { register, watch, setValue, trigger, formState: { errors } } = useFormContext<WasteStreamTransportFormValues>();
  const [isWasteStreamTableOpen, setIsWasteStreamTableOpen] = useState(false);

  // Watch the consignor selection to conditionally show waste stream section
  const consignorPartyId = watch('consignorPartyId');
  const selectedWasteStream = watch('wasteStreamData');

  const handleWasteStreamSelect = (wasteStream: WasteStreamData) => {
    setValue('wasteStreamNumber', wasteStream.wasteStreamNumber || '');
    setValue('wasteStreamData', wasteStream);
    setValue('quantity', '');
    setValue('weight', '');

    // Trigger validation for the updated fields
    trigger(['wasteStreamNumber']);
  };

  const handleOpenWasteStreamTable = () => {
    setIsWasteStreamTableOpen(true);
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
            <h3 className="text-subtitle-1 text-color-text-primary">
              Selecteer afvalstroom
            </h3>

            {/* Selected Waste Stream Display */}
            {selectedWasteStream && (
              <div className="bg-color-surface-primary border border-color-border-primary rounded-radius-md p-3 w-full">
                <div className="flex gap-4 items-center">
                  <div className="flex flex-col items-start self-stretch gap-1 flex-1">
                    <span className="text-caption-2">Afvalstroom</span>
                    <div className="flex items-center h-full">
                      <span className="text-subtitle-2 text-color-text-primary">
                        {selectedWasteStream.wasteStreamNumber} - {selectedWasteStream.wasteName}
                      </span>
                    </div>
                  </div>

                  <div className="w-20">
                    <NumberFormField<WasteStreamTransportFormValues>
                      title="Hoeveelheid"
                      placeholder="#"
                      value={'1'}
                      step={1}
                      formHook={{
                        register,
                        name: 'quantity',
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
                        name: 'weight',
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
            )}

            {/* Select from List Button */}
            <Button
              variant={'secondary'}
              label="Selecteer uit lijst"
              onClick={handleOpenWasteStreamTable}
              fullWidth
            />
          </div>
        )}

      </div>
      {/* Waste Stream Selection Table */}
      <WasteStreamSelectionTable
        isOpen={isWasteStreamTableOpen}
        setIsOpen={setIsWasteStreamTableOpen}
        onSelect={handleWasteStreamSelect}
        consignorId={consignorPartyId}
      />
    </>
  );
};
