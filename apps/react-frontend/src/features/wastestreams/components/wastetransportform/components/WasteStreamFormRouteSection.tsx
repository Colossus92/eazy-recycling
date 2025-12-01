import { useFormContext } from 'react-hook-form';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook.ts';
import { ConsignorClassificationSelect } from '@/features/wastestreams/components/wastetransportform/components/ConsignorClassificationSelect';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { ProcessorPartySelectFormField } from '@/components/ui/form/ProcessorPartySelectFormField';

interface WasteStreamFormRouteSectionProps {
  disabled?: boolean;
}

export const WasteStreamFormRouteSection = ({
  disabled = false,
}: WasteStreamFormRouteSectionProps) => {
  const formContext = useFormContext<WasteStreamFormValues>();

  return (
    <div className={'flex flex-col items-start self-stretch gap-4'}>
      <CompanySelectFormField
        title={'Afzender'}
        placeholder={'Selecteer een afzender'}
        name="consignorPartyId"
        rules={{ required: 'Afzender is verplicht' }}
        disabled={disabled}
      />
      <ConsignorClassificationSelect
        formHook={{
          name: 'consignorClassification',
          rules: { required: 'Afzender type is verplicht' },
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
        disabled={disabled}
      />
      <AddressFormField
        control={formContext.control}
        name="pickupLocation"
        label="Locatie van herkomst"
        entity="afvalstroomnummer"
        disabled={disabled}
      />
      <ProcessorPartySelectFormField
        name="processorPartyId"
        rules={{ required: 'Verwerker is verplicht' }}
        disabled={disabled}
      />
    </div>
  );
};
