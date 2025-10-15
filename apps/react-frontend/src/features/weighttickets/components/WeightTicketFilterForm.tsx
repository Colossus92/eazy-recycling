import { Button } from '@/components/ui/button/Button.tsx';
import { CheckboxField } from '@/components/ui/form/CheckboxField.tsx';
import { useForm, UseFormReturn } from 'react-hook-form';
import { toast } from 'react-toastify';
import { useEffect } from 'react';
import { WeightTicketStatusTag, WeightTicketStatusTagProps } from './WeightTicketStatusTag';

export interface WeightTicketFilterFormValues {
  isDraft: boolean;
  isCompleted: boolean;
  isInvoice: boolean;
  isCancelled: boolean;
}

interface StatusFilterOptionProps {
  formContext: UseFormReturn<WeightTicketFilterFormValues>;
  status: WeightTicketStatusTagProps['status'];
  fieldName: keyof WeightTicketFilterFormValues;
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
        <WeightTicketStatusTag status={status}></WeightTicketStatusTag>
      </CheckboxField>
    </div>
  );
};

interface WeightTicketFilterFormProps {
  closeDialog: () => void;
  onSubmit: (values: WeightTicketFilterFormValues) => void;
  currentValues: WeightTicketFilterFormValues;
}

export const WeightTicketFilterForm = ({
  closeDialog,
  onSubmit,
  currentValues,
}: WeightTicketFilterFormProps) => {
  const formContext = useForm<WeightTicketFilterFormValues>({
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
      isCompleted: false,
      isInvoice: false,
      isCancelled: false,
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
              fieldName={'isCompleted'}
              formContext={formContext}
              status={'COMPLETED'}
            />
            <StatusFilterOption
              fieldName={'isInvoice'}
              formContext={formContext}
              status={'INVOICED'}
            />
            <StatusFilterOption
              fieldName={'isCancelled'}
              formContext={formContext}
              status={'CANCELLED'}
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
