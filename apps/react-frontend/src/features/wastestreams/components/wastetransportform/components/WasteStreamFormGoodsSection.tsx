import { Eural, ProcessingMethodDto as ProcessingMethod } from '@/api/client';
import { euralService } from '@/api/services/euralService';
import { materialService } from '@/api/services/materialService';
import { processingMethodService } from '@/api/services/processingMethodService';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { SelectFormField } from '@/components/ui/form/selectfield/SelectFormField.tsx';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook';
import { useQuery } from '@tanstack/react-query';
import { useFormContext, useWatch } from 'react-hook-form';
import { useEffect } from 'react';

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
  const { data: materials = [] } = useQuery({
    queryKey: ['materials'],
    queryFn: () => materialService.getAll(),
  });
  const euralOptions = eurals.map((eural) => ({
    value: eural.code,
    label: eural.code + ' - ' + eural.description,
  }));
  const processingMethodOptions = processingMethods.map((processingMethod) => ({
    value: processingMethod.code,
    label: processingMethod.code + ' - ' + processingMethod.description,
  }));
  const materialOptions = materials.map((material) => ({
    value: material.id.toString(),
    label: material.code + ' - ' + material.name,
  }));

  const selectedMaterialId = useWatch({
    control: formContext.control,
    name: 'catalogItemId',
  });

  useEffect(() => {
    if (selectedMaterialId) {
      const selectedMaterial = materials.find(
        (m) => m.id.toString() === selectedMaterialId
      );
      if (selectedMaterial) {
        formContext.setValue('goodsName', selectedMaterial.name);
      }
    }
  }, [selectedMaterialId, materials, formContext]);

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
          <SelectFormField
            title={'Materiaal'}
            placeholder={'Selecteer een materiaal (optioneel)'}
            options={materialOptions}
            testId="material-select"
            disabled={disabled}
            formHook={{
              register: formContext.register,
              name: 'catalogItemId',
              errors: formContext.formState.errors,
              control: formContext.control,
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
