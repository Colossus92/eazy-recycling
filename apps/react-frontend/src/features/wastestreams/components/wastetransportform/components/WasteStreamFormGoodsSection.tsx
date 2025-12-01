import { useFormContext } from 'react-hook-form';
import { useQuery } from '@tanstack/react-query';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { companyService, Company } from '@/api/services/companyService.ts';
import { processingMethodService } from '@/api/services/processingMethodService';
import { ProcessingMethodDto as ProcessingMethod } from '@/api/client';
import { euralService } from '@/api/services/euralService';
import { Eural } from '@/api/client';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook';

interface WasteStreamFormGoodsSectionProps {
  disabled?: boolean;
}

export const WasteStreamFormGoodsSection = ({
  disabled = false,
}: WasteStreamFormGoodsSectionProps) => {
  const formContext = useFormContext<WasteStreamFormValues>();

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
        disabled={disabled}
        formHook={{
          register: formContext.register,
          name: 'pickupPartyId',
          rules: { required: 'Ontdoener is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
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
            testId='eural-code-select'
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
            testId='processing-method-select'
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
