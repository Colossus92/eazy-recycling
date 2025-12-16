import Plus from '@/assets/icons/Plus.svg?react';
import TrashSimple from '@/assets/icons/TrashSimple.svg?react';
import { useFieldArray, useFormContext, useWatch } from 'react-hook-form';
import { InvoiceFormValues, InvoiceLineFormValue } from './useInvoiceFormHook';
import { catalogService } from '@/api/services/catalogService';
import { useMemo, useState, useCallback } from 'react';
import AsyncSelect from 'react-select/async';

interface CatalogOption {
  value: string;
  label: string;
  name: string;
}

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

const InvoiceLineRow = ({
  index,
  onRemove,
}: {
  index: number;
  onRemove: () => void;
}) => {
  const { register, setValue, watch } = useFormContext<InvoiceFormValues>();
  const line = watch(`lines.${index}`);
  const lineTotal = parseNumber(line?.quantity) * parseNumber(line?.unitPrice);

  const [selectedOption, setSelectedOption] = useState<CatalogOption | null>(
    line?.catalogItemId
      ? { value: line.catalogItemId, label: line.catalogItemName, name: line.catalogItemName }
      : null
  );

  const loadOptions = useCallback(async (inputValue: string): Promise<CatalogOption[]> => {
    const items = await catalogService.search(inputValue || undefined, undefined);
    return items.map((item) => ({
      value: String(item.id),
      label: item.wasteStreamNumber ? `${item.name} (${item.wasteStreamNumber})` : item.name,
      name: item.name,
    }));
  }, []);

  return (
    <tr className="border-t border-color-border">
      <td className="p-2">
        <AsyncSelect<CatalogOption>
          value={selectedOption}
          loadOptions={loadOptions}
          defaultOptions
          cacheOptions
          placeholder="Selecteer artikel"
          isClearable
          classNamePrefix="react-select"
          noOptionsMessage={() => 'Geen items gevonden'}
          loadingMessage={() => 'Laden...'}
          menuPortalTarget={document.body}
          styles={{
            control: (base) => ({
              ...base,
              minHeight: '32px',
              height: '32px',
              borderRadius: '4px',
              fontSize: '14px',
            }),
            valueContainer: (base) => ({
              ...base,
              padding: '0 8px',
            }),
            input: (base) => ({
              ...base,
              margin: 0,
              padding: 0,
            }),
            menuPortal: (base) => ({
              ...base,
              zIndex: 9999,
            }),
          }}
          onChange={(option) => {
            setSelectedOption(option);
            setValue(`lines.${index}.catalogItemId`, option?.value || '');
            setValue(`lines.${index}.catalogItemName`, option?.name || '');
          }}
        />
      </td>
      <td className="p-2">
        <input
          type="text"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2"
          placeholder="Omschrijving"
          {...register(`lines.${index}.description`)}
        />
      </td>
      <td className="p-2">
        <input
          type="number"
          step="0.01"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 text-right"
          placeholder="0"
          {...register(`lines.${index}.quantity`)}
        />
      </td>
      <td className="p-2">
        <input
          type="number"
          step="0.01"
          className="w-full px-2 py-1.5 border border-color-border rounded-radius-sm text-body-2 text-right"
          placeholder="0.00"
          {...register(`lines.${index}.unitPrice`)}
        />
      </td>
      <td className="p-2 text-right text-body-2">
        {formatCurrency(lineTotal)}
      </td>
      <td className="p-2">
        <button
          type="button"
          onClick={onRemove}
          className="flex items-center justify-center w-6 h-6 rounded-radius-sm text-color-error hover:bg-color-error hover:text-color-on-error transition-colors"
          title="Verwijder regel"
        >
          <TrashSimple className="w-4 h-4" />
        </button>
      </td>
    </tr>
  );
};

export const InvoiceLinesSection = () => {
  const formContext = useFormContext<InvoiceFormValues>();
  const { control } = formContext;

  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lines',
  });

  const lines = useWatch({ control, name: 'lines' });

  const totals = useMemo(() => {
    let totalExclVat = 0;
    lines?.forEach((line: InvoiceLineFormValue) => {
      const qty = parseNumber(line.quantity);
      const price = parseNumber(line.unitPrice);
      totalExclVat += qty * price;
    });
    return { totalExclVat };
  }, [lines]);

  const handleAddLine = () => {
    append({
      catalogItemId: '',
      catalogItemName: '',
      date: '',
      description: '',
      quantity: '1',
      unitPrice: '0',
      orderReference: '',
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex justify-between items-center">
        <span className="text-subtitle-1">Factuurregels</span>
        <button
          type="button"
          onClick={handleAddLine}
          className="flex items-center justify-center w-8 h-8 rounded-radius-sm bg-color-primary text-color-on-primary hover:bg-color-primary-hover transition-colors"
          title="Voeg regel toe"
        >
          <Plus className="w-5 h-5" />
        </button>
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
                <th className="p-2 w-[25%]">Omschrijving</th>
                <th className="p-2 w-[12%] text-right">Aantal</th>
                <th className="p-2 w-[15%] text-right">Prijs</th>
                <th className="p-2 w-[13%] text-right">Totaal</th>
                <th className="p-2 w-[5%]"></th>
              </tr>
            </thead>
            <tbody>
              {fields.map((field, index) => (
                <InvoiceLineRow
                  key={field.id}
                  index={index}
                  onRemove={() => remove(index)}
                />
              ))}
            </tbody>
            <tfoot className="bg-color-surface-secondary border-t border-color-border">
              <tr className="text-subtitle-2">
                <td colSpan={4} className="p-2 text-right">Subtotaal excl. BTW:</td>
                <td className="p-2 text-right">{formatCurrency(totals.totalExclVat)}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      )}
    </div>
  );
};
