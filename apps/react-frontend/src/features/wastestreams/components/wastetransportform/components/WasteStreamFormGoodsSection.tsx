import { Eural, ProcessingMethodDto as ProcessingMethod } from '@/api/client';
import { euralService } from '@/api/services/euralService';
import { processingMethodService } from '@/api/services/processingMethodService';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook';
import { useQuery } from '@tanstack/react-query';
import { useFormContext } from 'react-hook-form';

interface WasteStreamFormGoodsSectionProps {
  disabled?: boolean;
}

export const WasteStreamFormGoodsSection = ({
  disabled = false,
}: WasteStreamFormGoodsSectionProps) => {
  const formContext = useFormContext<WasteStreamFormValues>();

  const { data: eurals = [] } = useQuery<Eural[]>({
    queryKey: ['eurals'],
    queryFn: () => euralService.getAll(),
  });
  const { data: processingMethods = [] } = useQuery<ProcessingMethod[]>({
    queryKey: ['processingMethods'],
    queryFn: () => processingMethodService.getAll(),
  });
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
      <CompanySelectFormField
        title={'Ontdoener'}
        placeholder={'Selecteer een ontdoener'}
        name={'pickupPartyId'}
        rules={{ required: 'Ontdoener is verplicht' }}
        disabled={disabled}
      />
      <div className="flex flex-col items-start self-stretch gap-4 p-4 bg-color-surface-secondary rounded-radius-md">
        <span className="text-subtitle-1">Afvalstof</span>
        <div className="flex items-start self-stretch gap-4 flex-grow">
          <TextFormField
            title={'Gebruikelijke benaming'}
            placeholder={'Gebruikelijke benaming afvalstof'}
            disabled={disabled}
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
            testId="eural-code-select"
            disabled={disabled}
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
            testId="processing-method-select"
            disabled={disabled}
            formHook={{
              register: formContext.register,
              name: 'processingMethodCode',
              rules: { required: 'Verwerkingsmethode is verplicht' },
              errors: formContext.formState.errors,
            }}
          />
        </div>
      </div>
    </div>
  );
};
