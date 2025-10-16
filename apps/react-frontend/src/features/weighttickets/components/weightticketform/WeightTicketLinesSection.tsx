import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { useQuery } from '@tanstack/react-query';
import Plus from '@/assets/icons/Plus.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { useEffect, useMemo, useRef } from 'react';
import { useFieldArray, useFormContext, useWatch } from 'react-hook-form';
import { WeightTicketFormValues } from './useWeigtTicketFormHook';
import { wasteStreamService } from '@/api/services/wasteStreamService';
import { WasteStreamListView } from '@/api/client';

export const WeightTicketLinesSection = () => {
  const formContext = useFormContext<WeightTicketFormValues>();
  const {
    control,
    register,
    formState: { errors },
  } = formContext;

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lines',
  });

  // Watch the consignorPartyId to enable/disable adding lines
  const consignorPartyId = useWatch({
    control,
    name: 'consignorPartyId',
  });

  // Track previous consignorPartyId to detect actual changes (not initial load)
  const previousConsignorPartyIdRef = useRef<string | undefined>(undefined);
  const isInitialMount = useRef(true);

  // When the consignorPartyId changes (but not on initial load), reset the lines
  useEffect(() => {
    // Skip on initial mount to preserve loaded data
    if (isInitialMount.current) {
      isInitialMount.current = false;
      previousConsignorPartyIdRef.current = consignorPartyId;
      return;
    }

    // Only reset if consignor actually changed from a previous value
    if (
      previousConsignorPartyIdRef.current !== undefined &&
      previousConsignorPartyIdRef.current !== consignorPartyId
    ) {
      remove();
    }

    previousConsignorPartyIdRef.current = consignorPartyId;
  }, [consignorPartyId, append, remove]);

  // Fetch waste streams
  const { data: wasteStreams = [] } = useQuery<WasteStreamListView[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => wasteStreamService.getAll(),
  });

  // Filter waste streams by consignor party ID
  const filteredWasteStreams = useMemo(() => {
    if (!consignorPartyId) return [];
    return wasteStreams.filter(
      (ws) => ws.consignorPartyId === consignorPartyId
    );
  }, [wasteStreams, consignorPartyId]);

  const wasteStreamOptions = useMemo(() => {
    return filteredWasteStreams.map((ws) => ({
      value: ws.wasteStreamNumber,
      label: `${ws.wasteName} (${ws.wasteStreamNumber})`,
    }));
  }, [filteredWasteStreams]);

  const handleAddLine = () => {
    append({
      wasteStreamNumber: '',
      weightValue: '',
      weightUnit: 'KG',
    });
  };

  const hasConsignorSelected = consignorPartyId && consignorPartyId.length > 0;

  return (
    <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
      <div className="flex justify-between items-center self-stretch">
        <span className="text-subtitle-1">Wegingen</span>
        <button
          type="button"
          onClick={handleAddLine}
          disabled={!hasConsignorSelected}
          className="flex items-center justify-center w-8 h-8 rounded-radius-sm bg-color-primary text-color-on-primary hover:bg-color-primary-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title={!hasConsignorSelected ? 'Selecteer eerst een opdrachtgever' : 'Voeg weging toe'}
        >
          <Plus className="w-5 h-5" />
        </button>
      </div>

      {!hasConsignorSelected && (
        <div className="flex items-center max-w-96">
          <span className="text-body-2 text-color-text-secondary whitespace-normal break-words">
            Selecteer eerst een opdrachtgever om wegingen toe te voegen
          </span>
        </div>
      )}

      {fields.length > 0 && (
        <div className="flex flex-col items-start self-stretch gap-4">
          {fields.map((field, index) => (
            <div
              key={field.id}
              className="flex flex-col items-start self-stretch gap-4 p-3 border border-color-border rounded-radius-sm"
            >
              <div className="flex justify-between items-center self-stretch">
                <span className="text-body-2 font-medium">
                  Weging {index + 1}
                </span>
                <button
                  type="button"
                  onClick={() => remove(index)}
                  className="flex items-center justify-center w-6 h-6 rounded-radius-sm text-color-error hover:bg-color-error hover:text-color-on-error transition-colors"
                  title="Verwijder weging"
                >
                  <TrashSimple className="w-4 h-4" />
                </button>
              </div>

              <div className="flex items-start self-stretch gap-4">
                <div className="flex-1">
                  <SelectFormField
                    title={'Naam'}
                    placeholder={'Selecteer een afvalstroom'}
                    options={wasteStreamOptions}
                    testId={`waste-stream-select-${index}`}
                    formHook={{
                      register,
                      name: `lines.${index}.wasteStreamNumber` as const,
                      rules: { required: 'Afvalstroom is verplicht' },
                      errors,
                      control,
                    }}
                  />
                </div>

                <div className="w-32">
                  <TextFormField
                    title={'Hoeveelheid'}
                    placeholder={'0'}
                    formHook={{
                      register,
                      name: `lines.${index}.weightValue` as const,
                      rules: {
                        required: 'Hoeveelheid is verplicht',
                        pattern: {
                          value: /^\d+(\.\d+)?$/,
                          message: 'Voer een geldig getal in',
                        },
                      },
                      errors,
                    }}
                  />
                </div>

                <div className="flex flex-col items-start gap-2">
                  <label className="text-caption-2">
                    Eenheid
                  </label>
                  <div className="flex items-center justify-center px-3 py-1 bg-color-surface-tertiary rounded-radius-sm border border-color-border text-body-1 text-color-text-secondary">
                    kg
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
