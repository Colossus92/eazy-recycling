import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { Truck } from '@/api/client/models/truck.ts';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { fallbackRender } from '@/utils/fallbackRender';

interface TruckFormProps {
  onCancel: () => void;
  onSubmit: (data: Truck) => void;
  truck?: Truck;
}

export interface TruckFormValues extends FieldValues {
  brand: string;
  model: string;
  licensePlate: string;
}

function toTruck(data: TruckFormValues) {
  return {
    brand: data.brand,
    model: data.model,
    licensePlate: data.licensePlate,
  } as Truck;
}

export const TruckForm = ({ onCancel, onSubmit, truck }: TruckFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TruckFormValues>();

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit(toTruck(data));
        onCancel(); // Only close the form if submission was successful
      } catch (error) {
        handleError(error);
      }
    })();
  };

  const isEditing = !!truck;
  const formTitle = isEditing
    ? 'Vrachtwagen bewerken'
    : 'Vrachtwagen toevoegen';

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form
        className="flex flex-col items-center self-stretch"
        onSubmit={(e) => submitForm(e)}
      >
        <FormTopBar title={formTitle} onClick={onCancel} />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <TextFormField
            title={'Merk'}
            placeholder={'Vul merk in'}
            formHook={{
              register,
              name: 'brand',
              rules: { required: 'Merk is verplicht' },
              errors,
            }}
            value={truck?.brand}
          />
          <div className="flex items-start self-stretch gap-4">
            <TextFormField
              title={'Beschrijving'}
              placeholder={'Vul een beschrijving in'}
              formHook={{
                register,
                name: 'model',
                rules: { required: 'Beschrijving is verplicht' },
                errors,
              }}
              value={truck?.model}
            />
            <TextFormField
              title={'Kenteken'}
              placeholder={'Vul kenteken in'}
              formHook={{
                register,
                name: 'licensePlate',
                rules: { required: 'Kenteken is verplicht' },
                errors,
              }}
              value={truck?.licensePlate}
              disabled={isEditing}
            />
          </div>
        </div>
        <FormActionButtons onClick={onCancel} item={truck} />
      </form>

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
