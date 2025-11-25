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
import { NumberInput } from '@/components/ui/form/NumberInput';
import { DateFormField } from '@/components/ui/form/DateFormField';
import { NumberFormField } from '@/components/ui/form/NumberFormField';

interface WeightTicketLinesTabProps {
  disabled?: boolean;
}

export const UnitBadge = () => (
  <div className="flex items-center justify-center px-3 py-1 bg-color-surface-tertiary rounded-radius-sm border border-color-border text-body-1 text-color-text-secondary h-full">
    kg
  </div>
);

const LineRow = () => (
  <>
    <div></div>
    <div className="w-full col-span-3 h-px bg-color-border-hover"></div>
  </>
);

/**
 * Helper function to parse number strings that may use comma or period as decimal separator
 * @param value - string value to parse
 * @returns parsed number or 0 if invalid
 */
const parseNumber = (value: string | number | undefined): number => {
  if (value === undefined || value === null || value === '') return 0;
  if (typeof value === 'number') return value;
  // Replace comma with period to handle both decimal separators
  const normalizedValue = String(value).replace(',', '.');
  const parsed = parseFloat(normalizedValue);
  return isNaN(parsed) ? 0 : parsed;
};

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

  // Watch weight values for calculations
  const secondWeighingValue = useWatch({
    control,
    name: 'secondWeighingValue',
  });

  const tarraWeightValue = useWatch({
    control,
    name: 'tarraWeightValue',
  });

  // Watch line weight values to trigger recalculation
  const lineWeights = useWatch({
    control,
    name: 'lines',
  });

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

  // Calculate Weging 1 (sum of all line weights)
  const weging1 = useMemo(() => {
    return lineWeights.reduce((sum, field) => {
      const weight = parseNumber(field.weightValue as string);
      return sum + weight;
    }, 0);
  }, [lineWeights]);

  // Calculate Bruto (Weging 1 - Weging 2)
  const bruto = useMemo(() => {
    const weging2 = parseNumber(secondWeighingValue as unknown as string);
    return weging1 - weging2;
  }, [weging1, secondWeighingValue]);

  // Calculate Netto (Bruto - Tarra)
  const netto = useMemo(() => {
    const tarra = parseNumber(tarraWeightValue as unknown as string);
    return bruto - tarra;
  }, [bruto, tarraWeightValue]);

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
    <div className="flex flex-col items-start self-stretch gap-4">
      <div className="w-1/44">
        <DateFormField
          title="Datum weging"
          placeholder="Selecteer een datum"
          formHook={{
            register,
            name: 'weightedAt',
            rules: {
              required: true,
            },
            errors,
          }}
        />
      </div>
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
                    <NumberFormField
                      title={'Hoeveelheid'}
                      step={0.01}
                      placeholder={'0'}
                      disabled={disabled}
                      formHook={{
                        register,
                        name: `lines.${index}.weightValue` as const,
                        rules: {
                          validate: (value) => {
                            const lines = formContext.getValues('lines');
                            const wasteStreamNumber =
                              lines[index]?.wasteStreamNumber;
                            const numValue = typeof value === 'number' ? value : undefined;
                            // If both are empty, it's valid (will be filtered out)
                            if (numValue === undefined && !wasteStreamNumber) {
                              return true;
                            }
                            // If waste stream is filled but weight is not, show error
                            if (numValue === undefined && wasteStreamNumber) {
                              return 'Hoeveelheid is verplicht';
                            }
                            // Validate positive value
                            if (numValue !== undefined && numValue <= 0) {
                              return 'Voer een positief getal in';
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
                    <UnitBadge />
                  </div>
                </div>
              </div>
            ))}
            {/* Summary */}
            <div className="grid grid-cols-[auto_1fr_auto_auto] gap-4 gap-y-2 self-stretch w-1/2">
              {/* Weging 1 */}
              <span className="text-caption-2 self-center">Weging 1</span>
              <div className="flex flex-col items-start gap-1">
                <NumberInput
                  placeholder={'0'}
                  value={weging1.toFixed(2)}
                  disabled={true}
                  data-testid={"weging-1-disabled-input"}
                />
              </div>
              <UnitBadge />
              <div></div>

              {/* Weging 2 - with minus sign and underline */}
              <span className="text-caption-2 self-center">Weging 2</span>
              <div className="flex flex-col items-start gap-1">
                <NumberInput
                  placeholder={'Vul weging 2 in'}
                  step={'0.01'}
                  disabled={disabled}
                  formHook={{
                    register: formContext.register,
                    name: 'secondWeighingValue',
                    rules: {
                      validate: (value) => {
                        if (value === undefined || value === null || value === '') return true;
                        const numValue = parseNumber(value as string);
                        if (numValue < 0) {
                          return 'Weging 2 kan niet negatief zijn';
                        }
                        return true;
                      },
                    },
                    errors: formContext.formState.errors,
                  }}
                />
              </div>
              <UnitBadge />
              <span className="text-body-1 text-color-text-secondary self-center">−</span>

              {formContext.formState.errors.secondWeighingValue && (
                <>
                  <div></div>
                  <div className='col-span-3'>
                    <span className="text-caption-2 text-color-status-error-dark">
                      {formContext.formState.errors.secondWeighingValue.message as string}
                    </span>
                  </div>
                </>
              )}
              <LineRow />
              {/* Bruto */}
              <span className="text-caption-2 self-center">Bruto</span>
              <div className="flex flex-col items-start gap-1">
                <NumberInput
                  placeholder={'0'}
                  value={bruto.toFixed(2)}
                  disabled={true}
                  data-testid={"bruto-disabled-input"}
                />
              </div>
              <UnitBadge />
              <div></div>

              {/* Tarra - with minus sign and underline */}
              <span className="text-caption-2 self-center">Tarra</span>
              <div className="flex flex-col items-start gap-1">
                <NumberInput
                  placeholder={'Vul tarra in'}
                  step={'0.01'}
                  disabled={disabled}
                  formHook={{
                    register: formContext.register,
                    name: 'tarraWeightValue',
                    rules: {
                      validate: (value) => {
                        if (value === undefined || value === null || value === '') return true;
                        const numValue = parseNumber(value as string);
                        if (numValue < 0) {
                          return 'Tarra kan niet negatief zijn';
                        }
                        return true;
                      },
                    },
                    errors: formContext.formState.errors,
                  }}
                />
              </div>
              <UnitBadge />
              <span className="text-body-1 text-color-text-secondary self-center">−</span>
              {formContext.formState.errors.tarraWeightValue && (
                <>
                  <div></div>
                  <div className='col-span-3'>
                    <span className="text-caption-2 text-color-status-error-dark">
                      {formContext.formState.errors.tarraWeightValue.message as string}
                    </span>
                  </div>
                </>
              )}
              <LineRow />
              {/* Netto */}
              <span className="text-caption-2 self-center">Netto</span>
              <div className="flex flex-col items-start gap-1">
                <NumberInput
                  placeholder={'0'}
                  value={netto.toFixed(2)}
                  disabled={true}
                  data-testid={"netto-disabled-input"}
                />
              </div>
              <UnitBadge />
              <div></div>
            </div>
            {formContext.formState.errors.secondWeighingValue && (
              <span className="text-caption-2 text-color-status-error-dark">
                {formContext.formState.errors.secondWeighingValue.message as string}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
