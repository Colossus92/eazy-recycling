import { Button } from '@/components/ui/button/Button.tsx';
import { CheckboxField } from '@/components/ui/form/CheckboxField.tsx';
import { useForm, UseFormReturn } from 'react-hook-form';
import { toast } from 'react-toastify';
import { WasteStreamStatusTag, WasteStreamStatusTagProps } from './WasteStreamStatusTag';
import { useEffect } from 'react';

export interface WasteStreamFilterFormValues {
  isDraft: boolean;
  isActive: boolean;
  isInactive: boolean;
  isExpired: boolean;
}

interface StatusFilterOptionProps {
  formContext: UseFormReturn<WasteStreamFilterFormValues>;
  status: WasteStreamStatusTagProps['status'];
  fieldName: keyof WasteStreamFilterFormValues;
}

const StatusFilterOption = ({
  fieldName,
  formContext,
  status,
}: StatusFilterOptionProps) => {
  return (
    <div className="flex items-center self-stretch gap-2">
      <CheckboxField
        formHook={{
          register: formContext.register,
          name: fieldName,
          errors: formContext.formState.errors,
          control: formContext.control,
        }}
      >
        <WasteStreamStatusTag status={status}></WasteStreamStatusTag>
      </CheckboxField>
    </div>
  );
};

interface WasteStreamFilterFormProps {
  closeDialog: () => void;
  onSubmit: (values: WasteStreamFilterFormValues) => void;
  currentValues: WasteStreamFilterFormValues;
}

export const WasteStreamFilterForm = ({
  closeDialog,
  onSubmit,
  currentValues,
}: WasteStreamFilterFormProps) => {
  const formContext = useForm<WasteStreamFilterFormValues>({
    defaultValues: currentValues,
  });

  // Reset form with current values when they change (e.g., when drawer reopens)
  useEffect(() => {
    formContext.reset(currentValues);
  }, [currentValues, formContext]);

  const handleApplyFilter = formContext.handleSubmit((data) => {
    onSubmit(data);
    closeDialog();
    toast.success('Filters toegepast', {
      className: 'bg-color-status-success-light text-color-text-secondary',
      progressClassName: 'bg-color-status-success-primary',
    });
  });

  const handleReset = () => {
    formContext.reset({
      isDraft: false,
      isActive: false,
      isInactive: false,
      isExpired: false,
    });
    handleApplyFilter();
  };

  return (
    <>
      <div className="relative mt-6 flex-1 px-4 sm:px-6">
        <div className={'flex flex-col flex-1 items-start self-stretch gap-4'}>
          <div className="flex flex-col items-start self-stretch gap-2">
            <span className="text-caption-2">Status</span>
            <StatusFilterOption
              fieldName={'isDraft'}
              formContext={formContext}
              status={'DRAFT'}
            />
            <StatusFilterOption
              fieldName={'isActive'}
              formContext={formContext}
              status={'ACTIVE'}
            />
            <StatusFilterOption
              fieldName={'isInactive'}
              formContext={formContext}
              status={'INACTIVE'}
            />
            <StatusFilterOption
              fieldName={'isExpired'}
              formContext={formContext}
              status={'EXPIRED'}
            />
          </div>
        </div>
      </div>
      <div
        className={
          'flex justify-end items-center self-stretch py-3 px-4 gap-4 border-t border-solid border-color-border-primary'
        }
      >
        <div className={'flex-1'}>
          <Button
            variant={'secondary'}
            label={'Reset'}
            fullWidth={true}
            onClick={handleReset}
          />
        </div>
        <div className={'flex-1'}>
          <Button
            variant={'primary'}
            label={'Toepassen'}
            fullWidth={true}
            onClick={handleApplyFilter}
          />
        </div>
      </div>
    </>
  );
};
