import { FieldValues, useForm } from 'react-hook-form';
import { FormEvent } from 'react';
import { ErrorBoundary } from 'react-error-boundary';
import { TextFormField } from '@/components/ui/form/TextFormField.tsx';
import { WasteStream } from '@/api/services/wasteStreamService';
import { FormTopBar } from '@/components/ui/form/FormTopBar.tsx';
import { FormActionButtons } from '@/components/ui/form/FormActionButtons.tsx';
import { useErrorHandling } from '@/hooks/useErrorHandling.tsx';
import { fallbackRender } from '@/utils/fallbackRender';

interface WasteStreamFormProps {
  onCancel: () => void;
  onSubmit: (data: WasteStream) => void;
  wasteStream?: WasteStream;
}

export interface WasteStreamFormValues extends FieldValues {
  name: string;
  number: string;
}

function toWasteStream(data: WasteStreamFormValues) {
  return {
    name: data.name,
    number: data.number,
  } as WasteStream;
}

export const WasteStreamForm = ({
  onCancel,
  onSubmit,
  wasteStream,
}: WasteStreamFormProps) => {
  const { handleError, ErrorDialogComponent } = useErrorHandling();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<WasteStreamFormValues>();

  const submitForm = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    await handleSubmit(async (data) => {
      try {
        await onSubmit(toWasteStream(data));
        onCancel(); // Only close the form if submission was successful
      } catch (error) {
        handleError(error);
      }
    })();
  };

  const isEditing = !!wasteStream;
  const formTitle = isEditing
    ? 'Afvalstroomnummer bewerken'
    : 'Afvalstroomnummer toevoegen';

  return (
    <ErrorBoundary fallbackRender={fallbackRender}>
      <form
        className="flex flex-col items-center self-stretch"
        onSubmit={(e) => submitForm(e)}
      >
        <FormTopBar title={formTitle} onClick={onCancel} />
        <div className="flex flex-col items-center self-stretch p-4 gap-4">
          <TextFormField
            title={'Afvalstroomnummer'}
            placeholder={'Vul afvalstroomnummer in'}
            formHook={{
              register,
              name: 'number',
              rules: {
                required: 'Afvalstroomnummer is verplicht',
                pattern: {
                  value: /^.{12}$/,
                  message: 'Afvalstroomnummer moet uit 12 karakters bestaan',
                },
              },
              errors,
            }}
            value={wasteStream?.number}
          />
          <div className="flex items-start self-stretch gap-4">
            <TextFormField
              title={'Gebruikelijke benaming'}
              placeholder={'Vul een gebruikelijke benaming in'}
              formHook={{
                register,
                name: 'name',
                rules: {
                  required: 'Gebruikelijke benaming is verplicht',
                  validate: (value: string) => {
                    const trimmed = value?.trim() || '';
                    return (
                      trimmed !== '' ||
                      'Gebruikelijke benaming mag niet leeg zijn'
                    );
                  },
                },
                errors,
              }}
              value={wasteStream?.name}
            />
          </div>
        </div>
        <FormActionButtons onClick={onCancel} item={wasteStream} />
      </form>

      <ErrorDialogComponent />
    </ErrorBoundary>
  );
};
