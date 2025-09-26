import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { WasteTransportFormValues } from '@/features/planning/hooks/useWasteTransportForm.ts';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { companyService, Company } from '@/api/services/companyService.ts';
import { NumberFormField } from '@/components/ui/form/NumberFormField.tsx';
import { processingMethodService } from '@/api/services/processingMethodService';
import { ProcessingMethod } from '@/api/client';
import { wasteStreamService, WasteStream } from '@/api/services/wasteStreamService';
import { ComboboxFormField } from '@/components/ui/form/comboboxfield/ComboboxFormField';
import { euralService } from '@/api/services/euralService';
import { Eural } from '@/api/client';

export const WasteTransportFormGoodsSection = () => {
  const formContext = useFormContext<WasteTransportFormValues>();
  const [isGoodsNameDisabled, setIsGoodsNameDisabled] = useState(false);

  const { data: companies = [] } = useQuery<Company[]>({
    queryKey: ['companies'],
    queryFn: () => companyService.getAll(),
  });
  const { data: eurals = [] } = useQuery<Eural[]>({
    queryKey: ['eurals'],
    queryFn: () => euralService.getAll(),
  });
  const { data: processingMethods = [] } = useQuery<ProcessingMethod[]>({
    queryKey: ['processingMethods'],
    queryFn: () => processingMethodService.getAll(),
  });
  const { data: wasteStreams = [] } = useQuery<WasteStream[]>({
    queryKey: ['wasteStreams'],
    queryFn: () => wasteStreamService.getAll(),
  });

  // Watch for changes in wasteStreamNumber and update goodsName accordingly
  useEffect(() => {
    const subscription = formContext.watch((value, { name }) => {
      if (name === 'wasteStreamNumber' && value.wasteStreamNumber) {
        const selectedWasteStream = wasteStreams.find(
          (ws) => ws.number === value.wasteStreamNumber
        );
        if (selectedWasteStream) {
          formContext.setValue('goodsName', selectedWasteStream.name);
          setIsGoodsNameDisabled(true);
        }
      } else if (name === 'wasteStreamNumber' && !value.wasteStreamNumber) {
        // If wasteStreamNumber is cleared, enable goodsName field again
        setIsGoodsNameDisabled(false);
      }
    });

    // Check initial value
    const currentValue = formContext.getValues('wasteStreamNumber');
    if (currentValue) {
      const selectedWasteStream = wasteStreams.find(
        (ws) => ws.number === currentValue
      );
      if (selectedWasteStream) {
        formContext.setValue('goodsName', selectedWasteStream.name);
        setIsGoodsNameDisabled(true);
      }
    }

    return () => subscription.unsubscribe();
  }, [formContext, wasteStreams]);

  const companyOptions = companies.map((company) => ({
    value: company.id || '',
    label: company.name,
  }));
  const euralOptions = eurals.map((eural) => ({
    value: eural.code,
    label: eural.code + ' - ' + eural.description,
  }));
  const processingMethodOptions = processingMethods.map((processingMethod) => ({
    value: processingMethod.code,
    label: processingMethod.code + ' - ' + processingMethod.description,
  }));
  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <SelectFormField
        title={'Ontdoener'}
        placeholder={'Selecteer een ontdoener'}
        options={companyOptions}
        testId='pickup-party-select'
        formHook={{
          register: formContext.register,
          name: 'pickupPartyId',
          rules: { required: 'Ontdoener is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      />
      <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
        <span className="text-subtitle-1">Afvalstoffen</span>

        <div className="flex-grow-0 flex flex-col items-start self-stretch gap-4">
          <ComboboxFormField
            title="Afvalstroomnummer"
            placeholder="Selecteer een afvalstroomnummer"
            items={wasteStreams.map((ws) => ({
              id: ws.number,
              label: `${ws.number} - ${ws.name}`,
              displayValue: ws.number,
            }))}
            testId='waste-stream-number-combobox'
            formHook={{
              register: formContext.register,
              name: 'wasteStreamNumber',
              errors: formContext.formState.errors,
              control: formContext.control,
            }}
            getFilteredItems={(query, items) => {
              return query === ''
                ? items
                : items.filter((item) =>
                    item.label.toLowerCase().includes(query.toLowerCase())
                  );
            }}
          />
        </div>

        <div className="flex items-start self-stretch gap-4 flex-grow">
          <TextFormField
            title={'Gebruikelijke benaming'}
            placeholder={'Gebruikelijke benaming afvalstof'}
            disabled={isGoodsNameDisabled}
            formHook={{
              register: formContext.register,
              name: 'goodsName',
              rules: { required: 'Naam is verplicht' },
              errors: formContext.formState.errors,
            }}
          />
        </div>

        <div className="flex items-start self-stretch gap-4">
          <SelectFormField
            title={'Euralcode'}
            placeholder={'Selecteer een euralcode'}
            options={euralOptions}
            testId='eural-code-select'
            formHook={{
              register: formContext.register,
              name: 'euralCode',
              rules: { required: 'Euralcode is verplicht' },
              errors: formContext.formState.errors,
            }}
          />
        </div>

        <div className="flex items-start self-stretch gap-4">
          <SelectFormField
            title={'Verwerkingsmethode'}
            placeholder={'Selecteer een verwerkingsmethode'}
            options={processingMethodOptions}
            testId='processing-method-select'
            formHook={{
              register: formContext.register,
              name: 'processingMethodCode',
              rules: { required: 'Verwerkingsmethode is verplicht' },
              errors: formContext.formState.errors,
            }}
          />
        </div>

        <div className="flex items-start self-stretch gap-4">
          <NumberFormField
            title={'Geschatte gewicht (kg)'}
            placeholder={'Vul het geschatte gewicht in'}
            step={'any'}
            formHook={{
              register: formContext.register,
              name: 'weight',
              rules: {
                required: 'Gewicht is verplicht',
                min: { value: 0.1, message: 'Gewicht is minimaal 0.1' },
              },
              errors: formContext.formState.errors,
            }}
          />
          <NumberFormField
            title={'Aantal verpakkingen'}
            placeholder={'Vul het aantal verpakkingen in'}
            step={1}
            value={'1'}
            formHook={{
              register: formContext.register,
              name: 'quantity',
              rules: {
                required: 'Aantal verpakkingen is verplicht',
                min: { value: 1, message: 'Aantal verpakkingen is minimaal 1' },
              },
              errors: formContext.formState.errors,
            }}
          />
        </div>
      </div>
    </div>
  );
};
