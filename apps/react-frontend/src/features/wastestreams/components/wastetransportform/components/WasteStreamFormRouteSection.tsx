import { useFormContext, useWatch } from 'react-hook-form';
import { WasteStreamFormValues } from '@/features/wastestreams/components/wastetransportform/hooks/useWasteStreamFormHook.ts';
import { ConsignorClassificationSelect } from '@/features/wastestreams/components/wastetransportform/components/ConsignorClassificationSelect';
import { AddressFormField } from '@/components/ui/form/addressformfield/AddressFormField';
import { CompanySelectFormField } from '@/components/ui/form/CompanySelectFormField';
import { ProcessorPartySelectFormField } from '@/components/ui/form/ProcessorPartySelectFormField';
import { TextFormField } from '@/components/ui/form/TextFormField';
import { isCurrentTenant } from '@/config/tenant';

interface WasteStreamFormRouteSectionProps {
  disabled?: boolean;
}

export const WasteStreamFormRouteSection = ({
  disabled = false,
}: WasteStreamFormRouteSectionProps) => {
  const formContext = useFormContext<WasteStreamFormValues>();

  // Watch the processorPartyId to conditionally show wasteStreamNumber field
  const processorPartyId = useWatch({
    control: formContext.control,
    name: 'processorPartyId',
  });

  // Show wasteStreamNumber field when processor is not the current tenant
  const showWasteStreamNumberField =
    processorPartyId && !isCurrentTenant(processorPartyId);

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
      {showWasteStreamNumberField && (
        <TextFormField<WasteStreamFormValues>
          title="Afvalstroomnummer"
          placeholder="Bijv. 123450000001"
          formHook={{
            name: 'wasteStreamNumber',
            register: formContext.register,
            rules: {
              required:
                'Afvalstroomnummer is verplicht voor externe verwerkers',
            },
            errors: formContext.formState.errors,
          }}
          disabled={disabled}
          testId="waste-stream-number-input"
        />
      )}
    </div>
  );
};
