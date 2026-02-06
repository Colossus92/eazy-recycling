import Plus from '@/assets/icons/Plus.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import {
  FieldArrayWithId,
  useFieldArray,
  useFormContext,
  useWatch,
} from 'react-hook-form';
import { useEffect, useMemo, useRef } from 'react';
import { InvoiceFormValues, InvoiceLineFormValue } from './useInvoiceFormHook';
import { CatalogItemAsyncSelectFormField } from '@/components/ui/form/selectfield/CatalogItemAsyncSelectFormField';
import { Button } from '@/components/ui/button/Button';
import { CatalogItem } from '@/api/services/catalogService';

const parseNumber = (value: string | number | undefined): number => {
  if (value === undefined || value === null || value === '') return 0;
  if (typeof value === 'number') return value;
  const normalizedValue = String(value).replace(',', '.');
  const parsed = parseFloat(normalizedValue);
  return isNaN(parsed) ? 0 : parsed;
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('nl-NL', {
    style: 'currency',
    currency: 'EUR',
  }).format(value);
};

interface InvoiceLineRowProps {
  index: number;
  field: FieldArrayWithId<InvoiceFormValues, 'lines', 'id'>;
  update: (index: number, value: InvoiceLineFormValue & { id: string }) => void;
  onRemove: () => void;
  onCatalogItemSelected: (index: number, item: CatalogItem) => void;
  consignorPartyId?: string;
  isReadOnly?: boolean;
  isCreditNote?: boolean;
  invoiceType?: 'PURCHASE' | 'SALE';
}

const InvoiceLineRow = ({
  index,
  field,
  update,
  onRemove,
  onCatalogItemSelected,
  consignorPartyId,
  isReadOnly = false,
  isCreditNote = false,
  invoiceType,
}: InvoiceLineRowProps) => {
  const {
    register,
    watch,
    formState: { errors },
  } = useFormContext<InvoiceFormValues>();
  const line = watch(`lines.${index}`);
  const rawLineTotal =
    parseNumber(line?.quantity) * parseNumber(line?.unitPrice);
  const lineTotal =
    isCreditNote && rawLineTotal !== 0 ? -rawLineTotal : rawLineTotal;

  // Adapt field for CatalogItemAsyncSelectFormField (needs weightValue/weightUnit for compatibility)
  const catalogField = {
    id: field.id,
    catalogItemId: field.catalogItemId,
    wasteStreamNumber: undefined,
    weightValue: '',
    weightUnit: '',
  };

  const handleCatalogUpdate = (
    idx: number,
    value: {
      id: string;
      catalogItemId: string;
      wasteStreamNumber?: string;
      weightValue: string;
      weightUnit: string;
    },
    selectedItem?: CatalogItem
  ) => {
    // Preserve the database row id (line.id) when updating
    const currentLine = watch(`lines.${idx}`);
    update(idx, {
      ...field,
      id: currentLine?.id, // Preserve the database row id
      catalogItemId: value.catalogItemId,
    } as InvoiceLineFormValue & { id: string });
    if (selectedItem) {
      onCatalogItemSelected(idx, selectedItem);
    }
  };

  return (
    <tr className="border-t border-color-border">
      <td className="p-2">
        <CatalogItemAsyncSelectFormField
          title=""
          placeholder="Selecteer artikel"
          consignorPartyId={consignorPartyId}
          index={index}
          field={catalogField}
          update={handleCatalogUpdate}
          errors={errors}
          disabled={isReadOnly}
          invoiceType={invoiceType}
        />
      </td>
      <td className="p-2">
        <input
          type="number"
          step="0.01"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 text-right"
          placeholder="0"
          {...register(`lines.${index}.quantity`)}
          disabled={isReadOnly}
        />
      </td>
      <td className="p-2">
        <input
          type="text"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 bg-color-surface-secondary"
          disabled
          {...register(`lines.${index}.unitOfMeasure`)}
        />
      </td>
      <td className="p-2">
        <div className="relative">
          <span className="absolute left-2 top-1/2 -translate-y-1/2 text-body-2 text-color-text-secondary pointer-events-none">
            €
          </span>
          <input
            type="number"
            step="0.01"
            className="w-full pl-6 pr-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 text-right"
            placeholder="0,00"
            {...register(`lines.${index}.unitPrice`)}
            disabled={isReadOnly}
          />
        </div>
      </td>
      <td className="p-2">
        <input
          type="text"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 text-right bg-color-surface-secondary"
          disabled
          value={line?.isReverseCharge ? 'V' : line?.vatPercentage ? `${line.vatPercentage}%` : ''}
          readOnly
        />
      </td>
      <td className="p-2 text-right text-body-2">
        {formatCurrency(lineTotal)}
      </td>
      <td className="p-2">
        {!isReadOnly && (
          <Button
            variant="icon"
            size="small"
            icon={TrashSimple}
            showText={false}
            onClick={onRemove}
            type="button"
          />
        )}
      </td>
    </tr>
  );
};

interface InvoiceLinesSectionProps {
  isReadOnly?: boolean;
  isCreditNote?: boolean;
  invoiceType?: 'PURCHASE' | 'SALE';
}

export const InvoiceLinesSection = ({
  isReadOnly = false,
  isCreditNote = false,
  invoiceType,
}: InvoiceLinesSectionProps) => {
  const formContext = useFormContext<InvoiceFormValues>();
  const { control } = formContext;

  const { fields, append, remove, update } = useFieldArray({
    control,
    name: 'lines',
  });

  const lines = useWatch({ control, name: 'lines' });
  const customerId = useWatch({ control, name: 'customerId' });

  // Track previous customerId and invoiceType to detect changes
  const previousCustomerIdRef = useRef<string | undefined>(undefined);
  const previousInvoiceTypeRef = useRef<string | undefined>(undefined);
  const isInitialMount = useRef(true);

  // When customerId changes, reset invoice lines
  useEffect(() => {
    // Skip on initial mount to preserve loaded data
    if (isInitialMount.current) {
      isInitialMount.current = false;
      previousCustomerIdRef.current = customerId;
      previousInvoiceTypeRef.current = invoiceType;
      return;
    }

    // Only reset if customer actually changed from a previous non-empty value
    if (
      previousCustomerIdRef.current !== undefined &&
      previousCustomerIdRef.current !== '' &&
      previousCustomerIdRef.current !== customerId &&
      fields.length > 0
    ) {
      // Remove all existing lines
      for (let i = fields.length - 1; i >= 0; i--) {
        remove(i);
      }

      // Add one empty line
      append({
        catalogItemId: '',
        catalogItemName: '',
        date: '',
        description: '',
        quantity: '1',
        unitPrice: '0,00',
        unitOfMeasure: '',
        vatCode: '',
        vatPercentage: '21',
        isReverseCharge: false,
        orderReference: '',
      });
    }

    previousCustomerIdRef.current = customerId;
  }, [customerId, fields, formContext, remove, append]);

  // When invoiceType changes, negate only PRODUCT line prices
  useEffect(() => {
    // Skip if no previous value or same value
    if (
      previousInvoiceTypeRef.current === undefined ||
      previousInvoiceTypeRef.current === invoiceType
    ) {
      previousInvoiceTypeRef.current = invoiceType;
      return;
    }

    // Negate only PRODUCT line prices when switching between PURCHASE and SALE
    const currentLines = formContext.getValues('lines');
    currentLines.forEach((line, index) => {
      // Only negate prices for PRODUCT type items
      if (line.catalogItemType === 'PRODUCT') {
        const currentPrice = parseNumber(line.unitPrice);
        if (currentPrice !== 0) {
          formContext.setValue(
            `lines.${index}.unitPrice`,
            String(-currentPrice)
          );
        }
      }
    });

    previousInvoiceTypeRef.current = invoiceType;
  }, [invoiceType, formContext]);

  const totals = useMemo(() => {
    let totalExclVat = 0;
    let totalVat = 0;
    const vatBreakdown = new Map<string, { label: string; amount: number }>();

    lines?.forEach((line: InvoiceLineFormValue) => {
      const qty = parseNumber(line.quantity);
      const price = parseNumber(line.unitPrice);
      const lineTotal = qty * price;
      const vatPct = parseNumber(line.vatPercentage);
      const isReverseCharge = line.isReverseCharge ?? false;
      const vatAmount = isReverseCharge ? 0 : lineTotal * (vatPct / 100);

      totalExclVat += lineTotal;
      totalVat += vatAmount;

      // Group by vatCode for breakdown
      if (isReverseCharge) {
        const key = 'VERLEGD';
        const existing = vatBreakdown.get(key);
        vatBreakdown.set(key, {
          label: 'Btw verlegd',
          amount: (existing?.amount ?? 0),
        });
      } else if (vatPct > 0) {
        const key = String(vatPct);
        const existing = vatBreakdown.get(key);
        vatBreakdown.set(key, {
          label: `${vatPct}% btw`,
          amount: (existing?.amount ?? 0) + vatAmount,
        });
      }
    });

    // For credit notes, display totals as negative (but don't store them as negative)
    const multiplier = isCreditNote ? -1 : 1;
    return {
      totalExclVat: totalExclVat * multiplier,
      totalVat: totalVat * multiplier,
      totalInclVat: (totalExclVat + totalVat) * multiplier,
      vatBreakdown: Array.from(vatBreakdown.values()).map(v => ({
        label: v.label,
        amount: v.amount * multiplier,
      })),
    };
  }, [lines, isCreditNote]);

  const handleAddLine = () => {
    append({
      catalogItemId: '',
      catalogItemName: '',
      date: '',
      description: '',
      quantity: '1',
      unitPrice: '0,00',
      unitOfMeasure: '',
      vatCode: '',
      vatPercentage: '21',
      isReverseCharge: false,
      orderReference: '',
    });
  };

  const handleCatalogItemSelected = (index: number, item: CatalogItem) => {
    const currentLine = formContext.getValues(`lines.${index}`);
    formContext.setValue(`lines.${index}`, {
      ...currentLine,
      catalogItemName: item.name,
      catalogItemType: item.itemType as 'MATERIAL' | 'PRODUCT' | 'WASTE_STREAM',
      unitOfMeasure: item.unitOfMeasure || '',
      unitPrice: String(item.defaultPrice ?? 0),
      vatCode: item.vatCode || '',
      vatPercentage: String(item.vatPercentage ?? 21),
      isReverseCharge: item.isReverseCharge ?? false,
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-between items-center">
        <span className="text-subtitle-1">Factuurregels</span>
        {!isReadOnly && (
          <Button
            variant="icon"
            showText={false}
            icon={Plus}
            onClick={handleAddLine}
            title="Voeg regel toe"
            type="button"
          />
        )}
      </div>

      {fields.length === 0 ? (
        <div className="p-4 bg-color-surface-secondary rounded-radius-sm text-center text-color-text-secondary">
          Geen factuurregels. Klik op + om een regel toe te voegen.
        </div>
      ) : (
        <div className="border border-color-border rounded-radius-sm overflow-hidden">
          <table className="w-full">
            <thead className="bg-color-surface-secondary">
              <tr className="text-caption-2 text-left">
                <th className="p-2 w-[30%]">Artikel</th>
                <th className="p-2 w-[10%] text-right">Aantal</th>
                <th className="p-2 w-[10%]">Eenheid</th>
                <th className="p-2 w-[12%] text-right">Prijs</th>
                <th className="p-2 w-[10%] text-right">BTW</th>
                <th className="p-2 w-[15%] text-right">Totaal</th>
                <th className="p-2 w-[8%]"></th>
              </tr>
            </thead>
            <tbody>
              {fields.map((field, index) => (
                <InvoiceLineRow
                  key={field.id}
                  index={index}
                  field={field}
                  update={update}
                  onRemove={() => {
                    if (fields.length > 1) {
                      remove(index);
                    }
                  }}
                  onCatalogItemSelected={handleCatalogItemSelected}
                  consignorPartyId={customerId}
                  isReadOnly={isReadOnly}
                  isCreditNote={isCreditNote}
                  invoiceType={invoiceType}
                />
              ))}
            </tbody>
            <tfoot className="bg-color-surface-secondary border-t border-color-border">
              <tr className="text-body-2">
                <td colSpan={5} className="p-2 text-right">
                  Subtotaal:
                </td>
                <td className="p-2 text-right">
                  {formatCurrency(totals.totalExclVat)}
                </td>
                <td></td>
              </tr>
              {totals.vatBreakdown.map((vat, i) => (
                <tr key={i} className="text-body-2">
                  <td colSpan={5} className="p-2 text-right">
                    {vat.label}:
                  </td>
                  <td className="p-2 text-right">
                    {vat.label === 'Btw verlegd' ? '-' : formatCurrency(vat.amount)}
                  </td>
                  <td></td>
                </tr>
              ))}
              <tr className="text-subtitle-2">
                <td colSpan={5} className="p-2 text-right">
                  Totaal:
                </td>
                <td className="p-2 text-right">
                  {formatCurrency(totals.totalInclVat)}
                </td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  );
};
