import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { NumberFormField } from '@/components/ui/form/NumberFormField';
import { CatalogItemAsyncSelectFormField } from '@/components/ui/form/selectfield/CatalogItemAsyncSelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { memo, useEffect, useRef } from 'react';
import { useFieldArray, useFormContext, useWatch } from 'react-hook-form';
import { LineItemSection } from './LineItemSection';
import { WeightTicketFormValues } from './useWeigtTicketFormHook';

interface WeightTicketProductsTabProps {
  disabled?: boolean;
}

export const WeightTicketProductsTab = memo(({
  disabled = false,
}: WeightTicketProductsTabProps) => {
  const formContext = useFormContext<WeightTicketFormValues>();
  const {
    control,
    register,
    formState: { errors },
  } = formContext;

  const { fields, append, remove, update } = useFieldArray({
    control,
    name: 'productLines',
  });

  const consignorPartyId = useWatch({
    control,
    name: 'consignorPartyId',
  });

  // Track previous consignorPartyId to detect actual changes
  const previousConsignorPartyIdRef = useRef<string | undefined>(undefined);
  const isInitialMount = useRef(true);
  const hasUserInteracted = useRef(false);

  // When the consignorPartyId changes (but not on initial load), reset the product lines
  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
      previousConsignorPartyIdRef.current = consignorPartyId;
      return;
    }

    if (
      hasUserInteracted.current &&
      previousConsignorPartyIdRef.current !== undefined &&
      previousConsignorPartyIdRef.current !== '' &&
      previousConsignorPartyIdRef.current !== consignorPartyId &&
      fields.length > 0
    ) {
      remove();
    }

    previousConsignorPartyIdRef.current = consignorPartyId;
  }, [consignorPartyId, fields.length, remove]);

  const handleAddProductLine = () => {
    hasUserInteracted.current = true;
    append({
      catalogItemId: '',
      quantity: '',
      unit: 'stuk',
    });
  };

  const hasConsignorSelected = consignorPartyId && consignorPartyId.length > 0;

  return (
    <div className="flex flex-col items-start self-stretch gap-4">
      <LineItemSection
        title="Producten"
        onAddItem={handleAddProductLine}
        addButtonTitle="Voeg product toe"
        addButtonDisabledTitle={
          disabled
            ? 'Kan niet bewerken in deze status'
            : 'Selecteer eerst een opdrachtgever'
        }
        isAddDisabled={!hasConsignorSelected || disabled}
        emptyMessage="Selecteer eerst een opdrachtgever om producten toe te voegen"
        showEmptyMessage={!hasConsignorSelected}
      >
        {fields.length > 0 && (
          <div className="flex flex-col items-start self-stretch gap-4">
            {fields.map((field, index) => (
              <div
                key={field.id}
                className="flex flex-col items-start self-stretch gap-4 p-3 border border-color-border rounded-radius-sm"
              >
                <div className="flex justify-between items-center self-stretch">
                  <span className="text-body-2 font-medium">
                    Product {index + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => remove(index)}
                    disabled={disabled}
                    className="flex items-center justify-center w-6 h-6 rounded-radius-sm text-color-error hover:bg-color-error hover:text-color-on-error disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    title={
                      disabled
                        ? 'Kan niet bewerken in deze status'
                        : 'Verwijder product'
                    }
                  >
                    <TrashSimple className="w-4 h-4" />
                  </button>
                </div>

                <div className="flex items-start self-stretch gap-4">
                  <div className="flex-1">
                    <CatalogItemAsyncSelectFormField
                      title="Product"
                      placeholder="Zoek of selecteer een product"
                      consignorPartyId={consignorPartyId}
                      disabled={disabled}
                      testId={`product-select-${index}`}
                      index={index}
                      field={{
                        id: field.id,
                        catalogItemId: field.catalogItemId,
                        weightValue: '',
                        weightUnit: '',
                      }}
                      update={(idx, value, selectedItem) => {
                        update(idx, {
                          ...fields[idx],
                          catalogItemId: value.catalogItemId,
                          unit: selectedItem?.unitOfMeasure || fields[idx].unit,
                        });
                      }}
                      errors={errors}
                      typeFilter="PRODUCT"
                    />
                  </div>

                  <div className="w-32">
                    <NumberFormField
                      title="Aantal"
                      step={1}
                      placeholder="0"
                      disabled={disabled}
                      formHook={{
                        register,
                        name: `productLines.${index}.quantity` as const,
                        rules: {
                          validate: (value) => {
                            const productLines = formContext.getValues('productLines');
                            const catalogItemId = productLines[index]?.catalogItemId;
                            const numValue = typeof value === 'number' ? value : undefined;
                            if (numValue === undefined && !catalogItemId) {
                              return true;
                            }
                            if (numValue !== undefined && numValue < 0) {
                              return 'Voer een positief getal in';
                            }
                            return true;
                          },
                        },
                        errors,
                      }}
                    />
                  </div>

                  <div className="w-32">
                    <TextFormField
                      title="Eenheid"
                      placeholder="stuk"
                      disabled={disabled}
                      formHook={{
                        register,
                        name: `productLines.${index}.unit` as const,
                        errors,
                      }}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </LineItemSection>
    </div>
  );
});

WeightTicketProductsTab.displayName = 'WeightTicketProductsTab';
