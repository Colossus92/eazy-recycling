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
import { NumberFormField } from '@/components/ui/form/NumberFormField';

interface WeightTicketLinesTabProps {
  disabled?: boolean;
}

export const WeightTicketLinesTab = ({
  disabled = false,
}: WeightTicketLinesTabProps) => {
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
          disabled={!hasConsignorSelected || disabled}
          className="flex items-center justify-center w-8 h-8 rounded-radius-sm bg-color-primary text-color-on-primary hover:bg-color-primary-hover disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          title={
            disabled
              ? 'Kan niet bewerken in deze status'
              : !hasConsignorSelected
                ? 'Selecteer eerst een opdrachtgever'
                : 'Voeg weging toe'
          }
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
                  Materiaal {index + 1}
                </span>
                <button
                  type="button"
                  onClick={() => remove(index)}
                  disabled={disabled}
                  className="flex items-center justify-center w-6 h-6 rounded-radius-sm text-color-error hover:bg-color-error hover:text-color-on-error disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  title={
                    disabled
                      ? 'Kan niet bewerken in deze status'
                      : 'Verwijder weging'
                  }
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
                    disabled={disabled}
                    formHook={{
                      register,
                      name: `lines.${index}.wasteStreamNumber` as const,
                      rules: {
                        validate: (value) => {
                          const lines = formContext.getValues('lines');
                          const weightValue = lines[index]?.weightValue;
                          // If both are empty, it's valid (will be filtered out)
                          if (!value && !weightValue) {
                            return true;
                          }
                          // If weight is filled but waste stream is not, show error
                          if (!value && weightValue) {
                            return 'Afvalstroom is verplicht';
                          }
                          return true;
                        },
                      },
                      errors,
                      control,
                    }}
                  />
                </div>

                <div className="w-32">
                  <TextFormField
                    title={'Hoeveelheid'}
                    placeholder={'0'}
                    disabled={disabled}
                    formHook={{
                      register,
                      name: `lines.${index}.weightValue` as const,
                      rules: {
                        validate: (value) => {
                          const lines = formContext.getValues('lines');
                          const wasteStreamNumber = lines[index]?.wasteStreamNumber;
                          const stringValue = typeof value === 'string' ? value : '';
                          // If both are empty, it's valid (will be filtered out)
                          if (!stringValue && !wasteStreamNumber) {
                            return true;
                          }
                          // If waste stream is filled but weight is not, show error
                          if (!stringValue && wasteStreamNumber) {
                            return 'Hoeveelheid is verplicht';
                          }
                          // Validate number format if value exists
                          if (stringValue && !/^\d+(\.\d+)?$/.test(stringValue)) {
                            return 'Voer een geldig getal in';
                          }
                          return true;
                        },
                      },
                      errors,
                    }}
                  />
                </div>

                <div className="flex flex-col items-start gap-2">
                  <label className="text-caption-2">Eenheid</label>
                  <div className="flex items-center justify-center px-3 py-1 bg-color-surface-tertiary rounded-radius-sm border border-color-border text-body-1 text-color-text-secondary">
                    kg
                  </div>
                </div>

              </div>

            </div>
          ))}
          <div className="flex items-start self-stretch gap-4">
            <NumberFormField
              title={'Weging 2'}
              placeholder={'Vul weging 2 in'}
              step={0.01}
              disabled={disabled}
              formHook={{
                register: formContext.register,
                name: 'secondWeightValue',
                errors: formContext.formState.errors,
              }}
            />
            <div className="flex flex-col items-start gap-2">
              <label className="text-caption-2">
                Eenheid
              </label>
              <div className="flex items-center justify-center px-3 py-1 bg-color-surface-tertiary rounded-radius-sm border border-color-border text-body-1 text-color-text-secondary">
                kg
              </div>
            </div>
          </div>
          <div className="flex items-start self-stretch gap-4">
            <NumberFormField
              title={'Tarra'}
              placeholder={'Vul tarra in'}
              step={0.01}
              disabled={disabled}
              formHook={{
                register: formContext.register,
                name: 'tarraWeightValue',
                errors: formContext.formState.errors,
              }}
            />
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
      )}
    </div>
  );
};
