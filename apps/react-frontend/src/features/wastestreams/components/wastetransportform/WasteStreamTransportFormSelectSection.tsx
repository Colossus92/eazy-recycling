import { useFormContext } from 'react-hook-form';
import { useState } from 'react';
import { Button } from '@/components/ui/button/Button.tsx';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { WasteStreamTransportFormValues } from '@/features/wastestreams/hooks/useWasteStreamTransportForm';
import { WasteStreamSelectionTable, WasteStreamData } from '../WasteStreamSelectionTable';

export const WasteStreamTransportFormSelectSection = () => {
  const formContext = useFormContext<WasteStreamTransportFormValues>();
  const [isWasteStreamTableOpen, setIsWasteStreamTableOpen] = useState(false);

  // Watch the consignor selection to conditionally show waste stream section
  const consignorPartyId = formContext.watch('consignorPartyId');
  const selectedWasteStream = formContext.watch('wasteStreamData');

  const handleWasteStreamSelect = (wasteStream: WasteStreamData) => {
    formContext.setValue('wasteStreamNumber', wasteStream.wasteStreamNumber || '');
    formContext.setValue('wasteStreamData', wasteStream);

    // Trigger validation for the updated fields
    formContext.trigger(['wasteStreamNumber']);
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
                <div className="flex flex-col gap-2">
                  <div className="flex justify-between items-start">
                    <span className="text-subtitle-2 text-color-text-primary">
                      {selectedWasteStream.wasteStreamNumber}
                    </span>
                    <span className="text-body-2 text-color-text-secondary">
                      {selectedWasteStream.wasteName}
                    </span>
                  </div>
                  <div className="text-body-2 text-color-text-secondary">
                    Afzender: {selectedWasteStream.consignorPartyName}
                  </div>
                  <div className="text-body-2 text-color-text-secondary">
                    Herkomstlocatie: {selectedWasteStream.pickupLocation}
                  </div>
                  <div className="text-body-2 text-color-text-secondary">
                    Bestemmingslocatie: {selectedWasteStream.deliveryLocation}
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
      />
    </>
  );
};
